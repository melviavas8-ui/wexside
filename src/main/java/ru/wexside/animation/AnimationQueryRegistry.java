package ru.wexside.animation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import ru.wexside.misc.AnimationQuery;

public final class AnimationQueryRegistry {
    private static final Map<String, AnimationQuery> QUERIES = new LinkedHashMap<String, AnimationQuery>();

    private AnimationQueryRegistry() {
    }

    public static void registerDefaults() {
        if (!QUERIES.isEmpty()) {
            return;
        }
        AnimationQueryRegistry.register("wexside.sigmoid", 3, values -> AnimationQueryRegistry.sigmoid(values[0], values[1], values[2]));
        AnimationQueryRegistry.register("wexside.sigmoid_swing", 3, values -> AnimationQueryRegistry.sigmoid(values[2], values[0], values[1]));
        AnimationQueryRegistry.register("wexside.movewave_sin", 3, values -> AnimationQueryRegistry.movementWave(values, true));
        AnimationQueryRegistry.register("wexside.movewave_cos", 3, values -> AnimationQueryRegistry.movementWave(values, false));
        AnimationQueryRegistry.register("wexside.movewave", 3, values -> AnimationQueryRegistry.movementWave(values, true));
        AnimationQueryRegistry.register("wexside.linear_wave", 1, values -> AnimationQueryRegistry.triangleWave(values[0] + (((double[])values).length > 1 ? values[1] : 0.0)));
        AnimationQueryRegistry.register("wexside.lopsided_wave", 2, values -> AnimationQueryRegistry.lopsidedWave(values[0], values[1]));
        AnimationQueryRegistry.register("wexside.speed_vibration", 1, values -> values[0] * AnimationQueryRegistry.movementSpeed() * Math.sin(Math.toRadians(AnimationQueryRegistry.animationTime() * 1440.0)));
        AnimationQueryRegistry.register("wexside.interval", 2, values -> AnimationQueryRegistry.interval(values[0], values[1]));
        AnimationQueryRegistry.register("wexside.smoothclamp", 4, values -> AnimationQueryRegistry.smoothClamp(values[0], values[1], values[2], values[3]));
        AnimationQueryRegistry.register("wexside.smoothmin", 3, values -> AnimationQueryRegistry.smoothMinimum(values[0], values[1], values[2]));
        AnimationQueryRegistry.register("wexside.easeinback", 1, values -> AnimationQueryRegistry.easeInBack(values[0]));
        AnimationQueryRegistry.register("wexside.easeoutback", 1, values -> AnimationQueryRegistry.easeOutBack(values[0]));
        AnimationQueryRegistry.register("wexside.easeinoutback", 1, values -> AnimationQueryRegistry.easeInOutBack(values[0]));
        AnimationQueryRegistry.register("wexside.easeinoutquad", 1, values -> AnimationQueryRegistry.easeInOutPower(values[0], 2.0));
        AnimationQueryRegistry.register("wexside.easeinoutquint", 1, values -> AnimationQueryRegistry.easeInOutPower(values[0], 5.0));
        AnimationQueryRegistry.register("wexside.easeinoutsine", 1, values -> AnimationQueryRegistry.easeInOutSine(values[0]));
        AnimationQueryRegistry.register("wexside.random_integer", 2, values -> Math.floor(values[0] + Math.random() * (values[1] - values[0] + 1.0)));
        AnimationQueryRegistry.register("wexside.set_tick_step_value", 1, values -> values[0]);
        AnimationQueryRegistry.register("wexside.check_for_desert", 0, values -> AnimationQueryRegistry.isDesert());
        AnimationQueryRegistry.register("wexside.check_for_ocean", 0, values -> AnimationQueryRegistry.isInBiome((TagKey<Biome>)BiomeTags.IS_OCEAN));
        AnimationQueryRegistry.register("wexside.check_for_nether", 0, values -> AnimationQueryRegistry.isInBiome((TagKey<Biome>)BiomeTags.IS_NETHER));
        AnimationQueryRegistry.register("wexside.check_for_theend", 0, values -> AnimationQueryRegistry.isInBiome((TagKey<Biome>)BiomeTags.IS_END));
        AnimationQueryRegistry.register("wexside.check_for_biome", 1, values -> 0.0);
        AnimationQueryRegistry.register("Math.sin", 1, values -> Math.sin(Math.toRadians(values[0])));
        AnimationQueryRegistry.register("Math.cos", 1, values -> Math.cos(Math.toRadians(values[0])));
        AnimationQueryRegistry.register("bmath.cos", 1, values -> Math.cos(Math.toRadians(values[0])));
        AnimationQueryRegistry.register("query.is_crouching_smooth_easeoutsine", 1, values -> AnimationQueryRegistry.easeOutSine(AnimationQueryRegistry.crouching()));
        AnimationQueryRegistry.register("query.is_crouching_smooth_easeoutexpo", 1, values -> AnimationQueryRegistry.easeOutExpo(AnimationQueryRegistry.crouching()));
        AnimationQueryRegistry.register("query.is_in_water_smooth", 1, values -> AnimationQueryRegistry.inWater());
        AnimationQueryRegistry.register("query.is_in_water_smooth_easeoutsine", 1, values -> AnimationQueryRegistry.easeOutSine(AnimationQueryRegistry.inWater()));
        AnimationQueryRegistry.register("query.is_in_water_smooth_easeoutback", 2, values -> AnimationQueryRegistry.easeOutBack(AnimationQueryRegistry.inWater()));
        AnimationQueryRegistry.register("query.is_on_ground_smooth", 1, values -> AnimationQueryRegistry.onGround());
        AnimationQueryRegistry.register("query.is_on_ground_smooth_easeoutexpo", 1, values -> AnimationQueryRegistry.easeOutExpo(AnimationQueryRegistry.onGround()));
        AnimationQueryRegistry.register("query.body_x_rotation_smooth_easeinoutsine", 1, values -> AnimationQueryRegistry.easeInOutSine(AnimationQueryRegistry.bodyPitchFraction()));
    }

