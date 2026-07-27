package io.github.ximu.autofish.client.mixin;

import io.github.ximu.autofish.client.access.FishingHookAccess;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin implements FishingHookAccess {
    @Shadow
    private int nibble;

    @Override
    public int autofish$getNibble() {
        return nibble;
    }
}
