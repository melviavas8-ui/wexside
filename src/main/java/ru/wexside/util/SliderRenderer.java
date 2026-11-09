package ru.wexside.util;

import java.util.Objects;
import org.joml.Matrix4f;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.ScaleSettings;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.SliderTrack;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.NumberFormatting;

public final class SliderRenderer {
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
    private final float value11;
    private final float value12;

    public SliderRenderer() {
        this.value3 = 28.0f;
        this.value11 = 23.0f;
        this.value6 = 5.0f;
        this.value8 = 3.0f;
        this.value4 = 2.0f;
        this.value = 1.5f;
        this.value2 = 3.0f;
        this.value12 = 2.0f;
        this.value7 = 0.5f;
        this.value9 = 1.0f;
        this.value5 = 5.0f;
        this.value10 = 3.0f;
    }

    public void renderThumb(Matrix4f matrix4f, GuiDrawApi drawApi, float f, float f2, int n) {
        drawApi.drawRoundedRectangle(matrix4f, f - 3.0f, f2 - 3.0f, 6.0f, 6.0f, 6.0f, n);
        float f3 = Math.max(0.5f, 2.0f);
        drawApi.drawRoundedRectangle(matrix4f, f - f3, f2 - f3, f3 * 2.0f, f3 * 2.0f, f3 * 2.0f, ThemeColors.backgroundControl());
    }

    public void renderTrack(Matrix4f matrix4f, GuiDrawApi drawApi, SliderTrack track, int n) {
        drawApi.drawRoundedRectangle(matrix4f, track.x(), track.y(), track.width(), 1.5f, 2.0f, n);
    }

    public float getHorizontalPadding() {
        Objects.requireNonNull(this);
        return 5.0f;
    }

    public float getTopPadding() {
        Objects.requireNonNull(this);
        return 3.0f;
    }

    public float trackFractionToValueFraction(float f, ScaleSettings scaleSettings) {
        float f2 = Math.max(0.0f, Math.min(1.0f, f));
        int n = this.markerIntervalCount(scaleSettings);
        if (n <= 0) {
            return f2;
        }
        float f3 = this.totalSegmentWeight(n);
        if (f3 <= 0.0f) {
            return f2;
        }
        float f4 = f2 * f3;
        float f5 = 0.0f;
        for (int i = 0; i < n; ++i) {
            float f6 = this.segmentWeight(i, n);
            float f7 = f5 + f6;
            if (f4 <= f7 || i == n - 1) {
                if (f6 <= 0.0f) {
                    return (float)i / (float)n;
                }
                float f8 = (f4 - f5) / f6;
                f8 = Math.max(0.0f, Math.min(1.0f, f8));
                return ((float)i + f8) / (float)n;
            }
            f5 = f7;
        }
        return 1.0f;
    }

    public float valueFractionToTrackFraction(float f, ScaleSettings scaleSettings) {
        float f2 = Math.max(0.0f, Math.min(1.0f, f));
        int n = this.markerIntervalCount(scaleSettings);
        if (n <= 0) {
            return f2;
        }
        if (f2 >= 1.0f) {
            return 1.0f;
        }
        float f3 = f2 * (float)n;
        int n2 = Math.min(n - 1, (int)Math.floor(f3));
        float f4 = f3 - (float)n2;
        float f5 = this.totalSegmentWeight(n);
        if (f5 <= 0.0f) {
            return f2;
        }
        float f6 = 0.0f;
        for (int i = 0; i < n2; ++i) {
            f6 += this.segmentWeight(i, n);
        }
        float f7 = this.segmentWeight(n2, n);
        return (f6 + f7 * f4) / f5;
    }

    private float totalSegmentWeight(int n) {
        float f = 0.0f;
        for (int i = 0; i < n; ++i) {
            f += this.segmentWeight(i, n);
        }
        return f;
    }

    public void renderTickMarks(Matrix4f matrix4f, GuiDrawApi drawApi, SliderTrack track, ScaleSettings scaleSettings, int n, int n2) {
        int n3 = this.markerIntervalCount(scaleSettings);
        if (n3 <= 0) {
            return;
        }
        float f = track.y() + 1.5f;
        float f2 = f + 2.0f + 1.0f;
        for (int i = 0; i <= n3; ++i) {
            float f3;
            float f4 = (float)i / (float)n3;
            float f5 = track.x() + track.width() * this.valueFractionToTrackFraction(f4, scaleSettings);
            if (i > 0 && i < n3) {
                f3 = f5 - 0.25f;
                drawApi.drawRoundedRectangle(matrix4f, f3, f, 0.5f, 2.0f, 1.0f, n);
            }
            if (i == 0 || i == n3) continue;
            f3 = (float)i / (float)n3;
            double d = scaleSettings.minimum() + (scaleSettings.maximum() - scaleSettings.minimum()) * (double)f3;
            String string = NumberFormatting.format(d, scaleSettings.precision());
            float f6 = f5 - FontRegistry.font2.textWidth(string, 5.0f) / 2.0f;
            FontRegistry.font2.drawText(matrix4f, drawApi, string, f6, f2, 5.0f, n2);
        }
    }

    public float heightForMarkers(boolean bl) {
        return bl ? 28.0f : 23.0f;
    }

    private float segmentWeight(int n, int n2) {
        if (n < 0 || n >= n2) {
            return 0.0f;
        }
        float f = 3.0f;
        if (n == 0 || n == n2 - 1) {
            return 1.0f / f;
        }
        return 1.0f;
    }

    public SliderTrack createTrack(GuiBounds bounds2, float f) {
        float f2 = bounds2.getX() + 5.0f;
        float f3 = bounds2.getY() + 3.0f + f + 2.0f;
        float f4 = Math.max(1.0f, bounds2.getWidth() - 10.0f);
        return new SliderTrack(f2, f3, f4);
    }

    public float getTrackThickness() {
        Objects.requireNonNull(this);
        return 1.5f;
    }

    public int markerIntervalCount(ScaleSettings scaleSettings) {
        double d3;
        if (!scaleSettings.hasMarkers()) {
            return 0;
        }
        if (scaleSettings.hasMarkerSpacing()) {
            return (int)Math.round(NumberFormatting.normalize(scaleSettings.minimum(), scaleSettings.maximum(), scaleSettings.markerStep()) * 10.0);
        }
        if (scaleSettings.hasSnapStep()) {
            return (int)Math.round(NumberFormatting.normalize(scaleSettings.minimum(), scaleSettings.maximum(), scaleSettings.snapStep()) * 10.0);
        }
        double d2 = scaleSettings.maximum() - scaleSettings.minimum();
        if (d2 <= 0.0) {
            return 0;
        }
        if (scaleSettings.precision() == 0 && Math.abs((d3 = Math.rint(d2)) - d2) < 1.0E-9 && d3 >= 2.0 && d3 <= 12.0) {
            return (int)d3;
        }
        return 10;
    }
}

