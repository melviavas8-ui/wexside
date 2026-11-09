package ru.wexside.misc;

import java.util.Objects;
import java.util.function.IntSupplier;
import org.joml.Matrix4f;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class PopupHeader {
    private float titleFontSize = 5.5f;
    private IntSupplier intSupplier = ThemeColors::textPrimary;
    private final String string4;
    private float subtitleFontSize = 6.75f;
    private final float value3;
    private final GuiBounds bounds2;
    private final float value4;
    private final String string5;
    private final float value5;
    private final String string6;

    public PopupHeader(GuiBounds bounds2, String string, String string2, String string3) {
        this.value4 = 6.0f;
        this.value5 = 2.0f;
        this.value3 = 2.0f;
        this.bounds2 = bounds2;
        this.string6 = string;
        this.string5 = string2;
        this.string4 = string3;
    }

    public float getTitleFontSize() {
        return this.titleFontSize;
    }

    public void BlockHitResult(Matrix4f matrix4f, GuiDrawApi drawApi) {
        this.render(matrix4f, drawApi, 1.0f);
    }

    public float getHeight() {
        return this.getTitleHeight() + 2.0f + this.getDescriptionHeight();
    }

    private static int applyOpacity(int n, float f) {
        if (f >= 0.999f) {
            return n;
        }
        int n2 = n >>> 24 & 0xFF;
        return ColorUtils.withAlpha(n, (float)(n2 == 0 ? 255 : n2) * f);
    }

    public PopupHeader withSubtitleColor(IntSupplier intSupplier) {
        this.intSupplier = intSupplier;
        return this;
    }

    public GuiBounds getBounds() {
        return this.bounds2;
    }

    private float getTitleHeight() {
        return FontRegistry.font4.textHeight(this.string6, this.titleFontSize);
    }

    public float getIconFontSize() {
        Objects.requireNonNull(this);
        return 6.0f;
    }

    public void render(Matrix4f matrix4f, GuiDrawApi drawApi, float f) {
        FontRegistry.font4.drawText(matrix4f, drawApi, this.string6, this.bounds2.getX(), this.bounds2.getY(), this.titleFontSize, PopupHeader.applyOpacity(ThemeColors.accent(), f));
        float f2 = FontRegistry.font3.textHeight(this.string5, 6.0f);
        float f3 = this.bounds2.getX() + FontRegistry.font4.textWidth(this.string6, this.titleFontSize) + 2.0f;
        float f4 = this.bounds2.getY() + (this.getTitleHeight() - f2) / 2.0f;
        FontRegistry.font3.drawGlyphText(matrix4f, drawApi, this.string5, f3, f4, 6.0f, PopupHeader.applyOpacity(ThemeColors.accent(), f));
        FontRegistry.font2.drawText(matrix4f, drawApi, this.string4, this.bounds2.getX(), this.bounds2.getY() + this.getTitleHeight() + 2.0f, this.subtitleFontSize, PopupHeader.applyOpacity(this.intSupplier.getAsInt(), f));
    }

    public float getSubtitleFontSize() {
        return this.subtitleFontSize;
    }

    public String getString() {
        return this.string4;
    }

    public PopupHeader withDescriptionFontSize(float f) {
        this.subtitleFontSize = f;
        return this;
    }

    public String getString2() {
        return this.string6;
    }

    public PopupHeader withTitleFontSize(float f) {
        this.titleFontSize = f;
        return this;
    }

    public IntSupplier RaycastContext() {
        return this.intSupplier;
    }

    public float getTitleSubtitleGap() {
        Objects.requireNonNull(this);
        return 2.0f;
    }

    public String getString3() {
        return this.string5;
    }

    public float getTitleIconGap() {
        Objects.requireNonNull(this);
        return 2.0f;
    }

    private float getDescriptionHeight() {
        return FontRegistry.font2.textHeight(this.string4, this.subtitleFontSize);
    }
}

