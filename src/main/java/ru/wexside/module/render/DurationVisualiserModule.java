package ru.wexside.module.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.FontRegistry;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.render.BakedItemIcon;
import ru.wexside.render.ItemIconCache;
import ru.wexside.render.RenderProjection;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.util.GuiDrawApi;

public final class DurationVisualiserModule
extends Module
implements ConfigSerializable {
    private static final float ICON_SIZE = 20.0f;
    private static final float ICON_HALF = 10.0f;
    private static final float RING_RADIUS = 10.0f;
    private static final float RING_WIDTH = 3.0f;
    private static final float LABEL_SIZE = 7.0f;
    private static final float LABEL_OFFSET = 14.0f;
    private static final int BACKDROP_COLOR = 0x40000000;
    private static final int RING_COLOR = -1;
    private static final double TRACK_DISTANCE_SQ = 1024.0;
    private final BooleanSetting enabledSetting;
    private final ItemIconCache iconCache = new ItemIconCache();
    private final Map<PlayerEntity, UseTracker> trackers = new HashMap<PlayerEntity, UseTracker>();

    public DurationVisualiserModule(EventBus eventBus) {
        super(eventBus, "duration_visualiser", "Duration Visualiser", "Показывает время использования предмета у соседних игроков", ModuleCategory.valueOf("RENDER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Визуализирует время использования предмета").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
        this.listen(HudRenderEvent.class, this::onHudRender);
        this.listen(WorldSessionEvent.class, event -> {
            this.trackers.clear();
            this.iconCache.disposeCache();
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void onHudRender(HudRenderEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            if (!this.trackers.isEmpty()) {
                this.trackers.clear();
            }
            this.iconCache.disposeCache();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        this.updateTrackers(client.player);
        Matrix4f viewProjection = RenderProjection.viewProjectionMatrix();
        float scale = client.getWindow().getScaleFactor();
        Matrix4f guiMatrix = new Matrix4f().scale(scale);
        long now = System.currentTimeMillis();
        ArrayList<HudEntry> entries = new ArrayList<HudEntry>();
        Iterator<Map.Entry<PlayerEntity, UseTracker>> iterator = this.trackers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PlayerEntity, UseTracker> entry = iterator.next();
            PlayerEntity player = entry.getKey();
            UseTracker tracker = entry.getValue();
            long remaining = tracker.expiresAt - now;
            if (remaining <= 0L || !player.isUsingItem()) {
                iterator.remove();
                continue;
            }
            Vector2f screen = RenderProjection.projectEntityCenter((Entity)player, viewProjection);
            if (screen == null) continue;
            entries.add(new HudEntry(screen.x, screen.y, remaining, tracker.totalDuration, player.getActiveItem()));
        }
        if (entries.isEmpty()) {
            return;
        }
        this.iconCache.advanceFrame();
        for (HudEntry hudEntry : entries) {
            hudEntry.icon = this.iconCache.getOrCreateIcon(hudEntry.stack);
        }
        ArrayList<BakedIconEntry> bakes = new ArrayList<BakedIconEntry>();
        this.iconCache.appendPendingBakes(scale, bakes);
        if (!bakes.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(bakes);
        }
        GuiDrawApi renderer = WexSideClient.getHudRenderer();
        renderer.begin();
        try {
            for (HudEntry hudEntry : entries) {
                float x = hudEntry.x;
                float y = hudEntry.y;
                renderer.drawRoundedRectangleBordered(guiMatrix, x - 10.0f, y - 10.0f, 20.0f, 20.0f, 20.0f * scale, 0.0f, 0x40000000);
                if (hudEntry.icon != null) {
                    this.iconCache.drawBakedIcon(renderer, guiMatrix, hudEntry.icon, x - 5.6f, y - 5.6f, 11.2f);
                }
                float progress = (float)hudEntry.remaining / (float)hudEntry.totalDuration;
                renderer.drawCircle(guiMatrix, x, y, 0.0f, progress * 360.0f, 3.0f, 10.0f, -1);
                String label = String.format(Locale.US, "%.1f сек.", Float.valueOf((float)hudEntry.remaining / 1000.0f));
                float width = FontRegistry.font6.textWidth(label, 7.0f);
                FontRegistry.font6.drawText(guiMatrix, renderer, label, x - width / 2.0f, y + 14.0f, 7.0f, -1);
            }
        }
        finally {
            renderer.end();
        }
        this.iconCache.trimCache();
    }

    private void updateTrackers(ClientPlayerEntity local) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player.squaredDistanceTo((Entity)local) > 1024.0 || !player.isUsingItem()) {
                this.trackers.remove(player);
                continue;
            }
            ItemStack stack = player.getActiveItem();
            UseAction action = stack.getUseAction();
            if (stack.isEmpty() || action != UseAction.EAT && action != UseAction.DRINK) {
                this.trackers.remove(player);
                continue;
            }
            int maxUseTime = stack.getMaxUseTime((LivingEntity)player);
            int useTime = player.getItemUseTime();
            if (useTime != maxUseTime - 1) continue;
            long duration = (long)maxUseTime * 50L;
            this.trackers.put(player, new UseTracker(System.currentTimeMillis() + duration, duration));
        }
    }

    static final class UseTracker {
        final long expiresAt;
        final long totalDuration;

        UseTracker(long expiresAt, long totalDuration) {
            this.expiresAt = expiresAt;
            this.totalDuration = totalDuration;
        }
    }

    static final class HudEntry {
        final float x;
        final float y;
        final long remaining;
        final long totalDuration;
        final ItemStack stack;
        BakedItemIcon icon;

        HudEntry(float x, float y, long remaining, long totalDuration, ItemStack stack) {
            this.x = x;
            this.y = y;
            this.remaining = remaining;
            this.totalDuration = totalDuration;
            this.stack = stack;
        }
    }
}

