package io.github.ximu.autofish.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.ximu.autofish.client.config.ConfigManager;
import io.github.ximu.autofish.client.screen.AutoFishConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

public final class AutoFishClient implements ClientModInitializer {
    public static final String MOD_ID = "autofish";
    private static AutoFishClient instance;

    private final ConfigManager configManager = new ConfigManager();
    private final FishingController controller = new FishingController(configManager);
    private KeyMapping toggleKey;
    private KeyMapping configKey;

    @Override
    public void onInitializeClient() {
        instance = this;
        configManager.load();
        controller.updateDetection();

        KeyMapping.Category keyCategory = KeyMapping.Category.register(id("controls"));
        toggleKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.autofish.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, keyCategory));
        configKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.autofish.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, keyCategory));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("status"),
                (graphics, tickCounter) -> onRenderGui(graphics));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onLogout());
    }

    public static AutoFishClient instance() {
        return instance;
    }

    public void onFishingLogic(Entity owner, int nibble) {
        controller.tickFishingLogic(owner, nibble);
    }

    public void onPacket(Packet<?> packet) {
        controller.handlePacket(packet);
    }

    public void onSystemChat(ClientboundSystemChatPacket packet) {
        controller.handleSystemChat(packet);
    }

    public boolean shouldKeepWorldRunning() {
        return controller.shouldKeepWorldRunning();
    }

    private void onClientTick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            toggle(client);
        }
        while (configKey.consumeClick()) {
            if (!(client.screen instanceof AutoFishConfigScreen)) {
                client.setScreen(new AutoFishConfigScreen(client.screen, configManager));
            }
        }
        controller.tick(client);
    }

    private void onRenderGui(GuiGraphicsExtractor graphics) {
        if (!configManager.get().showHud || !configManager.get().enabled) {
            return;
        }
        Component text = Component.literal("Auto Fish: ").append(controller.statusText());
        graphics.text(Minecraft.getInstance().font, text, 8, 8, CommonColors.WHITE);
    }

    private void onLogout() {
        controller.reset();
        configManager.save();
    }

    private void toggle(Minecraft client) {
        configManager.get().enabled = !configManager.get().enabled;
        configManager.save();
        if (!configManager.get().enabled) {
            controller.reset();
        }
        if (client.player != null) {
            String key = configManager.get().enabled ? "text.autofish.enabled" : "text.autofish.disabled";
            client.player.sendSystemMessage(Component.translatable(key));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
