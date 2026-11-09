package ru.wexside.recovery.crystal;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import ru.wexside.event.CancellableEvent;

/** Original IlIllillIiIiIil retains both block and position (no defensive copy). */
public final class CrystalBlockInteractEvent extends CancellableEvent {
    private final Block block;
    private final BlockPos position;

    public CrystalBlockInteractEvent(Block block, BlockPos position) {
        this.block = block;
        this.position = position;
    }

    public Block getBlock() { return this.block; }
    public BlockPos getPosition() { return this.position; }
}
