package ru.wexside.misc;

import java.util.function.Predicate;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import ru.wexside.misc.BundleUse;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.SwapTiming;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.util.InventoryController;

public final class Bundles {
    private Bundles() {
    }

    public static boolean contains(PlayerInventory inventory, Predicate<ItemStack> predicate) {
        if (inventory == null) {
            return false;
        }
        for (int slot = 0; slot < inventory.size(); ++slot) {
            if (!predicate.test(inventory.getStack(slot))) continue;
            return true;
        }
        return false;
    }

    public static int[] findInBundle(PlayerInventory inventory, Predicate<ItemStack> predicate) {
        if (inventory == null) {
            return null;
        }
        for (int slot = 0; slot < inventory.size(); ++slot) {
            if (!predicate.test(inventory.getStack(slot))) continue;
            return new int[]{slot, 0};
        }
        return null;
    }

    public static void useFromBundle(ClientPlayerEntity player, InventoryController inventory, String owner, int slot, int nestedSlot, boolean funtime, Runnable use) {
        if (inventory == null || player == null) {
            return;
        }
        inventory.submit(InventoryTask.builder().action(inventory.createSwapUseRestore(slot < 9 ? slot + 36 : slot, player.getInventory().getSelectedSlot(), use, funtime ? SwapTiming.FUNTIME : SwapTiming.DEFAULT)).owner(owner).flag(TaskFlag.DEFAULT).policy(ClickPolicy.SILENT).priority(TaskPriority.NORMAL).build());
    }

    public static boolean useFromBundle(ClientPlayerEntity player, InventoryController inventory, String owner, int slot, int nestedSlot, int destinationSlot, boolean funtime) {
        Bundles.useFromBundle(player, inventory, owner, slot, nestedSlot, funtime, inventory::pressUseForTick);
        return true;
    }

    public static BundleUse useFromBundle(ClientPlayerEntity player, InventoryController inventory, String owner, int slot, int nestedSlot, boolean funtime) {
        Bundles.useFromBundle(player, inventory, owner, slot, nestedSlot, funtime, inventory::pressUseForTick);
        return new BundleUse(slot);
    }
}

