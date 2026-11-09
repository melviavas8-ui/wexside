package ru.wexside.util;

import java.awt.Color;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.PreparedLayer;
import ru.wexside.misc.ThemeColors;
import ru.wexside.setting.ColorSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.AbstractColorSlider;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.SliderMarkerRenderer;

public final class HueSaturationPicker
extends AbstractColorSlider
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final float value;
    private boolean cursorInitialized;
    private int lastRgb = Integer.MIN_VALUE;
    private float saturationFraction;
    private boolean cursorControlsColor;
    private float inverseBrightnessFraction;
    private final float cornerRadius;

    public HueSaturationPicker(GuiBounds bounds2, ColorSetting colorSetting, SliderMarkerRenderer sliderMarkerRenderer, float f) {
        super(bounds2, colorSetting, sliderMarkerRenderer);
        this.value = 2.0f;
        this.cornerRadius = f;
    }

    @Override
    public void update() {
        super.update();
        this.syncFromMouse();
        if (this.cursorControlsColor) {
            this.applyCursorToColor();
        }
    }

    private float calculateMarkerY() {
        return this.getBounds().getY() + this.inverseBrightnessFraction * this.getBounds().getHeight();
    }

    private boolean hasExternalColorChange() {
        if (!this.cursorInitialized) {
            return true;
        }
        return this.getCurrentRgb() != this.lastRgb;
    }

    @Override
    protected float getMarkerY() {
        if (!this.cursorInitialized) {
            this.syncCursorFromColor();
        }
        return this.calculateMarkerY();
    }

    private int getCurrentRgb() {
        int n = this.colorSettingColorChanged() ? this.getSettingColor() : this.getBaseColor();
        return n & 0xFFFFFF;
    }

    private boolean colorSettingColorChanged() {
        return this.cursorInitialized && (this.getSettingColor() & 0xFFFFFF) != this.lastRgb;
    }

    @Override
    protected void applyPointerPosition(float f, float f2) {
        this.setCursorFractions(this.normalizedPosition(f, this.getBounds().getWidth()), this.normalizedPosition(f2, this.getBounds().getHeight()));
        this.cursorControlsColor = true;
    }

    private float readSaturationFromCursor() {
        float f = Math.max(0.0f, this.getBounds().getWidth() - 2.0f);
        if (f <= 0.0f) {
            return 0.0f;
        }
        float f2 = this.getSliderMarkerRenderer().getAnimatedX(this.markerX());
        return this.clampUnit((f2 - this.getBounds().getX()) / f);
    }

    private void syncFromMouse() {
        if (this.hasExternalColorChange()) {
            this.syncCursorFromColor();
        }
    }

    private float markerX() {
        float f = Math.max(0.0f, this.getBounds().getWidth() - 2.0f);
        return this.getBounds().getX() + this.saturationFraction * f;
    }

    @Override
    protected void setMatrix4f(Matrix4f matrix4f) {
        this.syncFromMouse();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        GuiBounds bounds2 = this.getBounds();
        float f = bounds2.getX();
        float f2 = bounds2.getY();
        float f3 = bounds2.getWidth();
        float f4 = bounds2.getHeight();
        float[] fArray = this.getHsbComponents();
        int n = Color.HSBtoRGB(fArray[0], 1.0f, 1.0f) | 0xFF000000;
        PreparedLayer preparedLayer = drawApi.prepareDedicatedLayer(matrix4f, f, f2, f3, f4, 0.0f);
        drawApi.beginLayerFrame(preparedLayer.getTexture());
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)preparedLayer.getContentMatrix()).translate(preparedLayer.contentX(), preparedLayer.contentY(), 0.0f);
        drawApi.fillRectangle(matrix4f2, 0.0f, 0.0f, f3, f4, ThemeColors.backgroundControl());
        drawApi.drawColorGradient(matrix4f2, 0.0f, 0.0f, f3, f4, ColorUtils.withAlpha(ThemeColors.backgroundControl(), 0.0f), n, n, ColorUtils.withAlpha(ThemeColors.backgroundControl(), 0.0f));
        drawApi.drawColorGradient(matrix4f2, 0.0f, 0.0f, f3, f4, ColorUtils.rgba(0, 0, 0, 255), ColorUtils.rgba(0, 0, 0, 255), ColorUtils.rgba(0, 0, 0, 0), ColorUtils.rgba(0, 0, 0, 0));
        drawApi.endLayerFrame();
        drawApi.drawPreparedLayerRounded(matrix4f, preparedLayer, this.cornerRadius, -1);
    }

    private void syncCursorFromColor() {
        float[] fArray = this.getHsbComponents();
        this.saturationFraction = this.clampUnit(fArray[1]);
        this.inverseBrightnessFraction = this.clampUnit(1.0f - fArray[2]);
        this.cursorInitialized = true;
        this.cursorControlsColor = false;
        this.lastRgb = this.getCurrentRgb();
    }

    private void applyCursorToColor() {
        float[] fArray = this.getHsbComponents();
        this.setEditingHsb(fArray[0], this.readSaturationFromCursor(), 1.0f - this.readBrightnessFromCursor(), this.getColorAlpha());
        this.lastRgb = this.getCurrentRgb();
    }

    private void setCursorFractions(float f, float f2) {
        this.saturationFraction = this.clampUnit(f);
        this.inverseBrightnessFraction = this.clampUnit(f2);
        this.cursorInitialized = true;
    }

    private float readBrightnessFromCursor() {
        float f = this.getBounds().getHeight();
        if (f <= 0.0f) {
            return 0.0f;
        }
        float f2 = this.getSliderMarkerRenderer().getAnimatedY(this.getMarkerY());
        return this.clampUnit((f2 - this.getBounds().getY()) / f);
    }

    @Override
    protected float getMarkerX() {
        if (!this.cursorInitialized) {
            this.syncCursorFromColor();
        }
        return this.markerX();
    }
}

