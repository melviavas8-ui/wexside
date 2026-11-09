package ru.wexside.misc;

import ru.wexside.misc.InventoryAction;

public final class ClickSlotAction
implements InventoryAction {
    public final int slot;
    public final int button;

    public ClickSlotAction(int slot, int button) {
        this.slot = slot;
        this.button = button;
    }

    public int slot() {
        return this.slot;
    }

    public int hotbarButton() {
        return this.button;
    }
}

