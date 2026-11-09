package ru.wexside.module.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.lwjgl.opengl.GL46;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.SpriteAtlasRegion;
import ru.wexside.misc.WexsideHitParticles;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.ColorSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.ColorUtils;

public final class TrailsModule
extends Module
implements ConfigSerializable {
    private static final int MAX_LINEAR_POINTS = 400;
    private static final double MIN_SEGMENT = 4.0E-4;
    private static final double MAX_SEGMENT = 1024.0;
    private static final double PARTICLE_STEP = 0.01;
    private final BooleanSetting enabledSetting;
    private final ColorSetting color;
    private final BooleanSetting hideFirstPerson;
    private final ModeSetting renderType;
    private final ModeSetting particleType;
    private final BooleanSetting glow;
    private final NumberSetting lifeTime;
    private final NumberSetting count;
    private final NumberSetting size;
    private final NumberSetting spawnScale;
    private final List<LinearTrailPoint> linearTrail = new ArrayList<LinearTrailPoint>();
    private final List<TrailParticle> particles = new ArrayList<TrailParticle>();
    private Vec3d lastLinearPos;
    private double particleX;
    private double particleY;
    private double particleZ;
    private boolean particleStarted;
    private long lastNanoTime;

    public TrailsModule(EventBus eventBus) {
        super(eventBus, "trails", "Trails", "След за игроком", ModuleCategory.valueOf("RENDER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Включить след за игроком").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        ColorSetting colorSetting = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("Color").id("color").description("Цвет следа").aliases("color", "цвет")).build();
        colorSetting.setPrimaryColor(0, -11753627);
        colorSetting.setPrimaryColor(1, -1543135);
        colorSetting.setPrimaryColor(2, -9279489);
        colorSetting.setPrimaryColor(3, -46001);
        colorSetting.setPrimaryColor(4, -13218);
        colorSetting.setPrimaryColor(5, -10582785);
        colorSetting.setPrimaryColor(6, -2732032);
        this.color = colorSetting;
        this.registerSetting(colorSetting);
        this.hideFirstPerson = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Hide in first person").id("hide_first_person").description("Не рисовать след от первого лица")).build();
        this.registerSetting(this.hideFirstPerson);
        this.renderType = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Linear", "Particles").defaultOption("Particles").name("Render type").id("render_type").description("Тип отрисовки следа")).build();
        this.registerSetting(this.renderType);
        this.particleType = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Cross", "Dollar", "Star", "Bloom", "Snowflake", "Line", "Light").defaultOption("Cross").name("Particle type").id("particle_type").description("Текстура частицы").visibleWhen(this::particleMode)).build();
        this.registerSetting(this.particleType);
        this.glow = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Glowing").id("glow").description("Свечение текстуры частицы").visibleWhen(this::particleMode)).build();
        this.registerSetting(this.glow);
        this.lifeTime = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 5.0).defaultValue(1.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Life time").id("life_time").description("Время жизни частицы").visibleWhen(this::particleMode)).build();
        this.registerSetting(this.lifeTime);
        this.count = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 5.0).defaultValue(5.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Count").id("count").description("Количество частиц за шаг").aliases("count", "количество").visibleWhen(this::particleMode)).build();
        this.registerSetting(this.count);
        this.size = ((NumberSettingBuilder)NumberSetting.builder().range(10.0, 100.0).defaultValue(50.0).multiplier(0.005).precision(0).animationSpeed(20.0f).markers(10.0).snapTo(10.0).name("Size").id("size").description("Размер частицы").aliases("size", "размер").visibleWhen(this::particleMode)).build();
        this.registerSetting(this.size);
        this.spawnScale = ((NumberSettingBuilder)NumberSetting.builder().range(10.0, 100.0).defaultValue(50.0).multiplier(0.005).precision(0).animationSpeed(20.0f).markers(10.0).snapTo(10.0).name("Spawn scale").id("spawn_scale").description("Начальный масштаб частицы").visibleWhen(this::particleMode)).build();
        this.registerSetting(this.spawnScale);
    }

    @Override
    protected void initialize() {
        this.listen(WorldRenderEvent.class, this::onWorldRender);
        this.listen(WorldSessionEvent.class, event -> this.clear());
    }

    private void onWorldRender(WorldRenderEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            this.clear();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (client.world == null || player == null) {
            this.clear();
            return;
        }
        Vec3d pos = this.lerpedPos(player, event.getFloatType());
        if (this.particleMode()) {
            this.updateParticles(pos, player.getHeight() * 0.45f);
        } else {
            this.updateLinearTrail(pos);
        }
    }

    private void updateLinearTrail(Vec3d pos) {
        if (this.lastLinearPos == null) {
            this.lastLinearPos = pos;
        }
        this.ageLinear(this.frameDelta());
        double distance = pos.squaredDistanceTo(this.lastLinearPos);
        if (distance >= 1024.0) {
            this.lastLinearPos = pos;
        } else if (distance > 4.0E-4) {
            this.linearTrail.add(new LinearTrailPoint(this.lastLinearPos, pos));
            this.trimLinear();
            this.lastLinearPos = pos;
        }
        this.renderLinear();
    }

    private void updateParticles(Vec3d pos, float height) {
        this.lastLinearPos = null;
        if (!this.particleStarted) {
            this.particleX = pos.x;
            this.particleY = pos.y;
            this.particleZ = pos.z;
            this.particleStarted = true;
        }
        this.expireParticles();
        this.walkParticles(pos, height);
        this.renderParticles();
    }

    private void walkParticles(Vec3d target, float height) {
        double dx = target.x - this.particleX;
        double dy = target.y - this.particleY;
        double dz = target.z - this.particleZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq >= 1024.0) {
            this.particleX = target.x;
            this.particleY = target.y;
            this.particleZ = target.z;
            return;
        }
        while (distanceSq >= 1.0E-4) {
            double distance = Math.sqrt(distanceSq);
            double step = 0.1 / distance;
            this.particleX += dx * step;
            this.particleY += dy * step;
            this.particleZ += dz * step;
            this.spawnParticles(this.particleX, this.particleY, this.particleZ, height);
            dx = target.x - this.particleX;
            dy = target.y - this.particleY;
            dz = target.z - this.particleZ;
            distanceSq = dx * dx + dy * dy + dz * dz;
        }
    }

    private void spawnParticles(double x, double y, double z, float height) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float size = height * (float)this.size.getValue();
        float scale = (float)this.spawnScale.getValue();
        for (int i = 0; i < this.count.getIntValue(); ++i) {
            this.particles.add(new TrailParticle(x, y + 0.4, z, size, scale, 0.1, random));
        }
    }

    private void expireParticles() {
        long now = System.currentTimeMillis();
        long lifetime = (long)(this.lifeTime.getValue() * 1000.0);
        boolean paused = MinecraftClient.getInstance().isPaused();
        this.particles.removeIf(particle -> {
            if (particle.isExpired(now, lifetime)) {
                return true;
            }
            particle.tick(paused);
            return false;
        });
    }

    private void renderParticles() {
        if (this.particles.isEmpty() || this.hideInFirstPerson()) {
            return;
        }
        long now = System.currentTimeMillis();
        long lifetime = (long)(this.lifeTime.getValue() * 1000.0);
        boolean glow = this.glow.isEnabled();
        WexsideHitParticles texture = WexsideHitParticles.fromLabel(this.particleType.getSelectedOption());
        SpriteAtlasRegion sprite = texture.getSprite(glow);
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        Matrix4f matrix = new Matrix4f().rotation((Quaternionfc)camera.getRotation());
        BufferBuilder consumer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float invLifetime = 1.0f / (float)lifetime;
        float count = Math.max(1.0f, (float)this.particles.size() - 1.0f);
        for (int i = 0; i < this.particles.size(); ++i) {
            TrailParticle particle = this.particles.get(i);
            long age = now - particle.spawnTime;
            if (age > lifetime) continue;
            float progress = (float)age * invLifetime;
            float fadeIn = TrailsModule.clamp01(progress / 0.18f);
            float scale = this.overshoot(fadeIn, 1.28f);
            float rise = this.easeOutCubic(progress);
            float tail = progress <= 0.56f ? 1.0f : 1.0f - this.cubic(TrailsModule.clamp01((progress - 0.56f) / 0.44f));
            float alpha = this.amount11(0.8f, 1.0f, scale * 0.22f) * tail;
            float pulse = 0.9f + 0.1f * MathHelper.sin((double)(particle.phase + (float)age * 0.008333334f * 1.6f));
            float size = particle.baseSize * this.amount11(particle.spawnScale, 1.0f, scale) * (1.0f - progress * 0.12f) * pulse * particle.speed;
            double x = particle.x + particle.driftX * (double)rise * 0.09 + particle.liftX * (double)this.easeOutCubic(TrailsModule.clamp01((progress - 0.68f) / 0.32f)) * 0.18;
            double y = particle.y + 0.82 + particle.liftY * (double)rise + particle.driftY * (double)rise * 0.09 * 0.45;
            double z = particle.z + particle.driftZ * (double)rise * 0.09 + particle.liftZ * (double)this.easeOutCubic(TrailsModule.clamp01((progress - 0.68f) / 0.32f)) * 0.05;
            float rotation = particle.rotation * (1.0f - progress * 0.78f) + MathHelper.sin((double)(particle.phase + progress * 6.0f)) * 5.5f;
            int tint = ColorUtils.multiplyAlpha(this.sampleColor((float)i / count), alpha);
            this.drawBillboard(consumer, matrix, cameraPos, x, y, z, size * 0.6666667f, rotation + sprite.minU(), tint);
        }
        RenderLayers.debugQuads().draw(consumer.end());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderLinear() {
        if (this.linearTrail.isEmpty() || this.hideInFirstPerson()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        float height = player.getHeight() - 0.05f - (player.isUsingItem() ? 0.1f : 0.0f);
        float step = 1.0f / Math.max((float)this.linearTrail.size() * 2.0f, 1.0f);
        boolean cull = GL46.glIsEnabled((int)2884);
        GL46.glDisable((int)2884);
        try {
            Camera camera = client.gameRenderer.getCamera();
            Vec3d cameraPos = camera.getCameraPos();
            Matrix4f matrix = new Matrix4f().rotation((Quaternionfc)camera.getRotation());
            BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < this.linearTrail.size(); ++i) {
                this.drawLinearSegment(this.linearTrail.get(i), (float)i * step, height, cameraPos, matrix, fill, lines);
            }
            RenderLayers.debugQuads().draw(fill.end());
            RenderLayers.lines().draw(lines.end());
        }
        finally {
            if (cull) {
                GL46.glEnable((int)2884);
            }
        }
    }

    private void drawLinearSegment(LinearTrailPoint point, float gradient, float height, Vec3d cameraPos, Matrix4f matrix, BufferBuilder fill, BufferBuilder lines) {
        float width = TrailsModule.clamp01((float)(point.age * 0.02));
        float strength = TrailsModule.clamp01((1.0f - width) * 1.8f);
        if (strength <= 0.01f) {
            return;
        }
        int color = ColorUtils.multiplyAlpha(this.sampleColor(gradient), strength);
        int nextColor = ColorUtils.multiplyAlpha(this.sampleColor(Math.min(1.0f, gradient + 0.08f)), strength);
        Vec3d from = point.from.add(0.0, 0.15, 0.0);
        Vec3d to = point.to.add(0.0, 0.15, 0.0);
        Vec3d topTo = point.to.add(0.0, (double)height, 0.0);
        Vec3d topFrom = point.from.add(0.0, (double)height, 0.0);
        this.quad(fill, matrix, cameraPos, from, to, topTo, topFrom, color, nextColor);
        this.quad(fill, matrix, cameraPos, topFrom, topTo, to, from, color, nextColor);
        this.line(lines, matrix, cameraPos, from, to, color, nextColor);
    }

    private void ageLinear(double delta) {
        Iterator<LinearTrailPoint> iterator = this.linearTrail.iterator();
        while (iterator.hasNext()) {
            LinearTrailPoint point = iterator.next();
            point.age += delta * 20.0;
            if (!(point.age >= 50.0)) continue;
            iterator.remove();
        }
    }

    private void trimLinear() {
        int overflow = this.linearTrail.size() - 400;
        if (overflow > 0) {
            this.linearTrail.subList(0, overflow).clear();
        }
    }

    private void drawBillboard(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, double x, double y, double z, float size, float rotation, int color) {
        float half = size * 0.5f;
        float radians = (float)Math.toRadians(rotation);
        float cos = MathHelper.cos((double)radians);
        float sin = MathHelper.sin((double)radians);
        float px = (float)(x - cameraPos.x);
        float py = (float)(y - cameraPos.y);
        float pz = (float)(z - cameraPos.z);
        this.vertex(consumer, matrix, px + (-half * cos - -half * sin), py + (-half * sin + -half * cos), pz, color);
        this.vertex(consumer, matrix, px + (half * cos - -half * sin), py + (half * sin + -half * cos), pz, color);
        this.vertex(consumer, matrix, px + (half * cos - half * sin), py + (half * sin + half * cos), pz, color);
        this.vertex(consumer, matrix, px + (-half * cos - half * sin), py + (-half * sin + half * cos), pz, color);
    }

    private void quad(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, Vec3d a, Vec3d b, Vec3d c, Vec3d d, int colorA, int colorB) {
        this.vertex(consumer, matrix, cameraPos, a, colorA);
        this.vertex(consumer, matrix, cameraPos, b, colorB);
        this.vertex(consumer, matrix, cameraPos, c, colorB);
        this.vertex(consumer, matrix, cameraPos, d, colorA);
    }

    private void line(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, Vec3d a, Vec3d b, int colorA, int colorB) {
        this.vertex(consumer, matrix, cameraPos, a, colorA);
        this.vertex(consumer, matrix, cameraPos, b, colorB);
    }

    private void vertex(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, Vec3d pos, int color) {
        consumer.vertex((Matrix4fc)matrix, (float)(pos.x - cameraPos.x), (float)(pos.y - cameraPos.y), (float)(pos.z - cameraPos.z)).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24 & 0xFF);
    }

    private void vertex(BufferBuilder consumer, Matrix4f matrix, float x, float y, float z, int color) {
        consumer.vertex((Matrix4fc)matrix, x, y, z).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24 & 0xFF);
    }

    private int sampleColor(float t) {
        t = TrailsModule.clamp01(t);
        if (this.color.isAstolfoMode()) {
            return this.color.getEditingColor(t);
        }
        int from = this.color.getPrimaryColor();
        int to = this.color.isDoubleColorMode() ? this.color.getSecondaryColor() : from;
        return ColorUtils.lerp(from, to, t);
    }

    private Vec3d lerpedPos(ClientPlayerEntity player, float tickDelta) {
        return new Vec3d(MathHelper.lerp((double)tickDelta, (double)player.lastRenderX, (double)player.getX()), MathHelper.lerp((double)tickDelta, (double)player.lastRenderY, (double)player.getY()), MathHelper.lerp((double)tickDelta, (double)player.lastRenderZ, (double)player.getZ()));
    }

    private boolean hideInFirstPerson() {
        MinecraftClient client = MinecraftClient.getInstance();
        return this.hideFirstPerson.isEnabled() && client.options != null && client.options.getPerspective().isFirstPerson();
    }

    private boolean particleMode() {
        return "Particles".equals(this.renderType.getSelectedOption());
    }

    private void clear() {
        this.linearTrail.clear();
        this.particles.clear();
        this.lastLinearPos = null;
        this.particleStarted = false;
        this.lastNanoTime = 0L;
    }

    private double frameDelta() {
        long now = System.nanoTime();
        long previous = this.lastNanoTime;
        this.lastNanoTime = now;
        if (previous == 0L) {
            return 0.016666666666666666;
        }
        double delta = (double)(now - previous) * 1.0E-9;
        return !Double.isFinite(delta) || delta < 0.0 ? 0.016666666666666666 : Math.min(delta, 0.05);
    }

    private static float clamp01(float value) {
        return MathHelper.clamp((float)value, (float)0.0f, (float)1.0f);
    }

    private float easeOutCubic(float value) {
        float inverse = 1.0f - TrailsModule.clamp01(value);
        return 1.0f - inverse * inverse * inverse;
    }

    private float overshoot(float value, float amount) {
        float t = TrailsModule.clamp01(value) - 1.0f;
        float s = amount + 0.8f;
        return 1.0f + t * t * ((s + 1.0f) * t + s);
    }

    private float cubic(float value) {
        float t = TrailsModule.clamp01(value);
        return t * t * t;
    }

    private float amount11(float from, float to, float t) {
        return from + (to - from) * t;
    }

    static final class LinearTrailPoint {
        final Vec3d from;
        final Vec3d to;
        double age;

        LinearTrailPoint(Vec3d from, Vec3d to) {
            this.from = from;
            this.to = to;
        }
    }

    static final class TrailParticle {
        final double x;
        final double y;
        final double z;
        final float baseSize;
        final float spawnScale;
        final float speed;
        final float phase;
        float rotation;
        final double driftX;
        final double driftY;
        final double driftZ;
        final double liftX;
        final double liftY;
        final double liftZ;
        final long spawnTime;

        TrailParticle(double x, double y, double z, float baseSize, float spawnScale, double speed, ThreadLocalRandom random) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.baseSize = baseSize;
            this.spawnScale = spawnScale;
            this.speed = (float)speed;
            this.phase = random.nextFloat() * ((float)Math.PI * 2);
            this.rotation = random.nextFloat() * 360.0f;
            this.driftX = random.nextDouble(-0.02, 0.02);
            this.driftY = random.nextDouble(0.0, 0.02);
            this.driftZ = random.nextDouble(-0.02, 0.02);
            this.liftX = random.nextDouble(-0.01, 0.01);
            this.liftY = random.nextDouble(0.0, 0.04);
            this.liftZ = random.nextDouble(-0.01, 0.01);
            this.spawnTime = System.currentTimeMillis();
        }

        boolean isExpired(long now, long lifetime) {
            return now - this.spawnTime > lifetime;
        }

        void tick(boolean paused) {
            if (!paused) {
                this.rotation += 0.5f;
            }
        }
    }
}

