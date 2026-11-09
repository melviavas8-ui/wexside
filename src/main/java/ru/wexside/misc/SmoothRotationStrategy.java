package ru.wexside.misc;

import ru.wexside.misc.RotationApplyResult;
import ru.wexside.misc.RotationStrategy;
import ru.wexside.util.Angle;
import ru.wexside.util.FixedStepRotationSmoother;
import ru.wexside.util.RotationIntent;
import ru.wexside.util.RotationState;

public class SmoothRotationStrategy
implements RotationStrategy {
    private final FixedStepRotationSmoother rotationSmoother = new FixedStepRotationSmoother(45.0f);
    static final float value = 1.0f;
    static final float value2 = 2.0f;

    @Override
    public RotationApplyResult applyRotation(RotationState iIiIlIIliI2, RotationIntent intent) {
        Angle angle;
        Angle angle2 = iIiIlIIliI2.getPlayerAngle();
        Angle angle3 = angle = iIiIlIIliI2.getAppliedAngle() != null ? iIiIlIIliI2.getAppliedAngle() : angle2;
        if (!intent.hasTarget() || intent.targetAngle() == null) {
            if (angle.angularDistanceTo(angle2) < 1.0f) {
                return RotationApplyResult.notReady(angle2);
            }
            return RotationApplyResult.notReady(this.rotationSmoother.stepTowardAngle(angle, angle2));
        }
        Angle angle4 = this.rotationSmoother.stepTowardAngle(angle, intent.targetAngle());
        boolean bl = angle4.angularDistanceTo(intent.targetAngle()) < 2.0f;
        return RotationApplyResult.applied(angle4, bl && iIiIlIIliI2.isAttackReady());
    }

    @Override
    public void onDeactivated(RotationState iIiIlIIliI2) {
    }

    @Override
    public void onActivated(RotationState iIiIlIIliI2) {
    }
}

