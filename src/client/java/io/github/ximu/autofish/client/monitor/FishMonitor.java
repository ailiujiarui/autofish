package io.github.ximu.autofish.client.monitor;

import io.github.ximu.autofish.client.FishingController;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.projectile.FishingHook;

public interface FishMonitor {
    void hookTick(FishingController controller, Minecraft client, FishingHook hook);

    void handleHookRemoved();

    void handlePacket(FishingController controller, Packet<?> packet, Minecraft client);
}
