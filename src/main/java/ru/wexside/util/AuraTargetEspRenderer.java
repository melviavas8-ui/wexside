package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.wexside.event.EntityAttackEvent;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.LazyMeshModel;
import ru.wexside.misc.SpatialTransform;
import ru.wexside.misc.TargetEspEffect;
import ru.wexside.misc.TargetTransition;
import ru.wexside.misc.TransitionPhase;
import ru.wexside.module.render.TargetEspRenderer;
import ru.wexside.render.model.BuiltInMesh;
import ru.wexside.util.AnimationMath;
import ru.wexside.util.ModelRenderOptions;
import ru.wexside.util.ModelRenderQueue;
import ru.wexside.util.WorldMeshBatchRenderer;

public final class AuraTargetEspRenderer
extends TargetEspRenderer
implements TargetEspEffect {
    private final LazyMeshModel lazyMeshModel = LazyMeshModel.create(BuiltInMesh.CUBE);
    static final WorldMeshBatchRenderer worldMeshBatchRenderer = new WorldMeshBatchRenderer("target_aura_particles");
    static final long member12527 = 250L;
    static final float value = 0.45f;
    static final double[] value2 = new double[]{-0.8, -0.4, 0.0, 0.4, 0.8};
    private final Random random;
    private final List<CubeParticle> particles;
    static final int slot = 144;
    private final TargetTransition<LivingEntity> targetTransition2;
    private final ModelRenderQueue modelRenderQueue = new ModelRenderQueue();

    public AuraTargetEspRenderer() {
        this.particles = new ArrayList<CubeParticle>(144);
        this.random = new Random();
        this.targetTransition2 = new TargetTransition();
    }

    @Override
    public void setWorldRenderEvent(WorldRenderEvent floatTypeEvent2) {
        MinecraftClient mc = this.client();
        if (!this.canRender(mc)) {
            return;
        }
        this.targetTransition2.updateTarget(this.target(mc, LivingEntity.class));
        if (this.targetTransition2.isEmpty()) {
            return;
        }
        float f = this.getFloatType();
        LivingEntity entity2 = this.targetTransition2.current();
        if (f <= 0.0f || entity2 == null) {
            return;
        }
        if (!this.lazyMeshModel.isLoaded() || this.lazyMeshModel.getMeshModel() == null || this.lazyMeshModel.getMeshModel().getList().isEmpty()) {
            return;
        }
        long l = System.currentTimeMillis();
        this.modelRenderQueue.clearBatches();
        Vec3d vec = this.interpolatedPosition((Entity)entity2, floatTypeEvent2.getFloatType());
        double d = vec.x;
        double d2 = vec.y + 0.03;
        double d3 = vec.z;
        this.particles.removeIf(particle -> !particle.isAlive(l));
        while (this.particles.size() < 144) {
            this.particles.add(new CubeParticle(l, this.random));
        }
        float f2 = (float)(this.primaryColor() >>> 24 & 0xFF) / 255.0f;
        Vec3d vec2 = this.cameraPosition();
        double d4 = Math.atan2(vec2.x - d, vec2.z - d3) + Math.PI;
        double d5 = Math.sin(d4);
        double d6 = Math.cos(d4);
        double d7 = -d6;
        ModelRenderOptions modelRenderOptions = ModelRenderOptions.forOutlineEnabled(true).withDepthTestEnabled(true);
        for (CubeParticle cubeParticle : this.particles) {
            float f32;
            float f42;
            float f5 = cubeParticle.progress(l);
            float f6 = f5 * f5 * (3.0f - 2.0f * f5);
            double d8 = (double)(l - cubeParticle.member2664) / 1000.0;
            double d9 = cubeParticle.member9266 + cubeParticle.member4618 * d8;
            double d10 = cubeParticle.member11751 + cubeParticle.member7116 * (1.0 + 0.6 * (1.0 - (double)f5));
            double d11 = Math.cos(d9) * d10;
            double d12 = Math.sin(d9) * d10;
            double d13 = Math.sin(cubeParticle.member1916 + cubeParticle.member12196 * d8) * cubeParticle.member6745;
            double d14 = cubeParticle.member2418 + (cubeParticle.update13 ? 0.1 + d13 * 0.12 : 0.3 * (double)f6 + d13 * 0.28);
            float f7 = 0.0f;
            if (cubeParticle.member3001) {
                f42 = MathHelper.clamp((float)(f5 * (float)cubeParticle.member6904), (float)0.0f, (float)1.0f);
                double d15 = 1.0 - 0.3 * (double)Math.abs(cubeParticle.member2135);
                double d16 = 0.2 + 1.4 * (double)f42 * d15;
                double d17 = cubeParticle.member2135 == -2 || cubeParticle.member2135 == 2 ? 0.15 : 0.45;
                double d18 = value2[cubeParticle.member2135 + 2];
                d11 = d5 * d17 + d7 * d18;
                d12 = d6 * d17 + d5 * d18;
                d14 = d16;
                f7 = Math.max(0.0f, (f42 - 0.7f) / 0.3f);
                if (f7 > 0.0f) {
                    d11 += d5 * 0.35 * (double)f7 + d7 * cubeParticle.member9524 * 2.5 * (double)f7;
                    d12 += d6 * 0.35 * (double)f7 + d5 * cubeParticle.member9524 * 2.5 * (double)f7;
                    d14 += 0.55 * (double)f7;
                }
            }
            if ((f32 = (1.0f - (f42 = MathHelper.clamp((float)((f5 - 0.82f) * 5.55f), (float)0.0f, (float)1.0f))) * f2 * f) <= 0.0f) continue;
            float f8 = cubeParticle.member8099 - (cubeParticle.member8099 - cubeParticle.createBuffer8) * f6;
            if (cubeParticle.member3001) {
                f8 *= 1.0f - f7;
            }
            if ((f8 = Math.min(f8, 0.22f)) <= 0.0f) continue;
            this.modelRenderQueue.enqueueModel(this.lazyMeshModel.getMeshModel(), new SpatialTransform(d + d11, d2 + d14, d3 + d12, f8, f8, f8, (float)Math.toDegrees((double)cubeParticle.member6483 * d8), (float)Math.toDegrees((double)cubeParticle.member9632 * d8), (float)Math.toDegrees((double)cubeParticle.member10824 * d8)), AnimationMath.applyOpacity(this.primaryColor(), f32, 0.45f), modelRenderOptions);
        }
        if (!this.modelRenderQueue.isEmpty()) {
            worldMeshBatchRenderer.renderQueue(floatTypeEvent2.getMatrices(), mc.getBufferBuilders().getEntityVertexConsumers(), vec2, this.modelRenderQueue);
        }
    }

    @Override
    public void setEntityAttackEvent(EntityAttackEvent event) {
    }

    @Override
    public void update() {
        this.targetTransition2.reset();
        this.particles.clear();
        this.modelRenderQueue.clearBatches();
    }

    @Override
    public boolean isActive() {
        return false;
    }

    private float getFloatType() {
        if (this.targetTransition2.phase() == TransitionPhase.IDLE) {
            return 1.0f;
        }
        float f = this.targetTransition2.progress(250L);
        if (this.targetTransition2.phase() == TransitionPhase.APPEARING) {
            if (f >= 1.0f) {
                this.targetTransition2.finishAppearance();
            }
            return f;
        }
        if (f >= 1.0f) {
            this.targetTransition2.reset();
            return 0.0f;
        }
        return 1.0f - f;
    }

    private static final class CubeParticle {
        private final long member2664;
        private final long lifetime;
        private final double member9266;
        private final double member4618;
        private final double member11751;
        private final double member7116;
        private final double member1916;
        private final double member12196;
        private final double member6745;
        private final double member2418;
        private final boolean update13;
        private final boolean member3001;
        private final int member2135;
        private final double member6904;
        private final double member9524;
        private final float member8099;
        private final float createBuffer8;
        private final float member6483;
        private final float member9632;
        private final float member10824;

        private CubeParticle(long createdAt, Random random) {
            this.member2664 = createdAt;
            this.lifetime = 900L + (long)random.nextInt(1100);
            this.member9266 = random.nextDouble() * Math.PI * 2.0;
            this.member4618 = random.nextDouble(0.7, 2.1) * (random.nextBoolean() ? 1.0 : -1.0);
            this.member11751 = random.nextDouble(0.25, 0.85);
            this.member7116 = random.nextDouble(-0.18, 0.18);
            this.member1916 = random.nextDouble() * Math.PI * 2.0;
            this.member12196 = random.nextDouble(1.0, 3.0);
            this.member6745 = random.nextDouble(0.08, 0.25);
            this.member2418 = random.nextDouble(0.05, 1.65);
            this.update13 = random.nextBoolean();
            this.member3001 = random.nextDouble() < 0.22;
            this.member2135 = random.nextInt(5) - 2;
            this.member6904 = random.nextDouble(0.8, 1.4);
            this.member9524 = random.nextDouble(-0.25, 0.25);
            this.member8099 = random.nextFloat(0.1f, 0.22f);
            this.createBuffer8 = random.nextFloat(0.02f, 0.08f);
            this.member6483 = random.nextFloat(-2.0f, 2.0f);
            this.member9632 = random.nextFloat(-2.0f, 2.0f);
            this.member10824 = random.nextFloat(-2.0f, 2.0f);
        }

        private boolean isAlive(long now) {
            return now - this.member2664 < this.lifetime;
        }

        private float progress(long now) {
            return MathHelper.clamp((float)((float)(now - this.member2664) / (float)this.lifetime), (float)0.0f, (float)1.0f);
        }
    }
}

