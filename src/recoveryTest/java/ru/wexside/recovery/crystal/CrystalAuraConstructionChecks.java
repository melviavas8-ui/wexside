package ru.wexside.recovery.crystal;

import java.util.List;
import ru.wexside.event.EventBusImpl;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;

/** Constructor/configuration-boundary regression; does not claim in-world module behavior. */
public final class CrystalAuraConstructionChecks {
    private static BooleanSetting setting(String id, boolean enabled) {
        var builder = BooleanSetting.builder();
        builder.name(id).id(id);
        return builder.value(enabled).defaultValue(enabled).build();
    }

    public static void main(String[] args) {
        BooleanSetting enabled = setting("fixture_enabled", false);
        BooleanSetting fast = setting("fixture_fast", true);
        BooleanSetting test = setting("fixture_test", false);
        BooleanSetting guard = setting("fixture_guard", true);
        CrystalAuraSettings settings = new CrystalAuraSettings(enabled, fast, test, guard);
        List<BooleanSetting> order = List.of(guard, fast, enabled, test);
        CrystalAuraModule module = new CrystalAuraModule(new EventBusImpl(), "crystal_fixture", "Crystal fixture", "Fixture",
                ModuleCategory.COMBAT, new String[0], settings, order, null);
        if (!module.getSettings().equals(order)) throw new AssertionError("Explicit setting registration order");
        if (enabled.isEnabled() || !fast.isEnabled() || test.isEnabled() || !guard.isEnabled()) {
            throw new AssertionError("Constructor must not invent or overwrite defaults");
        }
        boolean rejected = false;
        try {
            new CrystalAuraModule(new EventBusImpl(), "crystal_fixture", "Crystal fixture", "Fixture",
                    ModuleCategory.COMBAT, new String[0], settings, List.of(enabled, fast, test, test), null);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        if (!rejected) throw new AssertionError("Incomplete or duplicate setting registration rejected");
        System.out.println("CRYSTAL AURA CONSTRUCTION CHECKS PASS");
    }
}
