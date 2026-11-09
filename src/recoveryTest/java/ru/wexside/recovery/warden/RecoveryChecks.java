package ru.wexside.recovery.warden;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/** Boundary checks for recovered policies; never initializes an original obfuscated class. */
public final class RecoveryChecks {
    private static int checks;

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(WardenLootLevel.LOW.scale(7) == 10, "LOW truncates 1.5 scaling");
        check(WardenLootLevel.HIGH.scale(7) == 5, "HIGH truncates 0.8 scaling");
        check(WardenLootLevel.fromLabel("Высокий") == WardenLootLevel.HIGH, "Decoded HIGH label");
        check(WardenLootLevel.fromLabel(null) == WardenLootLevel.MEDIUM, "Original unmatched fallback");

        WardenAnarchyRoute route = new WardenAnarchyRoute();
        check(!route.ready() && route.home() == -1 && route.target() == -1, "Original empty route");
        route.start(10, List.of(10, 20, 30));
        int first = route.target();
        route.advance();
        check(new HashSet<>(List.of(first, route.target())).equals(new HashSet<>(List.of(20, 30))),
                "Home excluded, shuffle preserves destinations");
        route.advance();
        check(route.target() == first, "Round-robin wraps");
        route.start(10, List.of(10));
        check(!route.ready() && route.target() == -1, "Route containing only home is empty");

        WardenConsumableWindow<String> window = new WardenConsumableWindow<>();
        check(window.shouldDeferConsumables("A", 0, false, 3.5, 180, 9, 11, 100),
                "Required duration accepts equality");
        check(!window.shouldDeferConsumables("A", 0, false, 3.5, 179, 9, 11, 101),
                "One tick short fails");
        check(!window.shouldDeferConsumables("A", 0, false, 3.5, 180, 8, 11, 102),
                "Food threshold is strict");
        check(!window.shouldDeferConsumables("A", 0, false, 3.5, 180, 9, 10, 103),
                "Health threshold is strict");
        check(!window.shouldDeferConsumables(new String("A"), 0, false, 3.5, 180, 9, 11, 15100),
                "Equal target keeps original start and 15000-ms boundary is exclusive");
        check(window.shouldDeferConsumables("B", 0, false, 3.5, 180, 9, 11, 15100),
                "Changed target restarts timer");
        check(!window.shouldDeferConsumables("B", 10000, false, 3.5, 1000, 9, 11, 15101),
                "10000-ms cooldown is excluded");
        check(window.shouldDeferConsumables("B", 10000, true, 3.5, 1000, 9, 11, 15102),
                "Ready status admits a target beyond cooldown gate");

        AtomicLong clock = new AtomicLong(1);
        WardenSupplyFailures failures = new WardenSupplyFailures(clock::get);
        failures.failed();
        check(failures.exhausted(), "One failure before first collection");
        failures.reset();
        failures.markCollectionAttempt();
        for (int n = 0; n < 9; n++) {
            failures.failed();
            failures.failed();
            clock.addAndGet(1_000_000_000L);
        }
        check(!failures.exhausted(), "Repeated same-timestamp failures count once");
        failures.failed();
        check(failures.exhausted(), "Ten spaced failures after collection");
        failures.clearFailures();
        check(!failures.exhausted() && failures.collectionAttempted(), "Clear keeps collection flag");

        List<String> effects = new ArrayList<>();
        var settings = new WardenSettings(() -> true, () -> true, () -> true, () -> true,
                () -> "20", () -> WardenLootLevel.MEDIUM);
        WardenCoordinator.Actions actions = new WardenCoordinator.Actions() {
            public void resetDepositHelper() { effects.add("deposit"); }
            public void resetRestockHelper() { effects.add("restock"); }
            public void resetContainerLoot() { effects.add("loot"); }
            public void stopNavigation() { effects.add("stop"); }
            public boolean navigationActive() { throw new AssertionError("Unexpected navigation query"); }
            public void moveTo(net.minecraft.util.math.BlockPos position) { throw new AssertionError("Unexpected movement"); }
            public void releaseInteraction() { effects.add("release"); }
            public void aimAt(net.minecraft.util.math.BlockPos position) { throw new AssertionError("Unexpected aim"); }
            public void resetDropsOnEscapeBoundary() { effects.add("drops"); }
            public void resetEscapePath() { effects.add("path"); }
            public void log(String line) { effects.add("log:" + line); }
            public void command(String line) { throw new AssertionError("Unexpected command"); }
        };
        WardenCoordinator.Targets targets = new WardenCoordinator.Targets() {
            public net.minecraft.util.math.BlockPos target() { throw new AssertionError("Unexpected target query"); }
            public long remaining(net.minecraft.util.math.BlockPos target) { throw new AssertionError("Unexpected cooldown query"); }
            public boolean shouldWait(long value) { throw new AssertionError("Unexpected cooldown policy"); }
            public void ban(net.minecraft.util.math.BlockPos target, long duration) { throw new AssertionError("Unexpected ban"); }
            public net.minecraft.util.math.BlockPos standPosition(net.minecraft.client.network.ClientPlayerEntity player,
                    net.minecraft.util.math.BlockPos chest) { throw new AssertionError("Unexpected stand query"); }
        };
        var coordinator = new WardenCoordinator(WardenState.SAVE, settings, actions, targets);
        coordinator.transition(WardenState.TAKE, "test");
        check(effects.equals(List.of("deposit", "restock", "loot", "log:SAVE -> TAKE: test")),
                "Leaving SAVE resets helpers in original order");
        effects.clear();
        coordinator.transition(WardenState.TAKE, "same");
        check(effects.isEmpty(), "Repeated state performs no boundary side effects");
        coordinator.transition(WardenState.ESCAPE, "test");
        check(effects.equals(List.of("loot", "log:TAKE -> ESCAPE: test", "stop", "release", "drops", "path")),
                "Escape boundary effect order");
        coordinator.transition(WardenState.COLLECT, "test");
        check(!coordinator.handleCollectionEscape(new WardenCoordinator.CollectionSnapshot(
                null, false, false, false, false, 100, 8, 0, 20, -1, false), value -> value),
                "Inventory escape threshold is strict");
        check(coordinator.handleCollectionEscape(new WardenCoordinator.CollectionSnapshot(
                null, false, false, false, false, 100, 9, 0, 20, -1, false), value -> value),
                "Inventory above threshold escapes");
        coordinator.transition(WardenState.COLLECT, "test");
        for (int tick = 0; tick < 2; tick++) {
            check(!coordinator.handleCollectionEscape(new WardenCoordinator.CollectionSnapshot(
                    null, true, false, false, true, 101, 0, 0, 20, -1, false), value -> value),
                    "Anger needs three consecutive calls");
        }
        check(coordinator.handleCollectionEscape(new WardenCoordinator.CollectionSnapshot(
                null, true, false, false, true, 101, 0, 0, 20, -1, false), value -> value)
                && coordinator.threatEscapeRequested(), "Third angry tick requests threat escape");
        System.out.println(checks + " recovered-policy boundary checks passed");
    }
}
