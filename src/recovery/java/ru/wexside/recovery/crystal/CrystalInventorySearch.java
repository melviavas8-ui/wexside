package ru.wexside.recovery.crystal;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;

/** Exact item lookup route lillllIlIiIlIil.lliiiIIIl(item,1) through original scattered helpers. */
public final class CrystalInventorySearch {
    private CrystalInventorySearch() {}

    public static int findFirstMainInventorySlot(PlayerInventory inventory, Item item) {
        if (inventory == null) {
            return -1;
        }
        for (int slot = 0; slot < 36; slot++) {
            if (inventory.getStack(slot).getItem() == item) {
                return slot;
            }
        }
        return -1;
    }
}
