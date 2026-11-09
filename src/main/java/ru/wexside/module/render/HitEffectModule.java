package ru.wexside.module.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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

public final class HitEffectModule
extends Module
implements ConfigSerializable {
    private static final int GHOST_LAYERS = 3;
    private static final float LAYER_GAP = 0.08f;
    private static final double LAYER_SPACING = 0.12;
    private static final double RISE = 0.18;
    private static final int MAX_GHOSTS = 24;
    private static final Vec3d FALLBACK_LOOK = new Vec3d(0.0, 0.0, 1.0);
    private final BooleanSetting enabledSetting;
    private final ColorSetting color;
    private final NumberSetting lifeTime;
    private final NumberSetting distance;
    private final BooleanSetting filled;
    private final BooleanSetting ignoreDepth;
    private final List<Ghost> ghosts = new ArrayList<Ghost>();

    public HitEffectModule(EventBus eventBus) {
        super(eventBus, "hit_effect", "Hit Effect", "Призрачный след модели игрока при ударе", ModuleCategory.valueOf("RENDER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Включить призрачный след при ударе по игроку").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        ColorSetting colorSetting = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("Color").id("color").description("Цвет призрака").aliases("color", "цвет")).build();
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
        this.distance = ((NumberSettingBuilder)NumberSetting.builder().range(0.5, 3.0).defaultValue(2.0).multiplier(1.0).precision(1).animationSpeed(20.0f).showMarkers().snapTo(0.5).name("Distance").id("distance").description("Дальность вылета следа")).build();
        this.registerSetting(this.distance);
        this.filled = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Filled").id("filled").description("Заливать модель призрака")).build();
        this.registerSetting(this.filled);
        this.ignoreDepth = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Ignore depth").id("ignore_depth").description("Рисовать сквозь стены")).build();
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
        this.ghosts.add(new Ghost(player, this.lookDirection(client), System.currentTimeMillis()));
        this.trimOverflow();
    }

    private void onWorldRender(WorldRenderEvent event) {
        BuiltBuffer outlineBuffer;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!this.enabledSetting.isEnabled()) {
            this.ghosts.clear();
            return;
        }
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        if (client.world == null || client.player == null || cameraPos == null) {
            this.ghosts.clear();
            return;
        }
        if (this.ghosts.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Ghost> iterator = this.ghosts.iterator();
        Matrix4f matrix = new Matrix4f().rotation((Quaternionfc)camera.getRotation());
        BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        BufferBuilder outline = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        while (iterator.hasNext()) {
            Ghost ghost = iterator.next();
            if (ghost.isExpired(now, this.lifetimeMs())) {
                iterator.remove();
                continue;
            }
            this.drawGhost(ghost, now, cameraPos, matrix, fill, outline);
        }
        BuiltBuffer fillBuffer = fill.endNullable();
        if (fillBuffer != null) {
            RenderLayers.debugQuads().draw(fillBuffer);
        }
        if ((outlineBuffer = outline.endNullable()) != null) {
            RenderLayers.lines().draw(outlineBuffer);
        }
    }

    private void drawGhost(Ghost ghost, long now, Vec3d cameraPos, Matrix4f matrix, BufferBuilder fill, BufferBuilder outline) {
        float progress = ghost.progress(now, this.lifetimeMs());
        for (int layer = 2; layer >= 0; --layer) {
            float delayed = Math.max(0.0f, progress - (float)layer * 0.08f);
            float fade = 1.0f - progress;
            fade *= fade;
            fade *= 1.0f - (float)layer * 0.2f;
            if (fade <= 0.025f) continue;
            float eased = this.easeOutCubic(delayed);
            double travel = this.distance.getValue() * (double)eased - (double)layer * 0.12;
            double rise = 0.18 * (double)this.sineIn(delayed);
            double wobble = Math.sin((double)(delayed + (float)layer * 0.18f) * Math.PI * 2.0) * 0.025;
            Vec3d pos = ghost.origin.add(ghost.look.multiply(travel)).add(0.0, rise + wobble, 0.0);
            float scale = 1.0f + 0.03f * (1.0f - delayed) + (float)layer * 0.012f;
            int outlineAlpha = MathHelper.clamp((int)((int)(210.0f * fade)), (int)0, (int)255);
            int fillAlpha = MathHelper.clamp((int)((int)(110.0f * fade)), (int)0, (int)255);
            int base = this.color.getColor();
            Box box = this.playerBox(ghost, pos, scale);
            if (this.filled.isEnabled() && fillAlpha > 3) {
                this.drawFilledBox(fill, matrix, cameraPos, box, HitEffectModule.withAlpha(base, fillAlpha));
            }
            if (outlineAlpha <= 3) continue;
            this.drawBoxOutline(outline, matrix, cameraPos, box, HitEffectModule.withAlpha(base, outlineAlpha));
        }
    }

    private Box playerBox(Ghost ghost, Vec3d pos, float scale) {
        double half = 0.3 * (double)scale;
        double height = ghost.height * scale;
        return new Box(pos.x - half, pos.y, pos.z - half, pos.x + half, pos.y + height, pos.z + half);
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

    private void trimOverflow() {
        int overflow = this.ghosts.size() - 24;
        if (overflow > 0) {
            this.ghosts.subList(0, overflow).clear();
        }
    }

    private Vec3d lookDirection(MinecraftClient client) {
        Vec3d look = client.player != null ? client.player.getRotationVec(1.0f) : Vec3d.ZERO;
        Vec3d vec3d = look;
        if (look.lengthSquared() <= 1.0E-6) {
            return FALLBACK_LOOK.normalize();
        }
        return look.normalize();
    }

    private float sineIn(float value) {
        return (float)Math.sin((double)MathHelper.clamp((float)value, (float)0.0f, (float)1.0f) * Math.PI * 0.5);
    }

    private float easeOutCubic(float value) {
        float clamped = MathHelper.clamp((float)value, (float)0.0f, (float)1.0f);
        float inverse = 1.0f - clamped;
        return 1.0f - inverse * inverse * inverse;
    }

    private long lifetimeMs() {
        return Math.max(1L, this.lifeTime.getLongValue());
    }

    private static int withAlpha(int color, int alpha) {
        return color & 0xFFFFFF | alpha << 24;
    }

    static final class Ghost {
        final Vec3d origin;
        final Vec3d look;
        final float height;
        final long spawnTime;

        Ghost(PlayerEntity player, Vec3d look, long spawnTime) {
            this.origin = new Vec3d(player.getX(), player.getY(), player.getZ());
            this.look = look;
            this.height = player.getHeight();
            this.spawnTime = spawnTime;
        }

        boolean isExpired(long now, long lifetime) {
            return now - this.spawnTime >= lifetime;
        }

        float progress(long now, long lifetime) {
            return MathHelper.clamp((float)((float)(now - this.spawnTime) / (float)lifetime), (float)0.0f, (float)1.0f);
        }
    }
}

