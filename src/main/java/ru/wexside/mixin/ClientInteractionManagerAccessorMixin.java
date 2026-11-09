package ru.wexside.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.wexside.misc.BlockBreakingAccessor;

@Mixin(value={ClientPlayerInteractionManager.class})
public abstract class ClientInteractionManagerAccessorMixin
implements BlockBreakingAccessor {
    @Shadow
    private float currentBreakingProgress;
    @Shadow
    private int blockBreakingCooldown;

    @Override
    public void setBreakingProgress(float progress) {
        this.currentBreakingProgress = progress;
    }

    @Override
    public void setBreakingCooldown(int cooldown) {
        this.blockBreakingCooldown = cooldown;
    }

    @Override
    public float getBreakingProgress() {
        return this.currentBreakingProgress;
    }
}

