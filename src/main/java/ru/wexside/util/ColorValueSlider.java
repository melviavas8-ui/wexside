package ru.wexside.util;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.input.InputBindings;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ScaleSettings;
import ru.wexside.misc.ThemeColors;
import ru.wexside.setting.ColorSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.SliderTrack;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.NumberFormatting;
import ru.wexside.util.SliderRenderer;

public final class ColorValueSlider
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private float animatedProgress = -1.0f;
    private final ScaleSettings scaleSettings;
    private final ColorSetting colorSetting;
    private final SliderRenderer sliderRenderer = new SliderRenderer();
    private boolean dragging;

    public ColorValueSlider(GuiBounds bounds2, ColorSetting colorSetting) {
        super(bounds2);
        this.scaleSettings = new ScaleSettings(1.0, 5.0, 0, true, 1.0, 1.0);
        this.colorSetting = colorSetting;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (n3 != 0 || !this.getBounds().contains(n, n2)) {
            return false;
        }
        SliderTrack track = this.createValueTrack(this.getBounds().getX(), this.getBounds().getY());
        if ((float)n2 < track.y() - 3.0f || (float)n2 > track.y() + this.sliderRenderer.getTrackThickness() + 3.0f) {
            return false;
        }
        this.dragging = true;
        this.setFloatType2(n);
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        float f2 = this.getNormalizedValue();
        this.animatedProgress = this.animatedProgress < 0.0f ? f2 : FrameInterpolator.lerpTowards(this.animatedProgress, f2, this.dragging ? 35.0f : 20.0f);
        FontRegistry.font4.drawText(matrix4f, drawApi, "Скорость переливания цвета", 0.0f, 0.0f, 5.5f, ThemeColors.textMuted());
        SliderTrack track = this.createValueTrack(0.0f, 0.0f);
        String string = NumberFormatting.format(this.colorSetting.getAstolfoSpeedPercent(), 0);
        String string2 = NumberFormatting.format(5.0, 0);
        String string3 = NumberFormatting.format(1.0, 0);
        String string4 = string3 + " - " + string2;
        float f3 = track.y() - FontRegistry.font2.textHeight("1", 6.5f) - 2.0f;
        FontRegistry.font2.drawText(matrix4f, drawApi, string, track.x(), f3, 6.5f, ThemeColors.accent());
        FontRegistry.font2.drawText(matrix4f, drawApi, string4, track.x() + track.width() - FontRegistry.font2.textWidth(string4, 6.5f), f3, 6.5f, ThemeColors.textMuted());
        int n = ThemeColors.borderSubtle();
        this.sliderRenderer.renderTrack(matrix4f, drawApi, track, n);
        this.sliderRenderer.renderTickMarks(matrix4f, drawApi, track, this.scaleSettings, n, ThemeColors.textDisabled());
        float f4 = track.width() * this.sliderRenderer.valueFractionToTrackFraction(this.animatedProgress, this.scaleSettings);
        drawApi.drawRoundedRectangle(matrix4f, track.x(), track.y(), f4, this.sliderRenderer.getTrackThickness(), 2.0f, ThemeColors.accent());
        this.sliderRenderer.renderThumb(matrix4f, drawApi, track.x() + f4, track.y() + this.sliderRenderer.getTrackThickness() / 2.0f, ThemeColors.accent());
        return bounds2.getY() + bounds2.getHeight();
    }

    public void setFloatType(float f) {
        if (!this.dragging) {
            return;
        }
        if (!InputBindings.isMouseButtonPressed(0)) {
            this.dragging = false;
            return;
        }
        this.setFloatType2(f);
    }

    private float getLabelHeight() {
        return FontRegistry.font4.textHeight("Скорость переливания цвета", 5.5f);
    }

    private void setFloatType2(float f) {
        SliderTrack track = this.createValueTrack(this.getBounds().getX(), this.getBounds().getY());
        float f2 = Math.max(0.0f, Math.min(track.width(), f - track.x()));
        float f3 = this.sliderRenderer.trackFractionToValueFraction(f2 / track.width(), this.scaleSettings);
        this.colorSetting.setAstolfoSpeedPercent((int)Math.round(1.0 + 4.0 * (double)f3));
    }

    private float getNormalizedValue() {
        return (float)(((double)this.colorSetting.getAstolfoSpeedPercent() - 1.0) / 4.0);
    }

    private SliderTrack createValueTrack(float f, float f2) {
        float f3 = f2 + this.getLabelHeight() + 2.5f;
        float f4 = this.sliderRenderer.getHorizontalPadding();
        GuiBounds bounds2 = new GuiBounds(f - f4, f3, this.getBounds().getWidth() + f4 * 2.0f, 0.0f);
        return this.sliderRenderer.createTrack(bounds2, FontRegistry.font2.textHeight("1", 6.5f));
    }

    public float getPreferredHeight() {
        return this.createValueTrack(0.0f, 0.0f).y() + this.sliderRenderer.getTrackThickness() + 4.0f;
    }
}

