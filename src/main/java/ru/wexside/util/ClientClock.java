package ru.wexside.util;

import net.minecraft.client.MinecraftClient;

public final class ClientClock {
    private static long ticks;

    private ClientClock() {
    }

    public static void advanceTick() {
        ++ticks;
    }

    public static long ticks() {
        return ticks;
    }

    public static double elapsedSeconds(float tickDelta) {
        return (double)((float)ticks + tickDelta) / 20.0;
    }

    public static float tickDelta() {
        return MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
    }
}

