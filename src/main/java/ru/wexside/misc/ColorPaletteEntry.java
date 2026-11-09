package ru.wexside.misc;

import java.util.Objects;
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
import ru.wexside.ui.GuiElement;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.ToggleIndicatorRenderer;

public final class ColorPaletteEntry
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final ColorSetting colorSetting;
    private final float value;
    private final int slot;
    private final ToggleIndicatorRenderer toggleIndicatorRenderer = new ToggleIndicatorRenderer();

    public ColorPaletteEntry(ColorSetting colorSetting, int n) {
        super(new GuiBounds(0.0f, 0.0f, 5.0f, 5.0f));
        this.value = 5.0f;
        this.colorSetting = colorSetting;
        this.slot = n;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        return n3 == 0 && this.getBounds().contains(n, n2);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        boolean bl = this.colorSetting.getSelectedIndex() == this.slot;
        this.toggleIndicatorRenderer.render(matrix4f, drawApi, bounds2, this.getPaletteColor(), bl);
        return bounds2.getX() + bounds2.getWidth();
    }

    public int getPaletteIndex() {
        return this.slot;
    }

    public float getSwatchSize() {
        return 5.0f;
    }

    public ToggleIndicatorRenderer getToggleIndicatorRenderer() {
        return this.toggleIndicatorRenderer;
    }

    private int getPaletteColor() {
        return this.colorSetting.getPrimaryColor(this.slot);
    }

    public ColorSetting getColorSetting() {
        return this.colorSetting;
    }

    public float getDefaultSwatchSize() {
        Objects.requireNonNull(this);
        return 5.0f;
    }
}

