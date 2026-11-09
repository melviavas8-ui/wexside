package ru.wexside.misc;

import ru.wexside.misc.RaycastMode;
import ru.wexside.misc.SprintResetMode;
import ru.wexside.util.Angle;

public record AttackOptions(Angle lookAngle, float range, int clicksPerSecond, boolean raycastEnabled, boolean sprintResetEnabled, SprintResetMode sprintResetMode, boolean criticalsOnly, RaycastMode raycastMode, boolean legacyCombat, double accuracyPercent, boolean breakShield, boolean desyncShield, boolean jumpOnly, float maceFallDistance) {
    public AttackOptions {
        sprintResetMode = sprintResetMode == null ? SprintResetMode.NONE : sprintResetMode;
        raycastMode = raycastMode == null ? RaycastMode.VISIBLE : raycastMode;
        clicksPerSecond = Math.max(1, clicksPerSecond);
        accuracyPercent = Math.clamp(accuracyPercent, 0.0, 100.0);
        range = Math.max(0.0f, range);
        maceFallDistance = Math.max(0.0f, maceFallDistance);
    }

    public boolean allowsThroughWalls() {
        return this.raycastMode == RaycastMode.THROUGH_WALLS;
    }
}

