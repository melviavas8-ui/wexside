package ru.wexside.util;

import net.minecraft.util.math.MathHelper;
import ru.wexside.util.Angle;

public class FixedStepRotationSmoother {
    private final float stepBudgetDegrees;

    public FixedStepRotationSmoother(float f) {
        this.stepBudgetDegrees = f;
    }

    public Angle stepTowardAngle(Angle angle, Angle angle2) {
        float f = MathHelper.wrapDegrees((float)(angle2.yawDegrees() - angle.yawDegrees()));
        float f2 = MathHelper.wrapDegrees((float)(angle2.pitchDegrees() - angle.pitchDegrees()));
        float f3 = Math.abs(f) + Math.abs(f2);
        if (f3 < 1.0E-4f) {
            return angle2;
        }
        float f4 = Math.abs(f) / f3 * this.stepBudgetDegrees;
        float f5 = Math.abs(f2) / f3 * this.stepBudgetDegrees;
        float f6 = angle.yawDegrees() + MathHelper.clamp((float)f, (float)(-f4), (float)f4);
        float f7 = MathHelper.clamp((float)(angle.pitchDegrees() + MathHelper.clamp((float)f2, (float)(-f5), (float)f5)), (float)-90.0f, (float)90.0f);
        return new Angle(f6, f7);
    }
}

