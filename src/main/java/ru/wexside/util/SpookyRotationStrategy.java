package ru.wexside.util;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import ru.wexside.misc.ReachHelper;
import ru.wexside.misc.RotationApplyResult;
import ru.wexside.misc.RotationStrategy;
import ru.wexside.util.Angle;
import ru.wexside.util.RotationIntent;
import ru.wexside.util.RotationState;

public final class SpookyRotationStrategy
implements RotationStrategy {
    static final float value = 360.0f;
    static final float value2 = 25.0f;
    static final int slot = 3;
    private float endYaw;
    static final float value5 = 5.0f;
    static final float value6 = 0.4f;
    static final float value7 = 1.0E-4f;
    static final float value8 = 16.0f;
    static final float value9 = 4.0f;
    static final float value10 = 1.70158f;
    private float interpolationProgress = 1.0f;
    private float startPitch;
    static final float value13 = 4.0f;
    static final float value14 = 0.1f;
    static final double value15 = 70.0;
    private float endPitch;
    static final int slot2 = 1;
    static final float value17 = 89.0f;
    static final float value18 = 6.0f;
    static final float value19 = 10.0f;
    private float startYaw;

    @Override
    public RotationApplyResult applyRotation(RotationState iIiIlIIliI2, RotationIntent intent) {
        Angle angle;
        Angle angle2 = iIiIlIIliI2.getPlayerAngle();
        Angle angle3 = angle = iIiIlIIliI2.getAppliedAngle() != null ? iIiIlIIliI2.getAppliedAngle() : angle2;
        if (!intent.hasTarget() || intent.targetAngle() == null) {
            if (angle.angularDistanceTo(angle2) < 1.0f) {
                this.interpolationProgress = 1.0f;
                return RotationApplyResult.notReady(angle2);
            }
            return RotationApplyResult.notReady(this.stepWithOscillation(iIiIlIIliI2, angle, angle2, null));
        }
        Angle angle4 = this.stepWithOscillation(iIiIlIIliI2, angle, intent.targetAngle(), iIiIlIIliI2.getTarget());
        boolean bl = angle4.angularDistanceTo(intent.targetAngle()) < 5.0f || this.isTargetOnRay(iIiIlIIliI2.getTarget(), angle4);
        return RotationApplyResult.applied(angle4, bl && iIiIlIIliI2.isAttackReady());
    }

    @Override
    public void onDeactivated(RotationState iIiIlIIliI2) {
        this.interpolationProgress = 1.0f;
    }

    @Override
    public void onActivated(RotationState iIiIlIIliI2) {
        this.interpolationProgress = 1.0f;
    }

    private boolean shouldPauseTurn(RotationState iIiIlIIliI2, Angle angle, LivingEntity entity2, ThreadLocalRandom threadLocalRandom) {
        if (iIiIlIIliI2.getTicksSinceAttack() <= threadLocalRandom.nextInt(1, 3)) {
            return true;
        }
        return this.isTargetOnRay(entity2, angle);
    }

    private Angle stepWithOscillation(RotationState iIiIlIIliI2, Angle angle, Angle angle2, LivingEntity entity2) {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        float f = MathHelper.wrapDegrees((float)(angle2.yawDegrees() - angle.yawDegrees()));
        float f2 = MathHelper.wrapDegrees((float)(angle2.pitchDegrees() - angle.pitchDegrees()));
        float f3 = (float)Math.hypot(Math.abs(f), Math.abs(f2));
        if (f3 < 1.0E-4f) {
            f3 = 1.0E-4f;
        }
        float f4 = threadLocalRandom.nextFloat(25.0f, 360.0f);
        float f5 = threadLocalRandom.nextFloat(4.0f, 10.0f);
        float f6 = (float)(5.0 * Math.cos((double)System.currentTimeMillis() / 70.0));
        float f7 = (float)(4.0 * Math.sin((double)System.currentTimeMillis() / 70.0));
        if (this.shouldPauseTurn(iIiIlIIliI2, angle, entity2, threadLocalRandom)) {
            f4 = 0.0f;
            f5 = 0.0f;
            f6 = 0.0f;
            f7 = 0.0f;
        }
        float f8 = Math.abs(f / f3) * f4;
        float f9 = Math.abs(f2 / f3) * f5;
        float f10 = angle.yawDegrees() + MathHelper.clamp((float)f, (float)(-f8), (float)f8);
        float f11 = MathHelper.clamp((float)(angle.pitchDegrees() + MathHelper.clamp((float)f2, (float)(-f9), (float)f9)), (float)-89.0f, (float)89.0f);
        return this.interpolateTurn(angle, f10 + f6, f11 + f7, threadLocalRandom.nextFloat(0.1f, 0.4f));
    }

    private static float easeOutBack(float f) {
        float f2 = 2.70158f;
        float f3 = f - 1.0f;
        return 1.0f + f2 * f3 * f3 * f3 + 1.70158f * f3 * f3;
    }

    private Angle interpolateTurn(Angle angle, float f, float f2, float f3) {
        float f4 = Math.abs(MathHelper.wrapDegrees((float)(f - this.endYaw)));
        float f5 = Math.abs(f2 - this.endPitch);
        if (this.interpolationProgress >= 1.0f || f4 > 6.0f || f5 > 6.0f) {
            this.startYaw = angle.yawDegrees();
            this.startPitch = angle.pitchDegrees();
            this.endYaw = f;
            this.endPitch = f2;
            this.interpolationProgress = 0.0f;
        }
        this.interpolationProgress = MathHelper.clamp((float)(this.interpolationProgress + f3), (float)0.0f, (float)1.0f);
        float f6 = SpookyRotationStrategy.easeOutBack(this.interpolationProgress);
        float f7 = MathHelper.wrapDegrees((float)(this.endYaw - this.startYaw));
        float f8 = this.endPitch - this.startPitch;
        float f9 = this.startYaw + f7 * f6;
        float f10 = MathHelper.clamp((float)(this.startPitch + f8 * f6), (float)-89.0f, (float)89.0f);
        return new Angle(f9, f10);
    }

    private boolean isTargetOnRay(LivingEntity entity2, Angle angle) {
        return entity2 != null && ReachHelper.raycastEntity((Entity)entity2, angle, 16.0f, true) != null;
    }
}

