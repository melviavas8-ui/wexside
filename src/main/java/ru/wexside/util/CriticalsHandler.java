package ru.wexside.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ru.wexside.misc.RaycastMode;
import ru.wexside.util.VelocityTracker;

public final class CriticalsHandler {
    private int ticksSinceAttack = 99;
    private final VelocityTracker velocityTracker = new VelocityTracker();

    public void onAttack() {
        this.resetAttackAge();
    }

    public void tick() {
        this.advanceAttackAge();
    }

    public void resetAttackAge() {
        this.ticksSinceAttack = 0;
    }

    public boolean passesCriticalRequirement(boolean bl, RaycastMode raycastMode) {
        if (!bl) {
            return true;
        }
        return this.canPerformCriticalAttack(raycastMode);
    }

    public void advanceAttackAge() {
        if (this.ticksSinceAttack < 100) {
            ++this.ticksSinceAttack;
        }
    }

    public VelocityTracker getVelocityTracker() {
        return this.velocityTracker;
    }

    private boolean canAttackInWater(ClientPlayerEntity player2) {
        if (!player2.isSneaking() && !MinecraftClient.getInstance().options.jumpKey.isPressed()) {
            return true;
        }
        boolean bl2 = !player2.isSneaking() && this.isAtFluidSurface(player2) && this.isFeetBlockAir(player2);
        boolean bl = player2.hasVehicle() || player2.isTouchingWater() && !bl2 || player2.getAbilities().flying;
        boolean bl3 = bl;
        if (!bl) {
            return !player2.isOnGround() && player2.fallDistance > 0.0;
        }
        return true;
    }

    private boolean hasOverheadCollision(ClientPlayerEntity player2) {
        ClientWorld world2 = MinecraftClient.getInstance().world;
        if (world2 == null) {
            return false;
        }
        double d = player2.getY() + (player2.isOnGround() ? 2.5 : 1.5);
        Box box = new Box(player2.getX() - 0.3, player2.getY(), player2.getZ() - 0.3, player2.getX() + 0.3, d, player2.getZ() + 0.3);
        return !world2.isSpaceEmpty((Entity)player2, box);
    }

    private boolean isFeetBlockAir(ClientPlayerEntity player2) {
        ClientWorld world2 = MinecraftClient.getInstance().world;
        if (world2 == null) {
            return false;
        }
        BlockPos pos = player2.getBlockPos().toImmutable();
        return world2.getBlockState(pos).isAir();
    }

    public int getTicksSinceAttack() {
        return this.ticksSinceAttack;
    }

    private boolean hasCriticalExemption(ClientPlayerEntity player2) {
        return player2.hasStatusEffect(StatusEffects.BLINDNESS) || player2.hasStatusEffect(StatusEffects.LEVITATION) || player2.hasStatusEffect(StatusEffects.SLOW_FALLING) || player2.isInLava() || player2.isClimbing() || player2.hasVehicle() || this.hasNonzeroVelocity(player2) || player2.getAbilities().flying;
    }

    private boolean hasNonzeroVelocity(ClientPlayerEntity player2) {
        return player2.getVelocity().lengthSquared() > 1.0E-7;
    }

    private boolean isAtFluidSurface(ClientPlayerEntity player2) {
        ClientWorld world2 = MinecraftClient.getInstance().world;
        if (world2 == null) {
            return false;
        }
        BlockPos pos = player2.getBlockPos();
        if (world2.getFluidState(pos).isEmpty()) {
            return false;
        }
        return world2.getFluidState(pos.up()).isEmpty();
    }

    private boolean isFallingWithoutVelocityGuard(ClientPlayerEntity player2) {
        if (player2.isOnGround() || player2.fallDistance <= 0.0) {
            return false;
        }
        return !this.velocityTracker.isHittingGround();
    }

    private boolean isOnFractionalHeightUnderBlock(ClientPlayerEntity player2) {
        if (!player2.isOnGround()) {
            return false;
        }
        double d = player2.getY();
        boolean bl = d != Math.floor(d);
        boolean bl2 = bl;
        if (!bl) {
            return false;
        }
        return this.hasOverheadCollision(player2);
    }

    public boolean canPerformCriticalAttack(RaycastMode raycastMode) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null) {
            return false;
        }
        if (player2.isTouchingWater()) {
            return this.canAttackInWater(player2);
        }
        if (this.hasCriticalExemption(player2)) {
            return true;
        }
        if (this.isOnFractionalHeightUnderBlock(player2)) {
            return true;
        }
        if (raycastMode == RaycastMode.THROUGH_WALLS) {
            boolean bl = MinecraftClient.getInstance().options.togglePerspectiveKey.isPressed();
            boolean bl2 = !player2.isOnGround() && player2.fallDistance > 0.0;
            return player2.isOnGround() && !bl || bl2;
        }
        return this.isFallingWithoutVelocityGuard(player2);
    }
}

