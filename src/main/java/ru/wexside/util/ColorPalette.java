package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.ColorPaletteEntry;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.setting.ColorSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;

public final class ColorPalette
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private float entryGap = 4.0f;
    private final List<ColorPaletteEntry> entries = new ArrayList<ColorPaletteEntry>();
    private final ColorSetting colorSetting;

    public ColorPalette(GuiBounds bounds2, ColorSetting colorSetting) {
        super(bounds2);
        this.colorSetting = colorSetting;
        for (int i = 0; i < colorSetting.getColorCount(); ++i) {
            ColorPaletteEntry colorPaletteEntry = new ColorPaletteEntry(colorSetting, i);
            this.entries.add(colorPaletteEntry);
            this.addChild(colorPaletteEntry);
        }
        this.layoutEntries();
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        int n4 = (int)((float)n - this.getBounds().getX());
        int n5 = (int)((float)n2 - this.getBounds().getY());
        for (ColorPaletteEntry colorPaletteEntry : this.entries) {
            if (!colorPaletteEntry.onMousePressed(n4, n5, n3)) continue;
            this.colorSetting.setSelectedIndex(colorPaletteEntry.getPaletteIndex());
            return true;
        }
        return false;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        this.layoutEntries();
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(this.getBounds().getX(), this.getBounds().getY(), 0.0f);
        float f2 = this.getBounds().getX() + this.getBounds().getWidth();
        for (ColorPaletteEntry colorPaletteEntry : this.entries) {
            f2 = colorPaletteEntry.render(f, matrix4f2);
        }
        return f2;
    }

    public float getPaletteWidth() {
        this.layoutEntries();
        return this.getBounds().getWidth();
    }

    public float getPaletteHeight() {
        this.layoutEntries();
        return this.getBounds().getHeight();
    }

    private void layoutEntries() {
        float f = 0.0f;
        float f2 = 0.0f;
        for (int i = 0; i < this.entries.size(); ++i) {
            ColorPaletteEntry colorPaletteEntry = this.entries.get(i);
            float f3 = colorPaletteEntry.getSwatchSize();
            colorPaletteEntry.getBounds().setPosition(f, 0.0f);
            colorPaletteEntry.getBounds().setSize(f3, f3);
            f += f3;
            if (i < this.entries.size() - 1) {
                f += this.entryGap;
            }
            f2 = Math.max(f2, f3);
        }
        this.getBounds().setSize(f, f2);
    }
}

