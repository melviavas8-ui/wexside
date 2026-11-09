package ru.wexside.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ru.wexside.misc.AttackUrgency;
import ru.wexside.misc.CorrectionMode;
import ru.wexside.util.Angle;

public record RotationIntent(LivingEntity target, Vec3d aimPoint, Angle targetAngle, AttackUrgency urgency, CorrectionMode correction, boolean changeViewLook) {
    public static RotationIntent empty() {
        return new RotationIntent(null, null, null, AttackUrgency.NONE, CorrectionMode.NONE, false);
    }

    public boolean hasTarget() {
        return this.target != null;
    }

    public boolean hasCorrection() {
        return this.correction != CorrectionMode.NONE;
    }
}

