package ru.wexside.util;

import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix4f;
import ru.wexside.misc.MsdfFont;
import ru.wexside.misc.MsdfGlyph;
import ru.wexside.render.GuiDrawMode;
import ru.wexside.render.TextGradient;
import ru.wexside.util.GuiDrawApi;

public class MsdfFontRenderer {
    static final int MEASURE_CACHE_LIMIT = 4096;
    private final MsdfFont font;
    private final Map<String, Float> widthCache = new HashMap<String, Float>();

    public MsdfFontRenderer(MsdfFont msdfFont) {
        this.font = msdfFont;
    }

    public float lineBlockHeight(String string, float f) {
        int n = 1;
        for (int i = 0; i < string.length(); ++i) {
            if (string.charAt(i) != '\n') continue;
            ++n;
        }
        return (float)n * this.font.getMetrics().getLineHeight() * f;
    }

    public void drawText(Matrix4f matrix4f, GuiDrawApi drawApi, String string, float f, float f2, float f3, int n) {
        this.drawStyledText(matrix4f, drawApi, string, f, f2, f3, 0.0f, 0.0f, n, n);
    }

    public float textWidth(String string, float f) {
        Float f2 = this.widthCache.get(string);
        if (f2 != null) {
            return f2.floatValue() * f;
        }
        float f3 = this.measureTextAdvance(string, 1.0f);
        if (this.widthCache.size() >= 4096) {
            this.widthCache.clear();
        }
        this.widthCache.put(string, Float.valueOf(f3));
        return f3 * f;
    }

    public float textHeight(String string, float f) {
        return this.roundHalfPixel(this.lineBlockHeight(string, f));
    }

    public void drawGlyphText(Matrix4f matrix4f, GuiDrawApi drawApi, String string, float f, float f2, float f3, int n) {
        this.drawStyledText(matrix4f, drawApi, string, f, f2, f3, 0.0f, 0.0f, n, n);
    }

    private float roundHalfPixel(float f) {
        return (float)Math.round(f * 2.0f) / 2.0f;
    }

    public void update() {
        if (this.font != null && this.font.getTexture() != null) {
            this.font.getTexture().getTextureId();
        }
    }

    public void drawStyledText(Matrix4f matrix4f, GuiDrawApi drawApi, String string, float f, float f2, float f3, float f4, float f5, int n, int n2) {
        int n3 = drawApi.bindTexture(this.font.getTexture().getTextureId(), this.font.getTexture().getWidth(), this.font.getTexture().getHeight());
        float f6 = this.font.getTexture().getWidth();
        float f7 = this.font.getTexture().getHeight();
        float f8 = this.font.getMsdfAtlas().getDistanceRange();
        float f9 = (float)(n2 >> 16 & 0xFF) / 255.0f;
        float f10 = (float)(n2 >> 8 & 0xFF) / 255.0f;
        float f11 = (float)(n2 & 0xFF) / 255.0f;
        float f12 = (float)(n2 >> 24 & 0xFF) / 255.0f;
        float f13 = (float)(n >> 16 & 0xFF) / 255.0f;
        float f14 = (float)(n >> 8 & 0xFF) / 255.0f;
        float f15 = (float)(n & 0xFF) / 255.0f;
        float f16 = (float)(n >> 24 & 0xFF) / 255.0f;
        float f17 = f;
        float f18 = f2 + this.font.getMetrics().getBaselineOffset() * f3;
        for (int i = 0; i < string.length(); ++i) {
            MsdfGlyph msdfGlyph;
            char c = string.charAt(i);
            if (c == '\n') {
                f17 = f;
                f18 += this.font.getMetrics().getLineHeight() * f3;
                continue;
            }
            if (c == ' ') {
                msdfGlyph = this.font.getGlyph(32);
                f17 += (msdfGlyph != null ? msdfGlyph.getAdvance() : this.font.getMetrics().getLineHeight() * 0.25f) * f3;
                continue;
            }
            msdfGlyph = this.font.getGlyph(c);
            if (msdfGlyph == null) continue;
            float f19 = msdfGlyph.getPlaneWidth() * f3;
            float f20 = msdfGlyph.getPlaneHeight() * f3;
            float f21 = f17 + msdfGlyph.getPlaneLeft() * f3;
            float f22 = f18 - msdfGlyph.getPlaneTop() * f3;
            drawApi.getGuiVertexBuffer().appendQuad(matrix4f, f21, f22, f19, f20, msdfGlyph.getU0(), msdfGlyph.getV0(), msdfGlyph.getU1(), msdfGlyph.getV1(), 0.0f, 0.0f, 0.0f, 0.0f, f4, f5, 0.2f, f3, f8, f6, f7, this.packArgb(f16, f13, f14, f15), this.packArgb(f12, f9, f10, f11), this.packArgb(f12, f9, f10, f11), this.packArgb(f12, f9, f10, f11), this.packArgb(f12, f9, f10, f11), n3, GuiDrawMode.MSDF_TEXT);
            f17 += msdfGlyph.getAdvance() * f3;
        }
    }

