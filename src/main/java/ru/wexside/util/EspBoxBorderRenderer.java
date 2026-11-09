package ru.wexside.util;

import org.joml.Matrix4f;
import ru.wexside.util.GuiDrawApi;

public final class EspBoxBorderRenderer {
    public static final int SHADOW_COLOR = 0x40000000;

    private EspBoxBorderRenderer() {
    }

    public static void drawFullBoxBorder(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n, int n2, int n3, int n4) {
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f, f2, f + 1.5f, f4, 0x40000000);
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f + 1.5f, f2, f3 - 1.5f, f2 + 1.5f, 0x40000000);
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f3 - 1.5f, f2, f3, f4, 0x40000000);
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f + 1.5f, f4 - 1.5f, f3 - 1.5f, f4, 0x40000000);
        EspBoxBorderRenderer.drawVerticalGradientRect(drawApi, matrix4f, f + 0.5f, f2 + 0.5f, f + 1.0f, f4 - 0.5f, n, n4);
        EspBoxBorderRenderer.drawHorizontalGradientRect(drawApi, matrix4f, f + 1.0f, f2 + 0.5f, f3 - 0.5f, f2 + 1.0f, n, n2);
        EspBoxBorderRenderer.drawVerticalGradientRect(drawApi, matrix4f, f3 - 1.0f, f2 + 0.5f, f3 - 0.5f, f4 - 0.5f, n2, n3);
        EspBoxBorderRenderer.drawHorizontalGradientRect(drawApi, matrix4f, f + 1.0f, f4 - 1.0f, f3 - 1.0f, f4 - 0.5f, n4, n3);
    }

    public static void drawVerticalGradientRect(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n, int n2) {
        drawApi.drawColorGradient(matrix4f, f, f2, f3 - f, f4 - f2, n2, n2, n, n);
    }

    public static void drawHorizontalGradientRect(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n, int n2) {
        drawApi.drawColorGradient(matrix4f, f, f2, f3 - f, f4 - f2, n, n2, n2, n);
    }

    public static void drawCornerBoxBorder(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n, int n2, int n3, int n4) {
        float f5 = f3 - f;
        float f6 = f4 - f2;
        float f7 = Math.min(f5 / 4.0f, f6 / 4.0f);
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f, f2, f + 1.5f, f2 + f7, 0x40000000);
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f, f2, f + f7, f2 + 1.5f, 0x40000000);
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f3 - f7, f2, f3, f2 + 1.5f, 0x40000000);
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f3 - 1.5f, f2, f3, f2 + f7, 0x40000000);
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f, f4 - f7, f + 1.5f, f4, 0x40000000);
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f, f4 - 1.5f, f + f7, f4, 0x40000000);
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f3 - 1.5f, f4 - f7, f3, f4, 0x40000000);
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f3 - f7, f4 - 1.5f, f3, f4, 0x40000000);
        EspBoxBorderRenderer.drawVerticalGradientRect(drawApi, matrix4f, f + 0.5f, f2 + 0.5f, f + 1.0f, f2 + f7 - 0.5f, n, n4);
        EspBoxBorderRenderer.drawVerticalGradientRect(drawApi, matrix4f, f + 0.5f, f2 + 0.5f, f + f7 - 0.5f, f2 + 1.0f, n, n4);
        EspBoxBorderRenderer.drawVerticalGradientRect(drawApi, matrix4f, f3 - f7 + 0.5f, f2 + 0.5f, f3 - 0.5f, f2 + 1.0f, n, n2);
        EspBoxBorderRenderer.drawVerticalGradientRect(drawApi, matrix4f, f3 - 1.0f, f2 + 0.5f, f3 - 0.5f, f2 + f7 - 0.5f, n, n2);
        EspBoxBorderRenderer.drawVerticalGradientRect(drawApi, matrix4f, f + 0.5f, f4 - f7 + 0.5f, f + 1.0f, f4 - 0.5f, n2, n3);
        EspBoxBorderRenderer.drawVerticalGradientRect(drawApi, matrix4f, f + 0.5f, f4 - 1.0f, f + f7 - 0.5f, f4 - 0.5f, n2, n3);
        EspBoxBorderRenderer.drawVerticalGradientRect(drawApi, matrix4f, f3 - 1.0f, f4 - f7 + 0.5f, f3 - 0.5f, f4 - 0.5f, n4, n3);
        EspBoxBorderRenderer.drawVerticalGradientRect(drawApi, matrix4f, f3 - f7 + 0.5f, f4 - 1.0f, f3 - 0.5f, f4 - 0.5f, n4, n3);
    }

    public static void fillBounds(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n) {
        drawApi.fillRectangle(matrix4f, f, f2, f3 - f, f4 - f2, n);
    }
}

