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
import ru.wexside.ui.color.ColorPickerButton;
import ru.wexside.ui.setting.SettingComponent;
import ru.wexside.util.ColorPalette;
import ru.wexside.util.GuiDrawApi;

public final class ColorSettingComponent
extends SettingComponent
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final float value;
    private final float value2;
    private final float value3;
    private final float value4;
    private final float value5;
    private final float value6;
    private final float value7;
    private final float value8;
    private final float value9;
    private final float value10;
    private final ColorPalette colorPalette;
    private final String string2;
    private final ColorPickerButton colorPickerButton;
    private final float value11;

    public ColorSettingComponent(ColorSetting colorSetting) {
        super(new GuiBounds(0.0f, 0.0f, 142.5f, 13.0f), colorSetting);
        this.value7 = 142.5f;
        this.value9 = 17.0f;
        this.value11 = 13.0f;
        this.value5 = 8.0f;
        this.value4 = 1.0f;
        this.value6 = 34.5f;
        this.value8 = 17.0f;
        this.value10 = 5.5f;
        this.value = 3.25f;
        this.value2 = 7.0f;
        this.value3 = 2.0f;
        this.string2 = "Z";
        this.colorPalette = new ColorPalette(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f), colorSetting);
        this.colorPickerButton = new ColorPickerButton("Z", 7.0f, colorSetting);
        this.addChild(this.colorPalette);
        this.addChild(this.colorPickerButton);
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
        float f = 35.0f;
        float f2 = 85.0f;
        float f3 = f + (f2 - this.colorPalette.getPaletteWidth());
        float f4 = (13.0f - this.colorPalette.getPaletteHeight()) / 2.0f;
        this.colorPalette.getBounds().setPosition(f3, f4);
        this.colorPalette.getBounds().setSize(this.colorPalette.getPaletteWidth(), this.colorPalette.getPaletteHeight());
        int n4 = (int)((float)n - this.getBounds().getX());
        int n5 = (int)((float)n2 - this.getBounds().getY());
        if (this.colorPalette.onMousePressed(n4, n5, n3)) {
            return true;
        }
        this.colorPickerButton.getBounds().setPosition(125.5f, 0.0f);
        this.colorPickerButton.getBounds().setSize(17.0f, 13.0f);
        if (this.colorPickerButton.onMousePressed(n4, n5, n3)) {
            return true;
        }
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        float f2;
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        int n = ThemeColors.borderPrimary();
        String string = ((ColorSetting)this.getSetting()).getModeLabel();
        float f3 = bounds2.getX() + 34.5f;
        float f4 = bounds2.getX() + bounds2.getWidth() - 17.0f;
        float f5 = 30.5f;
        for (f2 = 5.5f; f2 > 3.25f && FontRegistry.font2.textWidth(string, f2) > f5; f2 -= 0.25f) {
        }
        float f6 = FontRegistry.font2.textWidth(string, f2);
        float f7 = FontRegistry.font2.textHeight(string, f2);
        float f8 = bounds2.getX() + (34.5f - f6) / 2.0f;
        float f9 = bounds2.getY() + (13.0f - f7) / 2.0f;
        float f10 = 35.0f;
        float f11 = bounds2.getWidth() - 34.5f - 17.0f - 6.0f;
        float f12 = f10 + (f11 - this.colorPalette.getPaletteWidth());
        float f13 = (13.0f - this.colorPalette.getPaletteHeight()) / 2.0f;
        this.colorPalette.getBounds().setPosition(f12, f13);
        this.colorPalette.getBounds().setSize(this.colorPalette.getPaletteWidth(), this.colorPalette.getPaletteHeight());
        this.colorPickerButton.getBounds().setPosition(bounds2.getWidth() - 17.0f, 0.0f);
        this.colorPickerButton.getBounds().setSize(17.0f, 13.0f);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), 13.0f, 8.0f, 1.0f, ThemeColors.controlFill(), n);
        drawApi.fillRectangle(matrix4f, f3, bounds2.getY(), 0.5f, 13.0f, n);
        drawApi.fillRectangle(matrix4f, f4, bounds2.getY(), 0.5f, 13.0f, n);
        FontRegistry.font2.drawText(matrix4f, drawApi, string, f8, f9, f2, ThemeColors.textSecondary());
        this.colorPalette.render(f, new Matrix4f((Matrix4fc)matrix4f).translate(bounds2.getX(), bounds2.getY(), 0.0f));
        this.colorPickerButton.render(f, new Matrix4f((Matrix4fc)matrix4f).translate(bounds2.getX(), bounds2.getY(), 0.0f));
        return bounds2.getY() + 13.0f;
    }

    @Override
    public float getPreferredWidth() {
        return 142.5f;
    }

    @Override
    public float getPreferredHeight() {
        return 17.0f;
    }
}

