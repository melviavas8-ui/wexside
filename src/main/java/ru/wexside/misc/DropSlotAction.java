package ru.wexside.misc;

import ru.wexside.misc.InventoryAction;

public record DropSlotAction(int slot, boolean entireStack) implements InventoryAction
{
}

