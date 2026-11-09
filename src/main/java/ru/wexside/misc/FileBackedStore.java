package ru.wexside.misc;

import java.io.File;
import ru.wexside.misc.ConfigStore;

public abstract class FileBackedStore
implements ConfigStore {
    public final File file;

    public FileBackedStore(File file) {
        this.file = file;
    }
}

