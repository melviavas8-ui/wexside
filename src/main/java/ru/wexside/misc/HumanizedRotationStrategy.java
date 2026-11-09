package ru.wexside.misc;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.math.MathHelper;
import ru.wexside.misc.AimJitter;
import ru.wexside.misc.AttackUrgency;
import ru.wexside.misc.RotationApplyResult;
import ru.wexside.misc.RotationStrategy;
import ru.wexside.util.Angle;
import ru.wexside.util.RotationIntent;
import ru.wexside.util.RotationState;

public class HumanizedRotationStrategy
implements RotationStrategy {
    static final float value = 16.0f;
    private final AimJitter aimJitter = new AimJitter(20.0f, 10.0f, 2, 0.6f);
    static final int slot = 9;
    static final float value2 = 18.0f;
    static final float value3 = 24.0f;
    private Angle postHitAngle;
    static final float value4 = 12.0f;

    private Angle createPostHitOffset(Angle angle) {
        boolean bl;
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        float f = (threadLocalRandom.nextBoolean() ? 1.0f : -1.0f) * threadLocalRandom.nextFloat(18.0f, 24.0f);
        float f2 = threadLocalRandom.nextFloat(12.0f, 16.0f);
        boolean bl2 = angle.pitchDegrees() + f2 <= 90.0f;
        boolean bl3 = bl = angle.pitchDegrees() - f2 >= -90.0f;
        float f3 = bl2 && bl ? (threadLocalRandom.nextBoolean() ? 1.0f : -1.0f) : (bl ? -1.0f : 1.0f);
        float f4 = MathHelper.clamp((float)(angle.pitchDegrees() + f3 * f2), (float)-90.0f, (float)90.0f);
        return new Angle(angle.yawDegrees() + f, f4);
    }

    @Override
    public RotationApplyResult applyRotation(RotationState rotationState, RotationIntent intent) {
        Angle currentAngle = rotationState.getPlayerAngle();
        if (!intent.hasTarget() || intent.targetAngle() == null) {
            this.postHitAngle = null;
            return RotationApplyResult.notReady(this.aimJitter.applyJitter(currentAngle));
        }
        if (intent.urgency() == AttackUrgency.HIT) {
            this.postHitAngle = this.createPostHitOffset(intent.targetAngle());
            return RotationApplyResult.applied(intent.targetAngle(), true);
        }
        Angle baseAngle = this.postHitAngle != null && rotationState.getTicksSinceHit() < 9 ? this.postHitAngle : currentAngle;
        return RotationApplyResult.applied(this.aimJitter.applyJitter(baseAngle), false);
    }

    @Override
    public void onDeactivated(RotationState iIiIlIIliI2) {
        this.aimJitter.update();
        this.postHitAngle = null;
    }

    @Override
    public void onActivated(RotationState iIiIlIIliI2) {
        this.aimJitter.update();
        this.postHitAngle = null;
    }
}

