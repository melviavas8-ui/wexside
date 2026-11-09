package ru.wexside.misc;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import ru.wexside.misc.BlockEspConfigStore;

public class BlockEspStore {
    private final BlockEspConfigStore configStore;

    public BlockEspStore(BlockEspConfigStore blockEspConfigStore) {
        this.configStore = blockEspConfigStore;
    }

    public Map<String, Integer> getBlocks() {
        return Collections.unmodifiableMap(this.configStore.getMap());
    }

    public boolean put(String blockId, int color) {
        boolean changed;
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
        Integer previousColor = this.configStore.getMap().put(blockId, color);
        boolean bl = changed = previousColor == null || previousColor != color;
        if (changed) {
            this.persist();
        }
        return changed;
    }

    public boolean contains(String blockId) {
        return blockId != null && this.configStore.getMap().containsKey(blockId);
    }

    public boolean remove(String blockId) {
        boolean removed;
        if (blockId == null) {
            return false;
        }
        boolean bl = removed = this.configStore.getMap().remove(blockId) != null;
        if (removed) {
            this.persist();
        }
        return removed;
    }

    public void clear() {
        if (this.configStore.getMap().isEmpty()) {
            return;
        }
        this.configStore.getMap().clear();
        this.persist();
    }

    private void persist() {
        try {
            this.configStore.save();
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }
}

