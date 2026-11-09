package ru.wexside.util;

import java.util.Arrays;
import org.joml.Matrix4f;
import ru.wexside.misc.ListLayout;

public final class TwoColumnLayout {
    private final float rowGap;
    private final int columnCount;
    private final float columnGap;

    public TwoColumnLayout(int n, float f, float f2) {
        this.columnCount = Math.max(1, n);
        this.columnGap = f;
        this.rowGap = f2;
    }

    public float renderColumns(ListLayout callback75, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6) {
        float f7 = (f3 - this.columnGap * (float)(this.columnCount - 1)) / (float)this.columnCount;
        float f8 = f2 + f4;
        float[] fArray = new float[this.columnCount];
        float[] fArray2 = new float[this.columnCount];
        Arrays.fill(fArray, f8);
        Arrays.fill(fArray2, f8);
        int n = callback75.getItemCount();
        for (int i = 0; i < n; ++i) {
            int n2 = i % this.columnCount;
            float f9 = f + (float)n2 * (f7 + this.columnGap);
            float f10 = fArray[n2];
            float f11 = callback75.getItemHeight(i);
            float f12 = f10 + f11 < f5 || f10 > f6 ? f10 + f11 : callback75.renderItem(i, matrix4f, f9, f10, f7);
            fArray[n2] = f12 + this.rowGap;
            fArray2[n2] = f12;
        }
        float f13 = this.maxColumnHeight(fArray2) - f4;
        return Math.max(0.0f, f13 - f2);
    }

    public float measureColumns(ListLayout callback75) {
        int n = callback75.getItemCount();
        if (n == 0) {
            return 0.0f;
        }
        float[] fArray = new float[this.columnCount];
        for (int i = 0; i < n; ++i) {
            int n2 = i % this.columnCount;
            fArray[n2] = fArray[n2] + (callback75.getItemHeight(i) + this.rowGap);
        }
        return this.maxColumnHeight(fArray) - this.rowGap;
    }

    private float maxColumnHeight(float[] fArray) {
        float f = fArray[0];
        for (int i = 1; i < fArray.length; ++i) {
            if (!(fArray[i] > f)) continue;
            f = fArray[i];
        }
        return f;
    }
}

