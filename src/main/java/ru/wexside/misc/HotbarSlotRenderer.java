package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.module.hud.HealthHelperModule;
import ru.wexside.render.BakedItemIcon;
import ru.wexside.render.ItemIconCache;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class HotbarSlotRenderer {
    private static final int STENCIL_MASK_COLOR = -1;
    static final float value = 128.0f;
    static final float value2 = 51.0f;
    static final float value3 = 30.0f;
    private static final int SELECTION_BACKGROUND_COLOR = 0x26FFFFFF;
    private float value4;
    private float value5;
    static final float value6 = 20.0f;
    static final float value7 = 89.0f;
    private float value8;
    private static final int SELECTION_ACCENT_COLOR = -1;
    private static final int COOLDOWN_OVERLAY_COLOR = -16777216;
    private static final int USE_PROGRESS_COLOR = -1;
    private float value9;
    private float value10;
    static final float value11 = 200.0f;
    private int slot6;
    private final int slot7;
    static final float value12 = 102.0f;
    private static final int STACK_COUNT_COLOR = -1;
    static final float process = 76.0f;
    private float value13;

    public HotbarSlotRenderer(int n) {
        this.slot7 = n;
    }

    public void renderSlot(GuiDrawApi drawApi, Matrix4f matrix4f, ItemIconCache itemIconCache, BakedItemIcon iiIlilllII2, ItemStack stack, float f, float f2, float f3, float f4, float f5, boolean bl, boolean bl2) {
        int n;
        int n2 = n = stack == null || stack.isEmpty() ? 0 : HealthHelperModule.getItemHighlightColor(stack);
        if (n != 0) {
            this.slot6 = n;
        }
        this.value5 = FrameInterpolator.lerpTowards(this.value5, bl ? 1.0f : 0.0f, 30.0f);
        this.value9 = FrameInterpolator.lerpTowards(this.value9, n != 0 && !bl ? 1.0f : 0.0f, 30.0f);
        this.value13 = FrameInterpolator.lerpTowards(this.value13, this.updateCooldownProgress(stack) ? 1.0f : 0.0f, 20.0f);
        this.value4 = FrameInterpolator.lerpTowards(this.value4, this.updateUseProgress(stack, bl2) ? 1.0f : 0.0f, 20.0f);
        this.renderSelectionBackground(drawApi, matrix4f, f, f2, f3, f5);
        this.renderProgressOverlay(drawApi, matrix4f, f, f2, f3, f5, this.value10, this.value13, -16777216, 89.0f);
        this.renderProgressOverlay(drawApi, matrix4f, f, f2, f3, f5, this.value8, this.value4, -1, 128.0f);
        if (stack == null || stack.isEmpty()) {
            return;
        }
        float f6 = f + (f3 - f4) / 2.0f;
        float f7 = f2 + (f3 - f4) / 2.0f;
        itemIconCache.drawBakedIcon(drawApi, matrix4f, iiIlilllII2, f6, f7, f4);
        this.renderDurabilityBar(drawApi, matrix4f, stack, f6, f7, f4);
        this.renderStackCount(drawApi, matrix4f, stack, f6, f7, f4);
    }

    private boolean updateCooldownProgress(ItemStack stack) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null || stack == null || stack.isEmpty() || !player2.getItemCooldownManager().isCoolingDown(stack)) {
            this.value10 = 0.0f;
            return false;
        }
        this.value10 = player2.getItemCooldownManager().getCooldownProgress(stack, MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false));
        return true;
    }

    private void renderProgressOverlay(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int n, float f7) {
        if (f6 <= 0.001f) {
            return;
        }
        drawApi.drawRoundedRectangle(matrix4f, f, f2, f3, f3, f4, ColorUtils.withAlpha(n, 51.0f * f6));
        if (f5 > 0.0f) {
            float f8 = f3 * f5;
            drawApi.beginStencil(this.slot7);
            drawApi.fillRectangle(matrix4f, f, f2 + f3 - f8, f3, f8, -1);
            drawApi.applyStencilMask(this.slot7);
            drawApi.drawRoundedRectangle(matrix4f, f, f2, f3, f3, f4, ColorUtils.withAlpha(n, 76.0f * f6));
            drawApi.endStencil();
        }
        drawApi.drawRoundedOutline(matrix4f, f, f2, f3, f3, f4, f4 / 9.0f, ColorUtils.withAlpha(n, f7 * f6));
    }

    private void renderSlotGlow(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n, int n2, boolean bl) {
        drawApi.drawSplitGradientRectangle(matrix4f, f - f3, f2, f3 * 1.5f, f3, 0, n, 0, 2);
        drawApi.drawSplitGradientRectangle(matrix4f, f + f3 * 0.5f, f2, f3 * 1.5f, f3, 0, 0, n, 1);
        if (bl) {
            drawApi.drawGradientRoundedRectangleUniform(matrix4f, f, f2, f3, f3, f4, f4 / 9.0f, n2, n2, n2, n2);
        } else {
            drawApi.drawGradientRoundedRectangleUniform(matrix4f, f, f2, f3, f3, f4, f4 / 9.0f, n2, n2, 0, 0);
        }
    }

    private void renderDurabilityBar(GuiDrawApi drawApi, Matrix4f matrix4f, ItemStack stack, float f, float f2, float f3) {
        if (!stack.isDamageable() || stack.getDamage() <= 0) {
            return;
        }
        float f4 = 1.0f - (float)stack.getDamage() / (float)stack.getMaxDamage();
        float f5 = f3;
        float f6 = f2 + f3 - 1.0f;
        drawApi.fillRectangle(matrix4f, f, f6, f5, 1.0f, ColorUtils.rgba(0, 0, 0, 200));
        int n = ColorUtils.lerp(ColorUtils.rgb(230, 40, 40), ColorUtils.rgb(70, 220, 70), f4);
        drawApi.fillRectangle(matrix4f, f, f6, f5 * f4, 1.0f, n);
    }

    private void renderSelectionBackground(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4) {
        boolean bl;
        boolean bl2 = this.value9 > 0.001f && this.slot6 != 0;
        boolean bl3 = bl = this.value5 > 0.001f;
        if (!bl2 && !bl) {
            return;
        }
        drawApi.beginStencil(this.slot7);
        drawApi.drawRoundedRectangle(matrix4f, f, f2, f3, f3, f4, -1);
        drawApi.applyStencilMask(this.slot7);
        if (bl2) {
            float f5 = (float)ColorUtils.unpackRgba(this.slot6)[3] / 255.0f * this.value9;
            this.renderSlotGlow(drawApi, matrix4f, f, f2, f3, f4, ColorUtils.withAlpha(this.slot6, 102.0f * f5), ColorUtils.withAlpha(this.slot6, 200.0f * f5), true);
        }
        if (bl) {
            drawApi.drawRoundedRectangle(matrix4f, f, f2, f3, f3, f4, ColorUtils.multiplyAlpha(0x26FFFFFF, this.value5));
            this.renderSlotGlow(drawApi, matrix4f, f, f2, f3, f4, ColorUtils.withAlpha(-1, 102.0f * this.value5), ColorUtils.withAlpha(-1, 200.0f * this.value5), false);
        }
        drawApi.endStencil();
    }

    private boolean updateUseProgress(ItemStack stack, boolean bl) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null || !bl || stack == null || stack.isEmpty()) {
            this.value8 = 0.0f;
            return false;
        }
        int n = player2.getActiveItem().getMaxUseTime((LivingEntity)player2);
        this.value8 = n > 0 ? (float)player2.getItemUseTime() / (float)n : 0.0f;
        return true;
    }

    private void renderStackCount(GuiDrawApi drawApi, Matrix4f matrix4f, ItemStack stack, float f, float f2, float f3) {
        if (stack.getCount() <= 1) {
            return;
        }
        String string = String.valueOf(stack.getCount());
        float f4 = f3 * 0.42f;
        float f5 = FontRegistry.font6.textWidth(string, f4);
        float f6 = FontRegistry.font6.textHeight(string, f4);
        FontRegistry.font6.drawText(matrix4f, drawApi, string, f + f3 - f5, f2 + f3 - f6, f4, -1);
    }
}

