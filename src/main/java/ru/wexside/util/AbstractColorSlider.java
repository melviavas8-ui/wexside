package ru.wexside.util;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.input.InputBindings;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiInteractionState;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.setting.ColorSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.SliderMarkerRenderer;

abstract class AbstractColorSlider
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final SliderMarkerRenderer sliderMarkerRenderer;
    private boolean dragging;
    private boolean layoutInitialized;
    private float lastLayoutX;
    private float lastLayoutY;
    private final ColorSetting colorSetting;

    protected AbstractColorSlider(GuiBounds bounds2, ColorSetting colorSetting, SliderMarkerRenderer sliderMarkerRenderer) {
        super(bounds2);
        this.colorSetting = colorSetting;
        this.sliderMarkerRenderer = sliderMarkerRenderer;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
        if (this.dragging) {
            if (!InputBindings.isMouseButtonPressed(0)) {
                this.dragging = false;
                this.sliderMarkerRenderer.setHovered(false);
            } else {
                this.applyPointerPosition((float)GuiInteractionState.getInstance().getScaledMouseX() - this.getAbsoluteX(), (float)GuiInteractionState.getInstance().getScaledMouseY() - this.getAbsoluteY());
            }
        } else {
            this.sliderMarkerRenderer.setHovered(false);
        }
    }

    @Override
    public final boolean onMousePressed(int n, int n2, int n3) {
        if (n3 != 0 || !this.getBounds().contains(n, n2)) {
            return false;
        }
        this.dragging = true;
        this.sliderMarkerRenderer.setHovered(true);
        this.applyPointerPosition((float)n - this.getBounds().getX(), (float)n2 - this.getBounds().getY());
        return true;
    }

    @Override
    public final float render(float f, Matrix4f matrix4f) {
        this.updateLayout();
        this.setMatrix4f(matrix4f);
        this.sliderMarkerRenderer.render(matrix4f, WexSideClient.getGuiRenderer(), this.getMarkerX(), this.getMarkerY(), this.getDisplayColor());
        return this.getBounds().getY() + this.getBounds().getHeight();
    }

    @Override
    public boolean isActive2() {
        return this.dragging;
    }

    public ColorSetting getColorSetting() {
        return this.colorSetting;
    }

    protected boolean isAstolfoMode() {
        return this.colorSetting.isAstolfoMode();
    }

    protected abstract float getMarkerY();

    protected abstract void applyPointerPosition(float var1, float var2);

    protected int getBaseColor() {
        return this.colorSetting.getColor();
    }

    protected float normalizedPosition(float f, float f2) {
        if (f2 <= 0.0f) {
            return 0.0f;
        }
        return this.clampUnit(f / f2);
    }

    protected int getColorAlpha() {
        return this.getBaseColor() >>> 24 & 0xFF;
    }

    public SliderMarkerRenderer getSliderMarkerRenderer() {
        return this.sliderMarkerRenderer;
    }

    protected float normalizedHuePosition(float f, float f2) {
        if (f2 <= 0.0f) {
            return 0.0f;
        }
        float f3 = f / f2;
        float f4 = 1.0f - this.halfPixelHueMargin(f2);
        return Math.max(0.0f, Math.min(f4, f3));
    }

    protected void setEditingHsb(float f, float f2, float f3, int n) {
        this.colorSetting.setEditingHsb(this.clampUnit(f), this.clampUnit(f2), this.clampUnit(f3), n);
    }

    protected abstract void setMatrix4f(Matrix4f var1);

    public boolean isLayoutInitialized() {
        return this.layoutInitialized;
    }

    protected void setFloatType(float f) {
        this.colorSetting.setAstolfoPhaseOffset(f);
    }

    protected float[] getHsbComponents() {
        return this.colorSetting.getCurrentHsb();
    }

    protected int getSettingColor() {
        return this.colorSetting.getColor();
    }

    protected float halfPixelHueMargin(float f) {
        return Math.min(0.5f / f, 0.499f);
    }

    public float getLastLayoutY() {
        return this.lastLayoutY;
    }

    protected float clampUnit(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    private void updateLayout() {
        GuiBounds bounds2 = this.getBounds();
        float f = bounds2.getX();
        float f2 = bounds2.getY();
        if (!this.layoutInitialized) {
            this.lastLayoutX = f;
            this.lastLayoutY = f2;
            this.layoutInitialized = true;
            return;
        }
        float f3 = f - this.lastLayoutX;
        float f4 = f2 - this.lastLayoutY;
        if (f3 != 0.0f || f4 != 0.0f) {
            this.sliderMarkerRenderer.translate(f3, f4);
            this.lastLayoutX = f;
            this.lastLayoutY = f2;
        }
    }

    protected float getAstolfoHue() {
        return this.colorSetting.getAstolfoHue();
    }

    public float getLastLayoutX() {
        return this.lastLayoutX;
    }

    protected abstract float getMarkerX();

    protected int getDisplayColor() {
        return this.getBaseColor();
    }

    protected int astolfoGradientColorAt(float f) {
        return this.colorSetting.getAstolfoGradientColor(f);
    }

    protected void setFloatType2(float f) {
        this.colorSetting.setAstolfoHue(f);
    }

    protected float getAstolfoPhaseOffset() {
        return this.colorSetting.getAstolfoPhaseOffset();
    }
}

