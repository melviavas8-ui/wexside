package ru.wexside.recovery.warden;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Exact IlIiIlliIlIlIil/lililllillIllil bounds; these are retained constants, not configuration guesses. */
public final class WardenFarmRegion {
    public static final int MIN_X = -2070;
    public static final int MAX_X = -1921;
    public static final int MIN_Z = -2076;
    public static final int MAX_Z = -1929;
    public static final int MIN_Y = -60;
    public static final int MAX_Y = -35;

    private WardenFarmRegion() {}

    public static boolean containsHorizontal(BlockPos position) {
        return position.getX() >= MIN_X && position.getX() <= MAX_X
                && position.getZ() >= MIN_Z && position.getZ() <= MAX_Z;
    }

    public static boolean contains(ClientPlayerEntity player) {
        return player != null && player.getEntityWorld().getRegistryKey() == World.OVERWORLD
                && player.getX() >= MIN_X && player.getX() <= MAX_X
                && player.getZ() >= MIN_Z && player.getZ() <= MAX_Z;
    }

    public static boolean containsVolume(BlockPos position) {
        return containsHorizontal(position) && position.getY() >= MIN_Y && position.getY() <= MAX_Y;
    }

    public static int clampX(int value, int inset) {
        return Math.max(MIN_X + inset, Math.min(MAX_X - inset, value));
    }

    public static int clampZ(int value, int inset) {
        return Math.max(MIN_Z + inset, Math.min(MAX_Z - inset, value));
    }
}
