package ru.wexside.util;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.setting.ColorSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.AbstractColorSlider;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.SliderMarkerRenderer;

public final class AlphaSlider
extends AbstractColorSlider
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final float value;

    public AlphaSlider(GuiBounds bounds2, ColorSetting colorSetting, SliderMarkerRenderer sliderMarkerRenderer) {
        super(bounds2, colorSetting, sliderMarkerRenderer);
        this.value = 3.0f;
    }

    @Override
    protected float getMarkerY() {
        return this.getBounds().getY() + this.getBounds().getHeight() / 2.0f;
    }

    @Override
    protected void applyPointerPosition(float f, float f2) {
        float[] fArray = this.getHsbComponents();
        this.setEditingHsb(fArray[0], fArray[1], fArray[2], Math.round(this.clampUnit(f / this.getBounds().getWidth()) * 255.0f));
    }

    @Override
    protected void setMatrix4f(Matrix4f matrix4f) {
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        GuiBounds bounds2 = this.getBounds();
        float f = bounds2.getX();
        float f2 = bounds2.getY();
        float f3 = bounds2.getWidth();
        float f4 = bounds2.getHeight();
        int n = this.getBaseColor() | 0xFF000000;
        int n2 = n & 0xFFFFFF;
        drawApi.drawRoundedRectangleGradient(matrix4f, f, f2, f3, f4, 3.0f, n2, n, n, n2);
    }

    @Override
    protected float getMarkerX() {
        return this.getBounds().getX() + (float)this.getColorAlpha() / 255.0f * this.getBounds().getWidth();
    }
}

