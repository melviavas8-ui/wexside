package ru.wexside.recovery.flight;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Retained ElytraMotion control flow; removed inventory-action construction is an explicit integration port. */
public final class ElytraMotionBehavior {
    public static final String INVENTORY_OWNER = "elytra_motion";
    public static final long ROCKET_INTERVAL_MILLIS = 500L;
    public static final int RETAINED_INVENTORY_ARGUMENT = 40;

    private final boolean enabled;
    private final boolean automaticFireworks;
    private final float targetDistanceThreshold;
    private boolean freeze;
    private long lastRocketAttemptMillis;

    /** All settings and initial state are explicit because original constructor initialization is absent. */
    public ElytraMotionBehavior(boolean enabled, boolean automaticFireworks, float targetDistanceThreshold,
            boolean initialFreeze, long initialRocketAttemptMillis) {
        this.enabled = enabled;
        this.automaticFireworks = automaticFireworks;
        this.targetDistanceThreshold = targetDistanceThreshold;
        this.freeze = initialFreeze;
        this.lastRocketAttemptMillis = initialRocketAttemptMillis;
    }

    public record Target(boolean gliding, float distance) {}

    @FunctionalInterface
    public interface KeyControl {
        void setPressed(boolean pressed);
    }

    /** Real caller supplies the key whose original GameOptions field role has not yet been established. */
    public void tick(boolean localPlayerPresent, boolean localGliding, long nowMillis,
            Supplier<Optional<Target>> targetSource, KeyControl controlledKey, Runnable attemptRocketUse) {
        Objects.requireNonNull(targetSource, "targetSource");
        Objects.requireNonNull(controlledKey, "controlledKey");
        Objects.requireNonNull(attemptRocketUse, "attemptRocketUse");
        // Original iiililIilII: no-player return preserves state; disabled/not-gliding clears freeze.
        if (!localPlayerPresent) return;
        if (!this.enabled || !localGliding) {
            this.freeze = false;
            return;
        }
        boolean shouldFreeze = this.check(localPlayerPresent, localGliding, targetSource.get());
        controlledKey.setPressed(!shouldFreeze);
        this.freeze = shouldFreeze;
        // The original resolves the aura target again here, independently of the check() lookup.
        if (this.automaticFireworks && targetSource.get().isPresent()
                && nowMillis - this.lastRocketAttemptMillis >= ROCKET_INTERVAL_MILLIS) {
            attemptRocketUse.run();
            this.lastRocketAttemptMillis = nowMillis;
        }
    }

    /** Original liililIilII, reached by check() -> selector1 -> IlllliIiIiIllil.ilIliIllI. */
    public boolean check(boolean localPlayerPresent, boolean localGliding, Optional<Target> target) {
        Objects.requireNonNull(target, "target");
        if (target.isEmpty() || !localPlayerPresent) return false;
        Target value = target.get();
        return !this.isGlidingTarget(localPlayerPresent, localGliding, target, true)
                && value.distance < this.targetDistanceThreshold;
    }

    /** Original illIilIilII: strict=true requires local AND target gliding; false tests local only. */
    public boolean isGlidingTarget(boolean localPlayerPresent, boolean localGliding,
            Optional<Target> target, boolean strict) {
        Objects.requireNonNull(target, "target");
        return this.enabled && target.isPresent() && localPlayerPresent
                && localGliding && (!strict || target.get().gliding);
    }

    /** Original lilIilIilII cancels only the local entity event while freeze is set. */
    public boolean shouldCancelTravel(boolean eventEntityIsLocal) {
        return eventEntityIsLocal && this.freeze;
    }

    public boolean isFrozen() {
        return this.freeze;
    }

    public long lastRocketAttemptMillis() {
        return this.lastRocketAttemptMillis;
    }

    /** Surviving llIIilIilII selection path; action/task construction itself was erased. */
    public static boolean requestRocket(RocketInventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        if (!inventory.isAvailable()) return false;
        if (inventory.offhandHasFirework()) {
            inventory.useOffhand(INVENTORY_OWNER, "HIGH", "FAST", "SKIP_IF_BUSY");
            return true;
        }
        int slot = inventory.findFireworkSlot();
        if (slot == -1) return false;
        inventory.useInventorySlot(slot, RETAINED_INVENTORY_ARGUMENT,
                INVENTORY_OWNER, "HIGH", "FAST", "SKIP_IF_BUSY");
        return true;
    }

    /** Required real integration: implementations must supply recovered inventory task/action semantics. */
    public interface RocketInventory {
        boolean isAvailable();
        boolean offhandHasFirework();
        int findFireworkSlot();
        void useOffhand(String owner, String priority, String timing, String conflict);
        void useInventorySlot(int sourceSlot, int retainedArgument,
                String owner, String priority, String timing, String conflict);
    }
}
