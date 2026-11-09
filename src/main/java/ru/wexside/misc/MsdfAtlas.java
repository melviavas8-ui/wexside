package ru.wexside.misc;

import java.util.Objects;

public final class MsdfAtlas {
    private final float range;
    private final float width;
    private final float height;

    public MsdfAtlas(float f, float f2, float f3) {
        this.range = f;
        this.width = f2;
        this.height = f3;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MsdfAtlas)) {
            return false;
        }
        MsdfAtlas msdfAtlas = (MsdfAtlas)object;
        return Float.compare(this.range, msdfAtlas.range) == 0 && Float.compare(this.width, msdfAtlas.width) == 0 && Float.compare(this.height, msdfAtlas.height) == 0;
    }

    public String toString() {
        float f = this.height;
        float f2 = this.width;
        float f3 = this.range;
        return "Atlas[range=" + f3 + ", width=" + f2 + ", height=" + f + "]";
    }

    public int hashCode() {
        return Objects.hash(Float.valueOf(this.range), Float.valueOf(this.width), Float.valueOf(this.height));
    }

    public float getHeight() {
        return this.height;
    }

    public float getWidth() {
        return this.width;
    }

    public float getDistanceRange() {
        return this.range;
    }
}

