package ru.wexside.util;

import java.util.Comparator;
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
import ru.wexside.misc.TargetFilter;

public final class TargetSelector {
    private LivingEntity selectedTarget;

    public LivingEntity findTarget(TargetFilter filter, float range, String sorting, boolean throughWalls) {
        return this.selectTarget(filter, range, sorting, throughWalls);
    }

    public LivingEntity selectTarget(TargetFilter filter, float range, String sorting, boolean throughWalls) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            this.selectedTarget = null;
            return null;
        }
        Vec3d eyePosition = player.getEyePos();
        if (this.isValid(this.selectedTarget, player, filter, eyePosition, range)) {
            return this.selectedTarget;
        }
        Comparator<LivingEntity> comparator = TargetSelector.createComparator(player, eyePosition, sorting);
        LivingEntity bestVisible = null;
        LivingEntity bestOverall = null;
        for (Entity candidate : world.getEntities()) {
            LivingEntity living;
            if (!(candidate instanceof LivingEntity) || !this.isValid(living = (LivingEntity)candidate, player, filter, eyePosition, range)) continue;
            if (bestOverall == null || comparator.compare(living, bestOverall) < 0) {
                bestOverall = living;
            }
            if (throughWalls || !TargetSelector.hasLineOfSight(world, player, eyePosition, living) || bestVisible != null && comparator.compare(living, bestVisible) >= 0) continue;
            bestVisible = living;
        }
        this.selectedTarget = bestVisible != null ? bestVisible : bestOverall;
        return this.selectedTarget;
    }

    public LivingEntity getLivingEntity() {
        return this.selectedTarget;
    }

    public void reset() {
        this.selectedTarget = null;
    }

    public void update() {
        this.reset();
    }

    public static double distanceTo(LivingEntity entity) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player == null ? Double.MAX_VALUE : TargetSelector.distanceToBounds(player.getEyePos(), entity);
    }

    public static double distanceFromPlayerEyes(LivingEntity entity) {
        return TargetSelector.distanceTo(entity);
    }

    private boolean isValid(LivingEntity entity, ClientPlayerEntity player, TargetFilter filter, Vec3d origin, float range) {
        return entity != null && entity != player && entity.isAlive() && filter.matches(entity) && TargetSelector.distanceToBounds(origin, entity) <= (double)range;
    }

    private static boolean hasLineOfSight(ClientWorld world, ClientPlayerEntity player, Vec3d origin, LivingEntity target) {
        Vec3d destination = TargetSelector.closestPoint(origin, target.getBoundingBox());
        RaycastContext context = new RaycastContext(origin, destination, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)player);
        return world.raycast(context).getType() == HitResult.Type.MISS;
    }

    private static Comparator<LivingEntity> createComparator(ClientPlayerEntity player, Vec3d origin, String sorting) {
        Comparator<LivingEntity> distance = Comparator.comparingDouble((LivingEntity entity) -> TargetSelector.distanceToBounds(origin, entity));
        return switch (sorting) {
            case "Health" -> Comparator.comparingDouble((LivingEntity entity) -> entity.getHealth()).thenComparing(distance);
            case "Crosshair" -> Comparator.comparingDouble((LivingEntity entity) -> TargetSelector.crosshairDeviation(player, origin, entity)).thenComparing(distance);
            default -> distance.thenComparingDouble(LivingEntity::getHealth);
        };
    }

    private static double crosshairDeviation(ClientPlayerEntity player, Vec3d origin, LivingEntity target) {
        Vec3d direction = TargetSelector.closestPoint(origin, target.getBoundingBox()).subtract(origin);
        if (direction.lengthSquared() < 1.0E-7) {
            return -1.0;
        }
        return 1.0 - player.getRotationVec(1.0f).normalize().dotProduct(direction.normalize());
    }

    private static double distanceToBounds(Vec3d origin, LivingEntity entity) {
        return origin.distanceTo(TargetSelector.closestPoint(origin, entity.getBoundingBox()));
    }

    private static Vec3d closestPoint(Vec3d point, Box box) {
        return new Vec3d(MathHelper.clamp((double)point.x, (double)box.minX, (double)box.maxX), MathHelper.clamp((double)point.y, (double)box.minY, (double)box.maxY), MathHelper.clamp((double)point.z, (double)box.minZ, (double)box.maxZ));
    }
}

