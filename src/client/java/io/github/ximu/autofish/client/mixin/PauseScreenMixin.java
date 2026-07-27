package io.github.ximu.autofish.client.mixin;

import io.github.ximu.autofish.client.AutoFishClient;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class PauseScreenMixin {
    @Inject(method = "isPauseScreen", at = @At("HEAD"), cancellable = true)
    private void autofish$keepFishingWhileOpen(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!((Object) this instanceof PauseScreen)) {
            return;
        }
        AutoFishClient autoFish = AutoFishClient.instance();
        if (autoFish != null && autoFish.shouldKeepWorldRunning()) {
            callbackInfo.setReturnValue(false);
        }
    }
}
