package ru.wexside.misc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.wexside.misc.ConfigFileEntry;
import ru.wexside.misc.ConfigReadResult;
import ru.wexside.misc.ConfigRegistry;
import ru.wexside.misc.EncryptedConfigIO;
import ru.wexside.misc.JsonConfigStore;

public final class ConfigProfile
extends JsonConfigStore {
    private static final String FORMAT = "wexside-profile";
    private final ConfigRegistry registry;
    private String displayName;

    public ConfigProfile(File file, Gson gson, ConfigRegistry registry) {
        super(file, gson);
        this.registry = registry;
    }

    @Override
    public void load() {
        this.applyPreparedLoad(this.prepareLoad());
    }

    record PreparedLoad(String displayName, List<ConfigFileEntry> entries, boolean needsMigration) {
        PreparedLoad {
            entries = List.copyOf(entries);
        }
    }

    PreparedLoad prepareLoad() {
        if (!this.file.isFile()) {
            throw new IllegalStateException("Config profile does not exist: " + String.valueOf(this.file));
        }
        ConfigReadResult result = EncryptedConfigIO.readConfig(this.file, this.gson2);
        JsonObject root = result.json();
        if (!root.has("magic") || !FORMAT.equals(root.get("magic").getAsString())) {
            throw new IllegalStateException("Invalid config profile format: " + String.valueOf(this.file));
        }
        String loadedDisplayName = root.has("name") && !root.get("name").isJsonNull() ? root.get("name").getAsString() : null;
        ArrayList<ConfigFileEntry> entries = new ArrayList<ConfigFileEntry>();
        JsonArray array = root.has("entries") && root.get("entries").isJsonArray() ? root.getAsJsonArray("entries") : new JsonArray();
        for (JsonElement element : array) {
            JsonObject entry;
            if (!element.isJsonObject() || !(entry = element.getAsJsonObject()).has("path") || !entry.has("data")) continue;
            entries.add(new ConfigFileEntry(entry.get("path").getAsString(), entry.get("data").getAsString()));
        }
        return new PreparedLoad(loadedDisplayName, entries, result.needsMigration());
    }

    void applyPreparedLoad(PreparedLoad prepared) {
        this.displayName = prepared.displayName();
        try {
            this.registry.applyEntries(prepared.entries());
        }
        catch (IOException exception) {
            throw new IllegalStateException("Failed to apply config profile " + String.valueOf(this.file), exception);
        }
        if (prepared.needsMigration()) {
            try {
                this.save();
            }
            catch (IOException exception) {
                throw new IllegalStateException("Failed to migrate config profile " + String.valueOf(this.file), exception);
            }
        }
    }

    @Override
    public void save() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("magic", FORMAT);
        root.addProperty("version", (Number)1);
        if (this.displayName != null && !this.displayName.isBlank()) {
            root.addProperty("name", this.displayName);
        }
        JsonArray entries = new JsonArray();
        for (ConfigFileEntry entry : this.registry.serializeEntries()) {
            JsonObject value = new JsonObject();
            value.addProperty("path", entry.path());
            value.addProperty("data", entry.data());
            entries.add((JsonElement)value);
        }
        root.add("entries", (JsonElement)entries);
        EncryptedConfigIO.writeConfig(this.file, root, Set.of("magic", "version", "name"), this.gson2);
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String readDisplayName() {
        if (!this.file.isFile()) {
            return null;
        }
        JsonObject root = EncryptedConfigIO.readConfig(this.file, this.gson2).json();
        if (!root.has("magic") || !FORMAT.equals(root.get("magic").getAsString())) {
            return null;
        }
        return root.has("name") && !root.get("name").isJsonNull() ? root.get("name").getAsString() : null;
    }

    public List<ConfigFileEntry> getEntries() {
        ConfigReadResult result = EncryptedConfigIO.readConfig(this.file, this.gson2);
        if (result == null || !result.json().has("entries")) {
            return List.of();
        }
        ArrayList<ConfigFileEntry> entries = new ArrayList<ConfigFileEntry>();
        for (JsonElement element : result.json().getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            entries.add(new ConfigFileEntry(entry.get("path").getAsString(), entry.get("data").getAsString()));
        }
        return entries;
    }
}

