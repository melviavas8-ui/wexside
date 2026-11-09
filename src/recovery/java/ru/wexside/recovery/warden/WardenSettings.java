package ru.wexside.recovery.warden;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Six externally supplied settings. This type contains no invented original defaults or captions. */
public record WardenSettings(BooleanSupplier enabled,
        BooleanSupplier chestDistanceGate, BooleanSupplier collectDrops,
        BooleanSupplier useSpeed, Supplier<String> farmAnarchy,
        Supplier<WardenLootLevel> lootLevel) {
    public WardenSettings {
        Objects.requireNonNull(enabled);
        Objects.requireNonNull(chestDistanceGate);
        Objects.requireNonNull(collectDrops);
        Objects.requireNonNull(useSpeed);
        Objects.requireNonNull(farmAnarchy);
        Objects.requireNonNull(lootLevel);
    }
}
