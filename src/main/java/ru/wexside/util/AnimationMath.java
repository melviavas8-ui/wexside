package ru.wexside.util;

public final class AnimationMath {
    private AnimationMath() {
    }

    public static float sin(float value) {
        return (float)Math.sin(value);
    }

    public static float lerp(float start, float end, float progress) {
        return start + (end - start) * AnimationMath.clamp01(progress);
    }

    public static int lerpColor(int start, int end, float progress) {
        float t = AnimationMath.clamp01(progress);
        int a = Math.round(AnimationMath.lerp(start >>> 24, end >>> 24, t));
        int r = Math.round(AnimationMath.lerp(start >>> 16 & 0xFF, end >>> 16 & 0xFF, t));
        int g = Math.round(AnimationMath.lerp(start >>> 8 & 0xFF, end >>> 8 & 0xFF, t));
        int b = Math.round(AnimationMath.lerp(start & 0xFF, end & 0xFF, t));
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int applyOpacity(int color, float opacity, float scale) {
        int alpha = Math.round((float)(color >>> 24) * AnimationMath.clamp01(opacity) * AnimationMath.clamp01(scale));
        return color & 0xFFFFFF | alpha << 24;
    }

    public static float easeInOutSine(float progress) {
        float t = AnimationMath.clamp01(progress);
        return (1.0f - (float)Math.cos(Math.PI * (double)t)) * 0.5f;
    }

    public static float smoothStep(float progress) {
        float t = AnimationMath.clamp01(progress);
        return t * t * (3.0f - 2.0f * t);
    }

    public static float easeOut(float progress, float exponent) {
        float t = AnimationMath.clamp01(progress);
        return 1.0f - (float)Math.pow(1.0f - t, Math.max(0.01f, exponent));
    }

    public static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}

