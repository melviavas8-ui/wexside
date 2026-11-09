package ru.wexside.util;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.math.MathHelper;
import ru.wexside.misc.RotationApplyResult;
import ru.wexside.misc.RotationStrategy;
import ru.wexside.util.Angle;
import ru.wexside.util.RotationIntent;
import ru.wexside.util.RotationState;

public final class RandomizedDirectRotationStrategy
implements RotationStrategy {
    private static final float value = 60.0f;
    private static final float value2 = 1.0f;
    private static final float value3 = 60.0f;
    private static final float value4 = 45.0f;
    private static final float value5 = 5.0f;

    @Override
    public RotationApplyResult applyRotation(RotationState iIiIlIIliI2, RotationIntent intent) {
        Angle angle;
        Angle angle2 = iIiIlIIliI2.getPlayerAngle();
        Angle angle3 = angle = iIiIlIIliI2.getAppliedAngle() != null ? iIiIlIIliI2.getAppliedAngle() : angle2;
        if (!intent.hasTarget() || intent.targetAngle() == null) {
            if (angle.angularDistanceTo(angle2) < 1.0f) {
                return RotationApplyResult.notReady(angle2);
            }
            return RotationApplyResult.notReady(RandomizedDirectRotationStrategy.stepTowardAngle(angle, angle2));
        }
        Angle angle4 = RandomizedDirectRotationStrategy.stepTowardAngle(angle, intent.targetAngle());
        boolean bl = angle4.angularDistanceTo(intent.targetAngle()) < 5.0f;
        return RotationApplyResult.applied(angle4, bl && iIiIlIIliI2.isAttackReady());
    }

    @Override
    public void onDeactivated(RotationState iIiIlIIliI2) {
    }

    @Override
    public void onActivated(RotationState iIiIlIIliI2) {
    }

    private static Angle stepTowardAngle(Angle angle, Angle angle2) {
        float f = MathHelper.wrapDegrees((float)(angle2.yawDegrees() - angle.yawDegrees()));
        float f2 = MathHelper.wrapDegrees((float)(angle2.pitchDegrees() - angle.pitchDegrees()));
        float f3 = (float)Math.hypot(Math.abs(f), Math.abs(f2));
        if (f3 == 0.0f) {
            return angle;
        }
        float f4 = Math.abs(f / f3) * 60.0f;
        float f5 = Math.abs(f2 / f3) * 45.0f;
        float f6 = MathHelper.clamp((float)f, (float)(-f4), (float)f4);
        float f7 = MathHelper.clamp((float)f2, (float)(-f5), (float)f5);
        float f8 = Math.abs(f6) + Math.abs(f7);
        float f9 = 60.0f * ThreadLocalRandom.current().nextFloat(0.9f, 1.0f);
        float f10 = f8 == 0.0f ? 0.0f : Math.abs(f6 / f8) * f9;
        float f11 = f8 == 0.0f ? 0.0f : Math.abs(f7 / f8) * f9;
        float f12 = angle.yawDegrees() + MathHelper.clamp((float)f6, (float)(-f10), (float)f10);
        float f13 = angle.pitchDegrees() + MathHelper.clamp((float)f7, (float)(-f11), (float)f11);
        return new Angle(f12, f13);
    }
}

