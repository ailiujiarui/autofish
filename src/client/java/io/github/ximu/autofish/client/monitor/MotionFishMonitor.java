package io.github.ximu.autofish.client.monitor;

import io.github.ximu.autofish.client.FishingController;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.Util;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

public final class MotionFishMonitor implements FishMonitor {
    private static final double DOWNWARD_MOTION_THRESHOLD = -350.0 / 8000.0;
    private static final long START_CATCHING_AFTER_MS = 1000;
    private static final double HORIZONTAL_EPSILON = 1.0E-6;

    private boolean hasHitWater;
    private long bobberRiseAt;

    @Override
    public void hookTick(FishingController controller, Minecraft client, FishingHook hook) {
        if (hook.isInWater()) {
            hasHitWater = true;
        }
    }

    @Override
    public void handleHookRemoved() {
        hasHitWater = false;
        bobberRiseAt = 0;
    }

    @Override
    public void handlePacket(FishingController controller, Packet<?> packet, Minecraft client) {
        if (!(packet instanceof ClientboundSetEntityMotionPacket motionPacket)
                || client.player == null || client.player.fishing == null
                || client.player.fishing.getId() != motionPacket.id()) {
            return;
        }

        Vec3 movement = motionPacket.movement();
        long now = Util.getMillis();
        if (hasHitWater && bobberRiseAt == 0 && movement.y > 0) {
            bobberRiseAt = now;
        }

        if (hasHitWater && bobberRiseAt != 0 && now - bobberRiseAt > START_CATCHING_AFTER_MS
                && Math.abs(movement.x) < HORIZONTAL_EPSILON
                && Math.abs(movement.z) < HORIZONTAL_EPSILON
                && movement.y < DOWNWARD_MOTION_THRESHOLD) {
            controller.catchFish();
            handleHookRemoved();
        }
    }
}
