package ru.wexside.misc;

import ru.wexside.misc.ConfigStore;

public interface ConfigStoreRegistry {
    public <T extends ConfigStore> T getStore(Class<T> var1);

    public void registerStore(ConfigStore var1);
}

