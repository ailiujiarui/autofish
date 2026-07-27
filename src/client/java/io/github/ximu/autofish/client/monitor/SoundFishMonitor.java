package io.github.ximu.autofish.client.monitor;

import io.github.ximu.autofish.client.FishingController;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.entity.projectile.FishingHook;

public final class SoundFishMonitor implements FishMonitor {
    private static final String BOBBER_SPLASH = "minecraft:entity.fishing_bobber.splash";
    private static final double MAX_DISTANCE_SQUARED = 25.0;

    @Override
    public void hookTick(FishingController controller, Minecraft client, FishingHook hook) {
    }

    @Override
    public void handleHookRemoved() {
    }

    @Override
    public void handlePacket(FishingController controller, Packet<?> packet, Minecraft client) {
        if (!(packet instanceof ClientboundSoundPacket soundPacket)
                || !BOBBER_SPLASH.equals(soundPacket.getSound().value().location().toString())
                || client.player == null || client.player.fishing == null) {
            return;
        }

        FishingHook hook = client.player.fishing;
        if (hook.distanceToSqr(soundPacket.getX(), soundPacket.getY(), soundPacket.getZ()) < MAX_DISTANCE_SQUARED) {
            controller.catchFish();
        }
    }
}
