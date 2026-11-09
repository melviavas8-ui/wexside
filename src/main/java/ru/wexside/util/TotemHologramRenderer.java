package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector4f;
import ru.wexside.event.TotemPopEvent;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.LazyMeshModel;
import ru.wexside.misc.SpriteAtlasRegion;
import ru.wexside.misc.TotemEffectRenderer;
import ru.wexside.misc.WexsideHitParticles;
import ru.wexside.render.ParticleBillboardRenderer;
import ru.wexside.render.RenderCamera;
import ru.wexside.render.model.BuiltInMesh;
import ru.wexside.render.model.MeshBuilder;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.InlineMesh;
import ru.wexside.util.InlineMeshRenderer;
import ru.wexside.util.ModelRenderOptions;
import ru.wexside.util.TotemEffectSettings;

public final class TotemHologramRenderer
implements TotemEffectRenderer {
    private final List<TotemHologram> holograms;
    private MeshParticleTemplate meshParticleTemplate;
    static final float value2 = 1.65f;
    private final SpriteAtlasRegion spriteAtlasRegion;
    static final float value3 = 0.05f;
    static final double value4 = 0.5;
    private final Random random;
    private List<InlineMesh> values3;
    static final float value5 = 0.7f;
    static final int slot = 500;
    private List<InlineMesh> values4;
    private double meshSplitX;
    private static final InlineMeshRenderer HOLOGRAM_RENDERER = new InlineMeshRenderer("totem-hologram");
    private final TotemEffectSettings totemEffectSettings;
    private List<InlineMesh> values5;
    static final float getColorChannel8 = 15.0f;
    static final float value7 = 0.3f;
    static final float value8 = 0.5f;
    static final double value9 = 10000.0;
    static final float value10 = 1.0f;
    private final LazyMeshModel lazyMeshModel = LazyMeshModel.create(BuiltInMesh.PLAYER);
    static final long member13120 = 1399L;

    public TotemHologramRenderer(TotemEffectSettings totemEffectSettings) {
        this.holograms = new ArrayList<TotemHologram>();
        this.spriteAtlasRegion = WexsideHitParticles.LIGHT.getSpriteAtlasRegion();
        this.random = new Random();
        this.totemEffectSettings = totemEffectSettings;
    }

    @Override
    public void renderWorld(WorldRenderEvent floatTypeEvent2) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || RenderCamera.position() == null) {
            this.clearHolograms();
            return;
        }
        if (this.holograms.isEmpty()) {
            return;
        }
        if (!this.ensureMeshPrepared()) {
            return;
        }
        long l = System.currentTimeMillis();
        for (int i = this.holograms.size() - 1; i >= 0; --i) {
            TotemHologram iliIllIll2 = this.holograms.get(i);
            if (this.isHologramExpired(mc, iliIllIll2, l)) {
                this.holograms.remove(i);
                continue;
            }
            this.renderHologram(floatTypeEvent2, mc, iliIllIll2, l);
        }
    }

    @Override
    public void setTotemPopEvent(TotemPopEvent lIiillIliIEvent) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (lIiillIliIEvent.getEntity() == null || mc.player == null) {
            return;
        }
        Vec3d vec = new Vec3d(lIiillIliIEvent.getEntity().getX(), lIiillIliIEvent.getEntity().getY() + (double)lIiillIliIEvent.getEntity().getHeight() * 0.5, lIiillIliIEvent.getEntity().getZ());
        Vec3d vec2 = new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getHeight() * 0.5, mc.player.getZ());
        Vec3d vec3 = vec2.subtract(vec);
        if (vec3.lengthSquared() <= 1.0E-6) {
            vec3 = new Vec3d(0.0, 0.0, 1.0);
        }
        this.holograms.add(new TotemHologram(vec.add(vec3.normalize().multiply(0.5)), System.currentTimeMillis(), lIiillIliIEvent));
        this.trimHolograms();
    }

    private void renderHologram(WorldRenderEvent floatTypeEvent2, MinecraftClient mc, TotemHologram hologram, long l) {
        Vec3d vec = new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getHeight(), mc.player.getZ());
        double d = vec.x - hologram.position.x;
        double d2 = vec.y - hologram.position.y;
        double d3 = vec.z - hologram.position.z;
        float f = (float)Math.toDegrees(Math.atan2(d, d3));
        float f2 = (float)Math.toDegrees(-Math.atan2(d2, Math.sqrt(d * d + d3 * d3)));
        double d4 = hologram.progress(l);
        float f3 = this.easeInOutCubic((float)d4);
        float f4 = (float)((l - hologram.startedAt) % 3600L) / 10.0f;
        float f5 = (float)(1.0 + Math.sin(f4 * 0.05f) * 0.5);
        float f6 = (float)Math.sin((double)f3 * Math.PI * 6.0) * 0.35f;
        float f7 = (float)(0.2 * (1.0 - (double)f3));
        float f8 = f4 * 2.0f;
        float f9 = (float)Math.sin(d4 * Math.PI * 8.0) * 7.0f;
        float f10 = (this.random.nextFloat() - 0.5f) * 2.0f;
        float f11 = 1.65f * (float)(d4 < 0.5 ? 2.0 * d4 * (1.2 - d4) : 1.0 - (d4 - 0.5) * 2.0);
        Vec3d vec2 = RenderCamera.position();
        MatrixStack matrices2 = new MatrixStack();
        matrices2.translate(hologram.position.x - vec2.x, hologram.position.y - vec2.y, hologram.position.z - vec2.z);
        matrices2.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(f - 25.0f));
        matrices2.multiply((Quaternionfc)RotationAxis.POSITIVE_X.rotationDegrees(f2));
        matrices2.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(f4 * f5));
        matrices2.translate(0.0f, f6, 0.0f);
        matrices2.translate(Math.sin(Math.toRadians(f8)) * (double)f7, 0.0, Math.cos(Math.toRadians(f8)) * (double)f7);
        matrices2.multiply((Quaternionfc)RotationAxis.POSITIVE_Z.rotationDegrees(f9 + f10));
        matrices2.scale(f11, f11, f11);
        if (d4 < (double)0.3f) {
            matrices2.translate((this.random.nextFloat() - 0.5f) * 0.05f, (this.random.nextFloat() - 0.5f) * 0.05f, (this.random.nextFloat() - 0.5f) * 0.05f);
        }
        Matrix4f matrix4f = new Matrix4f((Matrix4fc)matrices2.peek().getPositionMatrix());
        float f12 = d4 < (double)0.3f ? 0.0f : MathHelper.clamp((float)((float)((d4 - (double)0.3f) / 0.19999998807907104)), (float)0.0f, (float)1.0f);
        float f13 = d4 >= 0.5 ? MathHelper.clamp((float)((float)((d4 - 0.5) / 0.19999998807907104)), (float)0.0f, (float)1.0f) : 0.0f;
        float f14 = d4 >= (double)0.7f ? MathHelper.clamp((float)((float)((d4 - (double)0.7f) / (double)0.3f)), (float)0.0f, (float)1.0f) : 0.0f;
        float f15 = d4 >= 0.5 ? 1.0f - f13 : 1.0f;
        float f16 = d4 >= 0.5 ? f13 * (1.0f - f14) : 0.0f;
        int n = this.totemEffectSettings.getIntType3();
        if (f15 > 0.0f) {
            if (d4 < (double)0.3f) {
                HOLOGRAM_RENDERER.renderMatrixInstances(floatTypeEvent2.getMatrices(), mc.getBufferBuilders().getEntityVertexConsumers(), Vec3d.ZERO, this.values4, null, null, this.multiplyColorAlpha(n, f15 * 2.5f), List.of(matrix4f), ModelRenderOptions.getDefaultRenderOptions().withDepthTestEnabled(true));
            } else {
                Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(1.0f * f12, 0.0f, 0.0f).rotateZ((float)Math.toRadians(15.0f * f12));
                Matrix4f matrix4f3 = new Matrix4f((Matrix4fc)matrix4f).translate(-1.0f * f12, 0.0f, 0.0f).rotateZ((float)Math.toRadians(-15.0f * f12));
                if (!this.values3.isEmpty()) {
                    HOLOGRAM_RENDERER.renderMatrixInstances(floatTypeEvent2.getMatrices(), mc.getBufferBuilders().getEntityVertexConsumers(), Vec3d.ZERO, this.values3, null, null, this.multiplyColorAlpha(n, f15 * 2.5f), List.of(matrix4f2), ModelRenderOptions.getDefaultRenderOptions().withDepthTestEnabled(true));
                }
                if (!this.values5.isEmpty()) {
                    HOLOGRAM_RENDERER.renderMatrixInstances(floatTypeEvent2.getMatrices(), mc.getBufferBuilders().getEntityVertexConsumers(), Vec3d.ZERO, this.values5, null, null, this.multiplyColorAlpha(n, f15), List.of(matrix4f3), ModelRenderOptions.getDefaultRenderOptions().withDepthTestEnabled(true));
                }
                if (f16 > 0.0f) {
                    hologram.initializeParticles(this.meshParticleTemplate, this.meshSplitX);
                    Vec3d vec3 = hologram.attractionPoint();
                    float f17 = 0.022f * (2.0f * (1.0f - f14));
                    float f18 = f14 * 2.0f + f13 * 0.2f;
                    float f19 = f13 + f14;
                    this.renderMeshParticles(hologram.startedAt, hologram.leftParticles, matrix4f3, vec3, n, f16, f18, f19, f14, f17, 0);
                    this.renderMeshParticles(hologram.startedAt, hologram.rightParticles, matrix4f2, vec3, n, f16, f18, f19, f14, f17, hologram.leftParticles.length);
                }
            }
        }
    }

    private boolean triangleOnSplitSide(float[] fArray, int n, int n2, int n3, double d, boolean bl) {
        return this.isOnSplitSide(fArray[n * 3], d, bl) && this.isOnSplitSide(fArray[n2 * 3], d, bl) && this.isOnSplitSide(fArray[n3 * 3], d, bl);
    }

    private boolean isHologramExpired(MinecraftClient mc, TotemHologram hologram, long l) {
        if (l - hologram.startedAt > 1399L) {
            return true;
        }
        Vec3d vec = mc.player != null ? new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()) : Vec3d.ZERO;
        return vec.squaredDistanceTo(hologram.position) > 10000.0;
    }

    private List<InlineMesh> filterSplitMeshes(List<InlineMesh> list, double d, boolean bl) {
        ArrayList<InlineMesh> arrayList = new ArrayList<InlineMesh>();
        for (InlineMesh inlineMesh : list) {
            int n;
            int[] nArray = inlineMesh.getIntType2();
            if (nArray == null || nArray.length == 0) continue;
            ArrayList<Integer> arrayList2 = new ArrayList<Integer>();
            float[] fArray = inlineMesh.getFloatType();
            int n2 = 0;
            while (n2 + 2 < nArray.length) {
                n = nArray[n2];
                int n3 = nArray[n2 + 1];
                int n4 = nArray[n2 + 2];
                if (this.triangleOnSplitSide(fArray, n, n3, n4, d, bl)) {
                    arrayList2.add(n);
                    arrayList2.add(n3);
                    arrayList2.add(n4);
                }
                n2 += 3;
            }
            if (arrayList2.isEmpty()) continue;
            int[] nArray2 = new int[arrayList2.size()];
            for (n = 0; n < arrayList2.size(); ++n) {
                nArray2[n] = (Integer)arrayList2.get(n);
            }
            arrayList.add(MeshBuilder.create(inlineMesh.getFloatType(), inlineMesh.getFloatType3(), inlineMesh.getFloatType2(), nArray2, inlineMesh.getFloatType4()));
        }
        return arrayList;
    }

    private Vec3d transformPoint(Matrix4f matrix4f, double d, double d2, double d3) {
        Vector4f vector4f = new Vector4f((float)d, (float)d2, (float)d3, 1.0f);
        matrix4f.transform(vector4f);
        return new Vec3d((double)vector4f.x(), (double)vector4f.y(), (double)vector4f.z());
    }

    private double meshCenterX(List<InlineMesh> list) {
        double d = Double.POSITIVE_INFINITY;
        double d2 = Double.NEGATIVE_INFINITY;
        for (InlineMesh inlineMesh : list) {
            float[] fArray = inlineMesh.getFloatType();
            int n = 0;
            while (n + 2 < fArray.length) {
                d = Math.min(d, (double)fArray[n]);
                d2 = Math.max(d2, (double)fArray[n]);
                n += 3;
            }
        }
        return (d + d2) * 0.5;
    }

    private void trimHolograms() {
        int n = this.holograms.size() - 12;
        if (n > 0) {
            this.holograms.subList(0, n).clear();
        }
    }

    private void renderMeshParticles(long l, MeshParticle[] iIililiIIIArray, Matrix4f matrix4f, Vec3d vec, int n, float f, float f2, float f3, float f4, float f5, int n2) {
        if (iIililiIIIArray.length == 0 || f5 <= 0.001f) {
            return;
        }
        Vec3d vec2 = RenderCamera.position();
        for (int i = 0; i < iIililiIIIArray.length; ++i) {
            Vec3d vec3;
            MeshParticle particle = iIililiIIIArray[i];
            Random random = new Random(l + (long)n2 + (long)i);
            double d = particle.position.x + particle.direction.x * (double)f2 + (double)((random.nextFloat() - 0.5f) * 0.5f * f3);
            double d2 = particle.position.y + particle.direction.y * (double)f2 + (double)((random.nextFloat() - 0.5f) * 0.5f * f3) - (double)(f4 * 2.0f);
            double d3 = particle.position.z + particle.direction.z * (double)f2 + (double)((random.nextFloat() - 0.5f) * 0.5f * f3);
            Vec3d vec4 = this.transformPoint(matrix4f, d, d2, d3).add(vec2.x, vec2.y, vec2.z);
            if (vec != null && (vec3 = vec.subtract(vec4)).lengthSquared() > 1.0E-6) {
                vec4 = vec4.add(vec3.normalize().multiply((double)(f4 * 2.0f)));
            }
            ParticleBillboardRenderer.draw(vec4.x, vec4.y, vec4.z, f5, f5, this.multiplyColorAlpha(n, f), WexsideHitParticles.getParticleTexture(), false, 0.0f, this.spriteAtlasRegion.minU(), this.spriteAtlasRegion.minV(), this.spriteAtlasRegion.maxU(), this.spriteAtlasRegion.maxV());
        }
    }

    private boolean ensureMeshPrepared() {
        if (!this.lazyMeshModel.isLoaded() || this.lazyMeshModel.getMeshModel() == null || this.lazyMeshModel.getMeshModel().getList().isEmpty()) {
            return false;
        }
        if (this.values4 != null) {
            return true;
        }
        this.values4 = List.copyOf(this.lazyMeshModel.getMeshModel().getList());
        this.meshSplitX = this.meshCenterX(this.values4);
        this.values5 = this.filterSplitMeshes(this.values4, this.meshSplitX, false);
        this.values3 = this.filterSplitMeshes(this.values4, this.meshSplitX, true);
        this.meshParticleTemplate = MeshParticleTemplate.from(this.values4);
        return true;
    }

    private boolean isOnSplitSide(float f, double d, boolean bl) {
        return bl ? (double)f >= d : (double)f < d;
    }

    private float easeInOutCubic(float f) {
        float f2 = MathHelper.clamp((float)f, (float)0.0f, (float)1.0f);
        return f2 < 0.5f ? 4.0f * f2 * f2 * f2 : 1.0f - (float)Math.pow(-2.0f * f2 + 2.0f, 3.0) * 0.5f;
    }

    private int multiplyColorAlpha(int n, float f) {
        int n2 = MathHelper.clamp((int)((int)((float)(n >>> 24 & 0xFF) * f)), (int)0, (int)255);
        return ColorUtils.withAlpha(n, (float)n2);
    }

    @Override
    public void clearHolograms() {
        this.holograms.clear();
    }

    private static final class TotemHologram {
        private final Vec3d position;
        private final long startedAt;
        private final TotemPopEvent event;
        private MeshParticle[] leftParticles = new MeshParticle[0];
        private MeshParticle[] rightParticles = new MeshParticle[0];
        private boolean particlesInitialized;

        private TotemHologram(Vec3d position, long startedAt, TotemPopEvent event) {
            this.position = position;
            this.startedAt = startedAt;
            this.event = event;
        }

        private double progress(long now) {
            return MathHelper.clamp((double)((double)(now - this.startedAt) / 1399.0), (double)0.0, (double)1.0);
        }

        private void initializeParticles(MeshParticleTemplate template, double splitX) {
            if (this.particlesInitialized || template == null) {
                return;
            }
            this.leftParticles = template.leftOf(splitX);
            this.rightParticles = template.rightOf(splitX);
            this.particlesInitialized = true;
        }

        private Vec3d attractionPoint() {
            if (this.event.getEntity() == null) {
                return this.position;
            }
            return this.event.getEntity().getLerpedPos(1.0f).add(0.0, (double)this.event.getEntity().getHeight() * 0.5, 0.0);
        }
    }

    private record MeshParticleTemplate(List<MeshParticle> particles) {
        private static MeshParticleTemplate from(List<InlineMesh> meshes) {
            ArrayList<MeshParticle> particles = new ArrayList<MeshParticle>();
            for (InlineMesh mesh : meshes) {
                float[] positions = mesh.getFloatType();
                float[] normals = mesh.getFloatType3();
                int index = 0;
                while (index + 2 < positions.length) {
                    Vec3d position = new Vec3d((double)positions[index], (double)positions[index + 1], (double)positions[index + 2]);
                    Vec3d direction = normals != null && index + 2 < normals.length ? new Vec3d((double)normals[index], (double)normals[index + 1], (double)normals[index + 2]) : (position.lengthSquared() > 1.0E-7 ? position.normalize() : Vec3d.ZERO);
                    particles.add(new MeshParticle(position, direction));
                    index += 3;
                }
            }
            if (particles.size() > 500) {
                int step = Math.max(1, particles.size() / 500);
                ArrayList<MeshParticle> sampled = new ArrayList<MeshParticle>(500);
                for (int index = 0; index < particles.size() && sampled.size() < 500; index += step) {
                    sampled.add((MeshParticle)particles.get(index));
                }
                particles = sampled;
            }
            return new MeshParticleTemplate(List.copyOf(particles));
        }

        private MeshParticle[] leftOf(double splitX) {
            return (MeshParticle[])this.particles.stream().filter(particle -> particle.position().x < splitX).toArray(MeshParticle[]::new);
        }

        private MeshParticle[] rightOf(double splitX) {
            return (MeshParticle[])this.particles.stream().filter(particle -> particle.position().x >= splitX).toArray(MeshParticle[]::new);
        }
    }

    private record MeshParticle(Vec3d position, Vec3d direction) {
    }
}

