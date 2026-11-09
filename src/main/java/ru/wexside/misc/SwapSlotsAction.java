package ru.wexside.misc;

import ru.wexside.misc.InventoryAction;

public final class SwapSlotsAction
implements InventoryAction {
    private final int fromSlot;
    private final int toSlot;

    public SwapSlotsAction(int fromSlot, int toSlot) {
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
    }

    public int getFromSlot() {
        return this.fromSlot;
    }

    public int getToSlot() {
        return this.toSlot;
    }
}

