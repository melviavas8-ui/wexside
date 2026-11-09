package ru.wexside.mixin;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.wexside.module.movement.TimerModule;

@Mixin(RenderTickCounter.Dynamic.class)
public abstract class RenderTickCounterMixin {
    @Redirect(method = "beginRenderTick(J)I", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F", remap = false))
    private float wexside$scaleTickDuration(FloatUnaryOperator targetMillisPerTick, float tickTime) {
        return targetMillisPerTick.apply(tickTime) / TimerModule.getTickRateMultiplier();
    }
}
