package ru.wexside.recovery.warden;

import java.util.Objects;

/** Complete IlIiIlliliIllil behavior. Times are milliseconds, invisibility duration is ticks. */
public final class WardenConsumableWindow<T> {
    private T target;
    private long started;

    public void reset() {
        target = null;
        started = 0L;
    }

    public boolean shouldDeferConsumables(T nextTarget, long cooldownMillis, boolean ready,
            double distance, int invisibilityTicks, int foodLevel, float health, long nowMillis) {
        boolean candidate = nextTarget != null
                && (cooldownMillis >= 0L && cooldownMillis < 10_000L
                    || ready || cooldownMillis < 0L && distance <= 8.0);
        if (!candidate) {
            reset();
            return false;
        }
        if (!Objects.equals(target, nextTarget)) {
            target = nextTarget;
            started = nowMillis;
        }
        double requiredSeconds = Math.max(Math.max(cooldownMillis, 0L) / 1000.0, distance / 3.5) + 8.0;
        return nowMillis - started < 15_000L && invisibilityTicks >= requiredSeconds * 20.0
                && foodLevel > 8 && health > 10.0f;
    }
}
