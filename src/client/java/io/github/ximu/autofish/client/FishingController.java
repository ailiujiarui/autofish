package io.github.ximu.autofish.client;

import io.github.ximu.autofish.client.config.AutoFishConfig;
import io.github.ximu.autofish.client.config.ConfigManager;
import io.github.ximu.autofish.client.monitor.FishMonitor;
import io.github.ximu.autofish.client.monitor.MotionFishMonitor;
import io.github.ximu.autofish.client.monitor.SoundFishMonitor;
import io.github.ximu.autofish.client.scheduler.ActionScheduler;
import io.github.ximu.autofish.client.scheduler.ActionType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class FishingController {
    private static final long PERSISTENT_CAST_INTERVAL_MS = 10_000;
    private static final long RECENT_HOOK_WINDOW_MS = 2_000;
    private static final TagKey<Item> COMMON_FISHING_RODS = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath("c", "tools/fishing_rod"));

    public enum Status {
        OFF("status.autofish.off"),
        READY("status.autofish.ready"),
        CASTING("status.autofish.casting"),
        WAITING_BITE("status.autofish.waiting_bite"),
        REELING("status.autofish.reeling"),
        DELAY("status.autofish.delay"),
        PAUSED("status.autofish.paused");

        private final String translationKey;

        Status(String translationKey) {
            this.translationKey = translationKey;
        }

        Component text() {
            return Component.translatable(translationKey);
        }
    }

    private Minecraft client;
    private final ConfigManager configManager;
    private final ActionScheduler scheduler = new ActionScheduler();

    private FishMonitor fishMonitor = new MotionFishMonitor();
    private boolean soundDetection;
    private boolean hookExists;
    private long hookRemovedAt;
    private long lastPersistentCastAt;
    private boolean worldPresent;
    private Status status = Status.OFF;
    private String pauseReason = "";

    public FishingController(ConfigManager configManager) {
        this.configManager = configManager;
        updateDetection();
    }

    public Component statusText() {
        if (status == Status.PAUSED && !pauseReason.isEmpty()) {
            return status.text().copy().append(": ").append(Component.translatable(pauseReason));
        }
        return status.text();
    }

    public void tick(Minecraft minecraft) {
        client = minecraft;
        AutoFishConfig config = configManager.get();
        if (!config.enabled) {
            reset();
            return;
        }
        if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            scheduler.clear();
            removeHook();
            worldPresent = false;
            pause("reason.autofish.not_in_world");
            return;
        }

        if (!worldPresent) {
            worldPresent = true;
            scheduler.clear();
            fishMonitor.handleHookRemoved();
            lastPersistentCastAt = Util.getMillis();
        }

        String reason = unavailableReason(config);
        if (reason != null) {
            if ("reason.autofish.no_rod".equals(reason) || "reason.autofish.not_alive".equals(reason)) {
                scheduler.clear();
                removeHook();
            }
            pause(reason);
            return;
        }

        if (soundDetection != config.useSoundDetection) {
            updateDetection();
        }

        pauseReason = "";
        Player player = minecraft.player;
        FishingHook hook = player.fishing;
        if (hook != null && hook.isAlive() && isHoldingFishingRod()) {
            hookExists = true;
            status = scheduler.hasQueued(ActionType.RECAST) ? Status.DELAY : Status.WAITING_BITE;
            if (shouldUseMultiplayerDetection()) {
                fishMonitor.hookTick(this, minecraft, hook);
            }
        } else {
            removeHook();
            status = scheduler.hasQueued(ActionType.RECAST) ? Status.DELAY : Status.READY;
        }

        long now = Util.getMillis();
        if (config.persistentMode && !hookExists && !scheduler.hasQueued(ActionType.RECAST)
                && now - lastPersistentCastAt >= PERSISTENT_CAST_INTERVAL_MS) {
            useRod();
            lastPersistentCastAt = now;
            status = Status.CASTING;
        }

        scheduler.tick();
    }

    public void handlePacket(Packet<?> packet) {
        if (client == null || !configManager.get().enabled || !shouldUseMultiplayerDetection()) {
            return;
        }
        fishMonitor.handlePacket(this, packet, client);
    }

    public void handleSystemChat(ClientboundSystemChatPacket packet) {
        AutoFishConfig config = configManager.get();
        if (client == null || !config.enabled || client.isSingleplayer() || !isHoldingFishingRod()
                || (!hookExists && Util.getMillis() - hookRemovedAt >= RECENT_HOOK_WINDOW_MS)
                || config.clearLagRegex.isBlank()) {
            return;
        }

        try {
            if (Pattern.compile(config.clearLagRegex, Pattern.CASE_INSENSITIVE).matcher(packet.content().getString()).find()) {
                queueRecast();
            }
        } catch (PatternSyntaxException ignored) {
            // Invalid user regex disables only ClearLag detection.
        }
    }

    public void tickFishingLogic(Entity owner, int nibble) {
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (!configManager.get().enabled || shouldUseMultiplayerDetection() || nibble <= 0
                    || owner == null || client.player == null || client.player.fishing == null
                    || !owner.getUUID().equals(client.player.getUUID())) {
                return;
            }
            catchFish();
        });
    }

    public void catchFish() {
        if (client == null || scheduler.hasQueued(ActionType.RECAST) || client.player == null || client.player.fishing == null
                || unavailableReason(configManager.get()) != null) {
            return;
        }

        status = Status.REELING;
        queueRodSwitch();
        queueRecast();
        useRod();
    }

    public void updateDetection() {
        soundDetection = configManager.get().useSoundDetection;
        fishMonitor = soundDetection ? new SoundFishMonitor() : new MotionFishMonitor();
    }

    public void reset() {
        scheduler.clear();
        hookExists = false;
        hookRemovedAt = 0;
        lastPersistentCastAt = Util.getMillis();
        fishMonitor.handleHookRemoved();
        status = Status.OFF;
        pauseReason = "";
    }

    public boolean shouldKeepWorldRunning() {
        AutoFishConfig config = configManager.get();
        return client != null && config.enabled && config.runWhilePaused
                && client.player != null && client.level != null && client.player.isAlive()
                && isHoldingFishingRod()
                && (client.player.fishing != null || scheduler.hasQueued(ActionType.RECAST) || config.persistentMode);
    }

    private void queueRecast() {
        if (scheduler.hasQueued(ActionType.RECAST)) {
            return;
        }
        scheduler.schedule(ActionType.RECAST, configManager.get().recastDelayMs, () -> {
            AutoFishConfig config = configManager.get();
            if (client.player == null || client.player.fishing != null || unavailableReason(config) != null
                    || (config.noBreak && wouldBreak(getHeldItem()))) {
                return;
            }
            useRod();
            status = Status.CASTING;
        });
    }

    private void queueRodSwitch() {
        long delay = configManager.get().recastDelayMs - 250L;
        scheduler.schedule(ActionType.ROD_SWITCH, delay, () -> {
            if (configManager.get().multiRod && client.player != null) {
                switchToFirstUsableRod(client.player);
            }
        });
    }

    private void switchToFirstUsableRod(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isFishingRod(stack) && (!configManager.get().noBreak || !wouldBreak(stack))) {
                inventory.setSelectedSlot(slot);
                return;
            }
        }
    }

    private void removeHook() {
        if (hookExists) {
            hookExists = false;
            hookRemovedAt = Util.getMillis();
            fishMonitor.handleHookRemoved();
        }
    }

    private void useRod() {
        if (client.player == null || client.level == null || client.gameMode == null || !isHoldingFishingRod()) {
            return;
        }
        InteractionHand hand = getCorrectHand();
        if (client.gameMode.useItem(client.player, hand).consumesAction()) {
            client.player.swing(hand);
        }
    }

    private String unavailableReason(AutoFishConfig config) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return "reason.autofish.not_in_world";
        }
        if (!client.player.isAlive()) {
            return "reason.autofish.not_alive";
        }
        if (client.screen != null && !(config.runWhilePaused && client.screen instanceof PauseScreen)) {
            return "reason.autofish.screen_open";
        }
        if (!client.isWindowActive()) {
            return "reason.autofish.unfocused";
        }
        if (config.requireSneaking && !client.player.isShiftKeyDown()) {
            return "reason.autofish.sneak_required";
        }
        if (!isHoldingFishingRod()) {
            return "reason.autofish.no_rod";
        }
        if (client.player.getDeltaMovement().horizontalDistanceSqr() > 0.0025) {
            return "reason.autofish.moving";
        }
        return null;
    }

    private boolean shouldUseMultiplayerDetection() {
        return configManager.get().forceMultiplayerDetection || !client.isSingleplayer();
    }

    private boolean isHoldingFishingRod() {
        return client.player != null && isFishingRod(getHeldItem());
    }

    private InteractionHand getCorrectHand() {
        if (!configManager.get().multiRod && isFishingRod(client.player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return InteractionHand.MAIN_HAND;
    }

    private ItemStack getHeldItem() {
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        if (!configManager.get().multiRod && isFishingRod(client.player.getOffhandItem())) {
            return client.player.getOffhandItem();
        }
        return client.player.getMainHandItem();
    }

    private static boolean isFishingRod(ItemStack stack) {
        return stack.getItem() instanceof FishingRodItem || stack.is(COMMON_FISHING_RODS);
    }

    private static boolean wouldBreak(ItemStack stack) {
        return stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= 1;
    }

    private void pause(String reason) {
        status = Status.PAUSED;
        pauseReason = reason;
    }
}
