package ru.wexside.module.player;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.ElapsedTimer;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.render.BakedItemIcon;
import ru.wexside.render.ItemIconCache;
import ru.wexside.render.RenderProjection;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.GuiDrawApi;

public class MineHelperModule
extends Module
implements ConfigSerializable {
    private static final int SCAN_SLICES = 2;
    private static final long SCAN_INTERVAL_MS = 1000L;
    private static final Map<Block, OreGroup> ORES = new HashMap<Block, OreGroup>();
    private final BooleanSetting enabledSetting;
    private final MultiSelectSetting ores;
    private final NumberSetting scanRadius;
    private final BooleanSetting renderMaterial;
    private final ItemIconCache iconCache = new ItemIconCache();
    private final ElapsedTimer scanTimer = new ElapsedTimer();
    private List<OreHit> scanBuffer;
    private List<OreHit> found = List.of();
    private BlockPos scanOrigin;
    private int scanOffset;
    private int scanExtent;

    public MineHelperModule(EventBus eventBus) {
        super(eventBus, "mine_helper", "Mine Helper", "Сканирует территорию в поиске руд", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        MultiSelectSetting oresSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Diamond", "Emerald", "Gold", "Iron", "Debris", "Lapis", "Quartz", "Redstone", "Coal").selectAll(true).optionListEnabled(false).name("Ores").id("ores").description("Какие руды искать").aliases("ores", "руды")).build();
        oresSetting.setOptions(new String[]{"Diamond", "Emerald", "Gold", "Iron", "Debris", "Lapis", "Quartz", "Redstone", "Coal"});
        this.ores = oresSetting;
        this.registerSetting(oresSetting);
        this.scanRadius = ((NumberSettingBuilder)NumberSetting.builder().range(5.0, 30.0).defaultValue(15.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Scan Radius").id("scan_radius").description("Радиус сканирования").aliases("scan radius", "радиус")).build();
        this.registerSetting(this.scanRadius);
        this.renderMaterial = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Render Material").id("render_material").description("Отображать выпадаемый материал над рудой").aliases("render material", "материал")).build();
        this.registerSetting(this.renderMaterial);
    }

    @Override
    protected void initialize() {
        this.listen(WorldRenderEvent.class, this::onWorldRender);
        this.listen(HudRenderEvent.class, event -> this.onHudRender());
    }

    private List<OreHit> visibleOres() {
        this.scanSlice();
        return this.found;
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
        if (alpha == 0) {
            alpha = 255;
        }
        consumer.vertex((Matrix4fc)matrix, (float)(x1 - cameraPos.x), (float)(y1 - cameraPos.y), (float)(z1 - cameraPos.z)).color(red, green, blue, alpha);
        consumer.vertex((Matrix4fc)matrix, (float)(x2 - cameraPos.x), (float)(y2 - cameraPos.y), (float)(z2 - cameraPos.z)).color(red, green, blue, alpha);
    }

    private void onWorldRender(WorldRenderEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        if (!this.enabledSetting.isEnabled() || client.world == null || client.player == null || cameraPos == null) {
            return;
        }
        List<String> selected = this.ores.getSelectedOptions();
        ArrayList<EspBox> boxes = new ArrayList<EspBox>();
        for (OreHit hit : this.visibleOres()) {
            Box box = this.blockBox(hit.pos());
            if (!selected.contains(hit.group().name()) || !this.inView(box, cameraPos)) continue;
            boxes.add(new EspBox(box, hit.group().color()));
        }
        if (boxes.isEmpty()) {
            return;
        }
        Matrix4f matrix = new Matrix4f().rotation((Quaternionfc)camera.getRotation());
        BufferBuilder consumer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (EspBox box : boxes) {
            this.drawBox(consumer, matrix, cameraPos, box.box(), box.color());
        }
        RenderLayers.lines().draw(consumer.end());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void onHudRender() {
        if (!this.enabledSetting.isEnabled() || !this.renderMaterial.isEnabled()) {
            this.iconCache.disposeCache();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        if (client.world == null || player == null || cameraPos == null) {
            return;
        }
        List<String> selected = this.ores.getSelectedOptions();
        Matrix4f projection = RenderProjection.viewProjectionMatrix();
        this.iconCache.advanceFrame();
        ArrayList<ScreenIcon> icons = new ArrayList<ScreenIcon>();
        for (OreHit hit : this.visibleOres()) {
            Vector2f screen;
            if (!selected.contains(hit.group().name())) continue;
            BlockPos pos = hit.pos();
            double x = (double)pos.getX() + 0.5;
            double y = (double)pos.getY() + 0.5;
            double z = (double)pos.getZ() + 0.5;
            Box box = this.blockBox(pos);
            if (!this.inView(box, cameraPos) || (screen = this.worldToScreen(x, y, z, projection)) == null) continue;
            float size = this.iconSize(player.squaredDistanceTo(x, y, z));
            icons.add(new ScreenIcon(screen.x, screen.y, size, hit.group().icon()));
        }
        if (icons.isEmpty()) {
            this.iconCache.trimCache();
            return;
        }
        float scale = client.getWindow().getScaleFactor();
        ArrayList<BakedItemIcon> bakedIcons = new ArrayList<BakedItemIcon>();
        for (ScreenIcon icon : icons) {
            bakedIcons.add(this.iconCache.getOrCreateIcon(icon.stack()));
        }
        ArrayList<BakedIconEntry> commands = new ArrayList<BakedIconEntry>();
        this.iconCache.appendPendingBakes(scale, commands);
        if (!commands.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(commands);
        }
        GuiDrawApi renderer = WexSideClient.getHudRenderer();
        Matrix4f scaled = new Matrix4f().scale(scale);
        renderer.begin();
        try {
            for (int index = 0; index < icons.size(); ++index) {
                ScreenIcon icon = (ScreenIcon)icons.get(index);
                this.iconCache.drawBakedIcon(renderer, scaled, bakedIcons.get(index), icon.x() - icon.size() / 2.0f, icon.y() - icon.size() / 2.0f, icon.size());
            }
        }
        finally {
            renderer.end();
        }
        this.iconCache.trimCache();
    }

    private float iconSize(double squaredDistance) {
        double distance = Math.max(Math.sqrt(squaredDistance), 4.0);
        return MathHelper.clamp((float)((float)(104.0 / distance)), (float)5.0f, (float)22.0f);
    }

    private static void registerOre(String name, int color, Item icon, Block ... blocks) {
        OreGroup group = new OreGroup(name, color | 0xFF000000, new ItemStack((ItemConvertible)icon));
        for (Block block : blocks) {
            ORES.put(block, group);
        }
    }

    private void scanSlice() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (world == null || player == null) {
            return;
        }
        if (this.scanOrigin == null) {
            if (!this.scanTimer.hasElapsed(1000L)) {
                return;
            }
            this.scanOrigin = player.getBlockPos();
            this.scanExtent = this.scanRadius.getIntValue();
            this.scanOffset = -this.scanExtent;
            this.scanBuffer = new ArrayList<OreHit>();
        }
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int slices = 0;
        while (slices < 2 && this.scanOffset <= this.scanExtent) {
            int x = this.scanOrigin.getX() + this.scanOffset;
            for (int yOff = -this.scanExtent; yOff <= this.scanExtent; ++yOff) {
                for (int zOff = -this.scanExtent; zOff <= this.scanExtent; ++zOff) {
                    mutable.set(x, this.scanOrigin.getY() + yOff, this.scanOrigin.getZ() + zOff);
                    OreGroup group = ORES.get(world.getBlockState((BlockPos)mutable).getBlock());
                    if (group == null) continue;
                    this.scanBuffer.add(new OreHit(mutable.toImmutable(), group));
                }
            }
            ++slices;
            ++this.scanOffset;
        }
        if (this.scanOffset > this.scanExtent) {
            this.found = this.scanBuffer;
            this.scanBuffer = null;
            this.scanOrigin = null;
            this.scanTimer.update();
        }
    }

    private Box blockBox(BlockPos pos) {
        return new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)pos.getX() + 1.0, (double)pos.getY() + 1.0, (double)pos.getZ() + 1.0);
    }

    private boolean inView(Box box, Vec3d cameraPos) {
        return cameraPos.squaredDistanceTo(box.getCenter()) < 4096.0;
    }

    private Vector2f worldToScreen(double x, double y, double z, Matrix4f matrix) {
        return RenderProjection.project(x, y, z, matrix);
    }

    static {
        MineHelperModule.registerOre("Diamond", 4910553, Items.DIAMOND, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        MineHelperModule.registerOre("Emerald", 1564002, Items.EMERALD, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        MineHelperModule.registerOre("Gold", 16576075, Items.GOLD_INGOT, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        MineHelperModule.registerOre("Iron", 14200723, Items.IRON_INGOT, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        MineHelperModule.registerOre("Debris", 6637376, Items.NETHERITE_SCRAP, Blocks.ANCIENT_DEBRIS);
        MineHelperModule.registerOre("Lapis", 1395125, Items.LAPIS_LAZULI, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        MineHelperModule.registerOre("Quartz", 15130844, Items.QUARTZ, Blocks.NETHER_QUARTZ_ORE);
        MineHelperModule.registerOre("Redstone", 0xFF0000, Items.REDSTONE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        MineHelperModule.registerOre("Coal", 0x363636, Items.COAL, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
    }

    private record OreHit(BlockPos pos, OreGroup group) {
    }

    private record OreGroup(String name, int color, ItemStack icon) {
    }

    private record EspBox(Box box, int color) {
    }

    private record ScreenIcon(float x, float y, float size, ItemStack stack) {
    }
}

