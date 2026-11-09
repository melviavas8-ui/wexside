package ru.wexside.misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.wexside.misc.BlockedSoundStore;

public class BlockedSoundList {
    private final BlockedSoundStore store;

    public BlockedSoundList(BlockedSoundStore store) {
        this.store = store;
    }

    public void clear() {
        if (this.store.getSet().isEmpty()) {
            return;
        }
        this.store.getSet().clear();
        this.persist();
    }

    public List<String> getBlockedSounds() {
        return new ArrayList<String>(this.store.getSet());
    }

    public boolean remove(String soundId) {
        String normalizedId = BlockedSoundList.normalize(soundId);
        if (normalizedId.isEmpty() || !this.store.getSet().remove(normalizedId)) {
            return false;
        }
        this.persist();
        return true;
    }

    public boolean add(String soundId) {
        String normalizedId = BlockedSoundList.normalize(soundId);
        if (normalizedId.isEmpty() || !this.store.getSet().add(normalizedId)) {
            return false;
        }
        this.persist();
        return true;
    }

    public boolean contains(String soundId) {
        String normalizedId = BlockedSoundList.normalize(soundId);
        return !normalizedId.isEmpty() && this.store.getSet().contains(normalizedId);
    }

    private static String normalize(String soundId) {
        return soundId == null ? "" : soundId.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isEmpty() {
        return this.store.getSet().isEmpty();
    }

    private void persist() {
        try {
            this.store.save();
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public int size() {
        return this.store.getSet().size();
    }
}

