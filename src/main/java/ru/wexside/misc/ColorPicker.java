package ru.wexside.misc;

import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.ColorFormatSelector;
import ru.wexside.misc.ColorModeSelector;
import ru.wexside.misc.ColorPreviewButton;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.color.ColorChannel;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.AlphaSlider;
import ru.wexside.util.ClippedLayerRenderer;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.ColorValueSlider;
import ru.wexside.util.DoubleColor;
import ru.wexside.util.HueSaturationPicker;
import ru.wexside.util.HueSlider;
import ru.wexside.util.SliderMarkerRenderer;

public final class ColorPicker
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final HueSlider hueSlider;
    private final DoubleColor doubleColor;
    private final float value;
    private final float value2;
    private final float value3;
    private int slot;
    private final ColorPreviewButton colorPreviewButton;
    private final float value4;
    private final float value5;
    private final float value6;
    private final ColorValueSlider colorValueSlider;
    private final ColorModeSelector colorModeSelector;
    private final AlphaSlider alphaSlider;
    private final float value7;
    private final float value8;
    private final float value9;
    private ColorChannel colorChannel;
    private final float value10;
    private final float value11;
    private final float value12;
    private final float value13;
    private final HueSaturationPicker hueSaturationPicker;
    private final float value14;
    private float value15;
    private final float value16;
    private final float value17;
    private boolean enabled;
    private final float value18;
    private final float value19;
    private final float value20;
    private final ColorSetting colorSetting;
    private final float value21;
    private final float value22;
    private final int slot2 = ColorUtils.rgba(0, 0, 0, 60);
    private final ColorFormatSelector colorFormatSelector;
    private final float value23;
    private float value24;

    public ColorPicker(GuiBounds bounds2, ColorSetting colorSetting) {
        super(bounds2);
        this.value16 = 7.0f;
        this.value23 = 11.0f;
        this.value9 = 5.0f;
        this.value12 = 15.0f;
        this.value21 = 12.0f;
        this.value20 = 5.0f;
        this.value18 = 6.0f;
        this.value = 7.5f;
        this.value5 = 6.0f;
        this.value8 = 15.0f;
        this.value3 = 64.0f;
        this.value22 = 3.0f;
        this.value11 = 5.0f;
        this.value4 = 4.0f;
        this.value6 = 5.0f;
        this.value7 = 1.0f;
        this.value13 = 7.0f;
        this.value2 = 30.0f;
        this.value19 = 20.0f;
        this.value10 = 30.0f;
        this.value14 = 30.0f;
        this.value17 = 1.0f;
        this.colorSetting = colorSetting;
        this.colorChannel = colorSetting.getEditingChannel();
        this.colorModeSelector = new ColorModeSelector(new GuiBounds(0.0f, 0.0f, 0.0f, 11.0f), colorSetting);
        this.colorFormatSelector = new ColorFormatSelector(new GuiBounds(0.0f, 0.0f, 0.0f, 12.0f), colorSetting);
        this.hueSaturationPicker = new HueSaturationPicker(new GuiBounds(0.0f, 0.0f, 0.0f, 64.0f), colorSetting, this.getSliderMarkerRenderer(), 7.0f);
        this.hueSlider = new HueSlider(new GuiBounds(0.0f, 0.0f, 0.0f, 3.0f), colorSetting, this.getSliderMarkerRenderer2());
        this.alphaSlider = new AlphaSlider(new GuiBounds(0.0f, 0.0f, 0.0f, 3.0f), colorSetting, this.getSliderMarkerRenderer2());
        this.doubleColor = new DoubleColor(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f));
        this.colorPreviewButton = new ColorPreviewButton(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f), colorSetting);
        this.colorValueSlider = new ColorValueSlider(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f), colorSetting);
        this.doubleColor.setConsumer(colorSetting::setColorMode);
        this.doubleColor.setColorMode(colorSetting.getColorMode());
        this.addChild(this.hueSaturationPicker);
        this.addChild(this.hueSlider);
        this.addChild(this.alphaSlider);
        this.addChild(this.colorFormatSelector);
        this.addChild(this.colorPreviewButton);
        this.addChild(this.doubleColor);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
        this.syncEditingChannel();
        this.updateAnimations();
        this.updateSectionAnimations();
        this.colorModeSelector.update();
        for (GuiElement element2 : this.children) {
            element2.update();
        }
        this.commitRecentColor();
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        boolean bl;
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        int n4 = this.colorSetting.getColor();
        int n5 = (int)((float)n - this.getBounds().getX());
        int n6 = (int)((float)n2 - this.getBounds().getY());
        boolean bl2 = bl = this.isActive3() && this.colorModeSelector.onMousePressed(n5, n6, n3);
        if (!bl && this.isEyedropperActive()) {
            bl = this.colorValueSlider.onMousePressed(n5, n6, n3);
        }
        if (!bl) {
            boolean bl3 = bl = super.onMousePressed(n5, n6, n3) || this.getBounds().contains(n, n2);
        }
        if (!this.enabled && this.isActive4()) {
            this.enabled = true;
            this.slot = n4;
        }
        return bl;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        this.syncEditingChannel();
        this.updateAnimations();
        this.updateSectionAnimations();
        this.layoutChildren();
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(this.getBounds().getX(), this.getBounds().getY(), 0.0f);
        this.renderModeSelector(f, matrix4f2);
        this.colorValueSlider.setFloatType(this.getLastMouseX() - this.getBounds().getX());
        this.renderValueSlider(f, matrix4f2);
        for (GuiElement element2 : this.children) {
            element2.render(f, matrix4f2);
        }
        return this.getBounds().getY() + this.getBounds().getHeight();
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        int n4 = (int)((float)n - this.getBounds().getX());
        int n5 = (int)((float)n2 - this.getBounds().getY());
        super.onMouseReleased(n4, n5, n3);
    }

    public ColorSetting getColorSetting() {
        return this.colorSetting;
    }

    public boolean isActive() {
        return this.enabled;
    }

    public ColorChannel getColorChannel() {
        return this.colorChannel;
    }

    public float getFloatType() {
        Objects.requireNonNull(this);
        return 5.0f;
    }

    private SliderMarkerRenderer getSliderMarkerRenderer() {
        return new SliderMarkerRenderer(5.0f, 1.0f, 7.0f, 20.0f, 30.0f, this.slot2);
    }

    public float getHorizontalPadding() {
        Objects.requireNonNull(this);
        return 7.0f;
    }

    public float getValueSliderVisibilityProgress() {
        return this.value15;
    }

    private boolean isEyedropperActive() {
        return this.value15 > 0.01f;
    }

    public float getFloatType4() {
        Objects.requireNonNull(this);
        return 5.0f;
    }

    public int getSliderMarkerShadowColor() {
        return this.slot2;
    }

    public float getFloatType5() {
        Objects.requireNonNull(this);
        return 1.0f;
    }

    public float getValueSliderSectionHeight() {
        return this.getAnimatedValueSliderHeight();
    }

    public float getFloatType7() {
        Objects.requireNonNull(this);
        return 1.0f;
    }

    private void updateAnimations() {
        this.value24 = FrameInterpolator.lerpTowards(this.value24, this.colorSetting.isDoubleColorMode() ? 1.0f : 0.0f, 15.0f);
    }

    public float getModeSelectorSectionHeight() {
        return this.getAnimatedModeSelectorHeight();
    }

    public ColorValueSlider getColorValueSlider() {
        return this.colorValueSlider;
    }

    private void renderModeSelector(float f, Matrix4f matrix4f2) {
        if (!this.isActive3()) {
            return;
        }
        ClippedLayerRenderer.renderLayer(WexSideClient.getGuiRenderer(), matrix4f2, this.colorModeSelector.getBounds().getX(), this.colorModeSelector.getBounds().getY(), this.colorModeSelector.getBounds().getWidth(), this.colorModeSelector.getBounds().getHeight(), 0.0f, this.value24 < 0.99f, ColorUtils.withAlpha(-1, 255.0f * this.value24), matrix4f -> this.colorModeSelector.render(f, (Matrix4f)matrix4f));
    }

    private void commitRecentColor() {
        if (this.isActive4()) {
            return;
        }
        if (!this.enabled) {
            return;
        }
        this.enabled = false;
        if (this.colorSetting.getColor() != this.slot) {
            this.colorSetting.addCurrentColorToRecents();
        }
    }

    public float getFloatType9() {
        Objects.requireNonNull(this);
        return 12.0f;
    }

    public float getFloatType10() {
        Objects.requireNonNull(this);
        return 20.0f;
    }

    private void updateSectionAnimations() {
        this.value15 = FrameInterpolator.lerpTowards(this.value15, this.colorSetting.isAstolfoMode() || this.colorSetting.isDoubleColorMode() ? 1.0f : 0.0f, 15.0f);
    }

    public float getFloatType11() {
        Objects.requireNonNull(this);
        return 6.0f;
    }

    public float getModeSelectorVisibilityProgress() {
        return this.value24;
    }

    public AlphaSlider getAlphaSlider() {
        return this.alphaSlider;
    }

    public ColorModeSelector getColorModeSelector() {
        return this.colorModeSelector;
    }

    public HueSaturationPicker getHueSaturationPicker() {
        return this.hueSaturationPicker;
    }

    public ColorPreviewButton getColorPreviewButton() {
        return this.colorPreviewButton;
    }

    private SliderMarkerRenderer getSliderMarkerRenderer2() {
        return new SliderMarkerRenderer(5.0f, 1.0f, 7.0f, 30.0f, 30.0f, this.slot2);
    }

    public DoubleColor getDoubleColor() {
        return this.doubleColor;
    }

    public float getFloatType13() {
        Objects.requireNonNull(this);
        return 4.0f;
    }

    public float getFloatType14() {
        Objects.requireNonNull(this);
        return 7.5f;
    }

    public float getFloatType15() {
        Objects.requireNonNull(this);
        return 5.0f;
    }

    private float getAnimatedValueSliderHeight() {
        return (this.colorValueSlider.getPreferredHeight() + 6.0f) * this.value15;
    }

    private void renderValueSlider(float f, Matrix4f matrix4f2) {
        if (!this.isEyedropperActive()) {
            return;
        }
        ClippedLayerRenderer.renderLayer(WexSideClient.getGuiRenderer(), matrix4f2, this.colorValueSlider.getBounds().getX(), this.colorValueSlider.getBounds().getY(), this.colorValueSlider.getBounds().getWidth(), this.colorValueSlider.getBounds().getHeight(), 4.0f, this.value15 < 0.99f, ColorUtils.withAlpha(-1, 255.0f * this.value15), matrix4f -> this.colorValueSlider.render(f, (Matrix4f)matrix4f));
    }

    public float getFloatType17() {
        Objects.requireNonNull(this);
        return 7.0f;
    }

    public float getFloatType18() {
        Objects.requireNonNull(this);
        return 15.0f;
    }

    public float getFloatType19() {
        Objects.requireNonNull(this);
        return 64.0f;
    }

    private float getAnimatedModeSelectorHeight() {
        return 16.0f * this.value24;
    }

    public float getFloatType21() {
        Objects.requireNonNull(this);
        return 30.0f;
    }

    public float getFloatType22() {
        Objects.requireNonNull(this);
        return 6.0f;
    }

    public float getFloatType23() {
        Objects.requireNonNull(this);
        return 11.0f;
    }

    public float getFloatType24() {
        Objects.requireNonNull(this);
        return 3.0f;
    }

    public float getFloatType25() {
        Objects.requireNonNull(this);
        return 30.0f;
    }

    public float getFloatType26() {
        Objects.requireNonNull(this);
        return 15.0f;
    }

    public ColorFormatSelector getColorFormatSelector() {
        return this.colorFormatSelector;
    }

    public HueSlider getHueSlider() {
        return this.hueSlider;
    }

    private boolean isActive3() {
        return this.value24 > 0.01f;
    }

    public float getFloatType27() {
        Objects.requireNonNull(this);
        return 30.0f;
    }

    private void syncEditingChannel() {
        this.doubleColor.setColorMode(this.colorSetting.getColorMode());
        ColorChannel colorChannel = this.colorSetting.getEditingChannel();
        if (this.colorChannel == colorChannel) {
            return;
        }
        this.colorChannel = colorChannel;
        this.colorFormatSelector.update2();
    }

    public float getFloatType28() {
        Objects.requireNonNull(this);
        return 5.0f;
    }

    public int getIntType2() {
        return this.slot;
    }

    private boolean isActive4() {
        return this.hueSaturationPicker.isActive2() || this.hueSlider.isActive2() || this.alphaSlider.isActive2();
    }

    public float getFloatType2() {
        this.syncEditingChannel();
        this.updateAnimations();
        this.updateSectionAnimations();
        this.colorPreviewButton.getBounds().setSize(this.getBounds().getWidth(), this.colorPreviewButton.getBounds().getHeight());
        return this.getAnimatedModeSelectorHeight() + 64.0f + 5.0f + 3.0f + 4.0f + 3.0f + 5.0f + 12.0f + this.getAnimatedValueSliderHeight() + 7.5f + this.colorPreviewButton.getPreferredHeight();
    }

    private void layoutChildren() {
        float f = this.getBounds().getWidth();
        float f2 = this.getAnimatedModeSelectorHeight();
        this.colorModeSelector.getBounds().setPosition(0.0f, 0.0f);
        this.colorModeSelector.getBounds().setSize(f, 11.0f);
        float f3 = f2;
        this.hueSaturationPicker.getBounds().setPosition(0.0f, f3);
        this.hueSaturationPicker.getBounds().setSize(f, 64.0f);
        this.hueSlider.getBounds().setPosition(0.0f, f3 += 69.0f);
        this.hueSlider.getBounds().setSize(f, 3.0f);
        this.alphaSlider.getBounds().setPosition(0.0f, f3 += 7.0f);
        this.alphaSlider.getBounds().setSize(f, 3.0f);
        this.colorFormatSelector.getBounds().setPosition(0.0f, f3 += 8.0f);
        this.colorFormatSelector.getBounds().setSize(f, 12.0f);
        float f4 = this.doubleColor.getPreferredWidth();
        this.doubleColor.getBounds().setPosition(f - f4, f3 + 12.0f + 6.0f);
        this.doubleColor.getBounds().setSize(f4, this.doubleColor.getPreferredHeight());
        this.colorValueSlider.getBounds().setPosition(0.0f, (f3 += 12.0f) + 6.0f);
        this.colorValueSlider.getBounds().setSize(f, this.colorValueSlider.getPreferredHeight());
        this.colorPreviewButton.getBounds().setPosition(0.0f, f3 += this.getAnimatedValueSliderHeight() + 7.5f);
        this.colorPreviewButton.getBounds().setSize(f, this.colorPreviewButton.getBounds().getHeight());
        float f5 = this.colorPreviewButton.getPreferredHeight();
        this.getBounds().setSize(this.getBounds().getWidth(), f3 + f5);
    }
}

