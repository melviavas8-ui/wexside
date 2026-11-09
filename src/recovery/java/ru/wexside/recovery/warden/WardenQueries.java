package ru.wexside.recovery.warden;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Body-preserving query extraction from llIillIillliIl; see method-evidence.json. */
public final class WardenQueries {
    private WardenQueries() {}

    public static boolean moving(ClientPlayerEntity player) {
        return player.getVelocity().horizontalLengthSquared() > 0.0025;
    }

    public static List<Block> candles() {
        List<Block> result = new ArrayList<>();
        for (Block block : Registries.BLOCK) {
            if (block.getDefaultState().isIn(BlockTags.CANDLES)) {
                result.add(block);
            }
        }
        return result;
    }

    public static boolean hasOtherPlayerWithin(ClientWorld world, ClientPlayerEntity self, double radius) {
        for (Entity entity : world.getEntities()) {
            if (entity instanceof PlayerEntity other && other != self
                    && self.squaredDistanceTo(other) < radius * radius) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasOtherPlayerNearBlock(ClientWorld world, ClientPlayerEntity self,
            BlockPos target, double radius) {
        Vec3d center = Vec3d.ofCenter(target);
        for (Entity entity : world.getEntities()) {
            if (entity instanceof PlayerEntity other && other != self
                    && other.getEntityPos().squaredDistanceTo(center) < radius * radius) {
                return true;
            }
        }
        return false;
    }

    public static boolean glowingNearAnotherPlayer(ClientWorld world, ClientPlayerEntity self) {
        return self.hasStatusEffect(StatusEffects.GLOWING) && hasOtherPlayerWithin(world, self, 32.0);
    }

    public static boolean intersectsNonAirBlock(ClientWorld world, ClientPlayerEntity player) {
        Box box = player.getBoundingBox().expand(0.05, 0.0, 0.05);
        for (BlockPos pos : BlockPos.iterate(
                BlockPos.ofFloored(box.minX, box.minY, box.minZ),
                BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ))) {
            if (!world.getBlockState(pos).isAir()) {
                return true;
            }
        }
        return false;
    }

    public static int firstEmptyMainInventorySlot(PlayerInventory inventory) {
        for (int slot = 9; slot < 36; slot++) {
            if (inventory.getStack(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /** Retains original parseInt overflow behavior rather than inventing a fallback. */
    public static int parseFarmAnarchy(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty() || !value.chars().allMatch(Character::isDigit)) {
            return -1;
        }
        return Integer.parseInt(value);
    }

    /** Inputs come from original consumers; no setting/default value is supplied here. */
    public static String escapeReason(boolean atHome, boolean hitByWarden, boolean insideFarmRegion,
            float health, int lootScore, IntUnaryOperator scaleThreshold,
            boolean glowingNearPlayer) {
        if (atHome) {
            return null;
        }
        if (hitByWarden) {
            return "ударил варден, домой";
        }
        if (!insideFarmRegion) {
            return null;
        }
        if (health <= 8.0f && lootScore >= scaleThreshold.applyAsInt(6)) {
            return "здоровье " + (int) health + ", лут " + lootScore;
        }
        if (glowingNearPlayer) {
            return null;
        }
        if (lootScore >= scaleThreshold.applyAsInt(12)) {
            return "ценность лута " + lootScore + ", на сдачу";
        }
        return null;
    }

    public static BlockPos orbitChest(int playerAge, BlockPos chest) {
        double angle = (double) (playerAge / 40) * 2.4;
        int x = chest.getX() + (int) (Math.cos(angle) * 10.0);
        int z = chest.getZ() + (int) (Math.sin(angle) * 10.0);
        return new BlockPos(WardenFarmRegion.clampX(x, 0), chest.getY(), WardenFarmRegion.clampZ(z, 0));
    }
}
