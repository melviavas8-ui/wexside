package ru.wexside.misc;

import ru.wexside.misc.InventoryAction;

public record PickupSlotAction(int slot, int button) implements InventoryAction
{
}

