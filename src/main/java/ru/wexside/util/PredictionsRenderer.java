package ru.wexside.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import ru.wexside.WexSideClient;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.LazyMeshModel;
import ru.wexside.misc.SpatialTransform;
import ru.wexside.module.render.PredictionsModule;
import ru.wexside.prediction.ImpactQuad;
import ru.wexside.prediction.LandingPrediction;
import ru.wexside.prediction.LandingTimeLabel;
import ru.wexside.prediction.ModelOrientation;
import ru.wexside.prediction.PotionImpactMarker;
import ru.wexside.prediction.ProjectileImpact;
import ru.wexside.prediction.TrajectoryPrediction;
import ru.wexside.render.DepthSampler;
import ru.wexside.render.ItemIconCache;
import ru.wexside.render.RenderCamera;
import ru.wexside.render.RenderProjection;
import ru.wexside.render.model.BuiltInMesh;
import ru.wexside.render.world.WorldLineRenderer;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.ModelRenderOptions;
import ru.wexside.util.ModelRenderQueue;
import ru.wexside.util.MsdfFontRenderer;
import ru.wexside.util.ProjectileType;
import ru.wexside.util.TrajectoryPredictor;
import ru.wexside.util.WorldMeshBatchRenderer;

public final class PredictionsRenderer {
    private final TrajectoryPredictor trajectoryPredictor;
    private final Map<Integer, Vec3d> stablePotionImpactPositions;
    private static final WorldMeshBatchRenderer MODEL_RENDERER = new WorldMeshBatchRenderer("trajectory-prediction");
    private final LazyMeshModel arrowModel;
    private static final MsdfFontRenderer LANDING_LABEL_FONT = FontRegistry.boldText;
    private float animationTimeSeconds;
    private final LazyMeshModel roundProjectileModel;
    private final ModelRenderQueue modelRenderQueue;
    private final PredictionsModule predictionsModule;
    private final List<LandingPrediction> landingPredictions;
    private final List<PotionImpactMarker> potionImpactMarkers;
    private final ItemIconCache itemIconCache = new ItemIconCache();
    private final LazyMeshModel entityBoxModel;
    private final List<ImpactQuad> impactQuads;
    private float pulseScale = 1.0f;
    private final DepthSampler depthSampler;
    private static final Identifier IMPACT_MARKER_TEXTURE = Identifier.of((String)"wexside", (String)"textures/prediction/arrow_cross.png");
    private final LazyMeshModel tridentModel;

    public PredictionsRenderer(PredictionsModule predictionsModule) {
        this.landingPredictions = new ArrayList<LandingPrediction>();
        this.impactQuads = new ArrayList<ImpactQuad>();
        this.depthSampler = new DepthSampler();
        this.tridentModel = LazyMeshModel.create(BuiltInMesh.TRIDENT);
        this.roundProjectileModel = LazyMeshModel.create(BuiltInMesh.SKULL);
        this.arrowModel = LazyMeshModel.create(BuiltInMesh.ARROW);
        this.entityBoxModel = LazyMeshModel.create(BuiltInMesh.CUBE);
        this.modelRenderQueue = new ModelRenderQueue();
        this.potionImpactMarkers = new ArrayList<PotionImpactMarker>();
        this.stablePotionImpactPositions = new HashMap<Integer, Vec3d>();
        this.predictionsModule = predictionsModule;
        this.trajectoryPredictor = new TrajectoryPredictor();
    }

    private double[][] rotationYMatrix(float f) {
        double d = Math.toRadians(f);
        double d2 = Math.cos(d);
        double d3 = Math.sin(d);
        return new double[][]{{d2, 0.0, d3}, {0.0, 1.0, 0.0}, {-d3, 0.0, d2}};
    }

    private void addPotionImpactBox(Vec3d vec) {
        if (!this.predictionsModule.showEntityHitBox()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return;
        }
        for (Entity entity2 : mc.world.getEntities()) {
            Vec3d vec2;
            if (!this.isLivingTarget(entity2) || !((vec2 = new Vec3d(entity2.getX(), entity2.getY(), entity2.getZ())).distanceTo(vec) <= 3.0)) continue;
            WorldLineRenderer.drawEntityBox(entity2, -45747);
        }
    }

    private int withAlpha(int n, float f) {
        int n2 = Math.clamp((long)Math.round(f * 255.0f), 0, 255);
        return n2 << 24 | n & 0xFFFFFF;
    }

