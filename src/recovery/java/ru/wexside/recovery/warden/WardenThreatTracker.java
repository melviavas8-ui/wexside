package ru.wexside.recovery.warden;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/** IiliIlliIlIill world-query bodies. Call tick/onSound from the original event hooks. */
public final class WardenThreatTracker {
    private final LongSupplier nanoClock;
    private final Map<BlockPos, Integer> rememberedPositions = new HashMap<>();
    private volatile long angrySoundUntil;

    public WardenThreatTracker(LongSupplier nanoClock) {
        this.nanoClock = Objects.requireNonNull(nanoClock);
    }

    public void reset() {
        rememberedPositions.clear();
        angrySoundUntil = 0L;
    }

    public void onSound(PlaySoundS2CPacket packet) {
        String path = packet.getSound().value().id().getPath();
        if (path.contains("warden.roar") || path.contains("warden.angry") || path.contains("warden.sonic")) {
            angrySoundUntil = nanoClock.getAsLong() + 5_000_000_000L;
        }
    }

    public void tick(ClientPlayerEntity player) {
        if (player == null) {
            rememberedPositions.clear();
            return;
        }
        for (WardenEntity warden : player.getEntityWorld().getEntitiesByClass(
                WardenEntity.class, player.getBoundingBox().expand(256.0), entity -> true)) {
            rememberedPositions.put(warden.getBlockPos(), player.age + 100);
        }
        rememberedPositions.values().removeIf(expiry -> player.age > expiry);
    }

    public boolean nearRememberedWarden(BlockPos target) {
        for (BlockPos position : rememberedPositions.keySet()) {
            if (position.getSquaredDistance(target) < 256.0) {
                return true;
            }
        }
        return false;
    }

    private static List<WardenEntity> nearbyWardens(ClientPlayerEntity player) {
        return player.getEntityWorld().getEntitiesByClass(
                WardenEntity.class, player.getBoundingBox().expand(Math.sqrt(900.0)), entity -> true);
    }

    public boolean wardenNearby(ClientPlayerEntity player) {
        if (player == null) {
            return false;
        }
        boolean recentSound = nanoClock.getAsLong() < angrySoundUntil;
        for (WardenEntity warden : nearbyWardens(player)) {
            double squaredDistance = player.squaredDistanceTo(warden);
            if (squaredDistance < 256.0 || recentSound && squaredDistance < 400.0) {
                return true;
            }
        }
        return false;
    }

    public boolean angryWardenApproaching(ClientPlayerEntity player) {
        if (player == null) {
            return false;
        }
        boolean recentSound = nanoClock.getAsLong() < angrySoundUntil;
        for (WardenEntity warden : nearbyWardens(player)) {
            double squaredDistance = player.squaredDistanceTo(warden);
            boolean angryAndNear = recentSound && squaredDistance < 400.0
                    || warden.getAnger() >= 80 && squaredDistance < 225.0;
            if (angryAndNear && facingPlayer(player, warden)
                    && (squaredDistance < 16.0 || movingTowardPlayer(player, warden))) {
                return true;
            }
        }
        return false;
    }

    private static boolean facingPlayer(ClientPlayerEntity player, WardenEntity warden) {
        double targetYaw = Math.toDegrees(Math.atan2(
                -(player.getX() - warden.getX()), player.getZ() - warden.getZ()));
        return Math.abs(MathHelper.wrapDegrees(warden.getBodyYaw() - targetYaw)) < 10.0;
    }

    private static boolean movingTowardPlayer(ClientPlayerEntity player, WardenEntity warden) {
        double movementX = warden.getX() - warden.lastX;
        double movementZ = warden.getZ() - warden.lastZ;
        return (player.getX() - warden.getX()) * movementX
                + (player.getZ() - warden.getZ()) * movementZ > 0.01;
    }

    public double nearestWardenDistance(ClientPlayerEntity player) {
        double nearest = Double.POSITIVE_INFINITY;
        for (WardenEntity warden : player.getEntityWorld().getEntitiesByClass(
                WardenEntity.class, player.getBoundingBox().expand(256.0), entity -> true)) {
            nearest = Math.min(nearest, Math.sqrt(player.squaredDistanceTo(warden)));
        }
        return nearest;
    }
}
