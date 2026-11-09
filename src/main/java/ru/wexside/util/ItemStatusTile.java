package ru.wexside.util;

import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.ItemBindBox;
import ru.wexside.misc.ThemeColors;
import ru.wexside.render.HudIconRenderer;
import ru.wexside.render.IconAtlasEntry;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class ItemStatusTile {
    private final float value;
    private int cachedItemCount = -1;
    private final float value2;
    static final long member8972 = 50000000L;
    private final float value3;
    private final ItemStack cooldownStack;
    private boolean visible;
    private float cooldownVisibility;
    private final float value5;
    private final float value6;
    private final float value7;
    private final BooleanSupplier booleanSupplier;
    private final int missingColor;
    private float useProgress;
    private float cooldownProgress;
    private int itemCount;
    private final float value10;
    private final float value11;
    private final float value12;
    private final float value13;
    private final float value14;
    private final int textColor = ColorUtils.rgba(227, 227, 227, 255);
    private float useVisibility;
    private final IconAtlasEntry renderPipeline10;
    private final int useColor;
    private ItemStack displayedStack;
    private final float value16;
    private final ItemBindBox itemBindBox;
    private final float value17;
    private final int slot6;
    private final int cooldownColor = ColorUtils.rgba(255, 204, 94, 255);
    private final float value18;
    private long lastCountScanNanos;
    private float missingVisibility;
    private final float value20;

    public ItemStatusTile(ItemBindBox itemBindBox, BooleanSupplier booleanSupplier) {
        this.value13 = 21.0f;
        this.value10 = 10.0f;
        this.value5 = 8.0f;
        this.value2 = 1.5f;
        this.value17 = 4.5f;
        this.value6 = 1.5f;
        this.value16 = 4.0f;
        this.value20 = 20.0f;
        this.value = 0.2f;
        this.value7 = 0.3f;
        this.value11 = 1.0f;
        this.value18 = 0.1f;
        this.value12 = 0.35f;
        this.value14 = 0.5f;
        this.value3 = 0.3f;
        this.slot6 = 1;
        this.useColor = ColorUtils.rgba(76, 167, 101, 255);
        this.missingColor = ColorUtils.rgba(255, 82, 82, 255);
        this.renderPipeline10 = new IconAtlasEntry();
        this.itemBindBox = itemBindBox;
        this.displayedStack = this.cooldownStack = new ItemStack((ItemConvertible)itemBindBox.getItem());
        this.booleanSupplier = booleanSupplier;
    }

    private int getBackgroundColor() {
        int n = ThemeColors.visualizerSlot();
        float f = 51.0f;
        if (this.cooldownVisibility > 0.001f) {
            n = ColorUtils.lerp(n, ColorUtils.withAlpha(this.cooldownColor, f), this.cooldownVisibility);
        }
        if (this.useVisibility > 0.001f) {
            n = ColorUtils.lerp(n, ColorUtils.withAlpha(this.useColor, f), this.useVisibility);
        }
        if (this.missingVisibility > 0.001f) {
            n = ColorUtils.lerp(n, ColorUtils.withAlpha(this.missingColor, f), this.missingVisibility);
        }
        return n;
    }

    public void update() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player2 = mc.player;
        if (player2 == null) {
            this.resetTileState();
            return;
        }
        this.itemCount = this.cachedMatchingItemCount(player2);
        boolean bl = this.itemCount > 0;
        boolean bl2 = mc.currentScreen instanceof InventoryScreen;
        this.visible = this.itemBindBox.isActive() && (bl2 || bl || this.booleanSupplier.getAsBoolean());
        boolean bl3 = this.updateCooldownProgress(mc, player2);
        boolean bl4 = this.updateItemUseProgress(player2);
        boolean bl5 = !bl && !bl2;
        this.cooldownVisibility = FrameInterpolator.lerpTowards(this.cooldownVisibility, bl3 ? 1.0f : 0.0f, 20.0f);
        this.useVisibility = FrameInterpolator.lerpTowards(this.useVisibility, bl4 ? 1.0f : 0.0f, 20.0f);
        this.missingVisibility = FrameInterpolator.lerpTowards(this.missingVisibility, bl5 ? 1.0f : 0.0f, 20.0f);
    }

    public boolean isActive() {
        return this.visible;
    }

    public void renderTile(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3) {
        float f4 = 21.0f * f3;
        float f5 = 10.0f * f3;
        drawApi.drawRoundedRectangle(matrix4f, f, f2, f4, f4, f5, this.getBackgroundColor());
        if (this.cooldownVisibility > 0.001f && this.cooldownProgress > 0.0f) {
            this.renderProgressFill(drawApi, matrix4f, f, f2, f4, f5, this.cooldownProgress, ColorUtils.withAlpha(this.cooldownColor, 76.5f * this.cooldownVisibility));
        }
        if (this.useVisibility > 0.001f && this.useProgress > 0.0f) {
            this.renderProgressFill(drawApi, matrix4f, f, f2, f4, f5, this.useProgress, ColorUtils.withAlpha(this.useColor, 76.5f * this.useVisibility));
        }
        drawApi.drawRoundedOutline(matrix4f, f, f2, f4, f4, f5, 1.0f * f3, this.getIntType());
        this.renderTileContents(drawApi, matrix4f, f, f2, f3, f4);
    }

    public BakedIconEntry bakeIcon(float f) {
        if (!this.renderPipeline10.needsBake(f)) {
            return null;
        }
        ItemStack stack = this.displayedStack;
        return new BakedIconEntry(this.renderPipeline10, (trimToWidth, n, n2, n3) -> HudIconRenderer.drawItem(trimToWidth, stack, n, n2, n3));
    }

    private int countMatchingItems(ClientPlayerEntity player2) {
        int n = 0;
        ItemStack stack = null;
        for (int i = 0; i < player2.getInventory().size(); ++i) {
            ItemStack stack2 = player2.getInventory().getStack(i);
            if (stack2.isEmpty() || !this.itemBindBox.getPredicate().test(stack2)) continue;
            n += stack2.getCount();
            if (stack != null) continue;
            stack = stack2;
        }
        this.setStack(stack);
        return n;
    }

    private boolean updateItemUseProgress(ClientPlayerEntity player2) {
        boolean bl;
        boolean bl2 = bl = player2.isUsingItem() && this.itemBindBox.getPredicate().test(player2.getActiveItem());
        if (bl) {
            int n = player2.getItemUseTime();
            int n2 = player2.getActiveItem().getMaxUseTime((LivingEntity)player2);
            this.useProgress = n2 > 0 ? (float)n / (float)n2 : 0.0f;
        } else {
            this.useProgress = 0.0f;
        }
        return bl;
    }

    private void setStack(ItemStack stack) {
        ItemStack stack2;
        ItemStack stack3 = stack2 = stack != null ? stack : this.cooldownStack;
        if (ItemStack.areEqual((ItemStack)stack2, (ItemStack)this.displayedStack)) {
            return;
        }
        this.displayedStack = stack != null ? stack.copy() : this.cooldownStack;
        this.renderPipeline10.invalidateBake();
    }

    public float getFloatType() {
        return 21.0f;
    }

    private boolean updateCooldownProgress(MinecraftClient mc, ClientPlayerEntity player2) {
        ItemCooldownManager cooldownManager = player2.getItemCooldownManager();
        boolean bl = cooldownManager.isCoolingDown(this.cooldownStack);
        if (bl) {
            float f = mc.getRenderTickCounter().getTickProgress(false);
            this.cooldownProgress = cooldownManager.getCooldownProgress(this.cooldownStack, f);
        } else {
            this.cooldownProgress = 0.0f;
        }
        return bl;
    }

    private void renderProgressFill(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int n) {
        float f6 = f5 * f3;
        float f7 = f2 + f3 - f6;
        drawApi.beginStencil(1);
        drawApi.fillRectangle(matrix4f, f, f7, f3, f6, -1);
        drawApi.applyStencilMask(1);
        drawApi.drawRoundedRectangle(matrix4f, f, f2, f3, f3, f4, n);
        drawApi.endStencil();
    }

    private float fitLabelFontSize(String string) {
        float f = 18.0f;
        float f2 = FontRegistry.font6.textWidth(string, 4.5f);
        if (f <= 0.0f || f2 <= f) {
            return 4.5f;
        }
        return Math.max(3.0f, 4.5f * f / f2);
    }

    private void renderTileContents(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4) {
        int n;
        boolean bl = MinecraftClient.getInstance().currentScreen instanceof InventoryScreen;
        boolean bl2 = this.itemBindBox.isActive2();
        boolean bl3 = bl && !bl2 && this.itemCount == 0;
        int n2 = n = bl3 ? 1 : this.itemCount;
        String string = bl2 ? this.itemBindBox.getBindSetting().getKeyDisplayName() : (bl3 ? "PREVIEW" : "None");
        float f5 = FontRegistry.font6.textHeight(string, 4.5f);
        float f6 = 9.5f + f5;
        float f7 = (21.0f - f6) / 2.0f;
        float f8 = 8.0f * f3;
        float f9 = f + 6.5f * f3;
        float f10 = f2 + f7 * f3;
        if (this.renderPipeline10.isAllocated()) {
            int n3 = drawApi.bindTexture(this.renderPipeline10.getTextureId(), this.renderPipeline10.getIconSize(), this.renderPipeline10.getIconSize());
            drawApi.drawTexture(matrix4f, f9, f10, f8, f8, 0.0f, 1.0f, 1.0f, 0.0f, n3, -1);
        }
        String string2 = String.valueOf(n);
        float f11 = FontRegistry.font6.textWidth(string2, 4.0f);
        float f12 = FontRegistry.font6.textHeight(string2, 4.0f);
        FontRegistry.font6.drawText(matrix4f, drawApi, string2, f9 + f8 - f11 * f3, f10 + f8 - f12 * f3, 4.0f * f3, this.textColor);
        float f13 = this.fitLabelFontSize(string);
        float f14 = FontRegistry.font6.textWidth(string, f13);
        float f15 = f + (f4 - f14 * f3) / 2.0f;
        float f16 = f10 + f8 + 1.5f * f3;
        FontRegistry.font6.drawText(matrix4f, drawApi, string, f15, f16, f13 * f3, this.textColor);
    }

    private int getIntType() {
        int n = ColorUtils.rgba(255, 255, 255, 25);
        if (this.cooldownVisibility > 0.001f) {
            n = ColorUtils.lerp(n, ColorUtils.withAlpha(this.cooldownColor, 89.25f), this.cooldownVisibility);
        }
        if (this.useVisibility > 0.001f) {
            n = ColorUtils.lerp(n, ColorUtils.withAlpha(this.useColor, 127.5f), this.useVisibility);
        }
        if (this.missingVisibility > 0.001f) {
            n = ColorUtils.lerp(n, ColorUtils.withAlpha(this.missingColor, 76.5f), this.missingVisibility);
        }
        return n;
    }

    private void resetTileState() {
        this.missingVisibility = 0.0f;
        this.useVisibility = 0.0f;
        this.cooldownVisibility = 0.0f;
        this.useProgress = 0.0f;
        this.cooldownProgress = 0.0f;
        this.itemCount = 0;
        this.visible = false;
        this.cachedItemCount = -1;
    }

    private int cachedMatchingItemCount(ClientPlayerEntity player2) {
        long l = System.nanoTime();
        if (this.cachedItemCount >= 0 && l - this.lastCountScanNanos < 50000000L) {
            return this.cachedItemCount;
        }
        this.cachedItemCount = this.countMatchingItems(player2);
        this.lastCountScanNanos = l;
        return this.cachedItemCount;
    }

    public void update3() {
        this.renderPipeline10.close();
    }
}

