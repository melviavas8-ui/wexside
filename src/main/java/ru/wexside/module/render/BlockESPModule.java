package ru.wexside.module.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.BlockEspStore;
import ru.wexside.misc.ElapsedTimer;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;

public final class BlockESPModule
extends Module
implements ConfigSerializable {
    private static final long SCAN_INTERVAL_MS = 1000L;
    private final BooleanSetting enabledSetting;
    private final NumberSetting scanRadius;
    private final ElapsedTimer scanTimer = new ElapsedTimer();
    private List<BlockHit> hits = new ArrayList<BlockHit>();

    public BlockESPModule(EventBus eventBus) {
        super(eventBus, "block_esp", "Block ESP", "Подсвечивает заданные блоки в мире", ModuleCategory.valueOf("RENDER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Подсветка заданных блоков.").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.scanRadius = ((NumberSettingBuilder)NumberSetting.builder().range(5.0, 32.0).defaultValue(16.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Scan Radius").id("scan_radius").description("Радиус сканирования блоков").aliases("scan radius", "радиус")).build();
        this.registerSetting(this.scanRadius);
    }

    @Override
    protected void initialize() {
        this.listen(WorldRenderEvent.class, this::onWorldRender);
        this.listen(WorldSessionEvent.class, event -> {
            this.hits = new ArrayList<BlockHit>();
        });
    }

    private void onWorldRender(WorldRenderEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        if (player == null || world == null || cameraPos == null) {
            return;
        }
        List<BlockHit> scanned = this.cachedHits();
        if (scanned.isEmpty()) {
            return;
        }
        ArrayList<VisibleBox> boxes = new ArrayList<VisibleBox>();
        for (BlockHit hit : scanned) {
            Box box = this.blockBox(hit.pos());
            if (cameraPos.squaredDistanceTo(box.getCenter()) > 4096.0) continue;
            boxes.add(new VisibleBox(box, hit.color()));
        }
        if (boxes.isEmpty()) {
            return;
        }
        Matrix4f matrix = new Matrix4f().rotation((Quaternionfc)camera.getRotation());
        BufferBuilder consumer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (VisibleBox box : boxes) {
            this.drawBox(consumer, matrix, cameraPos, box.box(), box.color());
        }
        RenderLayers.lines().draw(consumer.end());
    }

    private void drawBox(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, Box box, int color) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;
        this.drawLine(consumer, matrix, cameraPos, minX, minY, minZ, maxX, minY, minZ, color);
        this.drawLine(consumer, matrix, cameraPos, maxX, minY, minZ, maxX, minY, maxZ, color);
        this.drawLine(consumer, matrix, cameraPos, maxX, minY, maxZ, minX, minY, maxZ, color);
        this.drawLine(consumer, matrix, cameraPos, minX, minY, maxZ, minX, minY, minZ, color);
        this.drawLine(consumer, matrix, cameraPos, minX, maxY, minZ, maxX, maxY, minZ, color);
        this.drawLine(consumer, matrix, cameraPos, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        this.drawLine(consumer, matrix, cameraPos, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        this.drawLine(consumer, matrix, cameraPos, minX, maxY, maxZ, minX, maxY, minZ, color);
        this.drawLine(consumer, matrix, cameraPos, minX, minY, minZ, minX, maxY, minZ, color);
        this.drawLine(consumer, matrix, cameraPos, maxX, minY, minZ, maxX, maxY, minZ, color);
        this.drawLine(consumer, matrix, cameraPos, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        this.drawLine(consumer, matrix, cameraPos, minX, minY, maxZ, minX, maxY, maxZ, color);
    }

    private void drawLine(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        int alpha = color >>> 24 & 0xFF;
        consumer.vertex((Matrix4fc)matrix, (float)(x1 - cameraPos.x), (float)(y1 - cameraPos.y), (float)(z1 - cameraPos.z)).color(red, green, blue, alpha);
        consumer.vertex((Matrix4fc)matrix, (float)(x2 - cameraPos.x), (float)(y2 - cameraPos.y), (float)(z2 - cameraPos.z)).color(red, green, blue, alpha);
    }

    private Box blockBox(BlockPos pos) {
        return new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)pos.getX() + 1.0, (double)pos.getY() + 1.0, (double)pos.getZ() + 1.0);
    }

    private Map<Block, Integer> watchedBlocks() {
        HashMap<Block, Integer> map = new HashMap<Block, Integer>();
        BlockEspStore store = WexSideClient.getBlockEspStore();
        if (store == null) {
            return map;
        }
        for (Map.Entry<String, Integer> entry : store.getBlocks().entrySet()) {
            Identifier id = Identifier.tryParse((String)entry.getKey());
            if (id == null) continue;
            Registries.BLOCK.getOptionalValue(id).ifPresent(block -> map.put((Block)block, (Integer)entry.getValue()));
        }
        return map;
    }

    private List<BlockHit> cachedHits() {
        if (this.scanTimer.hasElapsed(1000L)) {
            this.hits = this.scanHits();
            this.scanTimer.update();
        }
        return this.hits;
    }

    private List<BlockHit> scanHits() {
        ArrayList<BlockHit> found = new ArrayList<BlockHit>();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (world == null || player == null) {
            return found;
        }
        Map<Block, Integer> colors = this.watchedBlocks();
        if (colors.isEmpty()) {
            return found;
        }
        int radius = this.scanRadius.getIntValue();
        BlockPos origin = player.getBlockPos();
        int minY = Math.max(origin.getY() - radius, world.getBottomY());
        int maxY = Math.min(origin.getY() + radius, world.getBottomY() + world.getHeight() - 1);
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                for (int y = minY; y <= maxY; ++y) {
                    cursor.set(origin.getX() + x, y, origin.getZ() + z);
                    Integer color = colors.get(world.getBlockState((BlockPos)cursor).getBlock());
                    if (color == null) continue;
                    found.add(new BlockHit(cursor.toImmutable(), color));
                }
            }
        }
        return found;
    }

    private record BlockHit(BlockPos pos, int color) {
    }

    private record VisibleBox(Box box, int color) {
    }
}

