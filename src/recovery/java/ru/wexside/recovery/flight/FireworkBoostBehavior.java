package ru.wexside.recovery.flight;

import java.util.Objects;

/** Retained FireworkBoost arithmetic; configuration defaults and module metadata are explicit inputs. */
public final class FireworkBoostBehavior {
    public static final String CUSTOM = "Custom";
    public static final String RW = "RW";
    public static final String B_HVH = " B-HVH";
    public static final float ANGLE_BUCKET_DEGREES = 5.0f;
    public static final double CUSTOM_MINIMUM = 1.5;
    public static final double CUSTOM_MAXIMUM = 2.5;
    public static final double CUSTOM_STEP_ARGUMENT = 0.01;
    public static final int CUSTOM_PRECISION_ARGUMENT = 2;

    // Original IlIlIllllililil.<clinit> tables, decoded offline with 32/64-bit JVM arithmetic.
    private static final float[] B_HVH_YAW = {1.60f, 1.63f, 1.65f, 1.71f, 1.85f, 1.84f, 1.93f, 2.04f, 2.04f};
    private static final float[] B_HVH_PITCH = {1.60f, 1.61f, 1.60f, 1.63f, 2.01f, 1.84f, 1.92f, 2.00f, 2.01f};
    private static final float[] RW_YAW = {1.60f, 1.63f, 1.66f, 1.71f, 1.73f, 1.81f, 1.81f, 1.81f, 1.81f};
    private static final float[] RW_PITCH = {1.60f, 1.62f, 1.60f, 1.62f, 1.64f, 1.68f, 1.75f, 1.90f, 2.16f};

    private final boolean enabled;
    private final String mode;
    private final float[] customYaw;
    private final float[] customPitch;

    public FireworkBoostBehavior(boolean enabled, String mode, float[] customYaw, float[] customPitch) {
        this.enabled = enabled;
        this.mode = Objects.requireNonNull(mode, "mode");
        this.customYaw = profileCopy(customYaw, "customYaw");
        this.customPitch = profileCopy(customPitch, "customPitch");
    }

    public record Speeds(float horizontal, float vertical) {}
    public record Vector(double x, double y, double z) {}

    /** iIIlIlllIII: the retained handler reads the local player's yaw and pitch, not event entity angles. */
    public Speeds updateSpeeds(boolean localPlayerPresent, float yaw, float pitch, Speeds incoming) {
        Objects.requireNonNull(incoming, "incoming");
        if (!this.enabled || !localPlayerPresent) return incoming;
        float foldedYaw = foldAngle(wrapDegrees(yaw));
        float foldedPitch = foldAngle(Math.abs(pitch));
        float[] yawProfile = B_HVH.equals(this.mode) ? B_HVH_YAW : RW.equals(this.mode) ? RW_YAW : this.customYaw;
        float[] pitchProfile = B_HVH.equals(this.mode) ? B_HVH_PITCH : RW.equals(this.mode) ? RW_PITCH : this.customPitch;
        float horizontal = sample(yawProfile, foldedYaw);
        float vertical = sample(pitchProfile, foldedPitch);
        // Original fcmpl/ifle sequence, intentionally not Math.max (different NaN behavior).
        if (vertical > horizontal) horizontal = vertical;
        return new Speeds(horizontal, vertical);
    }

    public boolean customSettingsVisible() {
        return CUSTOM.equals(this.mode);
    }

    /** Original IlilIlllIII, bci 56-140. Folding is separate from yaw wrapping. */
    public static float foldAngle(float angle) {
        float result = Math.abs(angle);
        if (result > 90.0f) result = 180.0f - result;
        if (result > 45.0f) result = 90.0f - result;
        return result;
    }

    public static int bucketIndex(int profileLength, float angle) {
        if (profileLength <= 0) throw new IllegalArgumentException("profile must not be empty");
        int index = (int)(angle / ANGLE_BUCKET_DEGREES);
        return Math.max(0, Math.min(profileLength - 1, index));
    }

    public static float sample(float[] profile, float angle) {
        Objects.requireNonNull(profile, "profile");
        return profile[bucketIndex(profile.length, angle)];
    }

    /** FireworkRocketEntity.iilIIiiIliI, bci 0-184: unchanged event returns supplied vanilla result. */
    public static Vector boostedVelocity(boolean eventBusAvailable, boolean boostedEntityIsLocal,
            Vector look, Vector previousVelocity, Vector vanillaVelocity, Speeds speeds) {
        Objects.requireNonNull(vanillaVelocity, "vanillaVelocity");
        Objects.requireNonNull(speeds, "speeds");
        if (!eventBusAvailable || !boostedEntityIsLocal || speeds.horizontal == 1.5f && speeds.vertical == 1.5f) {
            return vanillaVelocity;
        }
        Objects.requireNonNull(look, "look");
        Objects.requireNonNull(previousVelocity, "previousVelocity");
        return new Vector(
                previousVelocity.x + look.x * 0.1 + (look.x * speeds.horizontal - previousVelocity.x) * 0.5,
                previousVelocity.y + look.y * 0.1 + (look.y * speeds.vertical - previousVelocity.y) * 0.5,
                previousVelocity.z + look.z * 0.1 + (look.z * speeds.horizontal - previousVelocity.z) * 0.5);
    }

    private static float wrapDegrees(float value) {
        float wrapped = value % 360.0f;
        if (wrapped >= 180.0f) wrapped -= 360.0f;
        if (wrapped < -180.0f) wrapped += 360.0f;
        return wrapped;
    }

    private static float[] profileCopy(float[] profile, String name) {
        Objects.requireNonNull(profile, name);
        if (profile.length == 0) throw new IllegalArgumentException(name + " must not be empty");
        return profile.clone();
    }
}
