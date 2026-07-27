package io.github.ximu.autofish.client.mixin;

import io.github.ximu.autofish.client.AutoFishClient;
import io.github.ximu.autofish.client.access.FishingHookAccess;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin implements FishingHookAccess {
    @Shadow
    private int nibble;

    @Override
    public int autofish$getNibble() {
        return nibble;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void autofish$afterTick(CallbackInfo callbackInfo) {
        FishingHook hook = (FishingHook) (Object) this;
        AutoFishClient autoFish = AutoFishClient.instance();
        if (autoFish != null && !hook.level().isClientSide()) {
            autoFish.onFishingLogic(hook.getOwner(), nibble);
        }
    }
}
