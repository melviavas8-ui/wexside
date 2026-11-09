package ru.wexside.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;

public final class ParticleBillboardRenderer {
    private ParticleBillboardRenderer() {
    }

    public static void draw(double x, double y, double z, float width, float height, int color, Identifier texture, boolean cutout, float rotationDegrees, float u1, float v1, float u2, float v2) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || texture == null) {
            return;
        }
        Vec3d cameraPosition = client.gameRenderer.getCamera().getCameraPos();
        Matrix4f transform = new Matrix4f().translation((float)(x - cameraPosition.x), (float)(y - cameraPosition.y), (float)(z - cameraPosition.z)).rotate((Quaternionfc)client.gameRenderer.getCamera().getRotation()).rotateZ((float)Math.toRadians(rotationDegrees));
        RenderLayer layer = cutout ? RenderLayers.entityCutoutNoCull((Identifier)texture) : RenderLayers.entityTranslucent((Identifier)texture);
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vertices = consumers.getBuffer(layer);
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;
        vertices.vertex((Matrix4fc)transform, -halfWidth, -halfHeight, 0.0f).texture(u1, v2).color(color);
        vertices.vertex((Matrix4fc)transform, halfWidth, -halfHeight, 0.0f).texture(u2, v2).color(color);
        vertices.vertex((Matrix4fc)transform, halfWidth, halfHeight, 0.0f).texture(u2, v1).color(color);
        vertices.vertex((Matrix4fc)transform, -halfWidth, halfHeight, 0.0f).texture(u1, v1).color(color);
        consumers.draw(layer);
    }
}