    public void drawGradientTextInRange(Matrix4f matrix4f, GuiDrawApi drawApi, String string, float f, float f2, float f3, float f4, int n, TextGradient textGradient, float f5, float f6) {
        int n2 = drawApi.bindTexture(this.font.getTexture().getTextureId(), this.font.getTexture().getWidth(), this.font.getTexture().getHeight());
        float f7 = this.font.getTexture().getWidth();
        float f8 = this.font.getTexture().getHeight();
        float f9 = this.font.getMsdfAtlas().getDistanceRange();
        float f10 = f6 <= 1.0E-4f ? 1.0f : f6;
        float f11 = f;
        float f12 = f2 + this.font.getMetrics().getBaselineOffset() * f3;
        for (int i = 0; i < string.length(); ++i) {
            MsdfGlyph msdfGlyph;
            char c = string.charAt(i);
            if (c == '\n') {
                f11 = f;
                f12 += this.font.getMetrics().getLineHeight() * f3;
                continue;
            }
            if (c == ' ') {
                msdfGlyph = this.font.getGlyph(32);
                f11 += (msdfGlyph != null ? msdfGlyph.getAdvance() : this.font.getMetrics().getLineHeight() * 0.25f) * f3;
                continue;
            }
            msdfGlyph = this.font.getGlyph(c);
            if (msdfGlyph == null) continue;
            float f13 = msdfGlyph.getPlaneWidth() * f3;
            float f14 = msdfGlyph.getPlaneHeight() * f3;
            float f15 = f11 + msdfGlyph.getPlaneLeft() * f3;
            float f16 = f12 - msdfGlyph.getPlaneTop() * f3;
            float f17 = MsdfFontRenderer.clampUnit((f15 - f5) / f10);
            float f18 = MsdfFontRenderer.clampUnit((f15 + f13 - f5) / f10);
            int n3 = textGradient.sample(f17);
            int n4 = textGradient.sample(f18);
            drawApi.getGuiVertexBuffer().appendQuad(matrix4f, f15, f16, f13, f14, msdfGlyph.getU0(), msdfGlyph.getV0(), msdfGlyph.getU1(), msdfGlyph.getV1(), 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, f4, 0.2f, f3, f9, f7, f8, n, n3, n4, n4, n3, n2, GuiDrawMode.MSDF_TEXT);
            f11 += msdfGlyph.getAdvance() * f3;
        }
    }

    public void drawGradientText(Matrix4f matrix4f, GuiDrawApi drawApi, String string, float f, float f2, float f3, float f4, int n, TextGradient textGradient) {
        this.drawGradientTextInRange(matrix4f, drawApi, string, f, f2, f3, f4, n, textGradient, f, this.textWidth(string, f3));
    }

    private float measureTextAdvance(String string, float f) {
        float f2 = 0.0f;
        float f3 = 0.0f;
        for (int i = 0; i < string.length(); ++i) {
            MsdfGlyph msdfGlyph;
            char c = string.charAt(i);
            if (c == '\n') {
                if (f2 > f3) {
                    f3 = f2;
                }
                f2 = 0.0f;
                continue;
            }
            if (c == ' ') {
                msdfGlyph = this.font.getGlyph(32);
                f2 += (msdfGlyph != null ? msdfGlyph.getAdvance() : this.font.getMetrics().getLineHeight() * 0.25f) * f;
                continue;
            }
            msdfGlyph = this.font.getGlyph(c);
            if (msdfGlyph == null) continue;
            f2 += msdfGlyph.getAdvance() * f;
        }
        return Math.max(f3, f2);
    }

    private static float clampUnit(float f) {
        return f < 0.0f ? 0.0f : (f > 1.0f ? 1.0f : f);
    }

    private int packArgb(float f, float f2, float f3, float f4) {
        return (int)(f * 255.0f) << 24 | (int)(f2 * 255.0f) << 16 | (int)(f3 * 255.0f) << 8 | (int)(f4 * 255.0f);
    }

    public float glyphCenterX(char c, float f, float f2) {
        MsdfGlyph msdfGlyph = this.font.getGlyph(c);
        if (msdfGlyph == null) {
            return f;
        }
        return f + (msdfGlyph.getPlaneLeft() + msdfGlyph.getPlaneWidth() * 0.5f) * f2;
    }

    public float glyphCenterY(char c, float f, float f2) {
        MsdfGlyph msdfGlyph = this.font.getGlyph(c);
        if (msdfGlyph == null) {
            return f;
        }
        return f + (this.font.getMetrics().getBaselineOffset() - msdfGlyph.getPlaneTop() + msdfGlyph.getPlaneHeight() * 0.5f) * f2;
    }

    public void drawOutlinedText(Matrix4f matrix4f, GuiDrawApi drawApi, String string, float f, float f2, float f3, float f4, int n, int n2) {
        this.drawStyledText(matrix4f, drawApi, string, f, f2, f3, 0.0f, f4, n, n2);
    }
}

