package ru.wexside.util;

import java.awt.Color;
import ru.wexside.misc.AstolfoState;

public final class AstolfoColorController {
    public AstolfoState fromColor(AstolfoState state, int argb) {
        float[] hsb = AstolfoColorController.rgbToHsb(argb);
        float phaseOffset = state.getPhaseOffset();
        if (AstolfoColorController.hasVisibleHue(hsb)) {
            phaseOffset = this.phaseOffsetForHue(state, hsb[0], phaseOffset);
        }
        return new AstolfoState(phaseOffset, state.getHueSpeed(), AstolfoColorController.clamp01(hsb[1]), AstolfoColorController.clamp01(hsb[2]), AstolfoColorController.clamp01((float)(argb >>> 24 & 0xFF) / 255.0f));
    }

    public float getCurrentHue(AstolfoState state) {
        return AstolfoColorController.wrapHue(this.animatedBaseHue(state) + state.getPhaseOffset());
    }

    public AstolfoState withHsb(AstolfoState state, float hue, float saturation, float brightness, int alpha) {
        float phaseOffset = state.getPhaseOffset();
        if (saturation > 1.0E-4f && brightness > 1.0E-4f) {
            phaseOffset = this.phaseOffsetForHue(state, AstolfoColorController.clamp01(hue), state.getPhaseOffset());
        }
        return new AstolfoState(phaseOffset, state.getHueSpeed(), AstolfoColorController.clamp01(saturation), AstolfoColorController.clamp01(brightness), AstolfoColorController.clamp01((float)AstolfoColorController.clampByte(alpha) / 255.0f));
    }

    public int getColor(AstolfoState state, float phase) {
        return this.toArgb(state, this.animatedHue(state, state.getPhaseOffset(), phase));
    }

    public int getStaticGradientColor(AstolfoState state, float phase) {
        return this.toArgb(state, AstolfoColorController.mirroredHue(AstolfoColorController.wrapHue(phase)));
    }

    public AstolfoState withCurrentHue(AstolfoState state, float hue) {
        return state.withPhaseOffset(AstolfoColorController.wrapHue(hue - this.animatedBaseHue(state)));
    }

    public float[] getCurrentHsb(AstolfoState state) {
        return new float[]{this.animatedHue(state, state.getPhaseOffset(), 0.0f), state.getSaturation(), state.getBrightness()};
    }

    private int toArgb(AstolfoState state, float hue) {
        return AstolfoColorController.clampByte(Math.round(state.getAlpha() * 255.0f)) << 24 | Color.HSBtoRGB(hue, state.getSaturation(), state.getBrightness()) & 0xFFFFFF;
    }

    private float animatedHue(AstolfoState state, float offset, float phase) {
        float hue = AstolfoColorController.wrapHue(this.animatedBaseHue(state) + AstolfoColorController.wrapHue(offset) + phase * 0.5f);
        return AstolfoColorController.mirroredHue(hue);
    }

    private float phaseOffsetForHue(AstolfoState state, float hue, float fallbackOffset) {
        float target = Math.max(0.5f, AstolfoColorController.clamp01(hue));
        float baseHue = this.animatedBaseHue(state);
        float fallbackHue = AstolfoColorController.wrapHue(baseHue + fallbackOffset);
        float lower = AstolfoColorController.clamp01(target - 0.5f);
        float upper = AstolfoColorController.clamp01(1.5f - target);
        float nearest = AstolfoColorController.nearestCircularHue(fallbackHue, lower, upper);
        return AstolfoColorController.wrapHue(nearest - baseHue);
    }

    private float animatedBaseHue(AstolfoState state) {
        double value = (double)System.currentTimeMillis() * (double)state.getHueSpeed() / 50.0 % 1.0;
        return AstolfoColorController.wrapHue((float)value);
    }

    private static float mirroredHue(float hue) {
        float shifted = 0.5f + hue;
        if (shifted > 1.0f) {
            shifted = 2.0f - shifted;
        }
        return AstolfoColorController.clamp01(shifted);
    }

    private static float nearestCircularHue(float source, float first, float second) {
        return AstolfoColorController.circularDistance(source, first) <= AstolfoColorController.circularDistance(source, second) ? first : second;
    }

    private static float circularDistance(float first, float second) {
        float distance = Math.abs(first - second);
        return Math.min(distance, 1.0f - distance);
    }

    private static boolean hasVisibleHue(float[] hsb) {
        return hsb[1] > 1.0E-4f && hsb[2] > 1.0E-4f;
    }

    private static float[] rgbToHsb(int argb) {
        return Color.RGBtoHSB(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, null);
    }

    private static float wrapHue(float value) {
        float wrapped = value % 1.0f;
        return wrapped < 0.0f ? wrapped + 1.0f : wrapped;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static int clampByte(int value) {
        return Math.clamp((long)value, 0, 255);
    }
}

