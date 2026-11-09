package ru.wexside.render.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class SnowballImpactPredictor {
    private static final int MAX_TICKS = 120;
    private static final double DRAG = 0.99;
    private static final double GRAVITY = 0.03;

    private SnowballImpactPredictor() {
    }

    public static Vec3d predict(SnowballEntity snowball) {
        return SnowballImpactPredictor.trace(snowball.getEntityPos(), snowball.getVelocity(), (Entity)snowball);
    }

    public static Vec3d predict(PlayerEntity player) {
        Vec3d start = player.getEyePos().add(player.getRotationVec(1.0f).multiply(0.25));
        Vec3d velocity = player.getRotationVec(1.0f).normalize().multiply(1.5).add(player.getVelocity());
        return SnowballImpactPredictor.trace(start, velocity, (Entity)player);
    }

    private static Vec3d trace(Vec3d start, Vec3d initialVelocity, Entity source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }
        Vec3d position = start;
        Vec3d velocity = initialVelocity;
        for (int tick = 0; tick < 120; ++tick) {
            Vec3d next = position.add(velocity);
            BlockHitResult hit = client.world.raycast(new RaycastContext(position, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source));
            if (hit.getType() != HitResult.Type.MISS) {
                return hit.getPos();
            }
            position = next;
            velocity = velocity.multiply(0.99).add(0.0, -0.03, 0.0);
            if (position.y < (double)(client.world.getBottomY() - 8)) break;
        }
        return null;
    }
}

