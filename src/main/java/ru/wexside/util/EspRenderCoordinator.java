package ru.wexside.util;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.ChamsRenderer;
import ru.wexside.misc.GlowEspRenderer;
import ru.wexside.misc.WorldBoxSettings;
import ru.wexside.model.esp.EspTargetClassifier;
import ru.wexside.model.esp.EspTargetType;
import ru.wexside.render.BoxEspEntry;
import ru.wexside.render.BoxEspRenderer;
import ru.wexside.render.RenderCamera;
import ru.wexside.render.RenderProjection;
import ru.wexside.render.WorldSkeletonRenderer;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.EspFeatureRegistry;
import ru.wexside.util.NameplateRenderer;
import ru.wexside.util.ServerItemCooldownOverlay;

public final class EspRenderCoordinator {
    private final WorldSkeletonRenderer worldSkeletonRenderer;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ChamsRenderer chamsRenderer;
    private final NameplateRenderer nameplateRenderer;
    private final GlowEspRenderer glowEspRenderer;
    private final EspFeatureRegistry espFeatures;
    private static final float BOX_FILL_ALPHA = 0.25f;
    private final ServerItemCooldownOverlay serverItemCooldownOverlay;

    public EspRenderCoordinator(EventBus eventBus, EspFeatureRegistry espFeatures) {
        this.espFeatures = espFeatures;
        eventBus.subscribe(WorldRenderEvent.class, this::renderWorldBoxes);
        this.nameplateRenderer = new NameplateRenderer(eventBus, espFeatures);
        this.serverItemCooldownOverlay = new ServerItemCooldownOverlay(eventBus, espFeatures);
        this.chamsRenderer = new ChamsRenderer(eventBus, espFeatures);
        this.glowEspRenderer = new GlowEspRenderer(eventBus, espFeatures);
        this.worldSkeletonRenderer = new WorldSkeletonRenderer(eventBus, espFeatures);
    }

    private void renderWorldBoxes(WorldRenderEvent event) {
        if (!this.espFeatures.hasEnabledWorldBox()) {
            return;
        }
        ClientPlayerEntity player2 = this.mc.player;
        ClientWorld world2 = this.mc.world;
        Vec3d vec = RenderCamera.position();
        if (player2 == null || world2 == null || vec == null) {
            return;
        }
        Frustum frustum2 = RenderProjection.frustum();
        float tickProgress = event.getFloatType();
        ArrayList<BoxEspEntry> entries = new ArrayList<BoxEspEntry>();
        for (Entity entity : world2.getEntities()) {
            WorldBoxSettings settings;
            EspTargetType targetType;
            if (!entity.isAlive() || (targetType = EspTargetClassifier.targetType(entity, player2)) == null || (settings = this.espFeatures.getWorldBoxSettings(targetType, EspTargetClassifier.relation(entity))) == null || !settings.isEnabled() || !RenderProjection.isVisible(entity, frustum2)) continue;
            Box box = this.getExpandedBoundingBox(entity, tickProgress, this.getExpansion(settings.getScale()));
            entries.add(new BoxEspEntry(box, settings.getColor(), settings.isDottedStyle(), settings.isDepthTestEnabled()));
        }
        if (entries.isEmpty()) {
            return;
        }
        Matrix4f matrix = new Matrix4f((Matrix4fc)event.getMatrices().peek().getPositionMatrix());
        long animationTime = System.currentTimeMillis();
        this.renderEntries(entries, true, matrix, vec, animationTime);
        this.renderEntries(entries, false, matrix, vec, animationTime);
    }

    private void renderEntries(List<BoxEspEntry> entries, boolean depthTest, Matrix4f matrix, Vec3d cameraPosition, long animationTime) {
        if (entries.stream().noneMatch(entry -> entry.depthTest() == depthTest)) {
            return;
        }
        RenderLayer fillLayer = RenderLayers.debugQuads();
        BufferBuilder fillBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (BoxEspEntry entry2 : entries) {
            if (entry2.depthTest() != depthTest) continue;
            BoxEspRenderer.fill((VertexConsumer)fillBuffer, matrix, cameraPosition, entry2.box(), ColorUtils.multiplyAlpha(entry2.color(), 0.25f));
        }
        BuiltBuffer builtFill = fillBuffer.endNullable();
        if (builtFill != null) {
            fillLayer.draw(builtFill);
        }
        RenderLayer outlineLayer = depthTest ? RenderLayers.linesTranslucent() : RenderLayers.lines();
        BufferBuilder outlineBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (BoxEspEntry entry3 : entries) {
            if (entry3.depthTest() != depthTest) continue;
            if (entry3.animated()) {
                BoxEspRenderer.animatedOutline((VertexConsumer)outlineBuffer, matrix, cameraPosition, entry3.box(), entry3.color(), animationTime);
                continue;
            }
            BoxEspRenderer.outline((VertexConsumer)outlineBuffer, matrix, cameraPosition, entry3.box(), entry3.color());
        }
        BuiltBuffer builtOutline = outlineBuffer.endNullable();
        if (builtOutline != null) {
            outlineLayer.draw(builtOutline);
        }
    }

    private Box getExpandedBoundingBox(Entity entity, float tickProgress, float expansion) {
        Vec3d interpolatedPosition = entity.getLerpedPos(tickProgress);
        Box box = entity.getBoundingBox().offset(interpolatedPosition.x - entity.getX(), interpolatedPosition.y - entity.getY(), interpolatedPosition.z - entity.getZ());
        return expansion > 0.0f ? box.expand((double)expansion) : box;
    }

    private float getExpansion(int scale) {
        return (float)(Math.max(1, scale) - 1) * 0.05f;
    }
}

