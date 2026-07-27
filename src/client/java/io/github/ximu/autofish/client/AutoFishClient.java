package io.github.ximu.autofish.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.ximu.autofish.client.config.ConfigManager;
import io.github.ximu.autofish.client.access.FishingHookAccess;
import io.github.ximu.autofish.client.screen.AutoFishConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.lwjgl.glfw.GLFW;

@Mod(value = AutoFishClient.MOD_ID, dist = Dist.CLIENT)
public final class AutoFishClient {
    public static final String MOD_ID = "autofish";
    private static AutoFishClient instance;

    private final ConfigManager configManager = new ConfigManager();
    private final FishingController controller = new FishingController(configManager);
    private final KeyMapping.Category keyCategory;
    private final KeyMapping toggleKey;
    private final KeyMapping configKey;

    public AutoFishClient(IEventBus modBus, ModContainer container) {
        instance = this;
        configManager.load();
        controller.updateDetection();

        keyCategory = new KeyMapping.Category(id("controls"));
        toggleKey = new KeyMapping("key.autofish.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, keyCategory);
        configKey = new KeyMapping("key.autofish.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, keyCategory);

        modBus.addListener(this::registerKeys);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (ignored, parent) -> new AutoFishConfigScreen(parent, configManager));

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onRenderGui);
        NeoForge.EVENT_BUS.addListener(this::onEntityTick);
        NeoForge.EVENT_BUS.addListener(this::onLogout);
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

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(keyCategory);
        event.register(toggleKey);
        event.register(configKey);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
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

    private void onRenderGui(RenderGuiEvent.Post event) {
        if (!configManager.get().showHud || !configManager.get().enabled) {
            return;
        }
        Component text = Component.literal("Auto Fish: ").append(controller.statusText());
        event.getGuiGraphics().text(Minecraft.getInstance().font, text, 8, 8, CommonColors.WHITE);
    }

    private void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof FishingHook hook && !hook.level().isClientSide()) {
            onFishingLogic(hook.getOwner(), ((FishingHookAccess) hook).autofish$getNibble());
        }
    }

    private void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
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
