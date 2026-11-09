package ru.wexside.util;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.wexside.util.Angle;

public final class AimCalculator {
    private static final double HITBOX_MARGIN = 0.05;
    private static final double JITTER_DECAY = 0.85;
    private static final double JITTER_STRENGTH = 0.04;
    private static final double MAX_JITTER = 0.18;
    private static final double EPSILON = 1.0E-7;
    private static final int RANGE_SEARCH_STEPS = 8;
    private final Random random = new Random();
    private Vec3d jitter = Vec3d.ZERO;

    public Vec3d calculateAimPoint(LivingEntity target, Angle look, float attackRange, float searchRange, boolean throughWalls) {
        boolean targetWithinAttackRange;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || target == null || look == null) {
            return null;
        }
        Vec3d eye = player.getEyePos();
        Box hitbox = AimCalculator.shrinkHitbox(target.getBoundingBox());
        if (hitbox.contains(eye)) {
            return eye.add(look.toVec3d().multiply(0.5));
        }
        Vec3d closest = AimCalculator.closestPoint(hitbox, eye);
        double closestDistance = eye.distanceTo(closest);
        if (closestDistance > (double)searchRange) {
            return null;
        }
        Angle boundedLook = AimCalculator.clampLookToHitbox(eye, hitbox, look);
        Vec3d aimPoint = AimCalculator.raycastHitbox(hitbox, eye, boundedLook, searchRange).orElse(closest);
        boolean bl = targetWithinAttackRange = closestDistance <= (double)attackRange;
        if (targetWithinAttackRange && eye.distanceTo(aimPoint) > (double)attackRange) {
            aimPoint = AimCalculator.findPointWithinAttackRange(hitbox, eye, look, closest, attackRange, searchRange);
        }
        aimPoint = this.applyJitter(hitbox, eye, aimPoint, attackRange, searchRange, targetWithinAttackRange);
        if (!throughWalls && !AimCalculator.isVisible(player, eye, aimPoint) && AimCalculator.isVisible(player, eye, closest)) {
            return closest;
        }
        return aimPoint;
    }

    private Vec3d applyJitter(Box hitbox, Vec3d eye, Vec3d aimPoint, float attackRange, float searchRange, boolean targetWithinAttackRange) {
        this.jitter = this.jitter.multiply(0.85).add(this.random.nextGaussian() * 0.04, this.random.nextGaussian() * 0.04, this.random.nextGaussian() * 0.04);
        this.jitter = AimCalculator.limitLength(this.jitter, 0.18);
        Vec3d jittered = AimCalculator.closestPoint(hitbox, aimPoint.add(this.jitter));
        if (!targetWithinAttackRange) {
            return jittered;
        }
        Optional<Vec3d> intersection = AimCalculator.raycastHitbox(hitbox, eye, Angle.fromVectors(eye, jittered), searchRange);
        return intersection.isPresent() && eye.distanceTo(intersection.get()) <= (double)attackRange ? jittered : aimPoint;
    }

    private static Vec3d findPointWithinAttackRange(Box hitbox, Vec3d eye, Angle currentLook, Vec3d closest, float attackRange, float searchRange) {
        Angle closestAngle = Angle.fromVectors(eye, closest);
        float lower = 0.0f;
        float upper = 1.0f;
        for (int step = 0; step < 8; ++step) {
            float factor = (lower + upper) * 0.5f;
            Optional<Vec3d> intersection = AimCalculator.raycastHitbox(hitbox, eye, AimCalculator.interpolate(currentLook, closestAngle, factor), searchRange);
            if (intersection.isPresent() && eye.distanceTo(intersection.get()) <= (double)attackRange) {
                upper = factor;
                continue;
            }
            lower = factor;
        }
        return AimCalculator.raycastHitbox(hitbox, eye, AimCalculator.interpolate(currentLook, closestAngle, upper), searchRange).orElse(closest);
    }

    private static Angle clampLookToHitbox(Vec3d eye, Box hitbox, Angle look) {
        float yaw = AimCalculator.clampYawToHitbox(eye, hitbox, look.getYaw());
        float pitch = AimCalculator.clampPitchToHitbox(eye, hitbox, yaw, look.getPitch());
        return new Angle(yaw, pitch);
    }

    private static float clampYawToHitbox(Vec3d eye, Box hitbox, float yaw) {
        if (eye.x >= hitbox.minX && eye.x <= hitbox.maxX && eye.z >= hitbox.minZ && eye.z <= hitbox.maxZ) {
            return yaw;
        }
        float[] corners = new float[]{AimCalculator.yawTo(eye, hitbox.minX, hitbox.minZ), AimCalculator.yawTo(eye, hitbox.minX, hitbox.maxZ), AimCalculator.yawTo(eye, hitbox.maxX, hitbox.minZ), AimCalculator.yawTo(eye, hitbox.maxX, hitbox.maxZ)};
        Arrays.sort(corners);
        int arcStartIndex = 0;
        float largestGap = corners[0] + 360.0f - corners[3];
        for (int index = 1; index < corners.length; ++index) {
            float gap = corners[index] - corners[index - 1];
            if (!(gap > largestGap)) continue;
            largestGap = gap;
            arcStartIndex = index;
        }
        float arcStart = corners[arcStartIndex];
        float arcEnd = corners[(arcStartIndex + corners.length - 1) % corners.length];
        float arcLength = AimCalculator.positiveDegrees(arcEnd - arcStart);
        float position = AimCalculator.positiveDegrees(yaw - arcStart);
        if (position <= arcLength) {
            return yaw;
        }
        return position - arcLength <= 360.0f - position ? arcEnd : arcStart;
    }

    private static float clampPitchToHitbox(Vec3d eye, Box hitbox, float yaw, float pitch) {
        Vec3d horizontalDirection = new Angle(yaw, 0.0f).toVec3d();
        double[] xRange = AimCalculator.slabRange(eye.x, horizontalDirection.x, hitbox.minX, hitbox.maxX);
        double[] zRange = AimCalculator.slabRange(eye.z, horizontalDirection.z, hitbox.minZ, hitbox.maxZ);
        if (xRange == null || zRange == null) {
            return pitch;
        }
        double entry = Math.max(0.0, Math.max(xRange[0], zRange[0]));
        double exit = Math.min(xRange[1], zRange[1]);
        if (exit < entry) {
            return pitch;
        }
        double minY = hitbox.minY - eye.y;
        double maxY = hitbox.maxY - eye.y;
        float minPitch = Float.MAX_VALUE;
        float maxPitch = -3.4028235E38f;
        for (double distance : new double[]{entry, exit}) {
            for (double height : new double[]{minY, maxY}) {
                float candidate = (float)(-Math.toDegrees(Math.atan2(height, distance)));
                minPitch = Math.min(minPitch, candidate);
                maxPitch = Math.max(maxPitch, candidate);
            }
        }
        return MathHelper.clamp((float)pitch, (float)minPitch, (float)maxPitch);
    }

    private static double[] slabRange(double origin, double direction, double min, double max) {
        if (Math.abs(direction) < 1.0E-7) {
            double[] dArray;
            if (origin >= min && origin <= max) {
                double[] dArray2 = new double[2];
                dArray2[0] = 0.0;
                dArray = dArray2;
                dArray2[1] = Double.MAX_VALUE;
            } else {
                dArray = null;
            }
            return dArray;
        }
        double first = (min - origin) / direction;
        double second = (max - origin) / direction;
        return new double[]{Math.min(first, second), Math.max(first, second)};
    }

    private static Optional<Vec3d> raycastHitbox(Box hitbox, Vec3d eye, Angle angle, float distance) {
        return hitbox.raycast(eye, eye.add(angle.toVec3d().multiply((double)distance)));
    }

    private static Angle interpolate(Angle from, Angle to, float factor) {
        float yaw = from.getYaw() + MathHelper.wrapDegrees((float)(to.getYaw() - from.getYaw())) * factor;
        float pitch = from.getPitch() + (to.getPitch() - from.getPitch()) * factor;
        return new Angle(MathHelper.wrapDegrees((float)yaw), pitch);
    }

    private static float yawTo(Vec3d eye, double x, double z) {
        return MathHelper.wrapDegrees((float)((float)(Math.toDegrees(Math.atan2(z - eye.z, x - eye.x)) - 90.0)));
    }

    private static float positiveDegrees(float angle) {
        float normalized = angle % 360.0f;
        return normalized < 0.0f ? normalized + 360.0f : normalized;
    }

    private static Vec3d limitLength(Vec3d vector, double maximum) {
        double length = vector.length();
        return length <= maximum ? vector : vector.multiply(maximum / length);
    }

    private static Vec3d closestPoint(Box box, Vec3d point) {
        return new Vec3d(MathHelper.clamp((double)point.x, (double)box.minX, (double)box.maxX), MathHelper.clamp((double)point.y, (double)box.minY, (double)box.maxY), MathHelper.clamp((double)point.z, (double)box.minZ, (double)box.maxZ));
    }

    private static boolean isVisible(ClientPlayerEntity player, Vec3d start, Vec3d end) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return false;
        }
        RaycastContext context = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)player);
        return world.raycast(context).getType() == HitResult.Type.MISS;
    }

    private static Box shrinkHitbox(Box box) {
        double x = Math.min(0.05, box.getLengthX() / 4.0);
        double y = Math.min(0.05, box.getLengthY() / 4.0);
        double z = Math.min(0.05, box.getLengthZ() / 4.0);
        return box.expand(-x, -y, -z);
    }
}

