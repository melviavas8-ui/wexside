package ru.wexside.util;

import java.util.function.Consumer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import ru.wexside.misc.PreparedLayer;
import ru.wexside.util.GuiDrawApi;

public final class ClippedLayerRenderer {
    public static void renderLayer(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, boolean bl, int n, Consumer<Matrix4f> consumer) {
        float f6 = Math.max(Math.abs(matrix4f.m00()), 1.0f);
        float f7 = Math.max(Math.abs(matrix4f.m11()), 1.0f);
        float f8 = ClippedLayerRenderer.snapToPixel(f, f6);
        float f9 = ClippedLayerRenderer.snapToPixel(f2, f7);
        if (!bl) {
            consumer.accept(new Matrix4f((Matrix4fc)matrix4f).translate(f8, f9, 0.0f));
            return;
        }
        PreparedLayer preparedLayer = drawApi.prepareDedicatedLayer(matrix4f, f8, f9, f3, f4, f5);
        float f10 = f8 - f5;
        float f11 = f9 + f4 + f5;
        Vector4f vector4f = matrix4f.transform(new Vector4f(f10, f11, 0.0f, 1.0f));
        float f12 = (float)preparedLayer.getTexture().getHeight() * (1.0f - preparedLayer.maxV());
        float f13 = drawApi.getLayerOffsetX() + vector4f.x;
        float f14 = drawApi.getLayerOffsetY() + ((float)drawApi.getFramebufferHeight() - vector4f.y) - f12;
        drawApi.beginLayerFrame(preparedLayer.getTexture(), f13, f14);
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)preparedLayer.getContentMatrix()).translate(preparedLayer.contentX(), preparedLayer.contentY(), 0.0f);
        consumer.accept(matrix4f2);
        drawApi.endLayerFrame();
        drawApi.drawPreparedLayer(matrix4f, preparedLayer, n);
        drawApi.flushPending();
    }

    private static float snapToPixel(float f, float f2) {
        return (float)Math.round(f * f2) / f2;
    }
}

