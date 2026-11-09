package ru.wexside.misc;

public final class ShieldBreaker {
    private static volatile double useProgress;
    private static volatile boolean blocking;
    private static volatile boolean hasSample;

    private ShieldBreaker() {
    }

    public static void reset() {
        ShieldBreaker.update();
    }

    public static void consider(boolean blocking, double useProgress) {
        ShieldBreaker.setSample(blocking, useProgress);
    }

    public static void update() {
        hasSample = false;
        blocking = true;
        useProgress = 0.0;
    }

    public static void setSample(boolean bl, double d) {
        blocking = bl;
        useProgress = d;
        hasSample = true;
    }

    public static boolean isActive() {
        return hasSample && !blocking && useProgress > 0.0;
    }

    public static boolean isBlocking() {
        return blocking;
    }

    public static boolean hasSample() {
        return hasSample;
    }

    public static double getUseProgress() {
        return useProgress;
    }
}

