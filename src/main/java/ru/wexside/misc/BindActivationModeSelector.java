package ru.wexside.misc;

import java.util.function.Consumer;
import org.joml.Matrix4f;
import ru.wexside.misc.BindActivationMode;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.SegmentedControl;
import ru.wexside.util.SegmentedControlStyle;

public final class BindActivationModeSelector
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final Consumer<BindActivationMode> consumer;
    private final SegmentedControl segmentedControl;

    public BindActivationModeSelector(BindActivationMode bindActivationMode, Consumer<BindActivationMode> consumer) {
        super(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f));
        this.consumer = consumer == null ? bindActivationMode2 -> {} : consumer;
        SegmentedControlStyle segmentedControlStyle = new SegmentedControlStyle();
        this.segmentedControl = new SegmentedControl(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f), segmentedControlStyle, "HOLD", "TOGGLE");
        this.segmentedControl.setIntType2(BindActivationModeSelector.toIndex(bindActivationMode));
        this.segmentedControl.setIntConsumer(this::selectModeIndex);
        this.addChild(this.segmentedControl);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        this.segmentedControl.onMouseScroll(n, n2, d);
    }

    @Override
    public void update() {
        this.segmentedControl.update();
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        this.segmentedControl.setFloatType(bounds2.getWidth() / (float)this.segmentedControl.getList().size());
        this.segmentedControl.getBounds().setPosition(bounds2.getX(), bounds2.getY());
        this.segmentedControl.render(f, matrix4f);
        return bounds2.getY() + bounds2.getHeight();
    }

    public SegmentedControl getSegmentedControl() {
        return this.segmentedControl;
    }

    private static BindActivationMode fromIndex(int index) {
        return index == 0 ? BindActivationMode.HOLD : BindActivationMode.TOGGLE;
    }

    private void selectModeIndex(int n) {
        this.consumer.accept(BindActivationModeSelector.fromIndex(n));
    }

    private static int toIndex(BindActivationMode activationMode) {
        return activationMode == BindActivationMode.HOLD ? 0 : 1;
    }

    public Consumer<BindActivationMode> getConsumer() {
        return this.consumer;
    }

    public float getPreferredHeight() {
        return this.segmentedControl.getBounds().getHeight();
    }
}

