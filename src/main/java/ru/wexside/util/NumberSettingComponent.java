package ru.wexside.util;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.input.InputBindings;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.BoundsSupplier;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiInteractionState;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.PopupManager;
import ru.wexside.misc.PopupOwner;
import ru.wexside.misc.ScaleSettings;
import ru.wexside.misc.SingleNumberEditorPopup;
import ru.wexside.misc.ThemeColors;
import ru.wexside.setting.NumberSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.PopupPanel;
import ru.wexside.ui.SliderTrack;
import ru.wexside.ui.setting.SettingComponent;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.NumberFormatting;
import ru.wexside.util.SliderRenderer;

public final class NumberSettingComponent
extends SettingComponent
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
PopupOwner {
    private final SingleNumberEditorPopup editorPopup;
    private float dragScreenOriginX;
    private final float value2;
    private final SliderRenderer sliderRenderer = new SliderRenderer();
    private PopupManager popupManager;
    private float editorAnchorX;
    private float animatedValueFraction;
    private boolean dragging;

    public NumberSettingComponent(NumberSetting numberSetting) {
        super(new GuiBounds(0.0f, 0.0f, 110.0f, 0.0f), numberSetting);
        this.value2 = 6.5f;
        this.editorPopup = new SingleNumberEditorPopup(numberSetting);
        this.getBounds().setSize(this.getBounds().getWidth(), this.getPreferredHeight());
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        if (n3 == 1) {
            if (this.popupManager != null) {
                this.editorAnchorX = (float)GuiInteractionState.getInstance().getScaledMouseX() - GuiInteractionState.getInstance().getRootPanel().getBounds().getX();
                this.popupManager.toggle(this);
            }
            return true;
        }
        if (n3 != 0) {
            return true;
        }
        String string = NumberFormatting.format(((NumberSetting)this.getSetting()).getValue(), ((NumberSetting)this.getSetting()).getPrecision());
        float f = FontRegistry.font2.textHeight(string, 6.5f);
        SliderTrack track = this.sliderRenderer.createTrack(this.getBounds(), f);
        if ((float)n2 < track.y() - 3.0f || (float)n2 > track.y() + this.sliderRenderer.getTrackThickness() + 3.0f) {
            return true;
        }
        this.dragging = true;
        this.dragScreenOriginX = (float)GuiInteractionState.getInstance().getScaledMouseX() - ((float)n - this.getBounds().getX());
        this.setFloatType(n);
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        float f2;
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        NumberSetting numberSetting = (NumberSetting)this.getSetting();
        if (this.dragging) {
            if (!InputBindings.isMouseButtonPressed(0)) {
                this.dragging = false;
            } else {
                f2 = this.getBounds().getX() + (float)GuiInteractionState.getInstance().getScaledMouseX() - this.dragScreenOriginX;
                this.setFloatType(f2);
            }
        }
        f2 = this.getNormalizedValue();
        this.animatedValueFraction = FrameInterpolator.lerpTowards(this.animatedValueFraction, f2, this.dragging ? 35.0f : 20.0f);
        ScaleSettings scaleSettings = this.scaleSettings(numberSetting);
        String string = NumberFormatting.format(numberSetting.getValue(), numberSetting.getPrecision());
        float f3 = FontRegistry.font2.textHeight(string, 6.5f);
        float f4 = bounds2.getY() + this.sliderRenderer.getTopPadding();
        SliderTrack track = this.sliderRenderer.createTrack(bounds2, f3);
        String string2 = NumberFormatting.unit(numberSetting);
        float f5 = bounds2.getX() + bounds2.getWidth() - this.sliderRenderer.getHorizontalPadding() - FontRegistry.font2.textWidth(string2, 6.5f);
        float f6 = track.x();
        FontRegistry.font2.drawText(matrix4f, drawApi, string2, f5, f4 - 3.0f, 6.5f, ThemeColors.accent());
        FontRegistry.font2.drawText(matrix4f, drawApi, string, f6, f4 - 3.0f, 6.5f, ThemeColors.textMuted());
        int n = ThemeColors.borderSubtle();
        int n2 = ThemeColors.textDisabled();
        int n3 = ThemeColors.accent();
        this.sliderRenderer.renderTrack(matrix4f, drawApi, track, n);
        this.sliderRenderer.renderTickMarks(matrix4f, drawApi, track, scaleSettings, n, n2);
        float f7 = track.width() * this.sliderRenderer.valueFractionToTrackFraction(this.animatedValueFraction, scaleSettings);
        float f8 = track.x() + f7;
        float f9 = track.y() + this.sliderRenderer.getTrackThickness() / 2.0f;
        drawApi.drawRoundedRectangleGradient(matrix4f, track.x(), track.y(), f7, this.sliderRenderer.getTrackThickness(), 2.0f, n3, n3, n3, n3);
        this.sliderRenderer.renderThumb(matrix4f, drawApi, f8, f9, n3);
        return bounds2.getY() + bounds2.getHeight();
    }

    @Override
    public float getPreferredWidth() {
        return this.getBounds().getWidth();
    }

    public SingleNumberEditorPopup getEditorPopup() {
        return this.editorPopup;
    }

    @Override
    public PopupPanel getPopup() {
        return this.editorPopup;
    }

    private ScaleSettings scaleSettings(NumberSetting numberSetting) {
        return new ScaleSettings(numberSetting.getMinimum(), numberSetting.getMaximum(), numberSetting.getPrecision(), numberSetting.hasMarkers(), numberSetting.getMarkerStep(), numberSetting.getSnapStep());
    }

    private float getNormalizedValue() {
        NumberSetting numberSetting = (NumberSetting)this.getSetting();
        double d = numberSetting.getMaximum() - numberSetting.getMinimum();
        if (d <= 0.0) {
            return 0.0f;
        }
        return (float)((numberSetting.getValue() - numberSetting.getMinimum()) / d);
    }

    private GuiBounds getContainerBounds() {
        for (GuiElement element2 = this.getParent(); element2 != null; element2 = element2.getParent()) {
            if (!(element2 instanceof BoundsSupplier)) continue;
            BoundsSupplier callback13 = (BoundsSupplier)((Object)element2);
            return callback13.getBounds();
        }
        return null;
    }

    private void setFloatType(float f) {
        GuiBounds bounds2 = this.getContainerBounds();
        NumberSetting numberSetting = (NumberSetting)this.getSetting();
        ScaleSettings scaleSettings = this.scaleSettings(numberSetting);
        float f2 = bounds2.getX() + this.sliderRenderer.getHorizontalPadding();
        float f3 = Math.max(1.0f, bounds2.getWidth() - this.sliderRenderer.getHorizontalPadding() * 2.0f);
        float f4 = Math.max(0.0f, Math.min(f3, f - f2));
        float f5 = f4 / f3;
        double d = this.sliderRenderer.trackFractionToValueFraction(f5, scaleSettings);
        double d2 = numberSetting.getMinimum() + (numberSetting.getMaximum() - numberSetting.getMinimum()) * d;
        if (numberSetting.hasSnapStep()) {
            d2 = NumberFormatting.snap(d2, numberSetting.getMinimum(), numberSetting.getMaximum(), numberSetting.getSnapStep());
        }
        double d3 = NumberFormatting.round(d2, numberSetting.getPrecision());
        double d4 = (d3 - numberSetting.getMinimum()) / (numberSetting.getMaximum() - numberSetting.getMinimum());
        numberSetting.setNormalizedValue(Math.max(0.0, Math.min(1.0, d4)));
    }

    @Override
    public boolean containsScreenPoint(int n, int n2) {
        GuiBounds bounds2 = this.getBounds();
        if (bounds2 != null) {
            return new GuiBounds(bounds2.getX() + this.getBounds().getX(), bounds2.getY() + this.getBounds().getY(), this.getBounds().getWidth(), this.getBounds().getHeight()).contains(n, n2);
        }
        return new GuiBounds(this.getAbsoluteX(), this.getAbsoluteY(), this.getBounds().getWidth(), this.getBounds().getHeight()).contains(n, n2);
    }

    @Override
    public void update2() {
        GuiBounds bounds2 = this.getContainerBounds();
        float f = this.editorAnchorX;
        float f2 = bounds2 != null ? bounds2.getY() + this.getBounds().getY() : this.getAbsoluteY() + this.getBounds().getHeight() + 1.0f;
        this.editorPopup.getBounds().setPosition(f, f2);
    }

    @Override
    public void setPopupManager(PopupManager popupManager) {
        this.popupManager = popupManager;
    }

    @Override
    public float getPreferredHeight() {
        return this.sliderRenderer.heightForMarkers(((NumberSetting)this.getSetting()).hasMarkers());
    }
}

