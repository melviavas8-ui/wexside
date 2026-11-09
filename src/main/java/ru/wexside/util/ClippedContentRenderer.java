package ru.wexside.util;

import java.util.function.Consumer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import ru.wexside.misc.PreparedLayer;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class ClippedContentRenderer {
    private final boolean useSharedLayer;
    private final float startFadeSize;
    private final boolean horizontal;
    private final float endFadeSize;
    private final float edgePadding;

    public ClippedContentRenderer(float edgePadding, float startFadeSize, float endFadeSize, boolean useSharedLayer) {
        this(edgePadding, startFadeSize, endFadeSize, useSharedLayer, false);
    }

    public ClippedContentRenderer(float edgePadding, float startFadeSize, float endFadeSize, boolean useSharedLayer, boolean horizontal) {
        this.edgePadding = Math.max(0.0f, edgePadding);
        this.startFadeSize = Math.max(0.0f, startFadeSize);
        this.endFadeSize = Math.max(0.0f, endFadeSize);
        this.useSharedLayer = useSharedLayer;
        this.horizontal = horizontal;
    }

    public void render(GuiDrawApi renderer, Matrix4f matrix, float x, float y, float width, float height, float scrollOffset, float minimumOffset, Consumer<Matrix4f> drawContent) {
        if (width <= 0.5f || height <= 0.5f) {
            return;
        }
        if (minimumOffset >= -0.01f) {
            drawContent.accept(matrix);
            return;
        }
        float layerX = this.horizontal ? x - this.edgePadding : x;
        float layerY = this.horizontal ? y : y - this.edgePadding;
        float layerWidth = this.horizontal ? width + this.edgePadding : width;
        float layerHeight = this.horizontal ? height : height + this.edgePadding;
        PreparedLayer layer = this.useSharedLayer ? renderer.prepareLayer(matrix, layerX, layerY, layerWidth, layerHeight, 0.0f) : renderer.prepareDedicatedLayer(matrix, layerX, layerY, layerWidth, layerHeight, 0.0f);
        Vector4f transformedBottomLeft = matrix.transform(new Vector4f(layerX, layerY + layerHeight, 0.0f, 1.0f));
        float textureBottomPadding = (float)layer.getTexture().getHeight() * (1.0f - layer.maxV());
        renderer.beginLayerFrame(layer.getTexture(), renderer.getLayerOffsetX() + transformedBottomLeft.x, renderer.getLayerOffsetY() + ((float)renderer.getFramebufferHeight() - transformedBottomLeft.y) - textureBottomPadding);
        Matrix4f contentMatrix = new Matrix4f((Matrix4fc)layer.getContentMatrix()).translate(layer.contentX() - layerX, layer.contentY() - layerY, 0.0f);
        drawContent.accept(contentMatrix);
        renderer.endLayerFrame();
        this.drawFadedLayer(renderer, matrix, layer, layerX, layerY, layerWidth, layerHeight, scrollOffset, minimumOffset);
    }

    private int alphaColor(float alpha) {
        return ColorUtils.withAlpha(-1, (float)Math.max(0, Math.min(255, Math.round(alpha * 255.0f))));
    }

    private void drawFadedLayer(GuiDrawApi renderer, Matrix4f matrix, PreparedLayer layer, float x, float y, float width, float height, float scrollOffset, float minimumOffset) {
        float start = this.horizontal ? x : y;
        float length = this.horizontal ? width : height;
        float endFade = Math.min(this.endFadeSize, Math.max(0.0f, length - this.edgePadding) * 0.5f);
        float endAlpha = 1.0f - ClippedContentRenderer.ratio(scrollOffset - minimumOffset, endFade);
        float endFadeStart = start + length - endFade;
        float opaqueStart = start + this.edgePadding + this.startFadeSize * ClippedContentRenderer.ratio(-scrollOffset, this.startFadeSize);
        if (opaqueStart > endFadeStart) {
            opaqueStart = endFadeStart;
        }
        if (opaqueStart - start > 0.01f) {
            this.drawLayerSlice(renderer, matrix, layer, x, y, width, height, start, opaqueStart, 0.0f, 1.0f);
        }
        if (endFadeStart - opaqueStart > 0.01f) {
            this.drawLayerSlice(renderer, matrix, layer, x, y, width, height, opaqueStart, endFadeStart, 1.0f, 1.0f);
        }
        if (endFade > 0.01f) {
            this.drawLayerSlice(renderer, matrix, layer, x, y, width, height, endFadeStart, start + length, 1.0f, endAlpha);
        }
    }

    private void drawLayerSlice(GuiDrawApi renderer, Matrix4f matrix, PreparedLayer layer, float x, float y, float width, float height, float sliceStart, float sliceEnd, float startAlpha, float endAlpha) {
        int startColor = this.alphaColor(startAlpha);
        int endColor = this.alphaColor(endAlpha);
        if (this.horizontal) {
            float minU = (sliceStart - x) / width * layer.maxU();
            float maxU = (sliceEnd - x) / width * layer.maxU();
            renderer.drawLayerTextureGradient(matrix, layer.getTexture(), sliceStart, y, sliceEnd - sliceStart, height, minU, 1.0f, maxU, 1.0f - layer.maxV(), startColor, endColor, endColor, startColor);
            return;
        }
        float minV = 1.0f - (sliceStart - y) / height * layer.maxV();
        float maxV = 1.0f - (sliceEnd - y) / height * layer.maxV();
        renderer.drawLayerTextureGradient(matrix, layer.getTexture(), x, sliceStart, width, sliceEnd - sliceStart, 0.0f, minV, layer.maxU(), maxV, endColor, endColor, startColor, startColor);
    }

    private static float ratio(float value, float range) {
        if (range <= 0.01f) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value / range));
    }
}

