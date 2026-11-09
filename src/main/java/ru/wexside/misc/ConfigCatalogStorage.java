package ru.wexside.misc;

import java.io.IOException;
import java.util.List;
import ru.wexside.misc.ConfigFileEntry;

public interface ConfigCatalogStorage {
    public List<ConfigFileEntry> serializeEntries() throws IOException;

    public void applyEntries(List<ConfigFileEntry> var1) throws IOException;
}

