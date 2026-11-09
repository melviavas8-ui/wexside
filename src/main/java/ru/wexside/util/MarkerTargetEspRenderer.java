package ru.wexside.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ru.wexside.animation.TimedPulse;
import ru.wexside.event.EntityAttackEvent;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.LazyMeshModel;
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

public final class MarkerTargetEspRenderer
extends TargetEspRenderer
implements TargetEspEffect {
    private final LazyMeshModel lazyMeshModel = LazyMeshModel.create(BuiltInMesh.MARKER);
    static final long member5632 = 420L;
    private final PreviousValueTransition<TargetMarkerState> previousState;
    static final float value4 = 0.4f;
    private static final WorldMeshBatchRenderer MODEL_RENDERER = new WorldMeshBatchRenderer("target-marker");
    static final float value5 = 1.5f;
    static final float value6 = 0.42f;
    static final float value7 = 0.25f;
    private final ModelRenderOptions modelRenderOptions;
    private final TargetTransition<Entity> targetTransition;
    private TargetMarkerState lastState;
    static final long member8917 = 320L;
    static final long member12527 = 300L;
    static final int slot = -65476;
    static final float value8 = 1.28f;
    private final ModelRenderQueue modelRenderQueue = new ModelRenderQueue();
    static final float value9 = 0.5f;
    private final TimedPulse attackPulse;
    static final float value10 = 1.16f;

    public MarkerTargetEspRenderer() {
        this.modelRenderOptions = ModelRenderOptions.forOutlineEnabled(true).withDepthTestEnabled(false);
        this.targetTransition = new TargetTransition();
        this.attackPulse = new TimedPulse();
        this.previousState = new PreviousValueTransition();
    }

    @Override
    public void setEntityAttackEvent(EntityAttackEvent iIiiiilIiIEvent) {
        if (iIiiiilIiIEvent.getEntity() != null) {
            this.attackPulse.trigger();
        }
    }

    @Override
    public void setWorldRenderEvent(WorldRenderEvent floatTypeEvent2) {
        MinecraftClient mc = this.client();
        if (!this.canRender(mc)) {
            return;
        }
        if (!this.lazyMeshModel.isLoaded() || this.lazyMeshModel.getMeshModel() == null || this.lazyMeshModel.getMeshModel().getList().isEmpty()) {
            return;
        }
        this.updateTargetState(this.target(mc, Entity.class), floatTypeEvent2.getFloatType());
        if (this.targetTransition.isEmpty()) {
            return;
        }
        float f = this.getTransitionOpacity();
        Entity target = this.targetTransition.current();
        if (f <= 0.0f || target == null) {
            return;
        }
        TargetMarkerState state = this.captureState(target, floatTypeEvent2.getFloatType());
        if (state != null) {
            this.lastState = state;
        } else {
            state = this.lastState;
        }
        if (state == null) {
            this.resetTargetAnimation();
            return;
        }
        this.modelRenderQueue.clearBatches();
        this.renderMarker(this.interpolatePreviousState(state), this.getMarkerScale(), f);
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
        this.targetTransition.reset();
        this.attackPulse.reset();
        this.previousState.reset();
        this.lastState = null;
    }

    private void renderMarker(TargetMarkerState state, float f, float f2) {
        float f3;
        long l = System.currentTimeMillis();
        Vec3d vec = this.cameraPosition();
        double d = state.position().x;
        double d2 = state.position().y + state.height() * 0.65;
        double d3 = state.position().z;
        double d4 = vec.x - d;
        double d5 = vec.y - d2;
        double d6 = vec.z - d3;
        float f4 = (float)Math.toDegrees(Math.atan2(d4, d6));
        float f5 = (float)Math.toDegrees(-Math.atan2(d5, Math.sqrt(d4 * d4 + d6 * d6)));
        float f6 = 1.0f + AnimationMath.sin((float)l * 0.004f) * 0.035f;
        float f7 = AnimationMath.sin((float)l * 0.005f) * 0.035f;
        f *= f6;
        int n = this.pulsatingColor(this.primaryColor(), this.secondaryColor(), l);
        if (this.attackPulse.isActive(l, 320L)) {
            f3 = this.attackPulse.progress(l, 320L);
            float f8 = f3 < 0.5f ? f3 / 0.5f : (f3 - 0.5f) / 0.5f;
            float f9 = AnimationMath.easeInOutSine(f8);
            f = f3 < 0.5f ? AnimationMath.lerp(f, 0.4f, f9) : AnimationMath.lerp(0.4f, f, f9);
            n = f3 < 0.5f ? AnimationMath.lerpColor(n, -65476, f9) : AnimationMath.lerpColor(-65476, n, f9);
        }
        f3 = (float)Math.sin((double)System.currentTimeMillis() / 666.6666666666666) * 360.0f;
        this.modelRenderQueue.enqueueModel(this.lazyMeshModel.getMeshModel(), new SpatialTransform(d, d2 + (double)f7, d3, f, f, f, f4, f5, f3), AnimationMath.applyOpacity(n, f2, 0.5f), this.modelRenderOptions);
    }

    private void updateTargetState(Entity entity2, float f) {
        TargetChange<Entity> targetChange = this.targetTransition.updateTarget(entity2);
        if (!targetChange.changed()) {
            return;
        }
        TargetMarkerState oldState = this.lastState;
        TargetMarkerState newState = this.captureState(this.targetTransition.current(), f);
        if (oldState != null && newState != null) {
            this.previousState.remember(oldState);
        }
    }

    private int pulsatingColor(int n, int n2, long l) {
        float f = 0.5f + 0.5f * AnimationMath.sin((float)l * 0.004f);
        return AnimationMath.lerpColor(n, n2, f * 0.45f);
    }

    private TargetMarkerState captureState(Entity entity2, float f) {
        if (entity2 == null || !entity2.isAlive()) {
            return null;
        }
        return new TargetMarkerState(this.interpolatedPosition(entity2, f), entity2.getHeight());
    }

    private TargetMarkerState interpolatePreviousState(TargetMarkerState state) {
        if (!this.previousState.hasValue()) {
            return state;
        }
        float f = this.previousState.progress(420L);
        if (f >= 1.0f) {
            this.previousState.clear();
            return state;
        }
        float f2 = AnimationMath.easeOut(f, 1.16f);
        TargetMarkerState previous = this.previousState.get();
        Vec3d start = previous.position();
        Vec3d end = state.position();
        return new TargetMarkerState(new Vec3d((double)AnimationMath.lerp((float)start.x, (float)end.x, f2), (double)AnimationMath.lerp((float)start.y, (float)end.y, f2), (double)AnimationMath.lerp((float)start.z, (float)end.z, f2)), AnimationMath.lerp((float)previous.height(), (float)state.height(), f2));
    }

    private float getMarkerScale() {
        if (this.targetTransition.phase() == TransitionPhase.IDLE) {
            return 0.25f;
        }
        float f = this.targetTransition.progress(300L);
        if (this.targetTransition.phase() == TransitionPhase.APPEARING) {
            return AnimationMath.lerp(0.42f, 0.25f, AnimationMath.easeOut(f, 1.28f));
        }
        return AnimationMath.lerp(0.25f, 0.42f, AnimationMath.smoothStep(f));
    }

    private float getTransitionOpacity() {
        if (this.targetTransition.phase() == TransitionPhase.IDLE) {
            return 1.0f;
        }
        float f = this.targetTransition.progress(300L);
        if (this.targetTransition.phase() == TransitionPhase.APPEARING) {
            if (f >= 1.0f) {
                this.targetTransition.finishAppearance();
            }
            return AnimationMath.easeInOutSine(f);
        }
        if (f >= 1.0f) {
            this.resetTargetAnimation();
            return 0.0f;
        }
        return 1.0f - AnimationMath.smoothStep(f);
    }

    private record TargetMarkerState(Vec3d position, double height) {
    }
}

