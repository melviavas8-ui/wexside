package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.ColorModeLabels;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.setting.color.ColorMode;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorModeOptionButton;

public final class DoubleColor
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private ColorMode colorMode2;
    private final float value;
    private Consumer<ColorMode> consumer;
    private final List<ColorModeOptionButton> modeButtons = new ArrayList<ColorModeOptionButton>();
    private final float value2;
    private final float value3;

    public DoubleColor(GuiBounds bounds2) {
        super(bounds2);
        this.value = 12.5f;
        this.value3 = 9.5f;
        this.value2 = 2.0f;
        this.colorMode2 = ColorMode.STATIC;
        this.consumer = colorMode2 -> {};
        this.createModeButtons();
        this.layoutModeButtons();
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
        int n4 = (int)((float)n - this.getBounds().getX());
        int n5 = (int)((float)n2 - this.getBounds().getY());
        for (ColorModeOptionButton colorModeOptionButton : this.modeButtons) {
            if (!colorModeOptionButton.onMousePressed(n4, n5, n3)) continue;
            ColorMode colorMode2 = colorModeOptionButton.getColorModeLabels().getColorMode();
            this.setColorMode(colorMode2 == this.colorMode2 ? ColorMode.STATIC : colorMode2);
            return true;
        }
        return false;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        this.layoutModeButtons();
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(this.getBounds().getX(), this.getBounds().getY(), 0.0f);
        for (ColorModeOptionButton colorModeOptionButton : this.modeButtons) {
            colorModeOptionButton.render(f, matrix4f2);
        }
        return this.getBounds().getY() + this.getBounds().getHeight();
    }

    public float getPreferredWidth() {
        if (this.modeButtons.isEmpty()) {
            return 0.0f;
        }
        return (float)this.modeButtons.size() * 12.5f + (float)Math.max(0, this.modeButtons.size() - 1) * 2.0f;
    }

    private void syncSelection() {
        for (ColorModeOptionButton colorModeOptionButton : this.modeButtons) {
            colorModeOptionButton.setBooleanType(colorModeOptionButton.getColorModeLabels().getColorMode() == this.colorMode2);
        }
    }

    public void setColorMode(ColorMode colorMode2) {
        ColorMode colorMode3;
        ColorMode colorMode4 = colorMode3 = colorMode2 == null ? ColorMode.STATIC : colorMode2;
        if (this.colorMode2 == colorMode3) {
            return;
        }
        this.colorMode2 = colorMode3;
        this.syncSelection();
        this.consumer.accept(this.colorMode2);
    }

    public void setConsumer(Consumer<ColorMode> consumer) {
        this.consumer = consumer == null ? colorMode2 -> {} : consumer;
    }

    public float getSpacing() {
        Objects.requireNonNull(this);
        return 2.0f;
    }

    public Consumer<ColorMode> getConsumer() {
        return this.consumer;
    }

    private void createModeButtons() {
        List<ColorModeLabels> list = List.of(new ColorModeLabels(ColorMode.ASTOLFO, "Astolfo", "д"), new ColorModeLabels(ColorMode.DOUBLE_COLOR, "Double Color", "V"));
        for (ColorModeLabels colorModeLabels : list) {
            ColorModeOptionButton colorModeOptionButton = new ColorModeOptionButton(new GuiBounds(0.0f, 0.0f, 12.5f, 9.5f), colorModeLabels);
            this.modeButtons.add(colorModeOptionButton);
            this.addChild(colorModeOptionButton);
        }
        this.syncSelection();
    }

    public ColorMode getColorMode() {
        return this.colorMode2;
    }

    public float getOptionHeight() {
        Objects.requireNonNull(this);
        return 9.5f;
    }

    public float getOptionWidth() {
        Objects.requireNonNull(this);
        return 12.5f;
    }

    public List<ColorModeOptionButton> getList() {
        return this.modeButtons;
    }

    public float getPreferredHeight() {
        return 9.5f;
    }

    private void layoutModeButtons() {
        float f = 0.0f;
        for (ColorModeOptionButton colorModeOptionButton : this.modeButtons) {
            colorModeOptionButton.getBounds().setPosition(f, 0.0f);
            colorModeOptionButton.getBounds().setSize(12.5f, 9.5f);
            f += 14.5f;
        }
        this.getBounds().setSize(this.getPreferredWidth(), this.getPreferredHeight());
    }
}

