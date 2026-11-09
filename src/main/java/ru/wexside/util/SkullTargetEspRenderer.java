package ru.wexside.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ru.wexside.animation.TimedPulse;
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

public final class SkullTargetEspRenderer
extends TargetEspRenderer
implements TargetEspEffect {
    private final LazyMeshModel lazyMeshModel = LazyMeshModel.create(BuiltInMesh.SKULL);
    static final long member12318 = 240L;
    static final float value = 0.16f;
    private static final WorldMeshBatchRenderer MODEL_RENDERER = new WorldMeshBatchRenderer("target-skull");
    private final TimedPulse attackPulse;
    static final long member12527 = 320L;
    static final long member6565 = 200L;
    static final float value2 = 0.23f;
    static final int slot = -65536;
    static final float value4 = 0.34f;
    static final float value5 = 0.14f;
    static final boolean flag = true;
    static final float value6 = 1.3f;
    private final ModelRenderQueue modelRenderQueue = new ModelRenderQueue();
    static final float value7 = 0.65f;
    private final TargetTransition<LivingEntity> targetTransition2;
    private final ModelRenderOptions modelRenderOptions = ModelRenderOptions.forOutlineEnabled(true).withDepthTestEnabled(false);
    static final float value8 = 0.5f;

    public SkullTargetEspRenderer() {
        this.targetTransition2 = new TargetTransition();
        this.attackPulse = new TimedPulse();
    }

    @Override
    public void setEntityAttackEvent(EntityAttackEvent iIiiiilIiIEvent) {
        if (iIiiiilIiIEvent.getEntity() instanceof LivingEntity) {
            this.attackPulse.trigger();
        }
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
        float f = this.getTransitionOpacity();
        LivingEntity entity2 = this.targetTransition2.current();
        if (f <= 0.0f || entity2 == null) {
            return;
        }
        if (!this.lazyMeshModel.isLoaded() || this.lazyMeshModel.getMeshModel() == null || this.lazyMeshModel.getMeshModel().getList().isEmpty()) {
            return;
        }
        this.modelRenderQueue.clearBatches();
        this.enqueueSkull(entity2, floatTypeEvent2.getFloatType(), this.getModelScale(), f);
        if (!this.modelRenderQueue.isEmpty()) {
            MODEL_RENDERER.renderQueue(floatTypeEvent2.getMatrices(), mc.getBufferBuilders().getEntityVertexConsumers(), this.cameraPosition(), this.modelRenderQueue);
        }
    }

    @Override
    public void update() {
        this.resetTargetAnimation();
        this.modelRenderQueue.clearBatches();
    }

    private void resetTargetAnimation() {
        this.targetTransition2.reset();
        this.attackPulse.reset();
    }

    private int pulsatingColor(int n, int n2, long l) {
        float f = 0.5f + 0.5f * AnimationMath.sin((float)l * 0.004f);
        return AnimationMath.lerpColor(n, n2, f * 0.36f);
    }

    private float getModelScale() {
        if (this.targetTransition2.phase() == TransitionPhase.IDLE) {
            return 0.23f;
        }
        float f = this.targetTransition2.progress(320L);
        if (this.targetTransition2.phase() == TransitionPhase.APPEARING) {
            return AnimationMath.lerp(0.34f, 0.23f, AnimationMath.easeOut(f, 1.3f));
        }
        return AnimationMath.lerp(0.23f, 0.34f, AnimationMath.smoothStep(f));
    }

    private float getTransitionOpacity() {
        if (this.targetTransition2.phase() == TransitionPhase.IDLE) {
            return 1.0f;
        }
        float f = this.targetTransition2.progress(320L);
        if (this.targetTransition2.phase() == TransitionPhase.APPEARING) {
            if (f >= 1.0f) {
                this.targetTransition2.finishAppearance();
            }
            return AnimationMath.easeInOutSine(f);
        }
        if (f >= 1.0f) {
            this.resetTargetAnimation();
            return 0.0f;
        }
        return 1.0f - AnimationMath.smoothStep(f);
    }

    private float attackVerticalOffset(long l) {
        long l2 = this.attackPulse.elapsed(l);
        if (l2 < 240L) {
            float f = (float)l2 / 240.0f;
            float f2 = (float)Math.sin((double)f * Math.PI) * (1.0f - f);
            return -f2 * 0.16f;
        }
        return 0.0f;
    }

    private void enqueueSkull(LivingEntity entity2, float f, float f2, float f3) {
        long l = System.currentTimeMillis();
        Vec3d vec = this.interpolatedPosition((Entity)entity2, f);
        Vec3d vec2 = this.cameraPosition();
        double d = vec.x;
        double d2 = vec.y + (double)entity2.getHeight() + 0.25;
        double d3 = vec.z;
        float f4 = AnimationMath.sin((float)l * 0.0045f) * 0.045f;
        float f5 = this.attackVerticalOffset(l);
        Vec3d vec3 = this.attackShakeOffset(l);
        Vec3d vec4 = new Vec3d(d + vec3.x, d2 + (double)f4 + (double)f5, d3 + vec3.z);
        double d4 = vec2.x - vec4.x;
        double d5 = vec2.y - vec4.y;
        double d6 = vec2.z - vec4.z;
        float f6 = (float)Math.toDegrees(Math.atan2(d4, d6));
        float f7 = (float)Math.toDegrees(-Math.atan2(d5, Math.sqrt(d4 * d4 + d6 * d6)));
        float f8 = 1.0f + AnimationMath.sin((float)l * 0.004f) * 0.035f;
        float f9 = f2 * f8;
        int n = this.pulsatingColor(this.primaryColor(), this.secondaryColor(), l);
        if (this.attackPulse.isActive(l, 200L)) {
            float f10 = this.attackPulse.progress(l, 200L);
            n = f10 < 0.5f ? AnimationMath.lerpColor(this.primaryColor(), -65536, f10 / 0.5f) : AnimationMath.lerpColor(-65536, this.primaryColor(), (f10 - 0.5f) / 0.5f);
        }
        this.modelRenderQueue.enqueueModel(this.lazyMeshModel.getMeshModel(), new SpatialTransform(vec4.x, vec4.y, vec4.z, f9, f9, f9, f6, f7 + 90.0f, 0.0f), AnimationMath.applyOpacity(n, f3, 0.5f), this.modelRenderOptions);
    }

    private Vec3d attackShakeOffset(long l) {
        long l2 = this.attackPulse.elapsed(l);
        if (l2 < 200L) {
            float f = (float)l2 / 200.0f;
            float f2 = (float)Math.sin((double)f * Math.PI * 8.0) * (1.0f - f) * 0.14f;
            return new Vec3d((double)f2, 0.0, (double)(f2 * 0.65f));
        }
        return Vec3d.ZERO;
    }
}

