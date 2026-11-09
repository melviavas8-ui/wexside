package ru.wexside.event;

import net.minecraft.block.Block;
import ru.wexside.event.CancellableEvent;
import ru.wexside.event.Event;

public class BlockInteractEvent
extends CancellableEvent
implements Event {
    private final Block block;

    public BlockInteractEvent(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return this.block;
    }
}

