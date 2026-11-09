package ru.wexside.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.misc.ConfigCatalogStorage;
import ru.wexside.misc.ConfigFileEntry;

public class ConfigEntryCodec
implements ConfigCatalogStorage {
    private final List<ConfigSerializable> entries;

    public ConfigEntryCodec(List<ConfigSerializable> entries) {
        this.entries = entries;
    }

    @Override
    public List<ConfigFileEntry> serializeEntries() throws IOException {
        ArrayList<ConfigFileEntry> serializedEntries = new ArrayList<ConfigFileEntry>();
        for (ConfigSerializable entry : this.entries) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            entry.writeConfig(new DataOutputStream(buffer));
            serializedEntries.add(new ConfigFileEntry(entry.getConfigId(), Base64.getEncoder().encodeToString(buffer.toByteArray())));
        }
        return serializedEntries;
    }

    @Override
    public void applyEntries(List<ConfigFileEntry> entries) throws IOException {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        LinkedHashMap<String, ConfigSerializable> entriesById = new LinkedHashMap<String, ConfigSerializable>();
        for (ConfigSerializable entry : this.entries) {
            entriesById.put(entry.getConfigId(), entry);
        }
        for (ConfigFileEntry serializedEntry : entries) {
            ConfigSerializable target;
            if (serializedEntry == null || serializedEntry.path() == null || serializedEntry.data() == null || (target = (ConfigSerializable)entriesById.get(serializedEntry.path())) == null) continue;
            try {
                byte[] payload = Base64.getDecoder().decode(serializedEntry.data());
                target.readConfig(new DataInputStream(new ByteArrayInputStream(payload)));
            }
            catch (IOException | RuntimeException exception) {}
        }
    }
}

