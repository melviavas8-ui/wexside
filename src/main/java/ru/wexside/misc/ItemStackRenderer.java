package ru.wexside.misc;

import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.ThemeColors;
import ru.wexside.render.HudIconRenderer;
import ru.wexside.render.IconAtlasEntry;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class ItemStackRenderer {
    private final int slot = ColorUtils.rgba(255, 255, 255, 255);
    private final float value3;
    private final float value4;
    private ItemStack stack;
    private ItemStack stack2;
    private final IconAtlasEntry iconEntry = new IconAtlasEntry();
    private final float process2;
    private final float value5;
    private final float value6;
    private final float value7;

    public ItemStackRenderer() {
        this.process2 = 19.5f;
        this.value6 = 19.0f;
        this.value5 = 8.0f;
        this.value4 = 1.0f;
        this.value7 = 10.0f;
        this.value3 = 5.0f;
        this.stack = ItemStack.EMPTY;
        this.stack2 = ItemStack.EMPTY;
    }

    public void renderItemSlot(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3) {
        int n;
        float f4;
        float f5 = 19.5f * f3;
        float f6 = 19.0f * f3;
        drawApi.drawRoundedOutline(matrix4f, f, f2, f5, f6, 8.0f * f3, 1.0f * f3, ThemeColors.notificationOutline());
        if (this.stack.isEmpty()) {
            return;
        }
        if (this.iconEntry.isAllocated()) {
            float f7 = 10.0f * f3;
            float f8 = f + (f5 - f7) / 2.0f;
            f4 = f2 + (f6 - f7) / 2.0f;
            int n2 = drawApi.bindTexture(this.iconEntry.getTextureId(), this.iconEntry.getIconSize(), this.iconEntry.getIconSize());
            drawApi.drawTexture(matrix4f, f8, f4, f7, f7, 0.0f, 1.0f, 1.0f, 0.0f, n2, -1);
        }
        if ((n = this.stack.getCount()) > 1) {
            String string = String.valueOf(n);
            f4 = FontRegistry.font6.textWidth(string, 5.0f);
            float f9 = FontRegistry.font6.textHeight(string, 5.0f);
            FontRegistry.font6.drawText(matrix4f, drawApi, string, f + f5 - f4 * f3 - 2.0f * f3, f2 + f6 - f9 * f3 - 1.5f * f3, 5.0f * f3, this.slot);
        }
    }

    public void setStack(ItemStack stack3) {
        this.stack = stack3;
        if (!ItemStack.areEqual((ItemStack)stack3, (ItemStack)this.stack2)) {
            this.stack2 = stack3.isEmpty() ? ItemStack.EMPTY : stack3.copy();
            this.iconEntry.invalidateBake();
        }
    }

    public float getHeight() {
        return 19.0f;
    }

    public float getWidth() {
        return 19.5f;
    }

    public BakedIconEntry createIconBakeEntry(float f) {
        if (this.stack2.isEmpty()) {
            return null;
        }
        if (!this.iconEntry.needsBake(f)) {
            return null;
        }
        ItemStack stack3 = this.stack2;
        return new BakedIconEntry(this.iconEntry, (trimToWidth, n, n2, n3) -> HudIconRenderer.drawItem(trimToWidth, stack3, n, n2, n3));
    }

    public void update() {
        this.iconEntry.close();
    }
}

