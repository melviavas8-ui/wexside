package ru.wexside.util;

import ru.wexside.misc.FrameInterpolator;
import ru.wexside.render.RenderFrameClock;

public final class VisibilityAnimationCache {
    private int lastFrame = Integer.MIN_VALUE;
    private boolean initialized;
    private float progress;

    public float visibilityProgress(boolean bl, boolean bl2, float f) {
        float f2;
        int n = RenderFrameClock.currentFrame();
        if (this.lastFrame == n) {
            return this.progress;
        }
        float f3 = f2 = bl2 && bl ? 1.0f : 0.0f;
        if (!this.initialized) {
            this.progress = f2;
            this.initialized = true;
        } else {
            this.progress = FrameInterpolator.lerpTowards(this.progress, f2, f);
            if (Math.abs(f2 - this.progress) <= 0.001f) {
                this.progress = f2;
            }
        }
        this.lastFrame = n;
        return this.progress;
    }
}

