package ru.wexside.recovery.crystal;

import java.util.Random;
import net.minecraft.client.MinecraftClient;

/** Original IlIllillIillll quantizer, including segmented float rounding and Random consumption. */
public final class CrystalMouseQuantizer {
    private CrystalMouseQuantizer() {}

    public static float quantize(float base, float delta, Random random) {
        double sensitivity = MinecraftClient.getInstance().options.getMouseSensitivity().getValue();
        return quantize(base, delta, sensitivity, random);
    }

    public static float quantize(float base, float delta, double sensitivity, Random random) {
        double scaled = sensitivity * 0.6 + 0.2;
        double step = scaled * scaled * scaled * 8.0 * 0.15;
        if (step <= 0.0) return base + delta;
        long remaining = Math.round((double) delta / step);
        if (remaining == 0L) return base;
        float result = base;
        for (int segments = 3 + random.nextInt(8); segments > 0 && remaining != 0L; segments--) {
            long portion = segments == 1 ? remaining
                    : Math.round((double) remaining * (0.5 + random.nextDouble()) / (double) segments);
            if (portion == 0L) continue;
            if (Math.abs(portion) > Math.abs(remaining)) portion = remaining;
            result = (float) ((double) result + (double) portion * step);
            remaining -= portion;
        }
        return result;
    }
}
