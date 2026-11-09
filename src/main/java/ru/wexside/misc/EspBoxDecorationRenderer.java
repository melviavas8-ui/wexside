package ru.wexside.misc;

import org.joml.Matrix4f;
import ru.wexside.misc.BoxEspSettings;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.EspBoxBorderRenderer;
import ru.wexside.util.GuiDrawApi;

final class EspBoxDecorationRenderer {
    private static final float value = 0.6f;
    private static final float value2 = 0.7f;

    EspBoxDecorationRenderer() {
    }

    void member9165(GuiDrawApi drawApi, Matrix4f matrix4f, GuiBounds bounds2, BoxEspSettings rectangle) {
        float f = bounds2.getX();
        float f2 = bounds2.getY();
        float f3 = bounds2.getX() + bounds2.getWidth();
        float f4 = bounds2.getY() + bounds2.getHeight();
        if (rectangle.isBoxEnabled()) {
            int n = rectangle.getBoxColor(0.0f);
            int n2 = rectangle.getBoxColor(0.25f);
            int n3 = rectangle.getBoxColor(0.5f);
            int n4 = rectangle.getBoxColor(0.75f);
            if (rectangle.isCornerStyle()) {
                EspBoxBorderRenderer.drawCornerBoxBorder(drawApi, matrix4f, f, f2, f3, f4, n, n2, n3, n4);
            } else {
                EspBoxBorderRenderer.drawFullBoxBorder(drawApi, matrix4f, f, f2, f3, f4, n, n2, n3, n4);
            }
        }
        if (rectangle.isArmorBarEnabled()) {
            EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f, f4 + 0.5f, f3, f4 + 2.0f + 0.5f, 0x40000000);
            EspBoxBorderRenderer.drawHorizontalGradientRect(drawApi, matrix4f, f + 0.5f, f4 + 1.0f, f + 0.5f + (f3 - 0.5f - (f + 0.5f)) * 0.6f, f4 + 2.0f, rectangle.getArmorColor(0.25f), rectangle.getArmorColor(0.75f));
        }
        if (rectangle.isHealthBarEnabled()) {
            EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f - 2.5f, f2, f - 0.5f, f4, 0x40000000);
            EspBoxBorderRenderer.drawVerticalGradientRect(drawApi, matrix4f, f - 2.0f, f2 + 0.5f + (f4 - 1.0f - f2) - (f4 - 1.0f - f2) * 0.7f, f - 1.0f, f4 - 0.5f, rectangle.getHealthColor(0.0f), rectangle.getHealthColor(0.5f));
        }
    }
}

