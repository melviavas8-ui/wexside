package ru.wexside.misc;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.color.ColorChannel;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.SegmentedControl;
import ru.wexside.util.SegmentedControlStyle;

public final class ColorModeSelector
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final ColorSetting colorSetting;
    private final SegmentedControl segmentedControl;
    private final float value;
    private final SegmentedControlStyle segmentedControlStyle;

    public ColorModeSelector(GuiBounds bounds2, ColorSetting colorSetting) {
        super(bounds2);
        this.value = 11.0f;
        this.colorSetting = colorSetting;
        this.segmentedControlStyle = new SegmentedControlStyle().withBackgroundRadius(7.0f).withSelectionRadius(6.0f);
        this.segmentedControl = new SegmentedControl(new GuiBounds(0.0f, 0.0f, 0.0f, 11.0f), this.segmentedControlStyle, "Первый цвет", "Второй цвет");
        this.segmentedControl.setIntConsumer(n -> colorSetting.setEditingChannel(n == 1 ? ColorChannel.SECONDARY : ColorChannel.PRIMARY));
        this.syncChannel();
        this.addChild(this.segmentedControl);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
        this.segmentedControl.update();
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        int n4 = (int)((float)n - this.getBounds().getX());
        int n5 = (int)((float)n2 - this.getBounds().getY());
        return this.segmentedControl.onMousePressed(n4, n5, n3);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        this.layoutSegments();
        this.syncChannel();
        this.segmentedControl.render(f, new Matrix4f((Matrix4fc)matrix4f).translate(this.getBounds().getX(), this.getBounds().getY(), 0.0f));
        return this.getBounds().getY() + this.getBounds().getHeight();
    }

    private void syncChannel() {
        this.segmentedControl.setIntType2(this.colorSetting.getEditingChannel() == ColorChannel.SECONDARY ? 1 : 0);
    }

    public float getPreferredHeight() {
        return 11.0f;
    }

    private void layoutSegments() {
        float f = this.getBounds().getWidth() / 2.0f;
        this.segmentedControlStyle.withSegmentWidth(f);
        this.segmentedControlStyle.withSegmentHeight(11.0f);
        this.segmentedControl.getBounds().setPosition(0.0f, 0.0f);
        this.segmentedControl.getBounds().setSize(this.getBounds().getWidth(), 11.0f);
        this.getBounds().setSize(this.getBounds().getWidth(), 11.0f);
    }
}

