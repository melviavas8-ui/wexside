package ru.wexside.misc;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import ru.wexside.misc.StaffNameConfigStore;

public final class StaffNameStore {
    private final StaffNameConfigStore store;

    public StaffNameStore(StaffNameConfigStore store) {
        this.store = store;
    }

    public boolean add(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        boolean added = this.store.getSet().add(name);
        if (added) {
            this.save();
        }
        return added;
    }

    public boolean remove(String name) {
        if (name == null) {
            return false;
        }
        boolean removed = this.store.getSet().remove(name);
        if (removed) {
            this.save();
        }
        return removed;
    }

    public boolean contains(String name) {
        return name != null && this.store.getSet().contains(name);
    }

    public void clear() {
        if (this.store.getSet().isEmpty()) {
            return;
        }
        this.store.getSet().clear();
        this.save();
    }

    public Collection<String> getNames() {
        return Collections.unmodifiableCollection(this.store.getSet());
    }

    private void save() {
        try {
            this.store.save();
        }
        catch (IOException exception) {
            throw new IllegalStateException("Failed to save staff list", exception);
        }
    }
}

