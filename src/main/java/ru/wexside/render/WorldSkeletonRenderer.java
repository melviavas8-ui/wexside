package ru.wexside.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.ModelEspSettings;
import ru.wexside.model.esp.EspTargetClassifier;
import ru.wexside.render.RenderCamera;
import ru.wexside.util.EspFeatureRegistry;

public final class WorldSkeletonRenderer {
    private final EspFeatureRegistry espSettings;
    private final BufferAllocator bufferAllocator = new BufferAllocator(65536);
    private final VertexConsumerProvider.Immediate vertexConsumers = VertexConsumerProvider.immediate((BufferAllocator)this.bufferAllocator);

    public WorldSkeletonRenderer(EventBus eventBus, EspFeatureRegistry espSettings) {
        this.espSettings = espSettings;
        eventBus.subscribe(WorldRenderEvent.class, this::render);
    }

    private void render(WorldRenderEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camera = RenderCamera.position();
        if (!this.espSettings.hasEnabledModelEsp() || client.world == null || client.player == null || camera == null) {
            return;
        }
        Matrix4f matrix = new Matrix4f((Matrix4fc)event.getMatrices().peek().getPositionMatrix());
        this.renderSkeletons(matrix, client, camera, event.getFloatType());
    }

    private void renderSkeletons(Matrix4f matrix, MinecraftClient client, Vec3d camera, float tickProgress) {
        RenderLayer layer = RenderLayers.linesTranslucent();
        VertexConsumer vertices = null;
        for (PlayerEntity player : client.world.getPlayers()) {
            ModelEspSettings settings;
            if (player == client.player || !player.isAlive() || (settings = this.settingsFor(player)) == null || !settings.isEnabled() || settings.isModelStyle()) continue;
            if (vertices == null) {
                vertices = this.vertexConsumers.getBuffer(layer);
            }
            WorldSkeletonRenderer.drawPlayer(vertices, matrix, player, camera, tickProgress, settings.getOutlineColor());
        }
        if (vertices != null) {
            this.vertexConsumers.draw(layer);
        }
    }

    private ModelEspSettings settingsFor(PlayerEntity player) {
        return this.espSettings.getModelEspSettings(EspTargetClassifier.relation((Entity)player));
    }

    private static void drawPlayer(VertexConsumer vertices, Matrix4f matrix, PlayerEntity player, Vec3d camera, float tickProgress, int color) {
        Vec3d position = player.getLerpedPos(tickProgress).subtract(camera);
        float yaw = (float)Math.toRadians(-player.getLerpedYaw(tickProgress));
        float sin = (float)Math.sin(yaw);
        float cos = (float)Math.cos(yaw);
        float lean = player.isSneaking() ? 0.18f : 0.0f;
        Vector3f hips = WorldSkeletonRenderer.point(position, 0.0f, 0.72f, lean, sin, cos);
        Vector3f chest = WorldSkeletonRenderer.point(position, 0.0f, 1.35f, lean, sin, cos);
        Vector3f neck = WorldSkeletonRenderer.point(position, 0.0f, 1.52f, lean, sin, cos);
        Vector3f head = WorldSkeletonRenderer.point(position, 0.0f, 1.79f, lean, sin, cos);
        Vector3f leftShoulder = WorldSkeletonRenderer.point(position, 0.34f, 1.38f, lean, sin, cos);
        Vector3f rightShoulder = WorldSkeletonRenderer.point(position, -0.34f, 1.38f, lean, sin, cos);
        Vector3f leftHand = WorldSkeletonRenderer.point(position, 0.5f, 0.82f, lean, sin, cos);
        Vector3f rightHand = WorldSkeletonRenderer.point(position, -0.5f, 0.82f, lean, sin, cos);
        Vector3f leftHip = WorldSkeletonRenderer.point(position, 0.18f, 0.7f, lean, sin, cos);
        Vector3f rightHip = WorldSkeletonRenderer.point(position, -0.18f, 0.7f, lean, sin, cos);
        Vector3f leftFoot = WorldSkeletonRenderer.point(position, 0.2f, 0.02f, 0.0f, sin, cos);
        Vector3f rightFoot = WorldSkeletonRenderer.point(position, -0.2f, 0.02f, 0.0f, sin, cos);
        WorldSkeletonRenderer.line(vertices, matrix, hips, chest, color, 2.0f);
        WorldSkeletonRenderer.line(vertices, matrix, chest, neck, color, 2.0f);
        WorldSkeletonRenderer.line(vertices, matrix, neck, head, color, 2.0f);
        WorldSkeletonRenderer.line(vertices, matrix, leftShoulder, rightShoulder, color, 2.0f);
        WorldSkeletonRenderer.line(vertices, matrix, leftShoulder, leftHand, color, 2.0f);
        WorldSkeletonRenderer.line(vertices, matrix, rightShoulder, rightHand, color, 2.0f);
        WorldSkeletonRenderer.line(vertices, matrix, leftHip, rightHip, color, 2.0f);
        WorldSkeletonRenderer.line(vertices, matrix, leftHip, leftFoot, color, 2.0f);
        WorldSkeletonRenderer.line(vertices, matrix, rightHip, rightFoot, color, 2.0f);
    }

    private static Vector3f point(Vec3d origin, float localX, float localY, float localZ, float sin, float cos) {
        float rotatedX = localX * cos - localZ * sin;
        float rotatedZ = localX * sin + localZ * cos;
        return new Vector3f((float)origin.x + rotatedX, (float)origin.y + localY, (float)origin.z + rotatedZ);
    }

    private static void line(VertexConsumer vertices, Matrix4f matrix, Vector3f from, Vector3f to, int color, float width) {
        Vector3f direction = new Vector3f((Vector3fc)to).sub((Vector3fc)from);
        if (direction.lengthSquared() > 0.0f) {
            direction.normalize();
        }
        vertices.vertex((Matrix4fc)matrix, from.x, from.y, from.z).color(color).normal(direction.x, direction.y, direction.z).lineWidth(width);
        vertices.vertex((Matrix4fc)matrix, to.x, to.y, to.z).color(color).normal(direction.x, direction.y, direction.z).lineWidth(width);
    }
}

