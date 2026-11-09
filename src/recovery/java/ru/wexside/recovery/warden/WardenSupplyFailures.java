package ru.wexside.recovery.warden;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Complete IlllIililiIlIil retry policy, used for TAKE/MISSING_SUPPLIES outcomes. */
public final class WardenSupplyFailures {
    private final LongSupplier nanoClock;
    private boolean collectionAttempted;
    private int failures;
    private long nextCountAt;

    public WardenSupplyFailures(LongSupplier nanoClock) {
        this.nanoClock = Objects.requireNonNull(nanoClock);
    }

    public void reset() {
        collectionAttempted = false;
        clearFailures();
    }

    public boolean collectionAttempted() {
        return collectionAttempted;
    }

    public boolean exhausted() {
        return failures >= (collectionAttempted ? 10 : 1);
    }

    public void clearFailures() {
        failures = 0;
        nextCountAt = 0L;
    }

    public void markCollectionAttempt() {
        collectionAttempted = true;
    }

    public void failed() {
        long now = nanoClock.getAsLong();
        if (now < nextCountAt || exhausted()) {
            return;
        }
        nextCountAt = now + 1_000_000_000L;
        failures++;
    }
}
