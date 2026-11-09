package ru.wexside.module.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EntityAttackEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.ColorSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.ColorUtils;

public final class CriticalHitEffectModule
extends Module
implements ConfigSerializable {
    private static final int MAX_BLOCKS = 420;
    private static final int MAX_GHOSTS = 24;
    private static final int MAX_SPARKS = 260;
    private static final float SPARK_CHANCE = 0.2f;
    private final BooleanSetting enabledSetting;
    private final ColorSetting color;
    private final NumberSetting lifeTime;
    private final NumberSetting rise;
    private final NumberSetting pulseRadius;
    private final BooleanSetting filled;
    private final BooleanSetting ignoreDepth;
    private final List<CritGhost> ghosts = new ArrayList<CritGhost>();
    private final List<Spark> sparks = new ArrayList<Spark>();
    private long lastSparkTime = Long.MIN_VALUE;

    public CriticalHitEffectModule(EventBus eventBus) {
        super(eventBus, "crit_effect", "CriticalHit Effect", "Эффект души и волна по блокам при крит-ударе", ModuleCategory.valueOf("RENDER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Включить эффект при крит-ударе по игроку").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        ColorSetting colorSetting = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("Color").id("color").description("Цвет души и волны").aliases("color", "цвет")).build();
        colorSetting.setPrimaryColor(0, -11753627);
        colorSetting.setPrimaryColor(1, -1543135);
        colorSetting.setPrimaryColor(2, -9279489);
        colorSetting.setPrimaryColor(3, -46001);
        colorSetting.setPrimaryColor(4, -13218);
        colorSetting.setPrimaryColor(5, -10582785);
        colorSetting.setPrimaryColor(6, -2732032);
        this.color = colorSetting;
        this.registerSetting(colorSetting);
        this.lifeTime = ((NumberSettingBuilder)NumberSetting.builder().range(10.0, 30.0).defaultValue(20.0).multiplier(50.0).precision(0).animationSpeed(20.0f).snapTo(5.0).name("Life time").id("life_time").description("Время жизни следа")).build();
        this.registerSetting(this.lifeTime);
        this.rise = ((NumberSettingBuilder)NumberSetting.builder().range(0.5, 3.0).defaultValue(1.5).multiplier(1.0).precision(1).animationSpeed(20.0f).markers(0.5).snapTo(0.5).name("Rise").id("rise").description("Высота подъёма души")).build();
        this.registerSetting(this.rise);
        this.pulseRadius = ((NumberSettingBuilder)NumberSetting.builder().range(3.0, 8.0).defaultValue(5.0).multiplier(1.0).precision(0).animationSpeed(20.0f).markers(1.0).name("Pulse radius").id("pulse_radius").description("Радиус волны по блокам")).build();
        this.registerSetting(this.pulseRadius);
        this.filled = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Filled").id("filled").description("Заливать модель души")).build();
        this.registerSetting(this.filled);
        this.ignoreDepth = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Ignore depth").id("ignore_depth").description("Рисовать эффект сквозь стены")).build();
        this.registerSetting(this.ignoreDepth);
    }

    @Override
    protected void initialize() {
        this.listen(EntityAttackEvent.class, this::onEntityAttack);
        this.listen(WorldRenderEvent.class, this::onWorldRender);
    }

    private void onEntityAttack(EntityAttackEvent event) {
        PlayerEntity player;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!this.enabledSetting.isEnabled() || client.player == null || client.world == null) {
            return;
        }
        Entity target = event.getEntity();
        if (!(target instanceof PlayerEntity) || (player = (PlayerEntity)target) == client.player || !player.isAlive() || player.isRemoved()) {
            return;
        }
        if (!this.isCriticalHit(client)) {
            return;
        }
        Vec3d origin = this.groundPoint(client.world, player);
        this.ghosts.add(new CritGhost(player, origin, this.collectBlocks(client.world, origin), System.currentTimeMillis()));
        this.trimGhosts();
    }

    private void onWorldRender(WorldRenderEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!this.enabledSetting.isEnabled()) {
            this.clear();
            return;
        }
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        if (client.world == null || client.player == null || cameraPos == null) {
            this.clear();
            return;
        }
        if (this.ghosts.isEmpty() && this.sparks.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        this.tickSparks(now);
        boolean drawGhosts = !this.ghosts.isEmpty();
        Matrix4f matrix = new Matrix4f().rotation((Quaternionfc)camera.getRotation());
        BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Iterator<CritGhost> iterator = this.ghosts.iterator();
        while (iterator.hasNext()) {
            CritGhost ghost = iterator.next();
            if (ghost.isExpired(now, this.lifetimeMs())) {
                iterator.remove();
                continue;
            }
            this.drawPulse(ghost, now, cameraPos, matrix, fill, lines);
            if (!drawGhosts) continue;
            this.drawSoul(ghost, now, cameraPos, matrix, fill, lines);
        }
        this.drawSparks(cameraPos, matrix, fill);
        RenderLayers.debugQuads().draw(fill.end());
        RenderLayers.lines().draw(lines.end());
    }

    private void drawPulse(CritGhost ghost, long now, Vec3d cameraPos, Matrix4f matrix, BufferBuilder fill, BufferBuilder lines) {
        if (ghost.blocks.isEmpty()) {
            return;
        }
        float progress = ghost.progress(now, this.lifetimeMs());
        float fade = 1.0f - progress;
        fade *= fade;
        if (fade <= 0.025f) {
            return;
        }
        float eased = this.easeOutCubic(progress);
        double radius = this.pulseRadius.getValue() * (double)eased;
        double band = 0.95f + (1.0f - eased) * 0.22f;
        int base = this.color.getColor();
        int highlight = ColorUtils.lerp(base, -1, 0.38f);
        for (BlockSample sample : ghost.blocks) {
            float ring = 1.0f - (float)MathHelper.clamp((double)(Math.abs(sample.distance - radius) / band), (double)0.0, (double)1.0);
            if (ring <= 0.04f) continue;
            float strength = ring * fade;
            if (strength <= 0.02f) continue;
            if (!sample.sparked && radius + band * 0.45 >= sample.distance && ring > 0.55f) {
                sample.sparked = true;
                if (ThreadLocalRandom.current().nextFloat() < 0.2f) {
                    this.spawnSparks(sample, ghost.origin, strength, highlight);
                }
            }
            int inner = ColorUtils.multiplyAlpha(base, Math.min(1.0f, strength * 0.38f * 0.72f));
            int mid = ColorUtils.multiplyAlpha(highlight, Math.min(1.0f, strength * 0.22f));
            int outer = ColorUtils.multiplyAlpha(highlight, Math.min(1.0f, strength * 0.11f));
            int far = ColorUtils.multiplyAlpha(highlight, Math.min(1.0f, strength * 0.06f));
            int edge = ColorUtils.multiplyAlpha(highlight, Math.min(1.0f, strength));
            double inflate = 0.018 + (double)ring * 0.04;
            for (Box local : sample.boxes) {
                Box world = local.offset((double)sample.pos.getX(), (double)sample.pos.getY(), (double)sample.pos.getZ());
                this.drawFilledBox(fill, matrix, cameraPos, world.expand(inflate * 2.8), far);
                this.drawFilledBox(fill, matrix, cameraPos, world.expand(inflate * 1.8), outer);
                this.drawFilledBox(fill, matrix, cameraPos, world.expand(inflate * 0.95), mid);
                this.drawFilledBox(fill, matrix, cameraPos, world, inner);
            }
            this.drawBoxOutline(lines, matrix, cameraPos, Box.of((Vec3d)Vec3d.ofCenter((Vec3i)sample.pos), (double)1.0, (double)1.0, (double)1.0), edge);
        }
    }

    private void drawSoul(CritGhost ghost, long now, Vec3d cameraPos, Matrix4f matrix, BufferBuilder fill, BufferBuilder lines) {
        float progress = ghost.progress(now, this.lifetimeMs());
        float eased = this.easeOutCubic(progress);
        float fade = 1.0f - progress;
        fade *= fade;
        if (fade <= 0.025f) {
            return;
        }
        double lift = this.rise.getValue() * (double)eased;
        Vec3d pos = ghost.origin.add(0.0, lift, 0.0);
        int base = this.color.getColor();
        int highlight = ColorUtils.lerp(base, -1, 0.26f);
        float scale = 1.0f + (1.0f - progress) * 0.05f + (float)Math.sin((double)progress * Math.PI) * 0.12f;
        int outlineAlpha = MathHelper.clamp((int)((int)(230.0f * fade)), (int)0, (int)255);
        int fillAlpha = MathHelper.clamp((int)((int)(155.0f * fade)), (int)0, (int)255);
        Box box = this.playerBox(ghost, pos, scale);
        if (this.filled.isEnabled() && fillAlpha > 3) {
            this.drawFilledBox(fill, matrix, cameraPos, box.expand(0.15), ColorUtils.withAlpha(highlight, (float)MathHelper.clamp((int)((int)((float)fillAlpha * 0.34f)), (int)0, (int)255)));
            this.drawFilledBox(fill, matrix, cameraPos, box.expand(0.07), ColorUtils.withAlpha(highlight, (float)MathHelper.clamp((int)((int)((float)fillAlpha * 0.66f)), (int)0, (int)255)));
            this.drawFilledBox(fill, matrix, cameraPos, box, ColorUtils.withAlpha(base, (float)fillAlpha));
        }
        if (outlineAlpha > 3) {
            this.drawBoxOutline(lines, matrix, cameraPos, box, ColorUtils.withAlpha(highlight, (float)outlineAlpha) & 0xFFFFFF | outlineAlpha << 24);
        }
    }

    private void spawnSparks(BlockSample sample, Vec3d origin, float strength, int color) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3d away = new Vec3d(sample.centerX() - origin.x, 0.0, sample.centerZ() - origin.z);
        if (away.lengthSquared() <= 1.0E-6) {
            away = new Vec3d(random.nextDouble(-1.0, 1.0), 0.0, random.nextDouble(-1.0, 1.0));
        }
        away = away.normalize();
        Vec3d side = new Vec3d(-away.z, 0.0, away.x);
        boolean burst = random.nextFloat() < 0.1f;
        int count = burst ? 4 : 2;
        for (int i = 0; i < count; ++i) {
            double along = random.nextDouble(-0.22, 0.22);
            double out = random.nextDouble(-0.12, 0.12);
            Vec3d pos = new Vec3d(sample.centerX() + side.x * along + away.x * out, sample.centerY() + random.nextDouble(0.02, 0.1), sample.centerZ() + side.z * along + away.z * out);
            Vec3d velocity = away.multiply(random.nextDouble(0.02, 0.1)).add(side.multiply(random.nextDouble(-0.05, 0.05))).add(0.0, random.nextDouble(0.04, 0.16), 0.0);
            int tint = ColorUtils.lerp(color, -1, random.nextFloat() * 0.45f);
            this.sparks.add(new Spark(pos, velocity, tint, 0.028f + random.nextFloat() * 0.035f, Math.min(1.0f, strength * (0.5f + random.nextFloat() * 0.28f)), 0.18f + random.nextFloat() * 0.22f));
        }
        int overflow = this.sparks.size() - 260;
        if (overflow > 0) {
            this.sparks.subList(0, overflow).clear();
        }
    }

    private void tickSparks(long now) {
        if (this.lastSparkTime == Long.MIN_VALUE) {
            this.lastSparkTime = now;
            return;
        }
        double delta = Math.min((double)(now - this.lastSparkTime) / 1000.0, 0.05);
        this.lastSparkTime = now;
        for (int i = this.sparks.size() - 1; i >= 0; --i) {
            Spark spark = this.sparks.get(i);
            spark.tick(delta);
            if (!spark.isDead()) continue;
            this.sparks.remove(i);
        }
    }

    private void drawSparks(Vec3d cameraPos, Matrix4f matrix, BufferBuilder fill) {
        if (this.sparks.isEmpty()) {
            return;
        }
        for (Spark spark : this.sparks) {
            if (spark.life <= 0.02f || spark.size <= 0.004f) continue;
            int tint = ColorUtils.multiplyAlpha(ColorUtils.lerp(spark.color, -1, 0.12f), Math.min(1.0f, spark.life));
            this.drawBillboard(fill, matrix, cameraPos, spark.pos, spark.size, tint);
        }
    }

    private List<BlockSample> collectBlocks(ClientWorld world, Vec3d origin) {
        ArrayList<BlockSample> samples = new ArrayList<BlockSample>();
        if (world == null) {
            return samples;
        }
        int extent = Math.max(2, MathHelper.floor((double)this.pulseRadius.getValue()) + 2);
        BlockPos center = BlockPos.ofFloored((Position)origin);
        double maxDistance = this.pulseRadius.getValue() + (double)0.95f + 1.0;
        for (int x = -extent; x <= extent; ++x) {
            for (int y = -extent; y <= extent; ++y) {
                for (int z = -extent; z <= extent; ++z) {
                    BlockPos pos = new BlockPos(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = world.getBlockState(pos);
                    VoxelShape shape = state.getOutlineShape((BlockView)world, pos);
                    if (shape.isEmpty() || !this.hasAirNeighbor(world, pos)) continue;
                    List boxes = shape.getBoundingBoxes();
                    double distance = this.sampleDistance(origin, pos, boxes);
                    if (distance > maxDistance) continue;
                    samples.add(new BlockSample(pos, boxes, distance));
                }
            }
        }
        samples.sort(Comparator.comparingDouble(sample -> sample.distance));
        if (samples.size() > 420) {
            return new ArrayList<BlockSample>(samples.subList(0, 420));
        }
        return samples;
    }

    private Vec3d groundPoint(ClientWorld world, PlayerEntity player) {
        Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
        if (world == null) {
            return pos;
        }
        int x = MathHelper.floor((double)pos.x);
        int z = MathHelper.floor((double)pos.z);
        int y = MathHelper.floor((double)(pos.y + 0.35));
        for (int i = 0; i <= 8; ++i) {
            BlockPos blockPos = new BlockPos(x, y - i, z);
            VoxelShape shape = world.getBlockState(blockPos).getOutlineShape((BlockView)world, blockPos);
            if (shape.isEmpty()) continue;
            double top = shape.getBoundingBoxes().stream().mapToDouble(box -> box.maxY).max().orElse(1.0);
            return new Vec3d(pos.x, (double)blockPos.getY() + top + 0.008, pos.z);
        }
        return pos;
    }

    private boolean hasAirNeighbor(ClientWorld world, BlockPos pos) {
        if (world == null) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.offset(direction);
            if (!world.getBlockState(neighbor).getOutlineShape((BlockView)world, neighbor).isEmpty()) continue;
            return true;
        }
        return false;
    }

    private boolean isCriticalHit(MinecraftClient client) {
        if (client.player == null) {
            return false;
        }
        return client.player.fallDistance > 0.0 && !client.player.isOnGround() && !client.player.isClimbing() && !client.player.isTouchingWater() && !client.player.isSubmergedInWater() && client.player.getPose() != EntityPose.SWIMMING && !client.player.isSprinting() && !client.player.hasStatusEffect(StatusEffects.BLINDNESS) && client.player.getAttackCooldownProgress(0.5f) > 0.9f;
    }

    private double sampleDistance(Vec3d origin, BlockPos pos, List<Box> boxes) {
        double y = (double)pos.getY() + boxes.stream().mapToDouble(box -> (box.minY + box.maxY) * 0.5).average().orElse(0.5);
        double dx = (double)pos.getX() + 0.5 - origin.x;
        double dy = y - origin.y;
        double dz = (double)pos.getZ() + 0.5 - origin.z;
        return Math.sqrt(dx * dx + dz * dz + dy * dy * 0.35);
    }

    private Box playerBox(CritGhost ghost, Vec3d pos, float scale) {
        double half = 0.3 * (double)scale;
        return new Box(pos.x - half, pos.y, pos.z - half, pos.x + half, pos.y + (double)(ghost.height * scale), pos.z + half);
    }

    private void drawBillboard(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, Vec3d pos, float size, int color) {
        float half = size * 0.5f;
        float x = (float)(pos.x - cameraPos.x);
        float y = (float)(pos.y - cameraPos.y);
        float z = (float)(pos.z - cameraPos.z);
        this.vertex(consumer, matrix, x - half, y - half, z, color);
        this.vertex(consumer, matrix, x + half, y - half, z, color);
        this.vertex(consumer, matrix, x + half, y + half, z, color);
        this.vertex(consumer, matrix, x - half, y + half, z, color);
    }

    private void drawFilledBox(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, Box box, int color) {
        this.quad(consumer, matrix, cameraPos, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
        this.quad(consumer, matrix, cameraPos, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ, color);
        this.quad(consumer, matrix, cameraPos, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ, color);
        this.quad(consumer, matrix, cameraPos, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
        this.quad(consumer, matrix, cameraPos, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, color);
        this.quad(consumer, matrix, cameraPos, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ, color);
    }

    private void drawBoxOutline(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, Box box, int color) {
        this.line(consumer, matrix, cameraPos, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, color);
        this.line(consumer, matrix, cameraPos, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, color);
        this.line(consumer, matrix, cameraPos, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
        this.line(consumer, matrix, cameraPos, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, color);
        this.line(consumer, matrix, cameraPos, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, color);
        this.line(consumer, matrix, cameraPos, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
        this.line(consumer, matrix, cameraPos, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
        this.line(consumer, matrix, cameraPos, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, color);
        this.line(consumer, matrix, cameraPos, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, color);
        this.line(consumer, matrix, cameraPos, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, color);
        this.line(consumer, matrix, cameraPos, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, color);
        this.line(consumer, matrix, cameraPos, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
    }

    private void quad(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, int color) {
        this.vertex(consumer, matrix, cameraPos, x1, y1, z1, color);
        this.vertex(consumer, matrix, cameraPos, x2, y2, z2, color);
        this.vertex(consumer, matrix, cameraPos, x3, y3, z3, color);
        this.vertex(consumer, matrix, cameraPos, x4, y4, z4, color);
    }

    private void line(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        this.vertex(consumer, matrix, cameraPos, x1, y1, z1, color);
        this.vertex(consumer, matrix, cameraPos, x2, y2, z2, color);
    }

    private void vertex(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, double x, double y, double z, int color) {
        int alpha = color >>> 24 & 0xFF;
        if (alpha == 0) {
            alpha = 255;
        }
        consumer.vertex((Matrix4fc)matrix, (float)(x - cameraPos.x), (float)(y - cameraPos.y), (float)(z - cameraPos.z)).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha);
    }

    private void vertex(BufferBuilder consumer, Matrix4f matrix, float x, float y, float z, int color) {
        int alpha = color >>> 24 & 0xFF;
        if (alpha == 0) {
            alpha = 255;
        }
        consumer.vertex((Matrix4fc)matrix, x, y, z).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha);
    }

    private float easeOutCubic(float value) {
        float clamped = MathHelper.clamp((float)value, (float)0.0f, (float)1.0f);
        float inverse = 1.0f - clamped;
        return 1.0f - inverse * inverse * inverse;
    }

    private void trimGhosts() {
        int overflow = this.ghosts.size() - 24;
        if (overflow > 0) {
            this.ghosts.subList(0, overflow).clear();
        }
    }

    private void clear() {
        this.ghosts.clear();
        this.sparks.clear();
        this.lastSparkTime = Long.MIN_VALUE;
    }

    private long lifetimeMs() {
        return Math.max(1L, this.lifeTime.getLongValue());
    }

    static final class CritGhost {
        final Vec3d origin;
        final float height;
        final List<BlockSample> blocks;
        final long spawnTime;

        CritGhost(PlayerEntity player, Vec3d origin, List<BlockSample> blocks, long spawnTime) {
            this.origin = origin;
            this.height = player.getHeight();
            this.blocks = blocks;
            this.spawnTime = spawnTime;
        }

        boolean isExpired(long now, long lifetime) {
            return now - this.spawnTime >= lifetime;
        }

        float progress(long now, long lifetime) {
            return MathHelper.clamp((float)((float)(now - this.spawnTime) / (float)lifetime), (float)0.0f, (float)1.0f);
        }
    }

    static final class BlockSample {
        final BlockPos pos;
        final List<Box> boxes;
        final double distance;
        boolean sparked;

        BlockSample(BlockPos pos, List<Box> boxes, double distance) {
            this.pos = pos;
            this.boxes = boxes;
            this.distance = distance;
        }

        double centerX() {
            return (double)this.pos.getX() + 0.5;
        }

        double centerY() {
            return (double)this.pos.getY() + 0.5;
        }

        double centerZ() {
            return (double)this.pos.getZ() + 0.5;
        }
    }

    static final class Spark {
        Vec3d pos;
        Vec3d velocity;
        final int color;
        float size;
        float life;
        final float fadeRate;

        Spark(Vec3d pos, Vec3d velocity, int color, float size, float life, float fadeRate) {
            this.pos = pos;
            this.velocity = velocity;
            this.color = color;
            this.size = size;
            this.life = life;
            this.fadeRate = fadeRate;
        }

        void tick(double delta) {
            this.pos = this.pos.add(this.velocity.multiply(delta * 20.0));
            this.velocity = this.velocity.multiply(0.92).add(0.0, -0.008 * delta * 20.0, 0.0);
            this.life -= this.fadeRate * (float)delta * 4.0f;
            this.size *= 0.97f;
        }

        boolean isDead() {
            return this.life <= 0.02f || this.size <= 0.004f;
        }
    }
}

