package ru.wexside.recovery.crystal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ru.wexside.util.Angle;

/** Original IllilililiIlIil six-field value object. No new null normalization. */
public record CrystalRotationRequest(LivingEntity target, Vec3d aimPoint, Angle angle,
                                    CrystalRotationStrength strength,
                                    CrystalMovementCorrection correction,
                                    boolean changeViewLook) {
    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.target, this.aimPoint, this.angle, this.strength, this.correction, this.changeViewLook);
    }

    public static CrystalRotationRequest idle() {
        return new CrystalRotationRequest(null, null, null, CrystalRotationStrength.NONE, CrystalMovementCorrection.NONE, false);
    }

    public boolean hasTarget() { return this.target != null; }
    public boolean hasCorrection() { return this.correction != CrystalMovementCorrection.NONE; }
}