    private void renderActiveProjectilePredictions(WorldRenderEvent floatTypeEvent2, MinecraftClient mc) {
        HashSet<Integer> hashSet = new HashSet<Integer>();
        for (Entity entity2 : mc.world.getEntities()) {
            ProjectileType iiIlilIilI2;
            TrajectoryPrediction trajectoryPrediction;
            if (!this.shouldTrackEntity(entity2) || (trajectoryPrediction = this.trajectoryPredictor.predictEntityTrajectory(entity2, floatTypeEvent2.getFloatType())) == null) continue;
            boolean bl = this.isOwnedByLocalPlayer(mc, entity2);
            int n = this.getEntityTrajectoryColor(entity2, bl);
            this.renderTrajectory(trajectoryPrediction, n, false, 0);
            ProjectileImpact illliiiilI2 = trajectoryPrediction.impact();
            if (illliiiilI2 == null) continue;
            ItemStack landingItem = this.getProjectileItemStack(entity2);
            if (this.predictionsModule.showLandingTime() && !landingItem.isEmpty()) {
                this.landingPredictions.add(new LandingPrediction(illliiiilI2.position(), trajectoryPrediction.flightTicks(), landingItem));
            }
            if ((iiIlilIilI2 = ProjectileType.fromEntity(entity2)) == ProjectileType.POTION) {
                hashSet.add(entity2.getId());
                Vec3d vec = this.snapPotionImpactToGround(illliiiilI2.position(), illliiiilI2.face());
                Vec3d vec2 = this.stablePotionImpactPositions.get(entity2.getId());
                if (vec2 == null) {
                    if (entity2.getVelocity().lengthSquared() < 0.05) continue;
                    vec2 = vec;
                    this.stablePotionImpactPositions.put(entity2.getId(), vec2);
                } else if (vec2.squaredDistanceTo(vec) > 2.25) {
                    vec2 = vec;
                    this.stablePotionImpactPositions.put(entity2.getId(), vec2);
                }
                this.potionImpactMarkers.add(new PotionImpactMarker(vec2, n));
                if (!bl) continue;
                this.addPotionImpactBox(vec2);
                continue;
            }
            if (bl) {
                this.markHitEntity(illliiiilI2);
            }
            if (iiIlilIilI2 != null && this.predictionsModule.shouldShowTrajectory(PredictionsRenderer.toProjectileKind(iiIlilIilI2), bl)) {
                this.queueImpactModel(mc, iiIlilIilI2, illliiiilI2, n, floatTypeEvent2.getFloatType());
                continue;
            }
            this.renderCircularImpactMarker(illliiiilI2.position(), illliiiilI2.face(), illliiiilI2.hitEntity());
        }
        this.stablePotionImpactPositions.keySet().retainAll(hashSet);
    }

    private void flushModelQueue(WorldRenderEvent floatTypeEvent2, MinecraftClient mc) {
        if (this.modelRenderQueue.isEmpty()) {
            return;
        }
        MODEL_RENDERER.renderQueue(floatTypeEvent2.getMatrices(), mc.getBufferBuilders().getEntityVertexConsumers(), this.toVec3d(), this.modelRenderQueue);
        this.modelRenderQueue.clearBatches();
    }

    private void queuePotionAffectedEntityBoxes(MinecraftClient mc, Vec3d vec, int n, float f) {
        if (!this.entityBoxModel.isLoaded() || this.entityBoxModel.getMeshModel() == null || this.entityBoxModel.getMeshModel().getList().isEmpty()) {
            return;
        }
        ModelRenderOptions modelRenderOptions = ModelRenderOptions.forOutlineEnabled(true).withOutlineThroughFill(false).withOutlineLineWidth(2.2f).withDepthTestEnabled(true);
        int n2 = this.withScaledAlpha(n, 0.0f);
        for (Entity entity2 : mc.world.getEntities()) {
            LivingEntity entity3;
            if (!(entity2 instanceof LivingEntity) || !(entity3 = (LivingEntity)entity2).isAlive()) continue;
            if (new Vec3d(entity3.getX(), entity3.getY(), entity3.getZ()).distanceTo(vec) > 3.0) continue;
            Box box = entity3.getBoundingBox();
            double d = MathHelper.lerp((double)f, (double)entity3.lastX, (double)entity3.getX()) - entity3.getX();
            double d2 = MathHelper.lerp((double)f, (double)entity3.lastY, (double)entity3.getY()) - entity3.getY();
            double d3 = MathHelper.lerp((double)f, (double)entity3.lastZ, (double)entity3.getZ()) - entity3.getZ();
            this.modelRenderQueue.enqueueModel(this.entityBoxModel.getMeshModel(), new SpatialTransform((box.minX + box.maxX) / 2.0 + d, box.minY + d2 + (double)0.01f + (double)0.004f, (box.minZ + box.maxZ) / 2.0 + d3, (float)(box.maxX - box.minX + (double)0.12f), 0.02f, (float)(box.maxZ - box.minZ + (double)0.12f)), n2, modelRenderOptions);
        }
    }

    private double[][] rotationXMatrix(float f) {
        double d = Math.toRadians(f);
        double d2 = Math.cos(d);
        double d3 = Math.sin(d);
        return new double[][]{{1.0, 0.0, 0.0}, {0.0, d2, -d3}, {0.0, d3, d2}};
    }

