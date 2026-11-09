package ru.wexside.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.wexside.event.TotemPopEvent;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.SpriteAtlasRegion;
import ru.wexside.misc.TotemEffectRenderer;
import ru.wexside.misc.WexsideHitParticles;
import ru.wexside.render.ParticleBillboardRenderer;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.TotemEffectSettings;

public final class Snowflake
implements TotemEffectRenderer {
    private static final double FIXED_STEP = 0.016666666666666666;
    private final List<TotemParticle> particles = new ArrayList<TotemParticle>();
    private final TotemEffectSettings settings;
    private long previousFrameNanos;
    private double pendingTicks;

    public Snowflake(TotemEffectSettings settings) {
        this.settings = settings;
    }

    private void renderParticles(float tickDelta) {
        for (TotemParticle particle : this.particles) {
            if (particle.alpha <= 0.02f) continue;
            double x = MathHelper.lerp((double)tickDelta, (double)particle.previousPosition.x, (double)particle.position.x);
            double y = MathHelper.lerp((double)tickDelta, (double)particle.previousPosition.y, (double)particle.position.y);
            double z = MathHelper.lerp((double)tickDelta, (double)particle.previousPosition.z, (double)particle.position.z);
            SpriteAtlasRegion sprite = particle.style.texture().getSpriteAtlasRegion();
            int color = ColorUtils.withAlpha(particle.color, (float)MathHelper.clamp((int)((int)((float)(particle.color >>> 24 & 0xFF) * particle.alpha)), (int)0, (int)255));
            ParticleBillboardRenderer.draw(x, y, z, particle.size, particle.size, color, WexsideHitParticles.getParticleTexture(), false, particle.initialRotation + particle.rotationSpeed * (float)particle.age, sprite.minU(), sprite.minV(), sprite.maxU(), sprite.maxV());
        }
    }

    private ParticleStyle selectedStyle() {
        return switch (this.settings.getString()) {
            case "Mini-star" -> new ParticleStyle(WexsideHitParticles.STAR, 0.7f);
            case "Snowflake" -> new ParticleStyle(WexsideHitParticles.SNOWFLAKE, 1.0f);
            case "Dollar" -> new ParticleStyle(WexsideHitParticles.DOLLAR, 1.0f);
            case "Cross" -> new ParticleStyle(WexsideHitParticles.CROSS, 1.0f);
            default -> new ParticleStyle(WexsideHitParticles.STAR, 1.0f);
        };
    }

    private Vec3d randomVelocity(ThreadLocalRandom random) {
        double speed = this.settings.getDoubleType();
        if (this.settings.isActive()) {
            return new Vec3d(random.nextDouble(-speed, speed), random.nextDouble(-speed, speed + 0.2), random.nextDouble(-speed, speed));
        }
        double azimuth = random.nextDouble() * Math.PI * 2.0;
        double polar = random.nextDouble() * Math.PI;
        return new Vec3d(speed * Math.cos(azimuth) * Math.sin(polar), speed * Math.cos(polar) + 0.05, speed * Math.sin(azimuth) * Math.sin(polar));
    }

    private double frameSeconds() {
        long now = System.nanoTime();
        if (this.previousFrameNanos == 0L) {
            this.previousFrameNanos = now;
            return 0.016666666666666666;
        }
        double seconds = (double)(now - this.previousFrameNanos) / 1.0E9;
        this.previousFrameNanos = now;
        return Math.clamp(seconds, 0.0, 0.05);
    }

    private void simulate(MinecraftClient client) {
        Iterator<TotemParticle> iterator = this.particles.iterator();
        while (iterator.hasNext()) {
            BlockHitResult hit;
            TotemParticle particle = iterator.next();
            BlockPos blockPos = BlockPos.ofFloored((Position)particle.position);
            if (!client.world.isPosLoaded(blockPos) || particle.position.y < 0.0 || particle.alpha <= 0.0f) {
                iterator.remove();
                continue;
            }
            particle.previousPosition = particle.position;
            ++particle.age;
            Vec3d velocity = this.settings.isActive() ? particle.velocity.multiply(0.99).subtract(0.0, 0.03, 0.0) : particle.velocity.multiply(0.96).add(0.0, 0.003, 0.0);
            Vec3d end = particle.position.add(velocity);
            BlockHitResult collision = null;
            if (this.settings.isActive() && (hit = client.world.raycast(new RaycastContext(particle.position, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)client.player))).getType() == HitResult.Type.BLOCK) {
                Direction.Axis axis = hit.getSide().getAxis();
                velocity = new Vec3d(axis == Direction.Axis.X ? -velocity.x : velocity.x, axis == Direction.Axis.Y ? -velocity.y : velocity.y, axis == Direction.Axis.Z ? -velocity.z : velocity.z);
                collision = hit;
            }
            particle.position = collision != null ? collision.getPos() : particle.position.add(velocity);
            particle.velocity = velocity;
            particle.size *= 0.97f;
            if (particle.age < this.settings.getIntType2()) continue;
            particle.alpha = Math.max(0.0f, particle.alpha - 0.03f);
        }
    }

    @Override
    public void renderWorld(WorldRenderEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            this.clearHolograms();
            return;
        }
        if (this.particles.isEmpty()) {
            this.previousFrameNanos = System.nanoTime();
            return;
        }
        this.pendingTicks += this.frameSeconds() * 20.0;
        int updates = Math.min(10, (int)this.pendingTicks);
        if (updates > 0) {
            this.pendingTicks -= (double)updates;
            for (int i = 0; i < updates; ++i) {
                this.simulate(client);
            }
        }
        this.renderParticles(event.getFloatType());
    }

    @Override
    public void setTotemPopEvent(TotemPopEvent event) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ParticleStyle style = this.selectedStyle();
        int count = this.settings.getIntType() + random.nextInt(4);
        int color = this.settings.getIntType3();
        for (int i = 0; i < count; ++i) {
            this.particles.add(new TotemParticle(event.toVec3d(), this.randomVelocity(random), color, (0.2f + random.nextFloat() * 0.15f) * style.scale(), random.nextFloat() * 360.0f, random.nextFloat() * 5.0f + 2.5f, style));
        }
    }

    @Override
    public void clearHolograms() {
        this.particles.clear();
        this.previousFrameNanos = 0L;
        this.pendingTicks = 0.0;
    }

    private static final class TotemParticle {
        private final ParticleStyle style;
        private final int color;
        private Vec3d position;
        private Vec3d previousPosition;
        private Vec3d velocity;
        private float size;
        private float alpha = 1.0f;
        private final float initialRotation;
        private final float rotationSpeed;
        private int age;

        private TotemParticle(Vec3d position, Vec3d velocity, int color, float size, float initialRotation, float rotationSpeed, ParticleStyle style) {
            this.position = position;
            this.previousPosition = position;
            this.velocity = velocity;
            this.color = color;
            this.size = size;
            this.initialRotation = initialRotation;
            this.rotationSpeed = rotationSpeed;
            this.style = style;
        }
    }

    private record ParticleStyle(WexsideHitParticles texture, float scale) {
    }
}

