package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public final class FrameInterpolator {
    private FrameInterpolator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static float lerpTowards(float current, float target, float speed) {
        float progress = MathHelper.clamp((float)((float)(FrameInterpolator.getFrameDeltaSeconds() * (double)speed)), (float)0.0f, (float)1.0f);
        return MathHelper.lerp((float)progress, (float)current, (float)target);
    }

    public static double getFrameDeltaSeconds() {
        int fps = MinecraftClient.getInstance().getCurrentFps();
        return fps > 0 ? 1.0 / (double)fps : 1.0;
    }
}