    private ModelOrientation orientationAlongVelocity(Vec3d vec) {
        float f = (float)Math.toDegrees(Math.atan2(vec.x, vec.z)) - 90.0f;
        float f2 = (float)Math.toDegrees(Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z))) + 90.0f;
        double[][] dArray = this.multiplyMatrices(this.multiplyMatrices(this.rotationYMatrix(f), this.rotationZMatrix(f2)), this.rotationXMatrix(90.0f));
        return this.matrixToOrientation(dArray);
    }

    private void renderLandingLabel(GuiDrawApi drawApi, Matrix4f matrix4f, LandingTimeLabel label) {
        this.itemIconCache.drawBakedIcon(drawApi, matrix4f, label.bakedIcon, label.iconX, label.iconY, 9.0f);
        LANDING_LABEL_FONT.drawText(matrix4f, drawApi, label.text, label.textX, label.textY, 7.0f, -1);
    }

    private float impactOutlineWidth(float f) {
        float f2 = MathHelper.clamp((float)f, (float)0.0f, (float)1.0f);
        return 1.0f + 1.0f * f2;
    }

    private Vec3d offsetOnImpactPlane(Vec3d vec, Direction process17, double d, double d2) {
        return switch (process17.getAxis()) {
            default -> throw new MatchException(null, null);
            case Direction.Axis.Y -> vec.add(d, 0.0, d2);
            case Direction.Axis.Z -> vec.add(d, d2, 0.0);
            case Direction.Axis.X -> vec.add(0.0, d, d2);
        };
    }

    private LandingTimeLabel createLandingTimeLabel(float f, float f2, String string, ItemStack stack) {
        float f3 = LANDING_LABEL_FONT.textWidth(string, 7.0f);
        float f4 = LANDING_LABEL_FONT.textHeight(string, 7.0f);
        float f5 = Math.max(9.0f, f4);
        float f6 = 14.0f + f3 + 3.0f;
        float f7 = f5 + 6.0f;
        float f8 = f - f6 / 2.0f;
        float f9 = f2 - f7 / 2.0f;
        float f10 = f8 + 3.0f;
        float f11 = f9 + (f7 - 9.0f) / 2.0f;
        float f12 = f10 + 9.0f + 2.0f;
        float f13 = f9 + (f7 - f4) / 2.0f;
        return new LandingTimeLabel(f8, f9, f6, f7, f10, f11, f12, f13, string, stack);
    }

    private void renderCircularImpactMarker(Vec3d vec, Direction process17, Entity entity2) {
        if (!this.predictionsModule.showImpactMarker() || vec == null || process17 == null) {
            return;
        }
        int n = entity2 instanceof LivingEntity ? -45747 : this.predictionsModule.getImpactColorArgb();
        double d = 0.95 + 0.05 * Math.sin((double)this.animationTimeSeconds * Math.PI);
        double d2 = 0.3 * d;
        double d3 = 0.1 * d;
        ArrayList<WorldLineRenderer.Segment> lines = new ArrayList<WorldLineRenderer.Segment>(42);
        for (int i = 0; i < 40; ++i) {
            double d4 = Math.PI * 2 * (double)i / 40.0;
            double d5 = Math.PI * 2 * (double)(i + 1) / 40.0;
            lines.add(new WorldLineRenderer.Segment(this.offsetOnImpactPlane(vec, process17, Math.cos(d4) * d2, Math.sin(d4) * d2), this.offsetOnImpactPlane(vec, process17, Math.cos(d5) * d2, Math.sin(d5) * d2), n, n));
        }
        switch (process17.getAxis()) {
            case Y: {
                lines.add(new WorldLineRenderer.Segment(vec.add(-d3, 0.0, 0.0), vec.add(d3, 0.0, 0.0), n, n));
                lines.add(new WorldLineRenderer.Segment(vec.add(0.0, 0.0, -d3), vec.add(0.0, 0.0, d3), n, n));
                break;
            }
            case Z: {
                lines.add(new WorldLineRenderer.Segment(vec.add(-d3, 0.0, 0.0), vec.add(d3, 0.0, 0.0), n, n));
                lines.add(new WorldLineRenderer.Segment(vec.add(0.0, -d3, 0.0), vec.add(0.0, d3, 0.0), n, n));
                break;
            }
            case X: {
                lines.add(new WorldLineRenderer.Segment(vec.add(0.0, -d3, 0.0), vec.add(0.0, d3, 0.0), n, n));
                lines.add(new WorldLineRenderer.Segment(vec.add(0.0, 0.0, -d3), vec.add(0.0, 0.0, d3), n, n));
            }
        }
        WorldLineRenderer.draw(lines);
    }

    private void renderImpactQuads(WorldRenderEvent floatTypeEvent2, MinecraftClient mc) {
        if (this.impactQuads.isEmpty()) {
            return;
        }
        Vec3d vec = this.toVec3d();
        Matrix4f matrix4f = new Matrix4f((Matrix4fc)floatTypeEvent2.getMatrices().peek().getPositionMatrix());
        VertexConsumerProvider.Immediate iIiIliiliI2 = mc.getBufferBuilders().getEntityVertexConsumers();
        RenderLayer enabled = RenderLayers.entityTranslucent((Identifier)IMPACT_MARKER_TEXTURE);
        VertexConsumer iliIIiiliI2 = iIiIliiliI2.getBuffer(enabled);
        double d = 0.16 * (double)this.pulseScale;
        for (ImpactQuad marker : this.impactQuads) {
            int n = marker.color();
            int n2 = n >>> 24 & 0xFF;
            int n3 = n >> 16 & 0xFF;
            int n4 = n >> 8 & 0xFF;
            int n5 = n & 0xFF;
            Vec3d vec2 = marker.position();
            Direction process17 = marker.face();
            Vec3d vec3 = this.offsetOnImpactPlane(vec2, process17, -d, -d).subtract(vec);
            Vec3d vec4 = this.offsetOnImpactPlane(vec2, process17, d, -d).subtract(vec);
            Vec3d vec5 = this.offsetOnImpactPlane(vec2, process17, d, d).subtract(vec);
            Vec3d vec6 = this.offsetOnImpactPlane(vec2, process17, -d, d).subtract(vec);
            this.writeImpactVertex(iliIIiiliI2, matrix4f, vec3, 0.0f, 0.0f, n3, n4, n5, n2);
            this.writeImpactVertex(iliIIiiliI2, matrix4f, vec4, 1.0f, 0.0f, n3, n4, n5, n2);
            this.writeImpactVertex(iliIIiiliI2, matrix4f, vec5, 1.0f, 1.0f, n3, n4, n5, n2);
            this.writeImpactVertex(iliIIiiliI2, matrix4f, vec3, 0.0f, 0.0f, n3, n4, n5, n2);
            this.writeImpactVertex(iliIIiiliI2, matrix4f, vec5, 1.0f, 1.0f, n3, n4, n5, n2);
            this.writeImpactVertex(iliIIiiliI2, matrix4f, vec6, 0.0f, 1.0f, n3, n4, n5, n2);
        }
        iIiIliiliI2.draw(enabled);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void onHudRender(HudRenderEvent gameEvent20) {
        float f;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!this.predictionsModule.isModuleEnabled() || mc.player == null || mc.world == null || RenderCamera.position() == null) {
            this.itemIconCache.disposeCache();
            return;
        }
        if (!this.predictionsModule.showLandingTime() || this.landingPredictions.isEmpty()) {
            this.itemIconCache.disposeCache();
            return;
        }
        ArrayList<LandingTimeLabel> arrayList = new ArrayList<LandingTimeLabel>(this.landingPredictions.size());
        for (LandingPrediction landingPrediction : this.landingPredictions) {
            ItemStack stack;
            Vector2f vector2f = RenderProjection.project(landingPrediction.position());
            if (vector2f == null || (stack = landingPrediction.icon()).isEmpty()) continue;
            float f2 = f = (float)Math.round((float)landingPrediction.flightTicks() / 20.0f * 10.0f) / 10.0f;
            arrayList.add(this.createLandingTimeLabel(vector2f.x, vector2f.y, f2 + " сек.", stack));
        }
        if (arrayList.isEmpty()) {
            return;
        }
        float f3 = mc.getWindow().getScaleFactor();
        this.itemIconCache.advanceFrame();
        for (LandingTimeLabel label : arrayList) {
            label.bakedIcon = this.itemIconCache.getOrCreateIcon(label.item);
        }
        ArrayList<BakedIconEntry> arrayList2 = new ArrayList<BakedIconEntry>();
        this.itemIconCache.appendPendingBakes(f3, arrayList2);
        if (!arrayList2.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(arrayList2);
        }
        GuiDrawApi drawApi = WexSideClient.getHudRenderer();
        Matrix4f matrix = new Matrix4f().scale(f3);
        f = 2.0f / f3;
        drawApi.begin();
        try {
            for (LandingTimeLabel label : arrayList) {
                drawApi.drawRoundedOutline(matrix, label.x, label.y, label.width, label.height, 8.0f, f, -1340597728);
                this.renderLandingLabel(drawApi, matrix, label);
            }
        }
        finally {
            drawApi.end();
        }
        this.itemIconCache.trimCache();
    }

    public void onWorldRender(WorldRenderEvent floatTypeEvent2) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!this.predictionsModule.isModuleEnabled() || mc.player == null || mc.world == null) {
            this.clearFrameData();
            this.stablePotionImpactPositions.clear();
            return;
        }
        this.clearFrameData();
        float f = ((float)mc.world.getTimeOfDay() + floatTypeEvent2.getFloatType()) / 20.0f;
        this.pulseScale = 0.95f + 0.05f * MathHelper.sin((double)((float)((double)f * Math.PI)));
        this.animationTimeSeconds = f;
        this.renderHeldItemPredictions(floatTypeEvent2, mc);
        this.renderOtherPlayerPredictions(floatTypeEvent2, mc);
        this.renderActiveProjectilePredictions(floatTypeEvent2, mc);
        this.renderDroppedItemTrajectories(floatTypeEvent2, mc);
        this.queuePotionEffectVolumes(floatTypeEvent2, mc);
        this.flushModelQueue(floatTypeEvent2, mc);
        this.renderImpactQuads(floatTypeEvent2, mc);
        this.updatePotionDepthSampler();
    }

    public void resetSession() {
        this.clearFrameData();
        this.itemIconCache.disposeCache();
    }

    private void writeImpactVertex(VertexConsumer iliIIiiliI2, Matrix4f matrix4f, Vec3d vec, float f, float f2, int n, int n2, int n3, int n4) {
        iliIIiiliI2.vertex((Matrix4fc)matrix4f, (float)vec.x, (float)vec.y, (float)vec.z).texture(f, f2).color(n, n2, n3, n4);
    }

    private void markHitEntity(ProjectileImpact impact) {
        if (this.predictionsModule.showEntityHitBox() && this.isLivingTarget(impact.hitEntity())) {
            WorldLineRenderer.drawEntityBox(impact.hitEntity(), -45747);
        }
    }

    private double[][] rotationZMatrix(float f) {
        double d = Math.toRadians(f);
        double d2 = Math.cos(d);
        double d3 = Math.sin(d);
        return new double[][]{{d2, -d3, 0.0}, {d3, d2, 0.0}, {0.0, 0.0, 1.0}};
    }

    private void addClassicArrowImpact(MinecraftClient mc, ProjectileType iiIlilIilI2, ProjectileImpact illliiiilI2, int n, float f) {
        Vec3d vec = this.trajectoryPredictor.getLaunchOrigin((LivingEntity)mc.player, f);
        float f2 = this.calculateDistanceFade(vec.distanceTo(illliiiilI2.position()));
        if (f2 < 0.02f) {
            return;
        }
        int n2 = illliiiilI2.hitEntity() != null ? -45747 : n;
        Direction process17 = illliiiilI2.face() != null ? illliiiilI2.face() : Direction.UP;
        this.impactQuads.add(new ImpactQuad(this.adjustImpactPosition(iiIlilIilI2, illliiiilI2), process17, this.withAlpha(n2, f2)));
    }

    private int withScaledAlpha(int n, float f) {
        int n2 = Math.max(0, Math.min(255, Math.round(f * 0.85f * 255.0f)));
        return n2 << 24 | n & 0xFFFFFF;
    }

    private void renderTrajectory(TrajectoryPrediction prediction, int n, boolean bl, int n2) {
        List<Vec3d> list = prediction.points();
        if (list == null || list.size() < 2) {
            return;
        }
        int n3 = list.size() - 1;
        ArrayList<WorldLineRenderer.Segment> lines = new ArrayList<WorldLineRenderer.Segment>(n3);
        for (int i = 0; i < n3; ++i) {
            if (i < n2 || bl && i % 2 != 0) continue;
            float f = Math.max(bl ? 0.16f : 0.08f, 1.0f - (float)i / (float)n3);
            float f2 = Math.max(bl ? 0.08f : 0.04f, 1.0f - (float)(i + 1) / (float)n3);
            if (bl) {
                f *= 0.65f;
                f2 *= 0.65f;
            }
            lines.add(new WorldLineRenderer.Segment(list.get(i), list.get(i + 1), this.withAlpha(n, f), this.withAlpha(n, f2)));
        }
        WorldLineRenderer.draw(lines);
    }

    private Vec3d adjustImpactPosition(ProjectileType iiIlilIilI2, ProjectileImpact illliiiilI2) {
        if (illliiiilI2.hitEntity() != null || illliiiilI2.face() == null) {
            return illliiiilI2.position();
        }
        if (iiIlilIilI2 != ProjectileType.ARROW && iiIlilIilI2 != ProjectileType.CROSSBOW) {
            return illliiiilI2.position();
        }
        Vec3d normal = Vec3d.of((Vec3i)illliiiilI2.face().getVector());
        return illliiiilI2.position().add(normal.multiply(0.018));
    }

    private Vec3d snapPotionImpactToGround(Vec3d vec, Direction process17) {
        double d = process17 == Direction.UP ? vec.y + 0.003 : Math.floor(vec.y) + 0.003;
        return new Vec3d(vec.x, d, vec.z);
    }

    private void queueImpactModel(MinecraftClient mc, ProjectileType iiIlilIilI2, ProjectileImpact illliiiilI2, int n, float f) {
        float f2;
        float f3;
        ModelOrientation orientation;
        LazyMeshModel arrowModel;
        if (illliiiilI2 == null) {
            return;
        }
        if ((iiIlilIilI2 == ProjectileType.ARROW || iiIlilIilI2 == ProjectileType.CROSSBOW) && this.predictionsModule.isClassicArrowStyle()) {
            this.addClassicArrowImpact(mc, iiIlilIilI2, illliiiilI2, n, f);
            return;
        }
        switch (iiIlilIilI2) {
            case TRIDENT: {
                if (!this.tridentModel.isLoaded() || this.tridentModel.getMeshModel() == null || this.tridentModel.getMeshModel().getList().isEmpty()) {
                    return;
                }
                arrowModel = this.tridentModel;
                orientation = this.orientationAlongVelocity(illliiiilI2.velocityDirection());
                f3 = 0.2f;
                f2 = 0.5f;
                break;
            }
            case PEARL: 
            case POTION: {
                if (!this.roundProjectileModel.isLoaded() || this.roundProjectileModel.getMeshModel() == null || this.roundProjectileModel.getMeshModel().getList().isEmpty()) {
                    return;
                }
                arrowModel = this.roundProjectileModel;
                float f4 = (float)Math.toDegrees(Math.atan2(illliiiilI2.velocityDirection().x, illliiiilI2.velocityDirection().z)) - 90.0f;
                orientation = this.orientationFromYawPitch(f4, 90.0f);
                f3 = 0.3f;
                f2 = 0.5f;
                break;
            }
            case ARROW: 
            case CROSSBOW: {
                if (!this.arrowModel.isLoaded() || this.arrowModel.getMeshModel() == null || this.arrowModel.getMeshModel().getList().isEmpty()) {
                    return;
                }
                arrowModel = this.arrowModel;
                orientation = this.orientationAlongVelocity(illliiiilI2.velocityDirection());
                f3 = 0.1f;
                f2 = 1.0f;
                break;
            }
            case ITEM: {
                return;
            }
            default: {
                return;
            }
        }
        Vec3d vec = this.trajectoryPredictor.getLaunchOrigin((LivingEntity)mc.player, f);
        float f5 = this.calculateDistanceFade(vec.distanceTo(illliiiilI2.position()));
        if (f5 < 0.02f) {
            return;
        }
        float f6 = this.pulseScale;
        int n2 = illliiiilI2.hitEntity() != null ? -45747 : n;
        Vec3d vec2 = this.adjustImpactPosition(iiIlilIilI2, illliiiilI2);
        this.modelRenderQueue.enqueueModel(arrowModel.getMeshModel(), new SpatialTransform(vec2.x, vec2.y, vec2.z, f3 * f6, f3 * f6, f3 * f6, orientation.yaw(), orientation.pitch(), orientation.roll()), this.withScaledAlpha(n2, f5 * f2), ModelRenderOptions.forOutlineEnabled(true).withOutlineLineWidth(this.impactOutlineWidth(f5 * f2)).withDepthTestEnabled(false));
    }

    private boolean isOwnedByLocalPlayer(MinecraftClient mc, Entity entity2) {
        if (mc.player == null) {
            return false;
        }
        if (entity2 instanceof ProjectileEntity) {
            ProjectileEntity projectileEntity = (ProjectileEntity)entity2;
            return projectileEntity.getOwner() == mc.player;
        }
        return false;
    }

    private void renderDroppedItemTrajectories(WorldRenderEvent floatTypeEvent2, MinecraftClient mc) {
        if (!this.predictionsModule.isItemsEnabled()) {
            return;
        }
        for (Entity entity2 : mc.world.getEntities()) {
            TrajectoryPrediction trajectoryPrediction;
            ItemEntity iIIIlIIIII2;
            if (!(entity2 instanceof ItemEntity) || !(iIIIlIIIII2 = (ItemEntity)entity2).isAlive() || iIIIlIIIII2.isOnGround() || iIIIlIIIII2.getVelocity().lengthSquared() < 1.0E-5 || (trajectoryPrediction = this.trajectoryPredictor.predictDroppedItemTrajectory(iIIIlIIIII2, floatTypeEvent2.getFloatType())) == null) continue;
            this.renderTrajectory(trajectoryPrediction, this.predictionsModule.getTrajectoryColor(PredictionsRenderer.toProjectileKind(ProjectileType.ITEM), true), true, 0);
        }
    }

    private ModelOrientation orientationFromYawPitch(float f, float f2) {
        return this.matrixToOrientation(this.multiplyMatrices(this.rotationYMatrix(f), this.rotationXMatrix(f2)));
    }

    private ItemStack getProjectileItemStack(Entity entity2) {
        if (entity2 instanceof TridentEntity) {
            return new ItemStack((ItemConvertible)Items.TRIDENT);
        }
        if (entity2 instanceof ThrownItemEntity) {
            ThrownItemEntity thrownItemEntity = (ThrownItemEntity)entity2;
            return thrownItemEntity.getStack();
        }
        if (entity2 instanceof PersistentProjectileEntity) {
            return new ItemStack((ItemConvertible)Items.ARROW);
        }
        return ItemStack.EMPTY;
    }

    private void renderOtherPlayerPredictions(WorldRenderEvent floatTypeEvent2, MinecraftClient mc) {
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            ItemStack stack;
            ProjectileType iiIlilIilI2;
            if (player == mc.player || Math.abs(player.getPitch()) > 89.5f || (iiIlilIilI2 = ProjectileType.fromItem((stack = player.getMainHandStack()).getItem())) == null || !this.predictionsModule.shouldShowTrajectory(PredictionsRenderer.toProjectileKind(iiIlilIilI2), false)) continue;
            for (TrajectoryPrediction trajectoryPrediction : this.trajectoryPredictor.predictHeldItemTrajectories((LivingEntity)player, stack, floatTypeEvent2.getFloatType())) {
                ProjectileImpact illliiiilI2 = trajectoryPrediction.impact();
                if (illliiiilI2 == null) continue;
                this.queueImpactModel(mc, iiIlilIilI2, illliiiilI2, this.predictionsModule.getTrajectoryColor(PredictionsRenderer.toProjectileKind(iiIlilIilI2), false), floatTypeEvent2.getFloatType());
            }
        }
    }

    private void clearFrameData() {
        this.potionImpactMarkers.clear();
        this.landingPredictions.clear();
        this.impactQuads.clear();
    }

    private Vec3d toVec3d() {
        return RenderCamera.position() != null ? RenderCamera.position() : Vec3d.ZERO;
    }

    private boolean isLivingTarget(Entity entity2) {
        if (entity2 == null || !entity2.isAlive() || entity2 == MinecraftClient.getInstance().player) {
            return false;
        }
        return entity2 instanceof PlayerEntity || entity2 instanceof MobEntity;
    }

    private float calculateDistanceFade(double d) {
        float f = (float)MathHelper.clamp((double)((d - 0.5) / 13.5), (double)0.0, (double)1.0);
        return Math.min(f * f * (3.0f - 2.0f * f) * 2.5f, 1.0f);
    }

    private void renderHeldItemPredictions(WorldRenderEvent floatTypeEvent2, MinecraftClient mc) {
        if (Math.abs(mc.player.getPitch()) > 89.5f) {
            return;
        }
        ItemStack stack = PredictionsRenderer.getTrackedHeldStack(this.predictionsModule, (PlayerEntity)mc.player);
        if (stack.isEmpty()) {
            return;
        }
        ProjectileType iiIlilIilI2 = ProjectileType.fromItem(stack.getItem());
        if (iiIlilIilI2 == null) {
            return;
        }
        List<TrajectoryPrediction> list = this.trajectoryPredictor.predictHeldItemTrajectories((LivingEntity)mc.player, stack, floatTypeEvent2.getFloatType());
        boolean bl = !mc.options.getPerspective().isFirstPerson();
        for (TrajectoryPrediction trajectoryPrediction : list) {
            ProjectileImpact illliiiilI2;
            if (this.predictionsModule.showHeldItems()) {
                this.renderTrajectory(trajectoryPrediction, this.predictionsModule.getTrajectoryColor(PredictionsRenderer.toProjectileKind(iiIlilIilI2), true), false, bl ? 10 : 0);
            }
            if ((illliiiilI2 = trajectoryPrediction.impact()) == null) continue;
            if (iiIlilIilI2 == ProjectileType.POTION) {
                Vec3d vec = this.snapPotionImpactToGround(illliiiilI2.position(), illliiiilI2.face());
                this.potionImpactMarkers.add(new PotionImpactMarker(vec, this.predictionsModule.getTrajectoryColor(PredictionsRenderer.toProjectileKind(iiIlilIilI2), true)));
                this.addPotionImpactBox(vec);
                continue;
            }
            this.markHitEntity(illliiiilI2);
            if (this.predictionsModule.shouldShowTrajectory(PredictionsRenderer.toProjectileKind(iiIlilIilI2), true)) {
                this.queueImpactModel(mc, iiIlilIilI2, illliiiilI2, this.predictionsModule.getTrajectoryColor(PredictionsRenderer.toProjectileKind(iiIlilIilI2), true), floatTypeEvent2.getFloatType());
                continue;
            }
            this.renderCircularImpactMarker(illliiiilI2.position(), illliiiilI2.face(), illliiiilI2.hitEntity());
        }
    }

    private double clampUnit(double d) {
        return Math.max(-1.0, Math.min(1.0, d));
    }

    private void updatePotionDepthSampler() {
        if (!this.predictionsModule.isModuleEnabled() || !this.predictionsModule.isPotionsEnabled() || this.potionImpactMarkers.isEmpty()) {
            return;
        }
        if (RenderCamera.position() == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            return;
        }
        this.depthSampler.renderPotionDecals(this.potionImpactMarkers);
    }

    private ModelOrientation matrixToOrientation(double[][] dArray) {
        double d = Math.asin(this.clampUnit(-dArray[1][2]));
        double d2 = Math.atan2(dArray[1][0], dArray[1][1]);
        double d3 = Math.atan2(dArray[0][2], dArray[2][2]);
        return new ModelOrientation((float)Math.toDegrees(d3), (float)Math.toDegrees(d), (float)Math.toDegrees(d2));
    }

    private void queuePotionEffectVolumes(WorldRenderEvent floatTypeEvent2, MinecraftClient mc) {
        if (!this.predictionsModule.isPotionsEnabled() || this.potionImpactMarkers.isEmpty()) {
            return;
        }
        for (PotionImpactMarker marker : this.potionImpactMarkers) {
            this.queuePotionAffectedEntityBoxes(mc, marker.position(), marker.color(), floatTypeEvent2.getFloatType());
        }
    }

    private double[][] multiplyMatrices(double[][] dArray, double[][] dArray2) {
        double[][] dArray3 = new double[3][3];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                dArray3[i][j] = dArray[i][0] * dArray2[0][j] + dArray[i][1] * dArray2[1][j] + dArray[i][2] * dArray2[2][j];
            }
        }
        return dArray3;
    }

    public static PredictionsModule.ProjectileKind toProjectileKind(ProjectileType type) {
        if (type == null) {
            return PredictionsModule.ProjectileKind.UNKNOWN;
        }
        return switch (type) {
            default -> throw new MatchException(null, null);
            case ProjectileType.TRIDENT -> PredictionsModule.ProjectileKind.TRIDENT;
            case ProjectileType.PEARL -> PredictionsModule.ProjectileKind.PEARL;
            case ProjectileType.ARROW -> PredictionsModule.ProjectileKind.ARROW;
            case ProjectileType.CROSSBOW -> PredictionsModule.ProjectileKind.CROSSBOW;
            case ProjectileType.POTION -> PredictionsModule.ProjectileKind.POTION;
            case ProjectileType.ITEM -> PredictionsModule.ProjectileKind.ITEM;
        };
    }

    public static ItemStack getTrackedHeldStack(PredictionsModule module, PlayerEntity player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        Item mainItem = player.getMainHandStack().getItem();
        if (module.isHeldItemCategoryEnabled(PredictionsRenderer.toProjectileKind(ProjectileType.fromItem(mainItem)))) {
            return player.getMainHandStack();
        }
        Item offItem = player.getOffHandStack().getItem();
        if (module.isHeldItemCategoryEnabled(PredictionsRenderer.toProjectileKind(ProjectileType.fromItem(offItem)))) {
            return player.getOffHandStack();
        }
        return ItemStack.EMPTY;
    }

    private boolean shouldTrackEntity(Entity entity) {
        ProjectileType type = ProjectileType.fromEntity(entity);
        if (type == null || type == ProjectileType.ITEM) {
            return false;
        }
        if (entity instanceof TridentEntity) {
            TridentEntity slowEntity = (TridentEntity)entity;
            if (slowEntity.getVelocity().lengthSquared() < 1.0E-7) {
                return false;
            }
        }
        PredictionsModule.ProjectileKind kind = PredictionsRenderer.toProjectileKind(type);
        return switch (kind) {
            default -> throw new MatchException(null, null);
            case PredictionsModule.ProjectileKind.TRIDENT -> this.predictionsModule.isTridentEnabled();
            case PredictionsModule.ProjectileKind.PEARL -> this.predictionsModule.isPearlEnabled();
            case PredictionsModule.ProjectileKind.ARROW -> {
                if (this.predictionsModule.isArrowEnabled() || this.predictionsModule.isCrossbowEnabled()) {
                    yield true;
                }
                yield false;
            }
            case PredictionsModule.ProjectileKind.POTION -> this.predictionsModule.isPotionsEnabled();
            case PredictionsModule.ProjectileKind.CROSSBOW, PredictionsModule.ProjectileKind.ITEM, PredictionsModule.ProjectileKind.UNKNOWN -> false;
        };
    }

    private int getEntityTrajectoryColor(Entity entity, boolean self) {
        ProjectileType type = ProjectileType.fromEntity(entity);
        if (type == ProjectileType.ARROW && !this.predictionsModule.isArrowEnabled() && this.predictionsModule.isCrossbowEnabled()) {
            return this.predictionsModule.getTrajectoryColor(PredictionsModule.ProjectileKind.CROSSBOW, self);
        }
        return this.predictionsModule.getTrajectoryColor(PredictionsRenderer.toProjectileKind(type), self);
    }
}

