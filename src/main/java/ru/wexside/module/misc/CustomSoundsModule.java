package ru.wexside.module.misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.setting.NumberUnit;
import ru.wexside.util.CustomSoundLibrary;

public final class CustomSoundsModule
extends Module
implements ConfigSerializable {
    static volatile CustomSoundsModule customSoundsModule2;
    private final Map<String, String> displayNames = new LinkedHashMap<String, String>();
    private final Map<String, ModeSetting> soundSettings = new LinkedHashMap<String, ModeSetting>();
    private final Map<String, Map<String, String>> soundFilesByCategory = new LinkedHashMap<String, Map<String, String>>();
    private final Map<String, NumberSetting> volumeSettings = new LinkedHashMap<String, NumberSetting>();
    private final MultiSelectSetting categories;
    private final BooleanSetting enabledSetting;
    private final CustomSoundLibrary soundLibrary;

    public CustomSoundsModule(EventBus eventBus) {
        super(eventBus, "custom_sounds", "Custom Sounds", "Замена ванильных звуков своими файлами", ModuleCategory.valueOf("MISC"), new String[0]);
        customSoundsModule2 = this;
        this.soundLibrary = this.createSoundLibrary();
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Кастомные звуки вместо ванильных").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        MultiSelectSetting categoriesSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Нет файлов").selectAll(false).optionListEnabled(false).name("Categories").id("categories").description("Какие звуки подменять")).build();
        categoriesSetting.setOptions(new String[0]);
        this.categories = categoriesSetting;
        this.registerSetting(categoriesSetting);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.refreshSoundSettings());
    }

    private CustomSoundLibrary createSoundLibrary() {
        WexSideClient client = WexSideClient.getInstance();
        if (client == null || client.getConfigDirectory() == null) {
            return null;
        }
        try {
            return new CustomSoundLibrary(client.getConfigDirectory().resolve("client").resolve("sounds"));
        }
        catch (IOException ignored) {
            return null;
        }
    }

    private void refreshSoundSettings() {
        String[] stringArray;
        if (this.soundLibrary == null) {
            return;
        }
        this.soundLibrary.reload();
        for (String string : this.soundLibrary.getList()) {
            if (this.displayNames.containsKey(string)) continue;
            String string2 = this.uniqueDisplayName(CustomSoundsModule.formatDisplayName(string));
            this.displayNames.put(string, string2);
            this.registerSoundSettings(string, string2);
        }
        ArrayList<String> availableCategories = new ArrayList<String>();
        for (Map.Entry<String, String> entry : this.displayNames.entrySet()) {
            String soundId = entry.getKey();
            Map<String, String> files = CustomSoundsModule.buildDisplayNameMap(this.soundLibrary.soundsInCategory(soundId));
            this.soundFilesByCategory.put(soundId, files);
            ModeSetting modeSetting = this.soundSettings.get(soundId);
            if (modeSetting != null) {
                modeSetting.setOptions(files.keySet().toArray(new String[0]));
            }
            if (files.isEmpty()) continue;
            availableCategories.add(entry.getValue());
        }
        if (availableCategories.isEmpty()) {
            String[] stringArray2;
            stringArray = stringArray2 = new String[1];
            stringArray2[0] = "Нет файлов";
        } else {
            stringArray = availableCategories.toArray(new String[0]);
        }
        this.categories.setOptions(stringArray);
    }

    private void registerSoundSettings(String soundId, String displayName) {
        String settingId = CustomSoundsModule.sanitizeSettingId(soundId);
        Map<String, String> files = CustomSoundsModule.buildDisplayNameMap(this.soundLibrary.soundsInCategory(soundId));
        this.soundFilesByCategory.put(soundId, files);
        String defaultFile = files.isEmpty() ? "" : files.keySet().iterator().next();
        ModeSetting soundSetting = ((ModeSettingBuilder)((ModeSettingBuilder)((ModeSettingBuilder)((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().id("sound_" + settingId)).name(displayName + " Sound")).description("Файл для: " + displayName)).options(files.keySet().toArray(new String[0])).defaultOption(defaultFile).visibleWhen(() -> this.isCategoryEnabled(soundId))).build();
        NumberSetting volumeSetting = ((NumberSettingBuilder)((NumberSettingBuilder)((NumberSettingBuilder)((NumberSettingBuilder)NumberSetting.builder().id("volume_" + settingId)).name(displayName + " Volume")).description("Громкость: " + displayName)).range(1.0, 100.0).defaultValue(100.0).precision(0).formatter(NumberUnit.PERCENT).visibleWhen(() -> this.isCategoryEnabled(soundId))).build();
        this.soundSettings.put(soundId, soundSetting);
        this.volumeSettings.put(soundId, volumeSetting);
        this.registerSetting(soundSetting);
        this.registerSetting(volumeSetting);
    }

    private String uniqueDisplayName(String baseName) {
        if (!this.displayNames.containsValue(baseName)) {
            return baseName;
        }
        int suffix = 2;
        while (this.displayNames.containsValue(baseName + " [" + suffix + "]")) {
            ++suffix;
        }
        return baseName + " [" + suffix + "]";
    }

    private static String sanitizeSettingId(String soundId) {
        return soundId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "_");
    }

    private static String formatDisplayName(String soundId) {
        String[] parts = soundId.split("\\.");
        String label = parts.length >= 2 ? parts[parts.length - 2] + " " + parts[parts.length - 1] : parts[parts.length - 1];
        return CustomSoundsModule.titleCase(label.replace('_', ' '));
    }

    private static String titleCase(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        boolean capitalizeNext = true;
        for (int index = 0; index < value.length(); ++index) {
            char character = value.charAt(index);
            if (capitalizeNext && Character.isLetter(character)) {
                builder.append(Character.toUpperCase(character));
                capitalizeNext = false;
            } else {
                builder.append(character);
            }
            if (character != ' ' && character != '-') continue;
            capitalizeNext = true;
        }
        return builder.toString();
    }

    private boolean isCategoryEnabled(String soundId) {
        String displayName = this.displayNames.get(soundId);
        return displayName != null && this.categories.getSelectedOptions().contains(displayName);
    }

    private String resolveReplacementFile(String soundId) {
        String selected;
        Map<String, String> files = this.soundFilesByCategory.get(soundId);
        ModeSetting modeSetting = this.soundSettings.get(soundId);
        if (files == null || files.isEmpty()) {
            return null;
        }
        if (modeSetting != null && (selected = files.get(modeSetting.getSelectedOption())) != null && this.soundLibrary.containsSound(selected)) {
            return selected;
        }
        List<String> available = this.soundLibrary.soundsInCategory(soundId);
        return available.isEmpty() ? null : available.get(0);
    }

    private float volumeFor(String soundId) {
        NumberSetting volumeSetting = this.volumeSettings.get(soundId);
        double percent = volumeSetting == null ? 100.0 : volumeSetting.getValue();
        return (float)Math.max(0.0, Math.min(1.0, percent / 100.0));
    }

    public static boolean compute8(SoundInstance instance, long seed) {
        CustomSoundsModule module = customSoundsModule2;
        if (module == null || module.soundLibrary == null || instance == null || !module.enabledSetting.isEnabled()) {
            return false;
        }
        Identifier soundId = instance.getId();
        String normalizedId = "minecraft".equals(soundId.getNamespace()) ? soundId.getPath() : soundId.toString();
        String string = normalizedId;
        if (!module.soundLibrary.hasCategorySounds(normalizedId) || !module.isCategoryEnabled(normalizedId)) {
            return false;
        }
        String replacement = module.resolveReplacementFile(normalizedId);
        if (replacement == null) {
            return false;
        }
        float volume = module.volumeFor(normalizedId);
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
        boolean positional = cameraPos != null && !instance.isRelative() && instance.getAttenuationType() != SoundInstance.AttenuationType.NONE;
        boolean bl = positional;
        if (positional) {
            module.soundLibrary.playPositionalSound(replacement, instance.getX(), instance.getY(), instance.getZ(), cameraPos.x, cameraPos.y, cameraPos.z, volume, seed);
        } else {
            module.soundLibrary.playSound(replacement, volume, seed);
        }
        return true;
    }

    private static Map<String, String> buildDisplayNameMap(List<String> files) {
        LinkedHashMap<String, String> mapped = new LinkedHashMap<String, String>();
        for (String file : files) {
            int slash = file.indexOf(47);
            String baseName = slash >= 0 ? file.substring(slash + 1) : file;
            Object displayName = baseName;
            int suffix = 2;
            while (mapped.containsKey(displayName)) {
                displayName = baseName + " (" + suffix++ + ")";
            }
            mapped.put((String)displayName, file);
        }
        return mapped;
    }
}

