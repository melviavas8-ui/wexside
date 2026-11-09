package ru.wexside.recovery.crystal;

import java.util.HashMap;
import java.util.Map;

/** Exact supplied IiIiIiliIiIllil factory effects and GuiRepair.auraRuntimeName routing. */
public final class PatchedCrystalRotationStrategyFactory implements CrystalRotationStrategyFactory {
    private final Map<String, CrystalRotationStrategy> strategies = new HashMap<>();

    public PatchedCrystalRotationStrategyFactory() {
        initializeStrategies();
    }

    /** Original init accepts an unused context argument; its body reads only this map. */
    public void initializeStrategies() {
        strategies.clear();
        strategies.put("Simple", new PatchedCrystalRotationStrategy());
        strategies.put("Legit", new PatchedCrystalRotationStrategy());
    }

    public static String runtimeName(String name) {
        if ("Neuro".equals(name) || "Polar".equals(name) || "RW".equals(name)
                || "Spooky".equals(name) || "Spooky Test".equals(name) || "Spooky-T".equals(name)) {
            return "Legit";
        }
        return name;
    }

    @Override
    public CrystalRotationStrategy create(String name) {
        return strategies.getOrDefault(runtimeName(name), strategies.get("Legit"));
    }
}
