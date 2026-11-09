package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;

public final class CritCondition {
    public boolean canAttackUnderCriticalRules(boolean criticalsOnly, boolean jumpOnly, boolean waterCriticals) {
        boolean falling;
        if (!criticalsOnly) {
            return true;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }
        if (this.isForcedCrit(player)) {
            return true;
        }
        boolean bl = falling = !player.isOnGround() && player.fallDistance > 0.0;
        if (player.isTouchingWater()) {
            boolean waterOk = waterCriticals && !player.isSwimming() && this.hasWaterAbove(player) && this.feetInAir(player);
            return waterOk ? falling : true;
        }
        if (jumpOnly) {
            boolean jumping;
            boolean bl2 = jumping = MinecraftClient.getInstance().options.jumpKey.isPressed() || !player.isOnGround();
            if (!jumping) {
                return false;
            }
        }
        return falling;
    }

    private boolean hasWaterAbove(ClientPlayerEntity player) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return false;
        }
        return !world.getFluidState(player.getBlockPos().up()).isEmpty();
    }

    private boolean feetInAir(ClientPlayerEntity player) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return false;
        }
        BlockPos pos = player.getBlockPos().toImmutable();
        return world.getBlockState(pos).isAir();
    }

    private boolean isForcedCrit(ClientPlayerEntity player) {
        return player.isClimbing() || player.getAbilities().flying || player.hasStatusEffect(StatusEffects.BLINDNESS) || player.hasStatusEffect(StatusEffects.SLOW_FALLING) || player.isSubmergedInWater() || player.hasVehicle() || player.isGliding();
    }
}

