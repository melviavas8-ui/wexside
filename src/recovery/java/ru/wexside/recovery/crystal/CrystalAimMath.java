package ru.wexside.recovery.crystal;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.wexside.util.Angle;

/** Recovered clamp and yaw/pitch arithmetic used by the retained CrystalAura paths. */
public final class CrystalAimMath {
    private CrystalAimMath() {}

    public static Vec3d clampPointToBox(Vec3d point, Box box) {
        return new Vec3d(MathHelper.clamp(point.x, box.minX, box.maxX),
                MathHelper.clamp(point.y, box.minY, box.maxY),
                MathHelper.clamp(point.z, box.minZ, box.maxZ));
    }

    public static Angle angleToPoint(Vec3d eyePosition, Vec3d targetPoint) {
        Vec3d delta = targetPoint.subtract(eyePosition);
        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
        return new Angle(yaw, pitch);
    }
}
