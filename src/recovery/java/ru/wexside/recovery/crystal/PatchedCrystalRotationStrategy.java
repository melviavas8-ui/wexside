package ru.wexside.recovery.crystal;

import ru.wexside.misc.RotationApplyResult;
import ru.wexside.util.Angle;
import ru.wexside.util.RotationState;

/** Exact supplied patched llIlllIlllIiIl body; not a claim about an erased pre-patch strategy. */
public final class PatchedCrystalRotationStrategy implements CrystalRotationStrategy {
    public static float wrap(float degrees) {
        return (degrees % 360.0f + 540.0f) % 360.0f - 180.0f;
    }

    public static Angle approach(Angle current, Angle desired) {
        float yawDelta = wrap(desired.yawDegrees() - current.yawDegrees());
        float pitchDelta = desired.pitchDegrees() - current.pitchDegrees();
        float distance = (float) Math.hypot((double) yawDelta, (double) pitchDelta);
        float scale = distance > 45.0f ? 45.0f / distance : 1.0f;
        return new Angle(current.yawDegrees() + yawDelta * scale,
                Math.max(-90.0f, Math.min(90.0f, current.pitchDegrees() + pitchDelta * scale)));
    }

    @Override
    public RotationApplyResult step(RotationState state, CrystalRotationRequest request) {
        Angle player = state.playerAngle();
        Angle current = state.appliedAngle() == null ? player : state.appliedAngle();
        Angle desired = request == null ? null : request.angle();
        boolean tracking = request != null && request.hasTarget() && desired != null;
        if (!tracking) desired = player;
        Angle result = approach(current, desired);
        boolean converged = Math.hypot((double) wrap(desired.yawDegrees() - result.yawDegrees()),
                (double) (desired.pitchDegrees() - result.pitchDegrees())) < 2.0;
        return new RotationApplyResult(result, tracking && converged && state.attackReady());
    }

    /** Supplied bytecode contains only return. */
    @Override public void onDeactivated(RotationState state) {}
    /** Supplied bytecode contains only return. */
    @Override public void onActivated(RotationState state) {}
}
