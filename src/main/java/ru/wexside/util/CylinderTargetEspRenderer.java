package ru.wexside.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import ru.wexside.animation.TimedPulse;
import ru.wexside.event.EntityAttackEvent;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.PreviousValueTransition;
import ru.wexside.misc.TargetChange;
import ru.wexside.misc.TargetEspEffect;
import ru.wexside.misc.TargetTransition;
import ru.wexside.misc.TransitionPhase;
import ru.wexside.module.render.TargetEspRenderer;
import ru.wexside.util.AnimationMath;
import ru.wexside.util.ColorUtils;

public final class CylinderTargetEspRenderer
extends TargetEspRenderer
implements TargetEspEffect {
    static final float value4 = 1.0f;
    static final float value5 = 0.8f;
    static final float value6 = 1.0f;
    static final int slot = 360;
    private double animationPhase;
    static final int slot2 = 360;
    static final float value8 = 4.4f;
    static final float value9 = 0.86f;
    static final long member8917 = 260L;
    static final long member12527 = 300L;
    private final PreviousValueTransition<TargetBounds> previousBounds;
    static final double value10 = 1.0E-9;
    static final float value11 = 1.16f;
    static final float value12 = 1.0f;
    private TargetBounds lastBounds;
    static final long member5632 = 420L;
    static final double value13 = 0.45;
    static final double value14 = 0.05;
    private final TargetTransition<LivingEntity> targetTransition2 = new TargetTransition();
    private double previousAnimationPhase;
    static final float value16 = 0.01f;
    private long longType;
    static final float value17 = 1.12f;
    static final double value18 = 0.016666666666666666;
    private final TimedPulse attackPulse = new TimedPulse();
    static final int slot3 = -42663;

    public CylinderTargetEspRenderer() {
        this.previousBounds = new PreviousValueTransition();
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
        this.updateTargetBounds(this.target(mc, LivingEntity.class), floatTypeEvent2.getFloatType());
        if (this.targetTransition2.isEmpty()) {
            return;
        }
        float f = this.getTransitionOpacity();
        LivingEntity entity2 = this.targetTransition2.current();
        if (f <= 0.0f || entity2 == null) {
            return;
        }
        TargetBounds bounds = this.captureBounds(entity2, floatTypeEvent2.getFloatType());
        if (bounds != null) {
            this.lastBounds = bounds;
        } else {
            bounds = this.lastBounds;
        }
        if (bounds == null) {
            this.resetAnimation();
            return;
        }
        this.advanceAnimationPhase();
        this.renderRing(floatTypeEvent2.getMatrices(), mc.getBufferBuilders().getEntityVertexConsumers(), this.interpolatePreviousBounds(bounds), floatTypeEvent2.getFloatType(), this.getRadiusScale(), f);
    }

    @Override
    public void update() {
        this.resetAnimation();
    }

    private void updateTargetBounds(LivingEntity entity2, float f) {
        TargetChange<LivingEntity> targetChange = this.targetTransition2.updateTarget(entity2);
        if (!targetChange.changed()) {
            return;
        }
        TargetBounds oldBounds = this.lastBounds;
        TargetBounds newBounds = this.captureBounds(this.targetTransition2.current(), f);
        if (oldBounds != null && newBounds != null) {
            this.previousBounds.remember(oldBounds);
        }
    }

    private void resetAnimation() {
        this.targetTransition2.reset();
        this.attackPulse.reset();
        this.previousBounds.reset();
        this.lastBounds = null;
        this.animationPhase = 0.0;
        this.previousAnimationPhase = 0.0;
        this.longType = 0L;
    }

    private int highlightedCycleColor(int n, int n2, float f) {
        return ColorUtils.lerp(this.cyclingGradientColor(n, n2, f), -1, 0.2f);
    }

    private double sineWaveUnit(double d) {
        return Math.abs(1.0 + Math.sin(d)) * 0.5;
    }

    private float getRadiusScale() {
        if (this.targetTransition2.phase() == TransitionPhase.IDLE) {
            return 1.0f;
        }
        float f = this.targetTransition2.progress(300L);
        if (this.targetTransition2.phase() == TransitionPhase.APPEARING) {
            return AnimationMath.lerp(0.86f, 1.0f, AnimationMath.easeOut(f, 1.16f));
        }
        return AnimationMath.lerp(1.0f, 0.86f, AnimationMath.smoothStep(f));
    }

    private TargetBounds captureBounds(LivingEntity entity2, float f) {
        if (entity2 == null || !entity2.isAlive()) {
            return null;
        }
        Box box = entity2.getBoundingBox();
        return new TargetBounds(this.interpolatedPosition((Entity)entity2, f), entity2.getHeight(), box.maxX - box.minX, box.maxZ - box.minZ);
    }

    private void renderRing(MatrixStack matrices2, VertexConsumerProvider.Immediate iIiIliiliI2, TargetBounds bounds, float f, float f2, float f3) {
        float f4;
        float f5;
        float f6;
        float f7;
        matrices2.push();
        Vec3d camera = this.cameraPosition();
        matrices2.translate(bounds.position().x - camera.x, bounds.position().y - camera.y, bounds.position().z - camera.z);
        Matrix4f matrix4f = matrices2.peek().getPositionMatrix();
        VertexConsumer iliIIiiliI2 = iIiIliiliI2.getBuffer(RenderLayers.debugQuads());
        long l = System.currentTimeMillis();
        float f8 = this.attackPulse.isActive(l, 260L) ? 1.0f - Math.abs(this.attackPulse.progress(l, 260L) * 2.0f - 1.0f) : 0.0f;
        double d = this.previousAnimationPhase + (this.animationPhase - this.previousAnimationPhase) * (double)f;
        double d2 = this.sineWaveUnit(d - 0.45);
        double d3 = this.sineWaveUnit(d);
        float f9 = (float)(Math.max(bounds.width(), 0.1) * (double)0.8f) * f2;
        float f10 = (float)(d2 * bounds.height());
        float f11 = (float)(d3 * bounds.height());
        int n = Math.max(0, Math.min(255, Math.round(f3 * 1.0f * 255.0f)));
        int n2 = Math.max(0, Math.min(255, Math.round(f3 * 0.01f * 255.0f)));
        int n3 = Math.max(0, Math.min(255, Math.round(f3 * 255.0f)));
        for (int i = 0; i < 360; ++i) {
            float f12 = (float)(Math.PI * 2 * (double)i / 360.0);
            float f13 = (float)(Math.PI * 2 * (double)(i + 1) / 360.0);
            f7 = (float)Math.cos(f12) * f9;
            f6 = (float)Math.sin(f12) * f9;
            f5 = (float)Math.cos(f13) * f9;
            f4 = (float)Math.sin(f13) * f9;
            int n4 = this.applyAttackTint(this.cyclingGradientColor(i, 360, 4.4f), f8);
            int n5 = this.applyAttackTint(this.cyclingGradientColor(i + 1, 360, 4.4f), f8);
            iliIIiiliI2.vertex((Matrix4fc)matrix4f, f7, f11, f6).color(ColorUtils.withAlpha(n4, (float)n));
            iliIIiiliI2.vertex((Matrix4fc)matrix4f, f7, f10, f6).color(ColorUtils.withAlpha(n4, (float)n2));
            iliIIiiliI2.vertex((Matrix4fc)matrix4f, f5, f10, f4).color(ColorUtils.withAlpha(n5, (float)n2));
            iliIIiiliI2.vertex((Matrix4fc)matrix4f, f5, f11, f4).color(ColorUtils.withAlpha(n5, (float)n));
        }
        VertexConsumer iliIIiiliI3 = iIiIliiliI2.getBuffer(RenderLayers.linesTranslucent());
        Vector3f vector3f = new Vector3f(0.0f, 1.0f, 0.0f);
        for (int i = 0; i < 360; ++i) {
            f7 = (float)(Math.PI * 2 * (double)i / 360.0);
            f6 = (float)(Math.PI * 2 * (double)(i + 1) / 360.0);
            f5 = (float)Math.cos(f7) * f9;
            f4 = (float)Math.sin(f7) * f9;
            float f14 = (float)Math.cos(f6) * f9;
            float f15 = (float)Math.sin(f6) * f9;
            int n6 = this.applyAttackTint(this.highlightedCycleColor(i, 360, 4.4f), f8);
            int n7 = this.applyAttackTint(this.highlightedCycleColor(i + 1, 360, 4.4f), f8);
            iliIIiiliI3.vertex((Matrix4fc)matrix4f, f5, f11, f4).color(ColorUtils.withAlpha(n6, (float)n3)).normal(matrices2.peek(), vector3f).lineWidth(1.0f);
            iliIIiiliI3.vertex((Matrix4fc)matrix4f, f14, f11, f15).color(ColorUtils.withAlpha(n7, (float)n3)).normal(matrices2.peek(), vector3f).lineWidth(1.0f);
        }
        iIiIliiliI2.draw();
        matrices2.pop();
    }

    private TargetBounds interpolatePreviousBounds(TargetBounds bounds) {
        if (!this.previousBounds.hasValue()) {
            return bounds;
        }
        float f = this.previousBounds.progress(420L);
        if (f >= 1.0f) {
            this.previousBounds.clear();
            return bounds;
        }
        float f2 = AnimationMath.easeOut(f, 1.12f);
        TargetBounds previous = this.previousBounds.get();
        Vec3d from = previous.position();
        Vec3d to = bounds.position();
        return new TargetBounds(new Vec3d((double)AnimationMath.lerp((float)from.x, (float)to.x, f2), (double)AnimationMath.lerp((float)from.y, (float)to.y, f2), (double)AnimationMath.lerp((float)from.z, (float)to.z, f2)), AnimationMath.lerp((float)previous.height(), (float)bounds.height(), f2), AnimationMath.lerp((float)previous.width(), (float)bounds.width(), f2), AnimationMath.lerp((float)previous.depth(), (float)bounds.depth(), f2));
    }

    private void advanceAnimationPhase() {
        double d;
        long l = System.nanoTime();
        if (this.longType == 0L) {
            d = 0.016666666666666666;
        } else {
            d = Math.min((double)(l - this.longType) * 1.0E-9, 0.05);
            if (!Double.isFinite(d) || d < 0.0) {
                d = 0.016666666666666666;
            }
        }
        this.longType = l;
        this.previousAnimationPhase = this.animationPhase;
        this.animationPhase += d * (double)4.4f;
    }

    private int applyAttackTint(int n, float f) {
        return f <= 0.0f ? n : ColorUtils.lerp(n, -42663, f);
    }

    private float getTransitionOpacity() {
        if (this.targetTransition2.phase() == TransitionPhase.IDLE) {
            return 1.0f;
        }
        float f = this.targetTransition2.progress(300L);
        if (this.targetTransition2.phase() == TransitionPhase.APPEARING) {
            if (f >= 1.0f) {
                this.targetTransition2.finishAppearance();
            }
            return AnimationMath.easeInOutSine(f);
        }
        if (f >= 1.0f) {
            this.resetAnimation();
            return 0.0f;
        }
        return 1.0f - AnimationMath.smoothStep(f);
    }

    private int cyclingGradientColor(int n, int n2, float f) {
        long l = System.currentTimeMillis();
        float f2 = (float)l / (1000.0f / f) % (float)n2;
        int n3 = (int)(((float)n + f2) % (float)n2);
        float f3 = (float)n3 / (float)n2;
        if (f3 < 0.5f) {
            return ColorUtils.lerp(this.primaryColor(), this.secondaryColor(), f3 * 2.0f);
        }
        return ColorUtils.lerp(this.secondaryColor(), this.primaryColor(), (f3 - 0.5f) * 2.0f);
    }

    private record TargetBounds(Vec3d position, double height, double width, double depth) {
    }
}