    public static Map<String, AnimationQuery> queries() {
        AnimationQueryRegistry.registerDefaults();
        return Map.copyOf(QUERIES);
    }

    private static void register(String name, int argumentCount, ToDoubleFunction<double[]> function) {
        QUERIES.putIfAbsent(name, new AnimationQuery(argumentCount, function));
    }

    private static ClientPlayerEntity player() {
        return MinecraftClient.getInstance().player;
    }

    private static double animationTime() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return 0.0;
        }
        return (float)client.world.getTime() + client.getRenderTickCounter().getTickProgress(false);
    }

    private static double movementSpeed() {
        ClientPlayerEntity player = AnimationQueryRegistry.player();
        if (player == null) {
            return 0.0;
        }
        return Math.min(1.0, player.getVelocity().horizontalLength() * 4.0);
    }

    private static double crouching() {
        ClientPlayerEntity player = AnimationQueryRegistry.player();
        return player != null && player.isSneaking() ? 1.0 : 0.0;
    }

    private static double inWater() {
        ClientPlayerEntity player = AnimationQueryRegistry.player();
        return player != null && player.isTouchingWater() ? 1.0 : 0.0;
    }

    private static double onGround() {
        ClientPlayerEntity player = AnimationQueryRegistry.player();
        return player != null && player.isOnGround() ? 1.0 : 0.0;
    }

    private static double bodyPitchFraction() {
        ClientPlayerEntity player = AnimationQueryRegistry.player();
        return player == null ? 0.0 : Math.clamp(((double)player.getPitch() + 90.0) / 180.0, 0.0, 1.0);
    }

    private static double isInBiome(TagKey<Biome> tag) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        return client.world != null && player != null && client.world.getBiome(player.getBlockPos()).isIn(tag) ? 1.0 : 0.0;
    }

    private static double isDesert() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        return client.world != null && player != null && client.world.getBiome(player.getBlockPos()).matchesKey(BiomeKeys.DESERT) ? 1.0 : 0.0;
    }

    private static double interval(double period, double activeFraction) {
        if (period <= 0.0) {
            return 0.0;
        }
        double position = AnimationQueryRegistry.animationTime() % period;
        return position < period * activeFraction ? 1.0 : 0.0;
    }

    private static double movementWave(double[] values, boolean sine) {
        double angle = AnimationQueryRegistry.playerYaw() * values[0] + values[2];
        double wave = sine ? Math.sin(Math.toRadians(angle)) : Math.cos(Math.toRadians(angle));
        return wave * values[1] * AnimationQueryRegistry.movementSpeed();
    }

    private static double playerYaw() {
        ClientPlayerEntity player = AnimationQueryRegistry.player();
        return player == null ? 0.0 : (double)player.getBodyYaw();
    }

    private static double lopsidedWave(double angle, double peakPercent) {
        double phase = (angle % 360.0 + 360.0) % 360.0 / 360.0;
        double split = Math.clamp(peakPercent / 100.0, 0.01, 0.99);
        double normalized = phase < split ? phase / split * 0.5 : 0.5 + (phase - split) / (1.0 - split) * 0.5;
        return Math.sin(normalized * Math.PI * 2.0);
    }

    private static double triangleWave(double angle) {
        double phase = (angle % 360.0 + 360.0) % 360.0 / 360.0;
        return phase < 0.5 ? phase * 2.0 : 2.0 - phase * 2.0;
    }

    private static double smoothClamp(double value, double minimum, double maximum, double smoothing) {
        if (smoothing <= 0.0) {
            return Math.clamp(value, minimum, maximum);
        }
        return AnimationQueryRegistry.smoothMinimum(-AnimationQueryRegistry.smoothMinimum(-value, -minimum, smoothing), maximum, smoothing);
    }

    private static double smoothMinimum(double first, double second, double smoothing) {
        if (smoothing <= 0.0) {
            return Math.min(first, second);
        }
        double factor = Math.clamp(0.5 + 0.5 * (second - first) / smoothing, 0.0, 1.0);
        return first * factor + second * (1.0 - factor) - smoothing * factor * (1.0 - factor);
    }

    private static double easeOutSine(double value) {
        return Math.sin(value * Math.PI / 2.0);
    }

    private static double easeOutExpo(double value) {
        return value >= 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * value);
    }

    private static double easeInOutSine(double value) {
        return -(Math.cos(Math.PI * value) - 1.0) / 2.0;
    }

    private static double easeInBack(double value) {
        return value * value * (2.70158 * value - 1.70158);
    }

    private static double easeOutBack(double value) {
        double shifted = value - 1.0;
        return 1.0 + shifted * shifted * (2.70158 * shifted + 1.70158);
    }

    private static double easeInOutBack(double value) {
        double factor = 2.5949095;
        return value < 0.5 ? Math.pow(2.0 * value, 2.0) * ((factor + 1.0) * 2.0 * value - factor) / 2.0 : (Math.pow(2.0 * value - 2.0, 2.0) * ((factor + 1.0) * (2.0 * value - 2.0) + factor) + 2.0) / 2.0;
    }

    private static double easeInOutPower(double value, double power) {
        return value < 0.5 ? Math.pow(2.0, power - 1.0) * Math.pow(value, power) : 1.0 - Math.pow(-2.0 * value + 2.0, power) / 2.0;
    }

    private static double sigmoid(double value, double amplitude, double steepness) {
        return amplitude * (2.0 / (1.0 + Math.exp(-steepness * value)) - 1.0);
    }
}

