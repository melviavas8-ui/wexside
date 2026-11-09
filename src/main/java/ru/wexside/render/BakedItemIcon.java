package ru.wexside.render;

import net.minecraft.item.ItemStack;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.render.HudIconRenderer;
import ru.wexside.render.IconAtlas;
import ru.wexside.render.IconAtlasEntry;

public final class BakedItemIcon {
    private final IconAtlasEntry texture;
    private ItemStack stack = ItemStack.EMPTY;
    private int frame;
    private long lastUsedNanos;

    public BakedItemIcon(IconAtlas atlas) {
        this.texture = new IconAtlasEntry(atlas);
    }

    public IconAtlasEntry texture() {
        return this.texture;
    }

    public int frame() {
        return this.frame;
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }

    public long lastUsedNanos() {
        return this.lastUsedNanos;
    }

    public void markUsed(long nowNanos) {
        this.lastUsedNanos = nowNanos;
    }

    public void setStack(ItemStack stack) {
        ItemStack safeStack;
        ItemStack class_17992 = safeStack = stack == null ? ItemStack.EMPTY : stack;
        if (!ItemStack.areEqual((ItemStack)this.stack, (ItemStack)safeStack)) {
            this.stack = safeStack.isEmpty() ? ItemStack.EMPTY : safeStack.copy();
            this.texture.invalidateBake();
        }
    }

    public BakedIconEntry createBakeEntry(float scale) {
        if (this.stack.isEmpty() || !this.texture.needsBake(scale)) {
            return null;
        }
        ItemStack renderedStack = this.stack.copy();
        return new BakedIconEntry(this.texture, (context, x, y, size) -> HudIconRenderer.drawItem(context, renderedStack, x, y, size));
    }

    public void close() {
        this.texture.close();
    }
}

