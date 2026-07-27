package io.github.ximu.autofish.client.mixin;

import io.github.ximu.autofish.client.AutoFishClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void autofish$onSound(ClientboundSoundPacket packet, CallbackInfo callbackInfo) {
        AutoFishClient.instance().onPacket(packet);
    }

    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"))
    private void autofish$onEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo callbackInfo) {
        AutoFishClient.instance().onPacket(packet);
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void autofish$onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo callbackInfo) {
        AutoFishClient.instance().onSystemChat(packet);
    }
}
