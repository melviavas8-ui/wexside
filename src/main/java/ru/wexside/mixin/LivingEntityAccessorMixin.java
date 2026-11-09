package ru.wexside.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.wexside.misc.LivingEntityStateAccessor;

@Mixin(value={LivingEntity.class})
public abstract class LivingEntityAccessorMixin
implements LivingEntityStateAccessor {
    @Shadow
    private int jumpingCooldown;

    @Override
    public void setJumpingCooldown(int cooldown) {
        this.jumpingCooldown = cooldown;
    }
}

