package ru.wexside.misc;

import java.util.Objects;

public final class MsdfMetrics {
    private final float ascender;
    private final float lineHeight;
    private final float descender;

    public MsdfMetrics(float f, float f2, float f3) {
        this.lineHeight = f;
        this.ascender = f2;
        this.descender = f3;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MsdfMetrics)) {
            return false;
        }
        MsdfMetrics msdfMetrics = (MsdfMetrics)object;
        return Float.compare(this.lineHeight, msdfMetrics.lineHeight) == 0 && Float.compare(this.ascender, msdfMetrics.ascender) == 0 && Float.compare(this.descender, msdfMetrics.descender) == 0;
    }

    public String toString() {
        float f = this.descender;
        float f2 = this.ascender;
        float f3 = this.lineHeight;
        return "Metrics[lineHeight=" + f3 + ", ascender=" + f2 + ", descender=" + f + "]";
    }

    public int hashCode() {
        return Objects.hash(Float.valueOf(this.lineHeight), Float.valueOf(this.ascender), Float.valueOf(this.descender));
    }

    public float getDescender() {
        return this.descender;
    }

    public float getBaselineOffset() {
        return this.lineHeight + this.descender;
    }

    public float getAscender() {
        return this.ascender;
    }

    public float getLineHeight() {
        return this.lineHeight;
    }
}

