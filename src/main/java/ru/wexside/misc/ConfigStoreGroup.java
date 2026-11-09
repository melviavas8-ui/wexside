package ru.wexside.misc;

import java.io.IOException;
import java.util.List;
import ru.wexside.misc.ConfigStore;
import ru.wexside.misc.ConfigStoreGroupLifecycle;

public class ConfigStoreGroup
implements ConfigStoreGroupLifecycle {
    private final List<ConfigStore> stores;

    public ConfigStoreGroup(List<ConfigStore> list) {
        this.stores = list;
    }

    @Override
    public void saveAll() {
        for (ConfigStore store : this.stores) {
            try {
                store.save();
            }
            catch (IOException exception) {
                throw new IllegalStateException("Failed to save configuration", exception);
            }
        }
    }

    @Override
    public void loadAll() {
        this.stores.forEach(ConfigStore::load);
    }
}

