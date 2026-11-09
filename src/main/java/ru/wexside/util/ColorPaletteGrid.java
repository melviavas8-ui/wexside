package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.ColorSwatch;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.setting.ColorSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;

public final class ColorPaletteGrid
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final float value;
    private final List<ColorSwatch> swatches = new ArrayList<ColorSwatch>();
    private final int slot;
    private int visibleSwatchCount;
    private final float value2;

    public ColorPaletteGrid(GuiBounds bounds2, ColorSetting colorSetting) {
        super(bounds2);
        this.value2 = 4.0f;
        this.value = 4.0f;
        this.slot = 2;
        for (int i = 0; i < colorSetting.getRecentColors().size(); ++i) {
            ColorSwatch colorSwatch = new ColorSwatch(colorSetting, i);
            this.swatches.add(colorSwatch);
            this.addChild(colorSwatch);
        }
        this.layoutSwatches();
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
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
        this.layoutSwatches();
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(this.getBounds().getX(), this.getBounds().getY(), 0.0f);
        float f2 = this.getBounds().getX() + this.getBounds().getWidth();
        for (int i = 0; i < this.visibleSwatchCount; ++i) {
            f2 = this.swatches.get(i).render(f, matrix4f2);
        }
        return f2;
    }

    private float horizontalSwatchGap(int n, float f) {
        if (n <= 1) {
            return 0.0f;
        }
        return (this.getBounds().getWidth() - (float)n * f) / (float)(n - 1);
    }

    private int swatchColumnCount(float f) {
        float f2 = f + 4.0f;
        if (f2 <= 0.0f) {
            return 1;
        }
        return Math.max(1, (int)Math.floor((this.getBounds().getWidth() + 4.0f) / f2));
    }

    public float getPaletteHeight() {
        this.layoutSwatches();
        return this.getBounds().getHeight();
    }

    private void layoutSwatches() {
        if (this.swatches.isEmpty()) {
            this.getBounds().setSize(this.getBounds().getWidth(), 0.0f);
            this.visibleSwatchCount = 0;
            return;
        }
        float f = this.swatches.get(0).getSwatchSize();
        int n = this.swatchColumnCount(f);
        float f2 = this.horizontalSwatchGap(n, f);
        this.visibleSwatchCount = Math.min(this.swatches.size(), n * 2);
        int n2 = Math.max(1, (int)Math.ceil((double)this.visibleSwatchCount / (double)n));
        for (int i = 0; i < this.swatches.size(); ++i) {
            ColorSwatch colorSwatch = this.swatches.get(i);
            boolean bl = i < this.visibleSwatchCount;
            int n3 = i / n;
            int n4 = i % n;
            if (!bl) {
                colorSwatch.getBounds().setPosition(-9999.0f, -9999.0f);
                continue;
            }
            float f3 = (float)n4 * (f + f2);
            float f4 = (float)n3 * (f + 4.0f);
            colorSwatch.getBounds().setPosition(f3, f4);
            colorSwatch.getBounds().setSize(f, f);
        }
        float f5 = (float)n2 * f + (float)Math.max(0, n2 - 1) * 4.0f;
        this.getBounds().setSize(this.getBounds().getWidth(), f5);
    }
}

