package ru.wexside.misc;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BlockEspConfigStore;
import ru.wexside.misc.BlockedSoundStore;
import ru.wexside.misc.ConfigFileEntry;
import ru.wexside.misc.ConfigProfile;
import ru.wexside.misc.ConfigRegistry;
import ru.wexside.misc.ConfigStore;
import ru.wexside.misc.ConfigStoreGroup;
import ru.wexside.misc.ConfigStoreGroupLifecycle;
import ru.wexside.misc.ConfigStoreRegistry;
import ru.wexside.misc.FriendListStore;
import ru.wexside.misc.KeybindRegistry;
import ru.wexside.misc.MacroConfigStore;
import ru.wexside.misc.PasswordConfigStore;
import ru.wexside.misc.PotionPresetStore;
import ru.wexside.misc.StaffNameConfigStore;
import ru.wexside.misc.ThemeConfigStore;
import ru.wexside.misc.UserCacheStore;
import ru.wexside.misc.WaypointConfigStore;
import ru.wexside.notification.NotificationCategory;
import ru.wexside.notification.NotificationCenter;
import ru.wexside.notification.TextNotification;

public class ConfigManager
implements ConfigStoreRegistry {
    private final File clientDirectory;
    private final KeybindRegistry keybindRegistry;
    private final ConfigStoreGroupLifecycle baseStores;
    private boolean importedEntriesPending;
    private final List<ConfigStore> stores = new ArrayList<ConfigStore>();
    private final ConfigRegistry configRegistry;
    private String currentProfileName;
    private final Gson gson;

    public ConfigManager(File clientDirectory, Gson gson, ConfigRegistry configRegistry, KeybindRegistry keybindRegistry) {
        this.baseStores = new ConfigStoreGroup(this.stores);
        this.clientDirectory = clientDirectory;
        this.gson = gson;
        this.configRegistry = configRegistry;
        this.keybindRegistry = keybindRegistry;
        this.registerStore(new ThemeConfigStore(new File(clientDirectory, "theme.wex"), gson));
        this.registerStore(new UserCacheStore(new File(clientDirectory, "usercache.wex"), gson));
        this.registerStore(new FriendListStore(new File(clientDirectory, "friends.wex"), gson));
        this.registerStore(new BlockEspConfigStore(new File(clientDirectory, "blockesp.wex"), gson));
        this.registerStore(new StaffNameConfigStore(new File(clientDirectory, "staff.wex"), gson));
        this.registerStore(new WaypointConfigStore(new File(clientDirectory, "waypoints.wex"), gson));
        this.registerStore(new MacroConfigStore(new File(clientDirectory, "macros.wex"), gson));
        this.registerStore(new BlockedSoundStore(new File(clientDirectory, "sounds.wex"), gson));
        this.registerStore(new PasswordConfigStore(new File(clientDirectory, "autoauth.wex"), gson));
        this.registerStore(new PotionPresetStore(new File(clientDirectory, "potions.wex"), gson));
    }

    public void saveBaseStores() {
        this.baseStores.saveAll();
    }

    @Override
    public <T extends ConfigStore> T getStore(Class<T> clazz) {
        for (ConfigStore store : this.stores) {
            if (clazz != store.getClass()) continue;
            return (T)((ConfigStore)clazz.cast(store));
        }
        return null;
    }

    public boolean profileExists(String name) {
        return this.resolveProfileFile(name).exists();
    }

    public void openConfigFolder() throws IOException {
        File file = this.getConfigDirectory();
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Не удалось создать папку конфигов");
        }
        try {
            Util.getOperatingSystem().open(file);
        }
        catch (Exception exception) {
            throw new IOException("Не удалось открыть папку конфигов", exception);
        }
    }

    public String getCurrentProfileName() {
        return this.currentProfileName;
    }

    public void loadProfile(String name) {
        String profileName = this.normalizeProfileName(name);
        if (!this.profileExists(profileName)) {
            throw new IllegalArgumentException("Конфиг не найден: " + profileName);
        }
        ConfigProfile profile = this.openProfile(profileName);
        ConfigProfile.PreparedLoad prepared = profile.prepareLoad();
        this.keybindRegistry.resetSettingKeybinds();
        try {
            this.configRegistry.restoreBaseline();
        }
        catch (IOException exception) {
            throw new IllegalStateException("Не удалось восстановить настройки по умолчанию", exception);
        }
        profile.applyPreparedLoad(prepared);
        this.currentProfileName = profileName;
        this.importedEntriesPending = false;
        this.saveUserCache();
        NotificationCenter notifications = WexSideClient.getNotificationCenter();
        if (notifications != null) {
            notifications.push(new TextNotification(NotificationCategory.SYSTEM, "config", "C", "Загружен профиль " + profileName));
        }
    }

    public void saveProfile(String name) throws IOException {
        ConfigProfile profile;
        String profileName = this.normalizeProfileName(name);
        String displayName = (profile = this.openProfile(profileName)).readDisplayName();
        profile.setDisplayName(displayName != null ? displayName : "Общий");
        profile.save();
        this.currentProfileName = profileName;
        this.importedEntriesPending = false;
        this.saveUserCache();
    }

    public List<String> listProfiles() throws IOException {
        File configDirectory = this.getConfigDirectory();
        if (!configDirectory.exists()) {
            if (!configDirectory.mkdirs()) {
                throw new IOException("Не удалось создать папку конфигов");
            }
            return List.of();
        }
        File[] profileFiles = configDirectory.listFiles((directory, fileName) -> fileName != null && fileName.endsWith(".wex"));
        if (profileFiles == null || profileFiles.length == 0) {
            return List.of();
        }
        Arrays.sort(profileFiles, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        ArrayList<String> profileNames = new ArrayList<String>(profileFiles.length);
        for (File profileFile : profileFiles) {
            if (profileFile == null || !profileFile.isFile()) continue;
            String fileName2 = profileFile.getName();
            profileNames.add(fileName2.substring(0, fileName2.length() - ".wex".length()));
        }
        return profileNames;
    }

    public void resetProfile() throws IOException {
        this.keybindRegistry.resetSettingKeybinds();
        this.configRegistry.restoreBaseline();
        this.currentProfileName = null;
        this.importedEntriesPending = false;
        this.saveUserCache();
    }

    public boolean hasPendingImportedEntries() {
        return this.importedEntriesPending;
    }

    public void profileRenamed(String oldName, String newName) {
        if (oldName == null) {
            return;
        }
        String oldProfileName = this.normalizeProfileName(oldName);
        if (oldProfileName.equals(this.currentProfileName)) {
            this.currentProfileName = this.normalizeProfileName(newName);
            this.saveUserCache();
        }
    }

    public void saveProfileWithDisplayName(String name, String displayName) throws IOException {
        String profileName = this.normalizeProfileName(name);
        ConfigProfile profile = this.openProfile(profileName);
        profile.setDisplayName(displayName);
        profile.save();
        this.currentProfileName = profileName;
        this.importedEntriesPending = false;
        this.saveUserCache();
    }

    private static String sanitizeProfileName(String requestedName) {
        String sanitizedName;
        String string = sanitizedName = requestedName == null ? "" : requestedName.trim();
        if (sanitizedName.endsWith(".wex")) {
            sanitizedName = sanitizedName.substring(0, sanitizedName.length() - ".wex".length());
        }
        if ((sanitizedName = sanitizedName.replaceAll("[\\\\/:*?\"<>|]", "_").trim()).isBlank()) {
            sanitizedName = "shared";
        }
        return sanitizedName.length() > 32 ? sanitizedName.substring(0, 32).trim() : sanitizedName;
    }

    private String getSessionUsername() {
        try {
            return MinecraftClient.getInstance().getSession().getUsername();
        }
        catch (Exception exception) {
            return null;
        }
    }

    public void initialize() {
        String profileName;
        this.baseStores.loadAll();
        try {
            this.configRegistry.captureBaseline();
        }
        catch (IOException exception) {
            throw new IllegalStateException("Failed to capture default configuration", exception);
        }
        UserCacheStore userCacheStore = this.getStore(UserCacheStore.class);
        String string = profileName = userCacheStore == null ? null : userCacheStore.getLastLoadedConfig();
        if (profileName == null || profileName.isBlank() || !this.profileExists(profileName)) {
            String string2 = profileName = this.profileExists("default") ? "default" : null;
        }
        if (profileName != null) {
            this.loadProfile(profileName);
        }
    }

    private ConfigProfile openProfile(String name) {
        return new ConfigProfile(this.resolveProfileFile(name), this.gson, this.configRegistry);
    }

    public List<ConfigFileEntry> readProfileEntries(String name) {
        return this.openProfile(name).getEntries();
    }

    @Override
    public void registerStore(ConfigStore store) {
        this.stores.add(store);
    }

    public String importProfile(String requestedName, List<ConfigFileEntry> entries) throws IOException {
        String profileName = this.createUniqueProfileName(requestedName);
        this.keybindRegistry.resetSettingKeybinds();
        this.configRegistry.restoreBaseline();
        this.configRegistry.applyEntries(entries);
        this.saveProfileWithDisplayName(profileName, "Пати");
        return profileName;
    }

    public void profileDeleted(String name) {
        if (name != null && this.normalizeProfileName(name).equals(this.currentProfileName)) {
            this.currentProfileName = null;
            this.saveUserCache();
        }
    }

    public boolean deleteProfile(String name) throws IOException {
        File directory = this.getConfigDirectory().getCanonicalFile();
        File profile = this.resolveProfileFile(name).getCanonicalFile();
        if (!profile.toPath().startsWith(directory.toPath()) || !profile.isFile()) {
            return false;
        }
        if (!profile.delete()) {
            throw new IOException("Не удалось удалить конфиг " + name);
        }
        this.profileDeleted(name);
        return true;
    }

    private File resolveProfileFile(String name) {
        return new File(this.getConfigDirectory(), this.normalizeProfileName(name) + ".wex");
    }

    private String normalizeProfileName(String name) {
        return ConfigManager.sanitizeProfileName(name == null || name.isBlank() ? "default" : name);
    }

    public void stageImportedEntries(List<ConfigFileEntry> entries) throws IOException {
        this.keybindRegistry.resetSettingKeybinds();
        this.configRegistry.restoreBaseline();
        this.configRegistry.applyEntries(entries);
        this.importedEntriesPending = true;
    }

    public String createUniqueProfileName(String requestedName) {
        String baseName = ConfigManager.sanitizeProfileName(requestedName);
        if (!this.profileExists(baseName)) {
            return baseName;
        }
        int suffix = 2;
        while (suffix < 1000) {
            String candidate;
            if (this.profileExists(candidate = baseName + " (" + suffix++ + ")")) continue;
            return candidate;
        }
        return baseName + " (" + System.currentTimeMillis() + ")";
    }

    public File getConfigDirectory() {
        return new File(this.clientDirectory, "config");
    }

    private void saveUserCache() {
        UserCacheStore userCacheStore = this.getStore(UserCacheStore.class);
        if (userCacheStore == null) {
            return;
        }
        userCacheStore.setLastNickname(this.getSessionUsername());
        userCacheStore.setLastLoadedConfig(this.currentProfileName);
        try {
            userCacheStore.save();
        }
        catch (IOException exception) {
            WexSideClient.getInstance().getLogger().warn("Failed to save user cache", (Throwable)exception);
        }
    }
}

