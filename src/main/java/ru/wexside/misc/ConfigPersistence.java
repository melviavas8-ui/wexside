package ru.wexside.misc;

import java.io.IOException;
import java.util.List;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.misc.ConfigFileEntry;

public interface ConfigPersistence {
    public void register(ConfigSerializable var1);

    public void applyEntries(List<ConfigFileEntry> var1) throws IOException;

    public List<ConfigFileEntry> serializeEntries() throws IOException;
}

