package ru.wexside.render;

import com.mojang.blaze3d.systems.RenderPass;

public final class HandBlurRegion {
    private final int right;
    private final int top;
    private final int bottom;
    private final int left;

    public int top() {
        return this.top;
    }

    public int left() {
        return this.left;
    }

    public int right() {
        return this.right;
    }

    public HandBlurRegion(int n, int n2, int n3, int n4) {
        this.left = n;
        this.bottom = n2;
        this.right = n3;
        this.top = n4;
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof HandBlurRegion)) {
            return false;
        }
        HandBlurRegion handBlurRegion = (HandBlurRegion)object;
        if (this.left() != handBlurRegion.left()) {
            return false;
        }
        if (this.bottom() != handBlurRegion.bottom()) {
            return false;
        }
        if (this.right() != handBlurRegion.right()) {
            return false;
        }
        return this.top() == handBlurRegion.top();
    }

    public int hashCode() {
        int n = 59;
        int n2 = 1;
        n2 = n2 * 59 + this.left();
        n2 = n2 * 59 + this.bottom();
        n2 = n2 * 59 + this.right();
        n2 = n2 * 59 + this.top();
        return n2;
    }

    public HandBlurRegion scale(int n, int n2, int n3, int n4, float f) {
        double d = (float)this.left - f;
        double d2 = (float)this.bottom - f;
        double d3 = (float)this.right + f;
        double d4 = (float)this.top + f;
        if (d <= (double)n * 0.005) {
            d = 0.0;
        }
        if (d2 <= (double)n2 * 0.005) {
            d2 = 0.0;
        }
        if (d3 >= (double)n * 0.995) {
            d3 = n;
        }
        if (d4 >= (double)n2 * 0.995) {
            d4 = n2;
        }
        return new HandBlurRegion((int)Math.clamp(Math.floor(d * (double)n3 / (double)n), 0.0, (double)n3), (int)Math.clamp(Math.floor(d2 * (double)n4 / (double)n2), 0.0, (double)n4), (int)Math.clamp(Math.ceil(d3 * (double)n3 / (double)n), 0.0, (double)n3), (int)Math.clamp(Math.ceil(d4 * (double)n4 / (double)n2), 0.0, (double)n4));
    }

    public void apply(RenderPass renderPass) {
        renderPass.enableScissor(this.left, this.bottom, Math.max(0, this.right - this.left), Math.max(0, this.top - this.bottom));
    }

    public int bottom() {
        return this.bottom;
    }

    public boolean isEmpty() {
        return this.right <= this.left || this.top <= this.bottom;
    }
}
