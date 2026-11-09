package ru.wexside.module.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.ThemeColors;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.notification.ItemNotification;
import ru.wexside.notification.NotificationCenter;
import ru.wexside.render.ItemIconCache;
import ru.wexside.render.RenderProjection;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.util.GuiDrawApi;

public final class StructuresModule
extends Module
implements ConfigSerializable {
    private static final int TIMER_COLOR = -45747;
    private static final int NAME_COLOR = -1710619;
    private static final double MAX_DISTANCE_SQ = 2500.0;
    private static final long SETTLE_NS = 150000000L;
    private static final long DEFAULT_DURATION_MS = 30000L;
    private static final float CARD_HEIGHT = 16.0f;
    private static final float TITLE_SIZE = 7.0f;
    private static final float ICON_SIZE = 11.0f;
    private final BooleanSetting enabledSetting;
    private final BooleanSetting world;
    private final ItemIconCache icons = new ItemIconCache();
    private final List<StructureMarker> markers = new ArrayList<StructureMarker>();
    private final Set<BlockPos> pendingBlocks = new HashSet<BlockPos>();
    private final List<WorldEventHit> pendingEvents = new ArrayList<WorldEventHit>();
    private volatile ParticleBurst pendingBurst;
    private volatile long burstStartedAt;

    public StructuresModule(EventBus eventBus) {
        super(eventBus, "structures", "Structures", "Показывает время действия построек (трапки/пласты)", ModuleCategory.valueOf("RENDER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Таймер действия трапок и пластов в мире").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.world = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("World").id("world").description("Рисовать таймер на самом строении")).build();
        this.registerSetting(this.world);
    }

    @Override
    protected void initialize() {
        this.listen(IncomingPacketEvent.class, this::onPacket);
        this.listen(ClientTickEvent.class, this::onTick);
        this.listen(HudRenderEvent.class, this::onHudRender);
        this.listen(WorldSessionEvent.class, event -> this.reset());
    }

    private void onPacket(IncomingPacketEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof ParticleS2CPacket) {
            ParticleS2CPacket particle = (ParticleS2CPacket)packet;
            BlockPos pos2 = BlockPos.ofFloored((double)(particle.getX() + 0.5), (double)(particle.getY() + 0.5), (double)(particle.getZ() + 0.5)).toImmutable();
            if (this.pendingBurst == null) {
                this.burstStartedAt = System.nanoTime();
            }
            this.pendingBurst = new ParticleBurst(pos2, particle.getParameters());
            return;
        }
        if (packet instanceof ChunkDeltaUpdateS2CPacket) {
            ChunkDeltaUpdateS2CPacket delta = (ChunkDeltaUpdateS2CPacket)packet;
            if (this.pendingBurst != null) {
                delta.visitUpdates((pos, state) -> {
                    if (!state.isOf(Blocks.AIR)) {
                        this.pendingBlocks.add(pos.toImmutable());
                    }
                });
            }
            return;
        }
        if (packet instanceof WorldEventS2CPacket) {
            WorldEventS2CPacket worldEvent = (WorldEventS2CPacket)packet;
            if (this.pendingBurst != null) {
                this.pendingEvents.add(new WorldEventHit(worldEvent.getPos(), worldEvent.getEventId()));
            }
        }
    }

    private void onTick(ClientTickEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        ParticleBurst burst = this.pendingBurst;
        if (burst != null && System.nanoTime() - this.burstStartedAt >= 150000000L) {
            HashSet<BlockPos> blocks = new HashSet<BlockPos>(this.pendingBlocks);
            ArrayList<WorldEventHit> extras = new ArrayList<WorldEventHit>(this.pendingEvents);
            this.pendingBlocks.clear();
            this.pendingEvents.clear();
            this.pendingBurst = null;
            this.finishBurst(burst, blocks, extras);
        }
        this.markers.removeIf(StructureMarker::expired);
    }

    private void reset() {
        this.pendingBurst = null;
        this.pendingBlocks.clear();
        this.pendingEvents.clear();
        this.markers.clear();
        this.icons.disposeCache();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void onHudRender(HudRenderEvent event) {
        if (!this.enabledSetting.isEnabled() || !this.world.isEnabled()) {
            this.icons.disposeCache();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
        if (client.player == null || client.world == null || cameraPos == null || this.markers.isEmpty()) {
            return;
        }
        ArrayList<ScreenMarker> onScreen = new ArrayList<ScreenMarker>();
        for (StructureMarker structureMarker : this.markers) {
            Vector2f screen;
            double dz;
            double dy;
            double dx;
            if (structureMarker.expired() || (dx = structureMarker.x - cameraPos.x) * dx + (dy = structureMarker.y - cameraPos.y) * dy + (dz = structureMarker.z - cameraPos.z) * dz > 2500.0 || (screen = this.worldToScreen(structureMarker.x, structureMarker.y, structureMarker.z)) == null) continue;
            onScreen.add(new ScreenMarker(screen.x, screen.y, structureMarker));
        }
        if (onScreen.isEmpty()) {
            return;
        }
        float scale = client.getWindow().getScaleFactor();
        this.icons.advanceFrame();
        for (ScreenMarker screenMarker : onScreen) {
            screenMarker.icon = this.icons.getOrCreateIcon(screenMarker.marker.icon);
        }
        ArrayList<BakedIconEntry> arrayList = new ArrayList<BakedIconEntry>();
        this.icons.appendPendingBakes(scale, arrayList);
        if (!arrayList.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(arrayList);
        }
        Matrix4f matrix = new Matrix4f().scale(scale);
        GuiDrawApi renderer = WexSideClient.getHudRenderer();
        renderer.begin();
        try {
            for (ScreenMarker screenMarker : onScreen) {
                this.drawMarker(renderer, matrix, screenMarker);
            }
        }
        finally {
            renderer.end();
        }
        this.icons.trimCache();
    }

    private void finishBurst(ParticleBurst burst, Set<BlockPos> blocks, List<WorldEventHit> extras) {
        if (MinecraftClient.getInstance().world == null) {
            return;
        }
        Vec3d center = this.centroid(blocks);
        if (center == null) {
            return;
        }
        ItemStack icon = new ItemStack((ItemConvertible)Items.SANDSTONE);
        String name = extras.isEmpty() ? "Trap" : "Plast";
        this.markers.add(new StructureMarker(center.x, center.y, center.z, name, icon, System.currentTimeMillis() + 30000L));
        NotificationCenter overlays = WexSideClient.getNotificationCenter();
        if (overlays != null) {
            overlays.push(new ItemNotification(icon));
        }
    }

    private void drawMarker(GuiDrawApi renderer, Matrix4f matrix, ScreenMarker screenMarker) {
        StructureMarker marker = screenMarker.marker;
        String name = marker.name;
        int seconds = marker.remainingSeconds();
        String timer = seconds + "с";
        float nameWidth = FontRegistry.font2.textWidth(name + " ", 7.0f);
        float timerWidth = FontRegistry.font2.textWidth(timer, 7.0f);
        float width = 19.0f + nameWidth + timerWidth + 5.0f;
        float x = screenMarker.x - width / 2.0f;
        float y = screenMarker.y - 8.0f;
        renderer.drawRoundedRectangle(matrix, x, y, width, 16.0f, 8.0f, ThemeColors.separator());
        float iconX = x + 5.0f;
        float iconY = screenMarker.y - 5.5f;
        this.icons.drawBakedIcon(renderer, matrix, screenMarker.icon, iconX, iconY, 11.0f);
        float textX = iconX + 11.0f + 3.0f;
        float textY = screenMarker.y - 3.5f;
        FontRegistry.font2.drawText(matrix, renderer, name + " ", textX, textY, 7.0f, -1710619);
        FontRegistry.font2.drawText(matrix, renderer, timer, textX + nameWidth, textY, 7.0f, -45747);
    }

    private Vec3d centroid(Set<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return null;
        }
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for (BlockPos pos : blocks) {
            x += (double)pos.getX() + 0.5;
            y += (double)pos.getY() + 0.5;
            z += (double)pos.getZ() + 0.5;
        }
        int count = blocks.size();
        return new Vec3d(x / (double)count, y / (double)count, z / (double)count);
    }

    private Vector2f worldToScreen(double x, double y, double z) {
        return RenderProjection.project(new Vec3d(x, y, z));
    }

    private static final class ParticleBurst {
        final BlockPos pos;
        final Object parameters;

        ParticleBurst(BlockPos pos, Object parameters) {
            this.pos = pos;
            this.parameters = parameters;
        }
    }

    private record WorldEventHit(BlockPos pos, int eventId) {
    }

    private static final class StructureMarker {
        final double x;
        final double y;
        final double z;
        final String name;
        final ItemStack icon;
        final long expiresAt;

        StructureMarker(double x, double y, double z, String name, ItemStack icon, long expiresAt) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.name = name;
            this.icon = icon;
            this.expiresAt = expiresAt;
        }

        boolean expired() {
            return System.currentTimeMillis() >= this.expiresAt;
        }

        int remainingSeconds() {
            return (int)Math.max(0L, (this.expiresAt - System.currentTimeMillis()) / 1000L);
        }
    }

    private static final class ScreenMarker {
        final float x;
        final float y;
        final StructureMarker marker;
        Object icon;

        ScreenMarker(float x, float y, StructureMarker marker) {
            this.x = x;
            this.y = y;
            this.marker = marker;
        }
    }
}

