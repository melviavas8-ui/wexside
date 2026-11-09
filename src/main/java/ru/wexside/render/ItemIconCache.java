package ru.wexside.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.render.BakedItemIcon;
import ru.wexside.render.IconAtlas;
import ru.wexside.render.IconAtlasEntry;
import ru.wexside.render.ItemIconKey;
import ru.wexside.util.GuiDrawApi;

public final class ItemIconCache {
    private static final int MAX_ICONS = 96;
    private static final long IDLE_TIMEOUT_NANOS = 10000000000L;
    private final IconAtlas atlas = new IconAtlas(96, false);
    private final Map<ItemIconKey, BakedItemIcon> icons = new HashMap<ItemIconKey, BakedItemIcon>();
    private long nowNanos = System.nanoTime();
    private int frame;

    public BakedItemIcon get(ItemStack stack) {
        ItemIconKey key = new ItemIconKey(stack);
        BakedItemIcon icon = this.icons.computeIfAbsent(key, ignored -> new BakedItemIcon(this.atlas));
        icon.setStack(stack);
        icon.setFrame(this.frame);
        icon.markUsed(this.nowNanos);
        return icon;
    }

    public void beginFrame() {
        ++this.frame;
        this.nowNanos = System.nanoTime();
    }

    public void evictUnused() {
        this.icons.entrySet().removeIf(entry -> {
            BakedItemIcon icon = (BakedItemIcon)entry.getValue();
            if (this.nowNanos - icon.lastUsedNanos() <= 10000000000L) {
                return false;
            }
            icon.close();
            return true;
        });
        if (this.icons.size() <= 96) {
            return;
        }
        ArrayList<Map.Entry<ItemIconKey, BakedItemIcon>> oldest = new ArrayList<Map.Entry<ItemIconKey, BakedItemIcon>>(this.icons.entrySet());
        oldest.sort(Comparator.comparingLong(entry -> ((BakedItemIcon)entry.getValue()).lastUsedNanos()));
        for (int index = 0; this.icons.size() > 96 && index < oldest.size(); ++index) {
            Map.Entry<ItemIconKey, BakedItemIcon> entry2 = oldest.get(index);
            if (!this.icons.remove(entry2.getKey(), entry2.getValue())) continue;
            entry2.getValue().close();
        }
    }

    public void collectBakeEntries(float scale, List<BakedIconEntry> output) {
        for (BakedItemIcon icon : this.icons.values()) {
            BakedIconEntry entry;
            if (icon.frame() != this.frame || (entry = icon.createBakeEntry(scale)) == null) continue;
            output.add(entry);
        }
    }

    public void render(GuiDrawApi renderer, Matrix4f matrix, BakedItemIcon icon, float x, float y, float size, int color) {
        if (icon == null || !icon.texture().isAllocated()) {
            return;
        }
        IconAtlasEntry texture = icon.texture();
        int atlasSize = texture.getTextureSize();
        int textureId = renderer.bindTexture(texture.getTextureId(), atlasSize, atlasSize);
        renderer.drawTexture(matrix, x, y, size, size, texture.getMinU(), texture.getMaxV(), texture.getMaxU(), texture.getMinV(), textureId, color);
    }

    public void close() {
        for (BakedItemIcon icon : this.icons.values()) {
            icon.close();
        }
        this.icons.clear();
        this.atlas.close();
    }

    public BakedItemIcon getOrCreateIcon(ItemStack stack) {
        return this.get(stack);
    }

    public void advanceFrame() {
        this.beginFrame();
    }

    public void appendPendingBakes(float scale, List<BakedIconEntry> output) {
        this.collectBakeEntries(scale, output);
    }

    public void trimCache() {
        this.evictUnused();
    }

    public void disposeCache() {
        this.close();
    }

    public void drawBakedIcon(GuiDrawApi renderer, Matrix4f matrix, Object icon, float x, float y, float size) {
        this.render(renderer, matrix, (BakedItemIcon)icon, x, y, size, -1);
    }

    public void drawTintedBakedIcon(GuiDrawApi renderer, Matrix4f matrix, BakedItemIcon icon, float x, float y, float size, int color) {
        this.render(renderer, matrix, icon, x, y, size, color);
    }
}

