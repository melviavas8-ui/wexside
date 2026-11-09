package ru.wexside.event;

import net.minecraft.item.ItemStack;
import ru.wexside.event.CancellableEvent;
import ru.wexside.event.Event;

public class ItemHoverEvent
extends CancellableEvent
implements Event {
    private final ItemStack stack;

    public ItemHoverEvent(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack getStack() {
        return this.stack;
    }
}

