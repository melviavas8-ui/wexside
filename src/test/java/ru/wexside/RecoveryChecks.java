package ru.wexside;

import java.io.*;
import java.util.*;
import ru.wexside.event.*;
import ru.wexside.input.BindInput;
import ru.wexside.misc.*;
import ru.wexside.setting.*;
import ru.wexside.util.EspFeatureRegistry;

/** Standalone behavioral checks; no game, display, account or test framework required. */
public final class RecoveryChecks {
    private record ProbeEvent() implements Event {}

    public static void main(String[] args) throws Exception {
        inputRoundTrip();
        eventDispatch();
        bindingLifecycle();
        configRoundTrip();
        colorRoundTrip();
        espDefaultSelections();
        System.out.println("RECOVERY CHECKS PASS: input, events, bindings, configuration, color round trip, ESP default selections");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static BooleanSetting setting() {
        BooleanSettingBuilder builder = BooleanSetting.builder();
        builder.name("Feature enabled").id("enabled").withKeybind();
        BooleanSetting value = builder.value(false).build();
        value.bindModuleId("recovery");
        return value;
    }

    private static void inputRoundTrip() {
        List<BindInput> inputs = new ArrayList<>();
        inputs.add(BindInput.unbound());
        for (int key = 1; key <= 348; key++) inputs.add(BindInput.keyboard(key));
        for (int button = 0; button < 8; button++) inputs.add(BindInput.mouse(button));
        for (BindInput input : inputs) {
            require(input.equals(BindInput.fromLegacyCode(input.toLegacyCode())), "Input round trip: " + input);
        }
        require(!BindInput.keyboard(1).equals(BindInput.mouse(1)), "Devices must remain distinct");
    }

    private static void eventDispatch() {
        EventBus bus = new EventBusImpl();
        List<Integer> order = new ArrayList<>();
        bus.subscribe(ProbeEvent.class, event -> order.add(2), 2);
        bus.subscribe(ProbeEvent.class, event -> order.add(1), 1);
        bus.subscribe(ProbeEvent.class, event -> { throw new IllegalStateException("Expected listener failure"); }, 3);
        bus.subscribe(ProbeEvent.class, event -> order.add(4), 4);
        bus.post(new ProbeEvent());
        require(order.equals(List.of(1, 2, 4)), "Priority ordering and listener failure isolation: " + order);
    }

    private static void bindingLifecycle() {
        BooleanSetting value = setting();
        SettingKeybind binding = value.getKeybind();
        value.setEnabled(true);
        binding.captureCurrentValue();
        value.setEnabled(false);
        binding.setBindActivationMode(BindActivationMode.HOLD);
        binding.onPressed();
        binding.onPressed();
        require(value.isEnabled(), "HOLD activates configured value");
        binding.onReleased();
        require(!value.isEnabled(), "HOLD restores original value after repeated press");
        binding.setBindActivationMode(BindActivationMode.TOGGLE);
        binding.onPressed();
        require(value.isEnabled(), "TOGGLE first press");
        binding.onReleased();
        require(value.isEnabled(), "TOGGLE persists after release");
        binding.onPressed();
        require(!value.isEnabled(), "TOGGLE second press restores original value");
    }

    private static void configRoundTrip() throws Exception {
        BooleanSetting original = setting();
        original.setEnabled(true);
        original.getKeybind().setBindInput(BindInput.mouse(4));
        original.getKeybind().setBindActivationMode(BindActivationMode.HOLD);
        original.getKeybind().captureCurrentValue();
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(original);
        registry.register(original.getKeybind());
        registry.register(original);
        List<ConfigFileEntry> saved = registry.serializeEntries();
        require(saved.size() == 2, "Duplicate registration must not duplicate config entries");
        original.setEnabled(false);
        original.getKeybind().clear();
        original.getKeybind().setBindActivationMode(BindActivationMode.TOGGLE);
        registry.applyEntries(saved);
        require(original.isEnabled(), "Setting value restores from config");
        require(original.getKeybind().matchesMouse(4), "Mouse binding restores from config");
        require(original.getKeybind().getBindActivationMode() == BindActivationMode.HOLD, "Binding mode restores from config");
        // A setting's own wire format must be self-contained for direct callers as well.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        original.writeConfig(new DataOutputStream(bytes));
        BooleanSetting restored = setting();
        restored.readConfig(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        require(restored.isEnabled(), "Direct setting round trip");
        require(restored.getKeybind().matchesMouse(4), "Direct setting round trip preserves binding");
    }

    private static void colorRoundTrip() {
        ColorSettingBuilder builder = ColorSetting.builder();
        builder.name("Sky color");
        ColorSetting original = builder.build();
        original.setPrimaryColor(0xff5680eb);
        byte[] saved = original.copyPayload();
        original.restorePayload(saved);
        require(Arrays.equals(saved, original.copyPayload()), "Color config must preserve explicit secondary hue caches even when the secondary color is unassigned");
    }

    private static void espDefaultSelections() {
        EspFeatureRegistry registry = new EspFeatureRegistry(new EventBusImpl());
        for (ru.wexside.module.Module module : registry.getModules()) {
            for (Setting setting : module.getSettings()) {
                if (setting instanceof MultiSelectSetting multi) {
                    require(Arrays.asList(multi.getOptions()).containsAll(multi.getSelectedOptions()), "Unsupported default selection: " + module.getId() + "." + setting.getId());
                }
            }
        }
    }
}
