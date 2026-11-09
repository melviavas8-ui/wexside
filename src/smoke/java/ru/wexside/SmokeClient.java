package ru.wexside;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import ru.wexside.event.BrightnessEvent;
import ru.wexside.module.Module;
import ru.wexside.module.render.BrightnessModule;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.ui.WexsideScreen;

/** Opt-in GUI/registry smoke test. Never included in the release mod. */
public final class SmokeClient implements ClientModInitializer {
    private int ticks;

    @Override
    public void onInitializeClient() {
        try {
            Files.writeString(Path.of("smoke-result.txt"), "RUNNING\n");
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ticks++;
            try {
                if (ticks == 80) {
                    WexSideClient root = WexSideClient.getInstance();
                    ModuleHookChecks.run();
                    RecoveryGuiChecks.run(client);
                    ru.wexside.render.HandRenderChecks.run();
                    if (!"@kazah_teams".equals(WexSideClient.VERSION_LABEL)) throw new AssertionError("Client title");
                    if (!"@kazah_teams".equals(root.getClientProfile().getUsername())) throw new AssertionError("Default profile name");
                    List<Module> modules = root.getModuleManager().getModules();
                    Set<String> ids = new HashSet<>();
                    int settings = 0;
                    StringBuilder inventory = new StringBuilder("Class\tId\tSettings\n");
                    for (Module module : modules) {
                        if (!ids.add(module.getId())) throw new AssertionError("Duplicate module: " + module.getId());
                        settings += module.getSettings().size();
                        inventory.append(module.getClass().getName()).append('\t').append(module.getId()).append('\t').append(module.getSettings().size()).append('\n');
                    }
                    Files.writeString(Path.of("smoke-modules.tsv"), inventory.toString());
                    var entries = root.getConfigRegistry().serializeEntries();
                    BrightnessModule brightness = root.getModuleManager().getModule(BrightnessModule.class);
                    BooleanSetting enabled = (BooleanSetting) brightness.getToggleSetting();
                    boolean initialEnabled = enabled.isEnabled();
                    enabled.setEnabled(!initialEnabled);
                    root.getConfigRegistry().applyEntries(entries);
                    if (enabled.isEnabled() != initialEnabled) throw new AssertionError("Configuration did not restore changed brightness state");
                    var restoredEntries = root.getConfigRegistry().serializeEntries();
                    if (!entries.equals(restoredEntries)) {
                        StringBuilder difference = new StringBuilder("Path\tBefore\tAfter\n");
                        for (int i = 0; i < entries.size(); i++) {
                            if (!entries.get(i).equals(restoredEntries.get(i))) {
                                difference.append(entries.get(i).path()).append('\t').append(entries.get(i).data()).append('\t').append(restoredEntries.get(i).data()).append('\n');
                            }
                        }
                        Files.writeString(Path.of("smoke-config-diff.tsv"), difference.toString());
                        throw new AssertionError("Configuration payload round trip differs; see smoke-config-diff.tsv");
                    }
                    for (int i = 0; i < 3; i++) {
                        enabled.setEnabled(true);
                        BrightnessEvent event = new BrightnessEvent(0.0f);
                        WexSideClient.getEventBus().post(event);
                        if (Math.abs(event.getBrightness() - 0.3f) > 0.001f) throw new AssertionError("Brightness action");
                        enabled.setEnabled(false);
                        event = new BrightnessEvent(0.0f);
                        WexSideClient.getEventBus().post(event);
                        if (event.getBrightness() != 0.0f) throw new AssertionError("Brightness disabled state");
                    }
                    System.out.println("RECOVERY SMOKE: modules=" + modules.size() + ", settings=" + settings + ", configEntries=" + entries.size() + ", brightness action/toggle PASS");
                    client.setScreen(new WexsideScreen(root.getMiscellaneous()));
                }
                if (ticks == 160) {
                    if (!(client.currentScreen instanceof WexsideScreen)) throw new AssertionError("GUI closed unexpectedly");
                    if (!"@kazah_teams".equals(client.currentScreen.getTitle().getString())) throw new AssertionError("GUI title");
                    Files.writeString(Path.of("smoke-result.txt"), "PASS: registry/configuration, brightness toggle cycles, timer injection, alternative menu keys, potion popup ownership, hand mask/blur/material/energy GPU readback, GUI open for 80 client ticks. Full gameplay lifecycle, stability and FPS are not tested.\n");
                    System.out.println("RECOVERY SMOKE PASS");
                    client.scheduleStop();
                }
            } catch (Throwable failure) {
                failure.printStackTrace();
                try { Files.writeString(Path.of("smoke-result.txt"), "FAIL: " + failure); } catch (Exception ignored) {}
                throw new IllegalStateException("Recovery smoke test failed", failure);
            }
        });
    }
}
