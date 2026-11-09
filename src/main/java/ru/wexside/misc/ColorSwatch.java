package ru.wexside.misc;

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
import ru.wexside.util.RecentColorPalette;
import ru.wexside.util.ToggleIndicatorRenderer;

public final class ColorSwatch
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final ColorSetting colorSetting;
    private final ToggleIndicatorRenderer toggleIndicatorRenderer = new ToggleIndicatorRenderer();
    private final int slot;
    private final RecentColorPalette recentColorPalette;
    private final float value;

    public ColorSwatch(ColorSetting colorSetting, int n) {
        super(new GuiBounds(0.0f, 0.0f, 7.5f, 7.5f));
        this.value = 7.5f;
        this.colorSetting = colorSetting;
        this.recentColorPalette = colorSetting.getRecentColors();
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
        if (n3 != 0 || !this.getBounds().contains(n, n2)) {
            return false;
        }
        int n4 = this.recentColorPalette.getColor(this.slot);
        this.colorSetting.setEditingColor(n4);
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        int n = this.recentColorPalette.getColor(this.slot);
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.toggleIndicatorRenderer.render(matrix4f, drawApi, bounds2, n, n == this.colorSetting.getColor());
        return bounds2.getX() + bounds2.getWidth();
    }

    public float getSwatchSize() {
        return 7.5f;
    }
}

