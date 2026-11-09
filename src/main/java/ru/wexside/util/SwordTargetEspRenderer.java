package ru.wexside.util;

import java.util.Arrays;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.animation.TimedPulse;
import ru.wexside.event.EntityAttackEvent;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.LazyMeshModel;
import ru.wexside.misc.MeshModel;
import ru.wexside.misc.PreviousValueTransition;
import ru.wexside.misc.SpatialTransform;
import ru.wexside.misc.TargetChange;
import ru.wexside.misc.TargetEspEffect;
import ru.wexside.misc.TargetTransition;
import ru.wexside.misc.TransitionPhase;
import ru.wexside.module.render.TargetEspRenderer;
import ru.wexside.render.model.BuiltInMesh;
import ru.wexside.util.AnimationMath;
import ru.wexside.util.ModelRenderOptions;
import ru.wexside.util.ModelRenderQueue;
import ru.wexside.util.WorldMeshBatchRenderer;

public final class SwordTargetEspRenderer
extends TargetEspRenderer
implements TargetEspEffect {
    private final Matrix4f[] matrix4f;
    private static final SpatialTransform IDENTITY_TRANSFORM = new SpatialTransform(0.0, 0.0, 0.0, 1.0f, 1.0f, 1.0f);
    static final String string = "sword";
    static final int slot = 7;
    static final float value6 = 0.5f;
    static final float value7 = 0.12f;
    private final Matrix4f matrix4f2;
    private final TimedPulse attackPulse;
    private final PreviousValueTransition<TargetGeometry> previousGeometry;
    static final int slot2 = -65476;
    static final float value8 = 1.12f;
    static final float value9 = 1.28f;
    static final long member8917 = 300L;
    static final long member7850 = 240L;
    private MeshModel orbitModel;
    static final double value10 = 0.2;
    static final float value11 = -90.0f;
    private final float[] value12;
    private final ModelRenderQueue modelRenderQueue;
    private static final WorldMeshBatchRenderer MODEL_RENDERER = new WorldMeshBatchRenderer("target-sword-orbit");
    private final double[] value13;
    private final LazyMeshModel lazyMeshModel = LazyMeshModel.create(BuiltInMesh.SWORD);
    static final long member12443 = 380L;
    private final ModelRenderOptions modelRenderOptions;
    private final TargetTransition<LivingEntity> targetTransition;
    static final float value14 = 0.78f;
    static final boolean flag = true;
    static final double value15 = 0.0021;
    private final double[] value16;
    static final double value17 = 1.4;
    static final long member8766 = 380L;
    private TargetGeometry lastGeometry;

    public SwordTargetEspRenderer() {
        this.modelRenderQueue = new ModelRenderQueue();
        this.modelRenderOptions = ModelRenderOptions.forOutlineEnabled(true).withDepthTestEnabled(false);
        this.targetTransition = new TargetTransition();
        this.attackPulse = new TimedPulse();
        this.previousGeometry = new PreviousValueTransition();
        this.value13 = new double[7];
        this.value16 = new double[7];
        this.value12 = new float[7];
        this.matrix4f = new Matrix4f[7];
        this.matrix4f2 = new Matrix4f().scaling(0.0f);
        for (int i = 0; i < 7; ++i) {
            this.matrix4f[i] = new Matrix4f();
        }
    }

    private int swordColor(int n, int n2, long l, int n3) {
        float f = 0.5f + 0.5f * AnimationMath.sin((float)l * 0.0042f + (float)n3 * 0.9f);
        return AnimationMath.lerpColor(n, n2, f * 0.48f);
    }

    private TargetGeometry captureGeometry(LivingEntity entity2, float f) {
        if (entity2 == null || !entity2.isAlive()) {
            return null;
        }
        Box box = entity2.getBoundingBox();
        return new TargetGeometry(this.interpolatedPosition((Entity)entity2, f), entity2.getHeight(), box.maxX - box.minX, box.maxZ - box.minZ);
    }

    @Override
    public void setEntityAttackEvent(EntityAttackEvent iIiiiilIiIEvent) {
        if (iIiiiilIiIEvent.getEntity() != null) {
            this.attackPulse.trigger();
        }
    }

    @Override
    public void setWorldRenderEvent(WorldRenderEvent floatTypeEvent2) {
        int n;
        MinecraftClient mc = this.client();
        if (!this.canRender(mc)) {
            return;
        }
        this.updateTargetGeometry(this.target(mc, LivingEntity.class), floatTypeEvent2.getFloatType());
        if (this.targetTransition.isEmpty()) {
            return;
        }
        float f = this.transitionOpacity();
        LivingEntity entity2 = this.targetTransition.current();
        if (f <= 0.0f || entity2 == null) {
            return;
        }
        TargetGeometry geometry = this.captureGeometry(entity2, floatTypeEvent2.getFloatType());
        if (geometry != null) {
            this.lastGeometry = geometry;
        } else {
            geometry = this.lastGeometry;
        }
        if (geometry == null) {
            this.resetTargetAnimation();
            return;
        }
        geometry = this.interpolatePreviousGeometry(geometry);
        this.loadOrbitModel();
        if (!this.lazyMeshModel.isLoaded() || this.orbitModel == null) {
            return;
        }
        long l = System.currentTimeMillis();
        float f2 = this.transitionScale();
        float f3 = 1.0f + AnimationMath.sin((float)l * 0.004f) * 0.025f;
        double d = Math.max(geometry.width(), geometry.depth()) * 0.5 + 0.2;
        double d2 = (double)l * 0.0021 % (Math.PI * 2);
        double d3 = geometry.position().y + geometry.height() / 1.4 - (double)0.06f;
        Vec3d vec = this.cameraPosition();
        int n2 = 0;
        double d4 = Double.MAX_VALUE;
        for (n = 0; n < 7; ++n) {
            double d5 = d2 + Math.PI * 2 * (double)n / 7.0;
            double d6 = Math.cos(d5);
            double d7 = Math.sin(d5);
            double d8 = d + (double)AnimationMath.sin((float)l * 0.003f + (float)n * 0.7f) * 0.025;
            double d9 = geometry.position().x + d6 * d8;
            double d10 = geometry.position().z + d7 * d8;
            this.value13[n] = d9;
            this.value16[n] = d10;
            this.value12[n] = (float)Math.toDegrees(-Math.atan2(d6, -d7));
            double d11 = vec.squaredDistanceTo(d9, d3, d10);
            if (!(d11 < d4)) continue;
            d4 = d11;
            n2 = n;
        }
        this.modelRenderQueue.clearBatches();
        n = this.attackPulse.isActive(l, 300L) ? 1 : 0;
        float f4 = n != 0 ? this.attackPulse.progress(l, 300L) : 0.0f;
        int n3 = this.primaryColor();
        float f5 = 0.12f;
        double d12 = 0.0;
        double d13 = 0.0;
        double d14 = 0.0;
        float f6 = 0.0f;
        float f7 = 0.0f;
        boolean bl = false;
        for (int i = 0; i < 7; ++i) {
            float f8 = this.swordAppearanceProgress(i);
            float f9 = f * f8;
            float f10 = 0.12f * f2 * f3;
            int n4 = this.swordColor(this.primaryColor(), this.secondaryColor(), l, i);
            float f11 = AnimationMath.sin((float)l * 0.005f + (float)i * 0.65f) * 0.032f;
            float f12 = AnimationMath.sin((float)l * 0.004f + (float)i) * 2.5f;
            if (n != 0 && i == n2) {
                float f13 = f4 < 0.5f ? f4 / 0.5f : (f4 - 0.5f) / 0.5f;
                float f14 = AnimationMath.easeInOutSine(f13);
                f10 = f4 < 0.5f ? AnimationMath.lerp(f10, 0.17663999f, f14) : AnimationMath.lerp(0.17663999f, f10, f14);
                n3 = n4 = f4 < 0.5f ? AnimationMath.lerpColor(this.primaryColor(), -65476, f14) : AnimationMath.lerpColor(-65476, this.primaryColor(), f14);
                f5 = f10;
                d12 = this.value13[i];
                d13 = d3 + (double)f11;
                d14 = this.value16[i];
                f6 = this.value12[i];
                f7 = f12;
                bl = true;
                this.matrix4f[i].set((Matrix4fc)this.matrix4f2);
            } else {
                this.composeSwordTransform(this.matrix4f[i], this.value13[i], d3 + (double)f11, this.value16[i], f10, -f10, f10, this.value12[i], -90.0f, f12);
            }
            n4 = AnimationMath.applyOpacity(n4, f9, 0.5f);
        }
        this.modelRenderQueue.processModelInstances(this.orbitModel, Arrays.asList(this.matrix4f), IDENTITY_TRANSFORM, AnimationMath.applyOpacity(this.primaryColor(), f, 0.5f), this.modelRenderOptions);
        if (n != 0 && bl) {
            this.modelRenderQueue.enqueueModel(this.lazyMeshModel.getMeshModel(), new SpatialTransform(d12, d13, d14, f5, -f5, f5, f6, -90.0f, f7), AnimationMath.applyOpacity(n3, f, 0.5f), this.modelRenderOptions);
        }
        if (!this.modelRenderQueue.isEmpty()) {
            MODEL_RENDERER.renderQueue(floatTypeEvent2.getMatrices(), mc.getBufferBuilders().getEntityVertexConsumers(), vec, this.modelRenderQueue);
        }
    }

    @Override
    public void update() {
        this.resetTargetAnimation();
        this.modelRenderQueue.clearBatches();
    }

    private void composeSwordTransform(Matrix4f matrix4f, double d, double d2, double d3, float f, float f2, float f3, float f4, float f5, float f6) {
        matrix4f.identity().translate((float)d, (float)d2, (float)d3).rotateY((float)Math.toRadians(f4)).rotateX((float)Math.toRadians(f5)).rotateZ((float)Math.toRadians(f6)).scale(f, f2, f3);
    }

    private float swordAppearanceProgress(int n) {
        if (this.targetTransition.phase() != TransitionPhase.APPEARING) {
            return 1.0f;
        }
        float f = (float)n * 0.035f;
        float f2 = this.targetTransition.progress(380L);
        float f3 = Math.max(0.0f, Math.min(1.0f, (f2 - f) / Math.max(0.001f, 1.0f - f)));
        return AnimationMath.easeInOutSine(f3);
    }

    private void updateTargetGeometry(LivingEntity entity2, float f) {
        TargetChange<LivingEntity> targetChange = this.targetTransition.updateTarget(entity2);
        if (!targetChange.changed()) {
            return;
        }
        TargetGeometry oldGeometry = this.lastGeometry;
        TargetGeometry newGeometry = this.captureGeometry(this.targetTransition.current(), f);
        if (oldGeometry != null && newGeometry != null) {
            this.previousGeometry.remember(oldGeometry);
        }
    }

    private float transitionOpacity() {
        if (this.targetTransition.phase() == TransitionPhase.IDLE) {
            return 1.0f;
        }
        if (this.targetTransition.phase() == TransitionPhase.APPEARING) {
            float f = this.targetTransition.progress(380L);
            if (f >= 1.0f) {
                this.targetTransition.finishAppearance();
            }
            return AnimationMath.easeInOutSine(f);
        }
        float f = this.targetTransition.progress(240L);
        if (f >= 1.0f) {
            this.resetTargetAnimation();
            return 0.0f;
        }
        return 1.0f - AnimationMath.smoothStep(f);
    }

    private TargetGeometry interpolatePreviousGeometry(TargetGeometry geometry) {
        if (!this.previousGeometry.hasValue()) {
            return geometry;
        }
        float f = this.previousGeometry.progress(380L);
        if (f >= 1.0f) {
            this.previousGeometry.clear();
            return geometry;
        }
        float f2 = AnimationMath.easeOut(f, 1.12f);
        TargetGeometry previous = this.previousGeometry.get();
        Vec3d start = previous.position();
        Vec3d end = geometry.position();
        return new TargetGeometry(new Vec3d((double)AnimationMath.lerp((float)start.x, (float)end.x, f2), (double)AnimationMath.lerp((float)start.y, (float)end.y, f2), (double)AnimationMath.lerp((float)start.z, (float)end.z, f2)), AnimationMath.lerp((float)previous.height(), (float)geometry.height(), f2), AnimationMath.lerp((float)previous.width(), (float)geometry.width(), f2), AnimationMath.lerp((float)previous.depth(), (float)geometry.depth(), f2));
    }

    private void resetTargetAnimation() {
        this.targetTransition.reset();
        this.attackPulse.reset();
        this.previousGeometry.reset();
        this.lastGeometry = null;
    }

    private void loadOrbitModel() {
        if (!this.lazyMeshModel.isLoaded() || this.lazyMeshModel.getMeshModel() == null || this.lazyMeshModel.getMeshModel().getList().isEmpty()) {
            return;
        }
        if (this.orbitModel != null) {
            return;
        }
        this.orbitModel = this.lazyMeshModel.getMeshModel();
    }

    private float transitionScale() {
        if (this.targetTransition.phase() == TransitionPhase.IDLE) {
            return 1.0f;
        }
        if (this.targetTransition.phase() == TransitionPhase.APPEARING) {
            return AnimationMath.easeOut(this.targetTransition.progress(380L), 1.28f);
        }
        return AnimationMath.lerp(1.0f, 0.78f, AnimationMath.smoothStep(this.targetTransition.progress(240L)));
    }

    private record TargetGeometry(Vec3d position, double height, double width, double depth) {
    }
}

