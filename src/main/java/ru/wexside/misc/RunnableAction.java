package ru.wexside.misc;

import ru.wexside.misc.InventoryAction;

public final class RunnableAction
implements InventoryAction {
    public final Runnable runnable;

    public RunnableAction(Runnable runnable) {
        this.runnable = runnable;
    }

    public Runnable runnable() {
        return this.runnable;
    }
}

