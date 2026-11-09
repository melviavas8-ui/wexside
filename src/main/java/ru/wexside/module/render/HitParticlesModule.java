package ru.wexside.module.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EntityAttackEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldRenderEvent;
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

public final class HitParticlesModule
extends Module
implements ConfigSerializable {
    private static final double GRAVITY = 0.015;
    private static final double DRAG = 0.96;
    private static volatile HitParticlesModule instance;
    private final BooleanSetting enabledSetting;
    private final BooleanSetting removeDefault;
    private final ColorSetting color;
    private final NumberSetting count;
    private final NumberSetting maxCount;
    private final NumberSetting strength;
    private final NumberSetting size;
    private final NumberSetting duration;
    private final BooleanSetting glowing;
    private final ModeSetting particleTexture;
    private final ModeSetting animationType;
    private final List<Particle> particles = new ArrayList<Particle>();
    private long lastNanoTime;
    private double tickAccumulator;

    public HitParticlesModule(EventBus eventBus) {
        super(eventBus, "hit_particles", "Hit Particles", "Эффект частиц при ударе по сущности", ModuleCategory.valueOf("RENDER"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Включить эффект частиц при ударе по сущности").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.removeDefault = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Remove Default").id("remove_default").description("Убирает ванильные частицы удара")).build();
        this.registerSetting(this.removeDefault);
        ColorSetting colorSetting = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("Color").id("color").description("Цвет частиц").aliases("color", "цвет")).build();
        colorSetting.setPrimaryColor(0, -11753627);
        colorSetting.setPrimaryColor(1, -1543135);
        colorSetting.setPrimaryColor(2, -9279489);
        colorSetting.setPrimaryColor(3, -46001);
        colorSetting.setPrimaryColor(4, -13218);
        colorSetting.setPrimaryColor(5, -10582785);
        colorSetting.setPrimaryColor(6, -2732032);
        this.color = colorSetting;
        this.registerSetting(colorSetting);
        this.count = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 10.0).defaultValue(5.0).multiplier(2.5).precision(0).animationSpeed(20.0f).name("Spawn count").id("count").description("Количество частиц, появляющихся при ударе")).build();
        this.registerSetting(this.count);
        this.maxCount = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 16.0).defaultValue(4.0).multiplier(32.0).precision(0).animationSpeed(20.0f).name("Max count").id("max_count").description("Максимальное число живых частиц одновременно")).build();
        this.registerSetting(this.maxCount);
        this.strength = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 16.0).defaultValue(4.0).multiplier(0.05).precision(0).animationSpeed(20.0f).name("Move strength").id("strength").description("Сила начального разлёта частиц")).build();
        this.registerSetting(this.strength);
        this.size = ((NumberSettingBuilder)NumberSetting.builder().range(10.0, 100.0).defaultValue(50.0).multiplier(0.005).precision(0).animationSpeed(20.0f).markers(10.0).snapTo(10.0).name("Size").id("size").description("Размер частицы").aliases("size", "размер")).build();
        this.registerSetting(this.size);
        this.duration = ((NumberSettingBuilder)NumberSetting.builder().range(10.0, 50.0).defaultValue(30.0).multiplier(1.0).precision(0).animationSpeed(20.0f).snapTo(10.0).name("Life time").id("duration").description("Время жизни частицы")).build();
        this.registerSetting(this.duration);
        this.glowing = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Glowing").id("glowing").description("Свечение текстуры частицы")).build();
        this.registerSetting(this.glowing);
        this.particleTexture = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Cross", "Dollar", "Star", "Bloom", "Snowflake", "Line", "Light").defaultOption("Cross").name("Texture").id("particle_texture").description("Текстура частицы")).build();
        this.registerSetting(this.particleTexture);
        this.animationType = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Default", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9").defaultOption("Default").name("Type animation").id("animation_type").description("Анимация")).build();
        this.registerSetting(this.animationType);
    }

    @Override
    protected void initialize() {
        this.listen(EntityAttackEvent.class, this::onEntityAttack);
        this.listen(WorldRenderEvent.class, this::onWorldRender);
    }

    public static boolean isEnabled() {
        HitParticlesModule module = instance;
        return module != null && module.enabledSetting.isEnabled() && module.removeDefault.isEnabled();
    }

    private void onEntityAttack(EntityAttackEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity)target;
        this.spawnParticles(new Vec3d(living.getX(), living.getY() + (double)living.getHeight() * 0.5, living.getZ()));
    }

    private void onWorldRender(WorldRenderEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            this.clearParticles();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            this.resetClock();
            return;
        }
        this.stepPhysics();
        this.trimOverflow();
        this.renderParticles((float)this.tickAccumulator);
    }

    private void spawnParticles(Vec3d origin) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int room = Math.max(0, this.maxCount.getIntValue() - this.particles.size());
        int spawn = Math.min(room, this.count.getIntValue() + random.nextInt(4));
        for (int i = 0; i < spawn; ++i) {
            float particleSize = random.nextFloat() * 0.15f + this.size.getFloatValue() * 0.7f;
            double spread = this.strength.getValue();
            Vec3d velocity = new Vec3d(random.nextDouble(-spread, spread), random.nextDouble(-spread, spread + 0.1), random.nextDouble(-spread, spread));
            float rotation = (float)((double)Math.round(random.nextDouble(0.0, 360.0) / 15.0) * 15.0);
            this.particles.add(new Particle(origin, velocity, this.sampleColor(random.nextFloat()), particleSize, rotation, this.animationIndex()));
        }
    }

    private void stepPhysics() {
        double delta = this.frameDelta();
        this.tickAccumulator += delta * 20.0;
        int steps = (int)this.tickAccumulator;
        if (steps <= 0) {
            return;
        }
        this.tickAccumulator -= (double)steps;
        if (steps > 10) {
            steps = 10;
            this.tickAccumulator = 0.0;
        }
        int lifetime = this.duration.getIntValue();
        for (int i = 0; i < steps; ++i) {
            for (int p = this.particles.size() - 1; p >= 0; --p) {
                Particle particle = this.particles.get(p);
                if (particle.isDead(lifetime)) {
                    this.particles.remove(p);
                    continue;
                }
                particle.tick(1.0);
            }
        }
    }

    private void renderParticles(float tickDelta) {
        boolean glow = this.glowing.isEnabled();
        WexsideHitParticles texture = WexsideHitParticles.fromLabel(this.particleTexture.getSelectedOption());
        SpriteAtlasRegion sprite = texture.getSprite(glow);
        int lifetime = this.duration.getIntValue();
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        Matrix4f matrix = new Matrix4f().rotation((Quaternionfc)camera.getRotation());
        BufferBuilder consumer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Particle particle : this.particles) {
            float life = particle.life(lifetime);
            if (life <= 0.02f) continue;
            Vec3d previous = particle.previous;
            Vec3d current = particle.current;
            double x = MathHelper.lerp((double)tickDelta, (double)previous.x, (double)current.x);
            double y = MathHelper.lerp((double)tickDelta, (double)previous.y, (double)current.y);
            double z = MathHelper.lerp((double)tickDelta, (double)previous.z, (double)current.z);
            int tint = HitParticlesModule.withAlpha(particle.color, MathHelper.clamp((int)((int)(life * 255.0f)), (int)0, (int)255));
            this.drawBillboard(consumer, matrix, cameraPos, x, y, z, particle.size, particle.rotation + sprite.minU(), tint);
        }
        BuiltBuffer buffer = consumer.endNullable();
        if (buffer != null) {
            RenderLayers.debugQuads().draw(buffer);
        }
    }

    private void drawBillboard(BufferBuilder consumer, Matrix4f matrix, Vec3d cameraPos, double x, double y, double z, float size, float rotation, int color) {
        float half = size * 0.5f;
        float radians = (float)Math.toRadians(rotation);
        float cos = MathHelper.cos((double)radians);
        float sin = MathHelper.sin((double)radians);
        float x1 = -half * cos - -half * sin;
        float y1 = -half * sin + -half * cos;
        float x2 = half * cos - -half * sin;
        float y2 = half * sin + -half * cos;
        float x3 = half * cos - half * sin;
        float y3 = half * sin + half * cos;
        float x4 = -half * cos - half * sin;
        float y4 = -half * sin + half * cos;
        float px = (float)(x - cameraPos.x);
        float py = (float)(y - cameraPos.y);
        float pz = (float)(z - cameraPos.z);
        this.vertex(consumer, matrix, px + x1, py + y1, pz, color);
        this.vertex(consumer, matrix, px + x2, py + y2, pz, color);
        this.vertex(consumer, matrix, px + x3, py + y3, pz, color);
        this.vertex(consumer, matrix, px + x4, py + y4, pz, color);
    }

    private void vertex(BufferBuilder consumer, Matrix4f matrix, float x, float y, float z, int color) {
        consumer.vertex((Matrix4fc)matrix, x, y, z).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24 & 0xFF);
    }

    private void trimOverflow() {
        int overflow = this.particles.size() - this.maxCount.getIntValue();
        if (overflow > 0) {
            this.particles.subList(0, overflow).clear();
        }
    }

    private void clearParticles() {
        this.particles.clear();
        this.resetClock();
    }

    private void resetClock() {
        this.tickAccumulator = 0.0;
        this.lastNanoTime = 0L;
    }

    private double frameDelta() {
        long now = System.nanoTime();
        long previous = this.lastNanoTime;
        this.lastNanoTime = now;
        if (previous == 0L) {
            return 0.05;
        }
        double delta = (double)(now - previous) / 1.0E9;
        if (!Double.isFinite(delta) || delta < 0.0) {
            return 0.05;
        }
        return Math.min(delta, 0.25);
    }

    private int animationIndex() {
        String value = this.animationType.getSelectedOption();
        if (value == null) {
            return 0;
        }
        return switch (value) {
            case "0" -> 1;
            case "1" -> 2;
            case "2" -> 3;
            case "3" -> 4;
            case "4" -> 5;
            case "5" -> 6;
            case "6" -> 7;
            case "7" -> 8;
            case "8" -> 9;
            case "9" -> 10;
            default -> 0;
        };
    }

    private int sampleColor(float t) {
        float clamped = MathHelper.clamp((float)t, (float)0.0f, (float)1.0f);
        if (this.color.isAstolfoMode()) {
            return this.color.getEditingColor(clamped);
        }
        int from = this.color.getPrimaryColor();
        int to = this.color.isDoubleColorMode() ? this.color.getSecondaryColor() : from;
        return HitParticlesModule.lerpColor(from, to, clamped);
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a1 = from >>> 24 & 0xFF;
        int r1 = from >> 16 & 0xFF;
        int g1 = from >> 8 & 0xFF;
        int b1 = from & 0xFF;
        int a2 = to >>> 24 & 0xFF;
        int r2 = to >> 16 & 0xFF;
        int g2 = to >> 8 & 0xFF;
        int b2 = to & 0xFF;
        int a = (int)((float)a1 + (float)(a2 - a1) * t);
        int r = (int)((float)r1 + (float)(r2 - r1) * t);
        int g = (int)((float)g1 + (float)(g2 - g1) * t);
        int b = (int)((float)b1 + (float)(b2 - b1) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int withAlpha(int color, int alpha) {
        return color & 0xFFFFFF | alpha << 24;
    }

    static final class Particle {
        Vec3d previous;
        Vec3d current;
        Vec3d velocity;
        final int color;
        float size;
        float rotation;
        final int animation;
        int age;

        Particle(Vec3d origin, Vec3d velocity, int color, float size, float rotation, int animation) {
            this.previous = origin;
            this.current = origin;
            this.velocity = velocity;
            this.color = color;
            this.size = size;
            this.rotation = rotation;
            this.animation = animation;
            this.age = 0;
        }

        boolean isDead(int lifetime) {
            return this.age >= lifetime;
        }

        float life(int lifetime) {
            if (lifetime <= 0) {
                return 0.0f;
            }
            return MathHelper.clamp((float)(1.0f - (float)this.age / (float)lifetime), (float)0.0f, (float)1.0f);
        }

        void tick(double delta) {
            this.previous = this.current;
            this.current = this.current.add(this.velocity.multiply(delta));
            this.velocity = this.velocity.multiply(0.96).add(0.0, -0.015 * delta, 0.0);
            ++this.age;
            if (this.animation > 0) {
                this.rotation += (float)this.animation * 4.5f;
                this.size *= 0.985f;
            }
        }
    }
}

