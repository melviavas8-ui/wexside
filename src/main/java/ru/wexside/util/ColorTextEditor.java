package ru.wexside.util;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.WexSideClient;
import ru.wexside.misc.AbstractColorTextEditor;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.ColorTextFormat;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.setting.ColorSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ClippedLayerRenderer;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.HexColorEditor;
import ru.wexside.util.RgbaColorEditor;

public final class ColorTextEditor
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private ColorTextFormat colorTextFormat;
    private final float value;
    private final RgbaColorEditor rgbaColorEditor;
    private float rgbaTransitionProgress;
    private final HexColorEditor hexColorEditor;

    public ColorTextEditor(GuiBounds bounds2, ColorSetting colorSetting, ColorTextFormat colorTextFormat) {
        super(bounds2);
        this.value = 30.0f;
        this.hexColorEditor = new HexColorEditor(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f), colorSetting);
        this.rgbaColorEditor = new RgbaColorEditor(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f), colorSetting);
        this.colorTextFormat = colorTextFormat;
        this.rgbaTransitionProgress = colorTextFormat == ColorTextFormat.RGBA ? 1.0f : 0.0f;
        this.addChild(this.hexColorEditor);
        this.addChild(this.rgbaColorEditor);
        this.layoutEditors();
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
        this.editorForFormat(this.colorTextFormat).update();
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        int n4 = (int)((float)n - this.getBounds().getX());
        int n5 = (int)((float)n2 - this.getBounds().getY());
        return this.editorForFormat(this.colorTextFormat).onMousePressed(n4, n5, n3) || this.getBounds().contains(n, n2);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        this.layoutEditors();
        this.rgbaTransitionProgress = FrameInterpolator.lerpTowards(this.rgbaTransitionProgress, this.colorTextFormat == ColorTextFormat.RGBA ? 1.0f : 0.0f, 30.0f);
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(this.getBounds().getX(), this.getBounds().getY(), 0.0f);
        this.renderFadingEditor(f, matrix4f2, this.hexColorEditor, 1.0f - this.rgbaTransitionProgress);
        this.renderFadingEditor(f, matrix4f2, this.rgbaColorEditor, this.rgbaTransitionProgress);
        return this.getBounds().getY() + this.getBounds().getHeight();
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        int n4 = (int)((float)n - this.getBounds().getX());
        int n5 = (int)((float)n2 - this.getBounds().getY());
        this.hexColorEditor.onMouseReleased(n4, n5, n3);
        this.rgbaColorEditor.onMouseReleased(n4, n5, n3);
    }

    @Override
    public boolean onCharTyped(char c) {
        return this.editorForFormat(this.colorTextFormat).onCharTyped(c);
    }

    @Override
    public void update2() {
        this.hexColorEditor.update2();
        this.rgbaColorEditor.update2();
    }

    @Override
    public boolean onKeyPressed(int n) {
        return this.editorForFormat(this.colorTextFormat).onKeyPressed(n);
    }

    public float getFloatType() {
        return Math.max(this.hexColorEditor.getPreferredWidth(), this.rgbaColorEditor.getPreferredWidth());
    }

    private AbstractColorTextEditor editorForFormat(ColorTextFormat colorTextFormat) {
        return colorTextFormat == ColorTextFormat.RGBA ? this.rgbaColorEditor : this.hexColorEditor;
    }

    public void setColorTextFormat(ColorTextFormat colorTextFormat) {
        ColorTextFormat colorTextFormat2;
        ColorTextFormat colorTextFormat3 = colorTextFormat2 = colorTextFormat == null ? ColorTextFormat.HEX : colorTextFormat;
        if (this.colorTextFormat == colorTextFormat2) {
            return;
        }
        this.editorForFormat(this.colorTextFormat).update2();
        this.colorTextFormat = colorTextFormat2;
    }

    private void renderFadingEditor(float f, Matrix4f matrix4f2, AbstractColorTextEditor abstractColorTextEditor, float f2) {
        if (f2 <= 0.01f) {
            return;
        }
        ClippedLayerRenderer.renderLayer(WexSideClient.getGuiRenderer(), matrix4f2, abstractColorTextEditor.getBounds().getX(), abstractColorTextEditor.getBounds().getY(), abstractColorTextEditor.getBounds().getWidth(), abstractColorTextEditor.getBounds().getHeight(), 0.0f, f2 < 0.99f, ColorUtils.withAlpha(-1, 255.0f * f2), matrix4f -> abstractColorTextEditor.render(f, (Matrix4f)matrix4f));
    }

    public float getFloatType2() {
        return this.editorForFormat(this.colorTextFormat).getPreferredHeight();
    }

    private void layoutEditors() {
        float f = this.getFloatType();
        float f2 = this.getFloatType2();
        this.hexColorEditor.getBounds().setPosition(0.0f, 0.0f);
        this.hexColorEditor.getBounds().setSize(f, f2);
        this.rgbaColorEditor.getBounds().setPosition(0.0f, 0.0f);
        this.rgbaColorEditor.getBounds().setSize(f, f2);
        this.getBounds().setSize(f, f2);
    }
}

