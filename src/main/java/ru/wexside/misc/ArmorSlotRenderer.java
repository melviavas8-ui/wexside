package ru.wexside.misc;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.ItemStackRenderer;
import ru.wexside.misc.ThemeColors;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class ArmorSlotRenderer {
    private final int slot = ColorUtils.rgba(200, 0, 0, 110);
    private final float value3;
    private final int slot2;
    private ItemStack stack;
    private final float value4;
    private final float value5;
    private final EquipmentSlot equipmentSlot;
    private final int slot3;
    private final float value6;
    private final ItemStackRenderer itemStackRenderer;
    private final int slot4 = ColorUtils.rgba(0, 0, 0, 150);
    private final float value7;

    public ArmorSlotRenderer(EquipmentSlot iliiIIiliI2) {
        this.value5 = 8.0f;
        this.value3 = 0.1f;
        this.value6 = 2.0f;
        this.value4 = 1.0f;
        this.value7 = 2.0f;
        this.slot2 = ColorUtils.rgba(0, 200, 0, 255);
        this.slot3 = ColorUtils.rgba(200, 0, 0, 255);
        this.itemStackRenderer = new ItemStackRenderer();
        this.stack = ItemStack.EMPTY;
        this.equipmentSlot = iliiIIiliI2;
    }

    private void renderDurabilityBar(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5) {
        float f6 = this.getDurabilityFraction();
        if (f6 >= 1.0f) {
            return;
        }
        float f7 = f + 2.0f * f5;
        float f8 = f2 + f4 - 3.0f * f5;
        float f9 = f3 - 4.0f * f5;
        float f10 = 1.0f * f5;
        drawApi.fillRectangle(matrix4f, f7, f8, f9, f10, this.slot4);
        drawApi.fillRectangle(matrix4f, f7, f8, f9 * f6, f10, ColorUtils.lerp(this.slot3, this.slot2, f6));
    }

    private boolean isActive() {
        return System.currentTimeMillis() % 1000L < 500L;
    }

    public void renderSlot(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3) {
        float f4 = this.itemStackRenderer.getWidth() * f3;
        float f5 = this.itemStackRenderer.getHeight() * f3;
        drawApi.drawRoundedRectangle(matrix4f, f, f2, f4, f5, 8.0f * f3, ThemeColors.visualizerSlot());
        if (this.isActive3() && this.isActive()) {
            drawApi.drawRoundedRectangle(matrix4f, f, f2, f4, f5, 8.0f * f3, this.slot);
        }
        this.itemStackRenderer.renderItemSlot(drawApi, matrix4f, f, f2, f3);
        if (this.isActive2()) {
            this.renderDurabilityBar(drawApi, matrix4f, f, f2, f4, f5, f3);
        }
    }

    public float getHeight() {
        return this.itemStackRenderer.getHeight();
    }

    public float getWidth() {
        return this.itemStackRenderer.getWidth();
    }

    public BakedIconEntry createIconBakeEntry(float f) {
        return this.itemStackRenderer.createIconBakeEntry(f);
    }

    private boolean isActive2() {
        return !this.stack.isEmpty() && this.stack.getMaxDamage() > 0;
    }

    public boolean hasEquippedItem(ClientPlayerEntity player) {
        return player != null && !player.getEquippedStack(this.equipmentSlot).isEmpty();
    }

    private float getDurabilityFraction() {
        int n = this.stack.getMaxDamage();
        if (n <= 0) {
            return 1.0f;
        }
        return (float)(n - this.stack.getDamage()) / (float)n;
    }

    private boolean isActive3() {
        return this.isActive2() && this.getDurabilityFraction() < 0.1f;
    }

    public void updateEquipment(ClientPlayerEntity player) {
        this.stack = player == null ? ItemStack.EMPTY : player.getEquippedStack(this.equipmentSlot);
        this.itemStackRenderer.setStack(this.stack);
    }
}

