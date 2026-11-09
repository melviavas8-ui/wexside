package ru.wexside.misc;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.setting.ColorSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorPaletteGrid;
import ru.wexside.util.GuiDrawApi;

public final class ColorPreviewButton
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final float value;
    private final String string;
    private final ColorPaletteGrid colorPaletteGrid;
    private final float value2;

    public ColorPreviewButton(GuiBounds bounds2, ColorSetting colorSetting) {
        super(bounds2);
        this.string = "Предыдущие цвета";
        this.value2 = 5.5f;
        this.value = 5.0f;
        this.colorPaletteGrid = new ColorPaletteGrid(new GuiBounds(0.0f, 0.0f, bounds2.getWidth(), 0.0f), colorSetting);
        this.addChild(this.colorPaletteGrid);
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
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        int n4 = (int)((float)n - this.getBounds().getX());
        int n5 = (int)((float)n2 - this.getBounds().getY());
        return super.onMousePressed(n4, n5, n3);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        this.layoutRecentColors();
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(this.getBounds().getX(), this.getBounds().getY(), 0.0f);
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        FontRegistry.font4.drawText(matrix4f2, drawApi, "Предыдущие цвета", 0.0f, 0.0f, 5.5f, ThemeColors.textMuted());
        this.renderChildren(f, matrix4f2);
        return this.getBounds().getY() + this.getBounds().getHeight();
    }

    private void renderChildren(float f, Matrix4f matrix4f) {
        for (GuiElement element2 : this.children) {
            element2.render(f, matrix4f);
        }
    }

    public float getPreferredHeight() {
        this.layoutRecentColors();
        return this.getBounds().getHeight();
    }

    private void layoutRecentColors() {
        float f = FontRegistry.font4.textHeight("Предыдущие цвета", 5.5f);
        float f2 = f + 5.0f;
        this.colorPaletteGrid.getBounds().setPosition(0.0f, f2);
        this.colorPaletteGrid.getBounds().setSize(this.getBounds().getWidth(), this.colorPaletteGrid.getBounds().getHeight());
        float f3 = f2 + this.colorPaletteGrid.getPaletteHeight();
        this.getBounds().setSize(this.getBounds().getWidth(), f3);
    }
}

