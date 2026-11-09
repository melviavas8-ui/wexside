package ru.wexside.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import ru.wexside.misc.CaptureFramebuffer;
import ru.wexside.misc.ModelEspSettings;
import ru.wexside.render.OffscreenRenderManager;

public final class SkeletonPreviewRenderer {
    private final CaptureFramebuffer framebuffer = new CaptureFramebuffer();

    public void close() {
        this.framebuffer.delete();
    }

    public int render(OffscreenRenderManager pipeline, PlayerEntity player, int framebufferWidth, int framebufferHeight, float centerX, float centerY, float scale, float verticalOffset, float bodyYaw, ModelEspSettings settings) {
        if (pipeline == null || player == null || settings == null || framebufferWidth <= 0 || framebufferHeight <= 0) {
            return 0;
        }
        int color = settings.getOutlineColor();
        pipeline.captureModel(this.framebuffer, framebufferWidth, framebufferHeight, centerX, centerY, scale, verticalOffset, bodyYaw, (matrices, consumers) -> SkeletonPreviewRenderer.drawSkeleton(matrices, consumers, player, color, settings.isModelStyle()));
        return this.framebuffer.getColorAttachmentGlId();
    }

    private static void drawSkeleton(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, PlayerEntity player, int color, boolean modelStyle) {
        RenderLayer layer = RenderLayers.linesTranslucent();
        VertexConsumer vertices = consumers.getBuffer(layer);
        Matrix4f matrix = new Matrix4f((Matrix4fc)matrices.peek().getPositionMatrix());
        float bodyLean = player.isSneaking() ? 0.16f : 0.0f;
        Vector3f hips = new Vector3f(0.0f, 0.72f, bodyLean);
        Vector3f chest = new Vector3f(0.0f, 1.35f, bodyLean);
        Vector3f neck = new Vector3f(0.0f, 1.52f, bodyLean);
        Vector3f head = new Vector3f(0.0f, 1.78f, bodyLean);
        Vector3f leftShoulder = new Vector3f(0.34f, 1.38f, bodyLean);
        Vector3f rightShoulder = new Vector3f(-0.34f, 1.38f, bodyLean);
        Vector3f leftHand = new Vector3f(0.48f, 0.82f, bodyLean);
        Vector3f rightHand = new Vector3f(-0.48f, 0.82f, bodyLean);
        Vector3f leftHip = new Vector3f(0.18f, 0.7f, bodyLean);
        Vector3f rightHip = new Vector3f(-0.18f, 0.7f, bodyLean);
        Vector3f leftFoot = new Vector3f(0.2f, 0.02f, 0.0f);
        Vector3f rightFoot = new Vector3f(-0.2f, 0.02f, 0.0f);
        SkeletonPreviewRenderer.line(vertices, matrix, hips, chest, color);
        SkeletonPreviewRenderer.line(vertices, matrix, chest, neck, color);
        SkeletonPreviewRenderer.line(vertices, matrix, neck, head, color);
        SkeletonPreviewRenderer.line(vertices, matrix, leftShoulder, rightShoulder, color);
        SkeletonPreviewRenderer.line(vertices, matrix, leftShoulder, leftHand, color);
        SkeletonPreviewRenderer.line(vertices, matrix, rightShoulder, rightHand, color);
        SkeletonPreviewRenderer.line(vertices, matrix, leftHip, rightHip, color);
        SkeletonPreviewRenderer.line(vertices, matrix, leftHip, leftFoot, color);
        SkeletonPreviewRenderer.line(vertices, matrix, rightHip, rightFoot, color);
        if (modelStyle) {
            SkeletonPreviewRenderer.line(vertices, matrix, leftShoulder, leftHip, color);
            SkeletonPreviewRenderer.line(vertices, matrix, rightShoulder, rightHip, color);
            SkeletonPreviewRenderer.line(vertices, matrix, leftHand, new Vector3f(0.58f, 0.55f, bodyLean), color);
            SkeletonPreviewRenderer.line(vertices, matrix, rightHand, new Vector3f(-0.58f, 0.55f, bodyLean), color);
        }
        consumers.draw(layer);
    }

    private static void line(VertexConsumer vertices, Matrix4f matrix, Vector3f from, Vector3f to, int color) {
        Vector3f direction = new Vector3f((Vector3fc)to).sub((Vector3fc)from);
        if (direction.lengthSquared() > 0.0f) {
            direction.normalize();
        }
        vertices.vertex((Matrix4fc)matrix, from.x, from.y, from.z).color(color).normal(direction.x, direction.y, direction.z).lineWidth(2.0f);
        vertices.vertex((Matrix4fc)matrix, to.x, to.y, to.z).color(color).normal(direction.x, direction.y, direction.z).lineWidth(2.0f);
    }
}

