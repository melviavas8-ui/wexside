package ru.wexside.render;

import net.minecraft.item.ItemStack;

public final class ItemIconKey {
    private final ItemStack stack;

    public ItemIconKey(ItemStack stack) {
        this.stack = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object object) {
        if (!(object instanceof ItemIconKey)) return false;
        ItemIconKey other = (ItemIconKey)object;
        if (!ItemStack.areEqual((ItemStack)this.stack, (ItemStack)other.stack)) return false;
        return true;
    }

    public int hashCode() {
        return 31 * System.identityHashCode(this.stack.getItem()) + this.stack.getComponents().hashCode();
    }
}

