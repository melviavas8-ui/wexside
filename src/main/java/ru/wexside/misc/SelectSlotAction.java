package ru.wexside.misc;

import ru.wexside.misc.InventoryAction;

public record SelectSlotAction(int slot) implements InventoryAction
{
}

