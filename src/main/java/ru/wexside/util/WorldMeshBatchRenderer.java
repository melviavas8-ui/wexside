package ru.wexside.util;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.misc.ModelSurfaceMode;
import ru.wexside.render.ModelRenderBatch;
import ru.wexside.util.InlineMesh;
import ru.wexside.util.ModelRenderOptions;
import ru.wexside.util.ModelRenderQueue;

public final class WorldMeshBatchRenderer {
    public WorldMeshBatchRenderer(String debugName) {
    }

    public void renderQueue(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Vec3d cameraPosition, ModelRenderQueue queue) {
        if (matrices == null || consumers == null || queue == null || queue.isEmpty()) {
            return;
        }
        Vec3d camera = cameraPosition != null ? cameraPosition : Vec3d.ZERO;
        RenderLayer fillLayer = RenderLayers.debugQuads();
        RenderLayer outlineLayer = RenderLayers.lines();
        VertexConsumer fill = consumers.getBuffer(fillLayer);
        VertexConsumer outline = consumers.getBuffer(outlineLayer);
        boolean hasFill = false;
        boolean hasOutline = false;
        for (ModelRenderBatch batch : queue.getBatches()) {
            ModelRenderOptions options = batch.options();
            if (options.getModelSurfaceMode() == ModelSurfaceMode.HIDDEN) continue;
            Matrix4f base = new Matrix4f((Matrix4fc)matrices.peek().getPositionMatrix()).translate((float)(batch.x() - camera.x), (float)(batch.y() - camera.y), (float)(batch.z() - camera.z));
            for (int i = 0; i < batch.meshes().size(); ++i) {
                InlineMesh mesh = batch.meshes().get(i);
                Matrix4f model = new Matrix4f((Matrix4fc)base);
                if (batch.transforms() != null && i < batch.transforms().length && batch.transforms()[i] != null) {
                    model.mul((Matrix4fc)batch.transforms()[i]);
                }
                WorldMeshBatchRenderer.emitTriangles(fill, model, mesh, batch.color());
                hasFill = true;
                if (!options.isOutlineEnabled()) continue;
                WorldMeshBatchRenderer.emitOutline(outline, model, mesh, options.resolveOutlineColor(batch.outlineColor()), options.getClampedOutlineLineWidth());
                hasOutline = true;
            }
        }
        if (hasFill) {
            consumers.draw(fillLayer);
        }
        if (hasOutline) {
            consumers.draw(outlineLayer);
        }
    }

    private static void emitTriangles(VertexConsumer output, Matrix4f matrix, InlineMesh mesh, int color) {
        float[] positions = mesh.getFloatType();
        int[] indices = mesh.getIntType2();
        if (positions == null) {
            return;
        }
        if (indices == null) {
            for (int vertex = 0; vertex < positions.length / 3; ++vertex) {
                WorldMeshBatchRenderer.emitVertex(output, matrix, positions, vertex, color, 1.0f);
            }
            return;
        }
        for (int vertex : indices) {
            WorldMeshBatchRenderer.emitVertex(output, matrix, positions, vertex, color, 1.0f);
        }
    }

    private static void emitOutline(VertexConsumer output, Matrix4f matrix, InlineMesh mesh, int color, float lineWidth) {
        float[] positions = mesh.getFloatType();
        int[] edges = mesh.getIntType();
        if (positions == null || edges == null) {
            return;
        }
        for (int vertex : edges) {
            WorldMeshBatchRenderer.emitVertex(output, matrix, positions, vertex, color, lineWidth);
        }
    }

    private static void emitVertex(VertexConsumer output, Matrix4f matrix, float[] positions, int vertex, int color, float lineWidth) {
        int offset = vertex * 3;
        if (offset < 0 || offset + 2 >= positions.length) {
            return;
        }
        output.vertex((Matrix4fc)matrix, positions[offset], positions[offset + 1], positions[offset + 2]).color(color).lineWidth(lineWidth);
    }
}

