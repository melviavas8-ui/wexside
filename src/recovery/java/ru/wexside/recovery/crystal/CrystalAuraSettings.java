package ru.wexside.recovery.crystal;

import java.util.Objects;
import ru.wexside.setting.BooleanSetting;

/** Caller supplies metadata, defaults, toggling policy and registration order. */
public record CrystalAuraSettings(BooleanSetting enabled, BooleanSetting fast,
                                  BooleanSetting test, BooleanSetting nearbyItemGuard) {
    public CrystalAuraSettings {
        Objects.requireNonNull(enabled, "enabled");
        Objects.requireNonNull(fast, "fast");
        Objects.requireNonNull(test, "test");
        Objects.requireNonNull(nearbyItemGuard, "nearbyItemGuard");
    }
}
