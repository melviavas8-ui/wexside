package ru.wexside.misc;

import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.ColorTextFormat;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.setting.ColorSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorTextEditor;
import ru.wexside.util.SegmentedControl;

public final class ColorFormatSelector
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final SegmentedControl segmentedControl;
    private final float value;
    private final ColorTextEditor colorTextEditor;
    private final float value2;

    public ColorFormatSelector(GuiBounds bounds2, ColorSetting colorSetting) {
        super(bounds2);
        this.value2 = 2.0f;
        this.value = 12.0f;
        this.segmentedControl = new SegmentedControl(new GuiBounds(0.0f, 0.0f, 0.0f, 12.0f), ColorTextFormat.HEX.title, ColorTextFormat.RGBA.title);
        this.colorTextEditor = new ColorTextEditor(new GuiBounds(0.0f, 0.0f, 0.0f, 12.0f), colorSetting, ColorTextFormat.HEX);
        this.segmentedControl.setIntConsumer(n -> this.colorTextEditor.setColorTextFormat(ColorTextFormat.fromIndex(n)));
        this.addChild(this.segmentedControl);
        this.addChild(this.colorTextEditor);
        this.layoutControls();
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
        for (GuiElement element2 : this.children) {
            element2.update();
        }
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        int n4;
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        int n5 = (int)((float)n - this.getBounds().getX());
        return super.onMousePressed(n5, n4 = (int)((float)n2 - this.getBounds().getY()), n3) || this.getBounds().contains(n, n2);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        this.layoutControls();
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(this.getBounds().getX(), this.getBounds().getY(), 0.0f);
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

    public float getPreferredWidth() {
        return this.segmentedControl.getSegmentedControlStyle().getSegmentWidth() * (float)ColorTextFormat.values().length + 2.0f + this.colorTextEditor.getFloatType();
    }

    public SegmentedControl getSegmentedControl() {
        return this.segmentedControl;
    }

    public float getSpacing() {
        Objects.requireNonNull(this);
        return 2.0f;
    }

    public ColorTextEditor getColorTextEditor() {
        return this.colorTextEditor;
    }

    public float getRowHeight() {
        Objects.requireNonNull(this);
        return 12.0f;
    }

    public float getPreferredHeight() {
        return 12.0f;
    }

    private void layoutControls() {
        float f = this.segmentedControl.getSegmentedControlStyle().getSegmentWidth() * (float)ColorTextFormat.values().length;
        float f2 = this.colorTextEditor.getFloatType();
        this.segmentedControl.getBounds().setPosition(0.0f, 0.0f);
        this.segmentedControl.getBounds().setSize(f, 12.0f);
        this.colorTextEditor.getBounds().setPosition(f + 2.0f, 0.0f);
        this.colorTextEditor.getBounds().setSize(f2, 12.0f);
        this.getBounds().setSize(f + 2.0f + f2, 12.0f);
    }
}

