package ru.wexside.misc;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public final class PlacementPreview {
    private BlockPos blockPos;
    private Box box;

    public PlacementPreview(BlockPos blockPos, Box box) {
        this.update(blockPos, box);
    }

    public void update(BlockPos blockPos, Box box) {
        this.blockPos = blockPos;
        this.box = box;
    }

    public Vec3d center() {
        return this.blockPos == null ? null : Vec3d.ofCenter((Vec3i)this.blockPos);
    }

    public Box box() {
        return this.box;
    }
}

