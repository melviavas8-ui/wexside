package ru.wexside;

import com.google.gson.Gson;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import ru.wexside.event.EventBusImpl;
import ru.wexside.input.BindInput;
import ru.wexside.misc.*;
import ru.wexside.setting.*;

/** Focused checks for profile preflight ordering and immutable parsed-load reuse. */
public final class ConfigRecoveryChecks {
    public static void main(String[] args) throws Exception {
        invalidProfilePreservesRuntime();
        preparedProfileUsesOriginalParse();
        System.out.println("CONFIG RECOVERY CHECKS PASS: invalid profile isolation, parsed snapshot reuse, valid profile load");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static BooleanSetting setting() {
        BooleanSettingBuilder builder = BooleanSetting.builder();
        builder.name("Preflight probe").id("preflight").withKeybind();
        BooleanSetting setting = builder.value(false).build();
        setting.bindModuleId("config_recovery");
        return setting;
    }

    private static void setField(Object owner, String name, Object value) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private static void invalidProfilePreservesRuntime() throws Exception {
        Path directory = Files.createTempDirectory("wex-config-preflight-");
        BooleanSetting setting = setting();
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(setting);
        registry.captureBaseline();
        KeybindRegistry bindings = new KeybindRegistry(new EventBusImpl());
        SettingKeybind binding = setting.getKeybind();
        bindings.register(binding);
        ConfigManager manager = new ConfigManager(directory.toFile(), new Gson(), registry, bindings);
        setField(manager, "currentProfileName", "previous");
        setField(manager, "importedEntriesPending", true);
        setting.setEnabled(true);
        binding.captureCurrentValue();
        setting.setEnabled(false);
        binding.setBindInput(BindInput.mouse(4));
        binding.setBindActivationMode(BindActivationMode.HOLD);
        binding.onPressed();
        require(setting.isEnabled(), "HOLD session must be active before rejected load");
        Path bad = manager.getConfigDirectory().toPath().resolve("invalid.wex");
        Files.createDirectories(bad.getParent());
        for (String json : new String[]{"{", "{\"magic\":\"wrong\"}",
                "{\"magic\":\"wexside-profile\",\"entries\":[{\"path\":{},\"data\":\"x\"}]}"}) {
            Files.writeString(bad, json);
            boolean failed = false;
            try { manager.loadProfile("invalid"); }
            catch (RuntimeException expected) { failed = true; }
            require(failed, "Malformed profile must reject");
            require(setting.isEnabled(), "Rejected load changed setting/HOLD state");
            require(binding.matchesMouse(4), "Rejected load changed binding");
            require("previous".equals(manager.getCurrentProfileName()), "Rejected load changed selected profile");
            require(manager.hasPendingImportedEntries(), "Rejected load changed import flag");
        }
        binding.onReleased();
        require(!setting.isEnabled(), "Rejected load must preserve active HOLD restoration state");
    }

    private static void preparedProfileUsesOriginalParse() throws Exception {
        Path directory = Files.createTempDirectory("wex-config-snapshot-");
        File file = directory.resolve("valid.wex").toFile();
        BooleanSetting setting = setting();
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(setting);
        ConfigProfile profile = new ConfigProfile(file, new Gson(), registry);
        setting.setEnabled(true);
        profile.setDisplayName("Saved display name");
        profile.save();
        Method prepare = ConfigProfile.class.getDeclaredMethod("prepareLoad");
        prepare.setAccessible(true);
        Object parsed = prepare.invoke(profile);
        // Replacing the file after preflight must not change the payload applied.
        Files.writeString(file.toPath(), "invalid replacement");
        setting.setEnabled(false);
        Method apply = ConfigProfile.class.getDeclaredMethod("applyPreparedLoad", parsed.getClass());
        apply.setAccessible(true);
        apply.invoke(profile, parsed);
        require(setting.isEnabled(), "Prepared payload must apply without rereading file");
        require("Saved display name".equals(profile.getDisplayName()), "Prepared metadata must survive");
        // Public load still shares the exact parser/apply path for normal callers.
        profile.save();
        setting.setEnabled(false);
        profile.load();
        require(setting.isEnabled(), "Valid profile public load must restore setting");
    }
}
