package ru.wexside.misc;

import java.io.IOException;

public interface ConfigStore {
    public void load();

    public void save() throws IOException;
}

