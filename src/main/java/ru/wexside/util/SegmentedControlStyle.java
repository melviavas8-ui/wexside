package ru.wexside.util;

import java.util.function.Supplier;
import ru.wexside.WexSideClient;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.ThemeColors;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.MsdfFontRenderer;

public final class SegmentedControlStyle {
    private float selectionAnimationSpeed;
    private float backgroundRadius;
    private float iconSize;
    private float borderWidth;
    private MsdfFontRenderer font5;
    private float textAnimationSpeed;
    private float selectionRadius;
    private MsdfFontRenderer isActive2;
    private float segmentWidth;
    private float iconTextGap;
    private float labelSize;
    private float segmentHeight;
    private Supplier<GuiDrawApi> supplier;

    public SegmentedControlStyle() {
        this(WexSideClient::getGuiRenderer, FontRegistry.font4);
    }

    public SegmentedControlStyle(Supplier<GuiDrawApi> supplier, MsdfFontRenderer font5) {
        this.supplier = supplier;
        this.font5 = font5;
        this.isActive2 = FontRegistry.font3;
        this.segmentWidth = 21.25f;
        this.segmentHeight = 12.0f;
        this.backgroundRadius = 8.0f;
        this.selectionRadius = 8.0f;
        this.borderWidth = 0.75f;
        this.labelSize = 5.5f;
        this.iconSize = 6.5f;
        this.iconTextGap = 2.5f;
        this.selectionAnimationSpeed = 25.0f;
        this.textAnimationSpeed = 20.0f;
    }

    public SegmentedControlStyle withTextAnimationSpeed(float f) {
        this.textAnimationSpeed = Math.max(0.0f, f);
        return this;
    }

    public float getSelectionRadius() {
        return this.selectionRadius;
    }

    public SegmentedControlStyle withSegmentWidth(float f) {
        this.segmentWidth = Math.max(0.0f, f);
        return this;
    }

    public GuiDrawApi getGuiDrawApi() {
        return this.supplier == null ? null : this.supplier.get();
    }

    public SegmentedControlStyle withSelectionAnimationSpeed(float f) {
        this.selectionAnimationSpeed = Math.max(0.0f, f);
        return this;
    }

    public int getIntType() {
        return ThemeColors.backgroundControl();
    }

    public SegmentedControlStyle withLabelFont(MsdfFontRenderer font5) {
        this.font5 = font5;
        return this;
    }

    public SegmentedControlStyle withIconTextGap(float f) {
        this.iconTextGap = Math.max(0.0f, f);
        return this;
    }

    public SegmentedControlStyle withSelectionRadius(float f) {
        this.selectionRadius = Math.max(0.0f, f);
        return this;
    }

    public SegmentedControlStyle withLabelSize(float f) {
        this.labelSize = Math.max(0.1f, f);
        return this;
    }

    public float getBackgroundRadius() {
        return this.backgroundRadius;
    }

    public float getIconSize() {
        return this.iconSize;
    }

    public int getIntType2() {
        return ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f);
    }

    public MsdfFontRenderer getMsdfFontRenderer() {
        return this.isActive2;
    }

    public float getTextAnimationSpeed() {
        return this.textAnimationSpeed;
    }

    public int getIntType3() {
        return ThemeColors.hudBackground();
    }

    public Supplier<GuiDrawApi> getSupplier() {
        return this.supplier;
    }

    public float getSegmentWidth() {
        return this.segmentWidth;
    }

    public int getIntType4() {
        return ThemeColors.textMuted();
    }

    public SegmentedControlStyle withBorderWidth(float f) {
        this.borderWidth = Math.max(0.0f, f);
        return this;
    }

    public MsdfFontRenderer getMsdfFontRenderer2() {
        return this.font5;
    }

    public SegmentedControlStyle withIconSize(float f) {
        this.iconSize = Math.max(0.0f, f);
        return this;
    }

    public SegmentedControlStyle withIconFont(MsdfFontRenderer font5) {
        this.isActive2 = font5;
        return this;
    }

    public SegmentedControlStyle withSegmentHeight(float f) {
        this.segmentHeight = Math.max(0.0f, f);
        return this;
    }

    public float getBorderWidth() {
        return this.borderWidth;
    }

    public SegmentedControlStyle withBackgroundRadius(float f) {
        this.backgroundRadius = Math.max(0.0f, f);
        return this;
    }

    public float getSelectionAnimationSpeed() {
        return this.selectionAnimationSpeed;
    }

    public float getIconTextGap() {
        return this.iconTextGap;
    }

    public float getSegmentHeight() {
        return this.segmentHeight;
    }

    public int getIntType5() {
        return ThemeColors.accent();
    }

    public SegmentedControlStyle withDrawApiSupplier(Supplier<GuiDrawApi> supplier) {
        this.supplier = supplier;
        return this;
    }

    public int getIntType6() {
        return ThemeColors.borderPrimary();
    }

    public float getLabelSize() {
        return this.labelSize;
    }
}

