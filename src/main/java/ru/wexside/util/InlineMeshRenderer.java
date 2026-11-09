package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import ru.wexside.misc.SpatialTransform;
import ru.wexside.render.RenderProjection;
import ru.wexside.util.InlineMesh;
import ru.wexside.util.ModelRenderOptions;

public class InlineMeshRenderer {
    public InlineMeshRenderer(String name) {
    }

    public InlineMeshRenderer(String name, boolean cull) {
    }

    public InlineMeshRenderer(String name, Identifier vertexShader, Identifier fragmentShader, boolean cull) {
    }

    public InlineMeshRenderer(String name, Identifier vertexShader, Identifier fragmentShader) {
    }

    public void renderSpatialInstances(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Vec3d cameraPosition, List<InlineMesh> meshes, List<float[]> positionOverrides, List<Matrix4f> localTransforms, int color, List<SpatialTransform> transforms, ModelRenderOptions style) {
        if (meshes == null || meshes.isEmpty() || transforms == null || transforms.isEmpty()) {
            return;
        }
        Vec3d camera = cameraPosition == null ? Vec3d.ZERO : cameraPosition;
        ModelRenderOptions renderStyle = style == null ? ModelRenderOptions.getAlternateRenderOptions() : style;
        List<SpatialTransform> visibleTransforms = InlineMeshRenderer.visibleTransforms(transforms);
        if (visibleTransforms.isEmpty()) {
            return;
        }
        RenderLayer fillLayer = RenderLayers.debugQuads();
        VertexConsumer fill = consumers.getBuffer(fillLayer);
        for (SpatialTransform transform : visibleTransforms) {
            matrices.push();
            matrices.translate(transform.centerX() - camera.x, transform.centerY() - camera.y, transform.centerZ() - camera.z);
            InlineMeshRenderer.applyRotation(matrices, transform);
            matrices.scale(transform.scaleX(), transform.scaleY(), transform.scaleZ());
            InlineMeshRenderer.emitFilledMeshes(fill, meshes, positionOverrides, localTransforms, (Matrix4fc)matrices.peek().getPositionMatrix(), color);
            matrices.pop();
        }
        consumers.draw(fillLayer);
        InlineMeshRenderer.emitTransformedOutlines(matrices, consumers, camera, meshes, positionOverrides, localTransforms, color, visibleTransforms, renderStyle);
    }

    public void renderMatrixInstances(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Vec3d cameraPosition, List<InlineMesh> meshes, List<float[]> positionOverrides, List<Matrix4f> localTransforms, int color, List<Matrix4f> worldTransforms, ModelRenderOptions style) {
        if (meshes == null || meshes.isEmpty() || worldTransforms == null || worldTransforms.isEmpty()) {
            return;
        }
        Vec3d camera = cameraPosition == null ? Vec3d.ZERO : cameraPosition;
        ModelRenderOptions renderStyle = style == null ? ModelRenderOptions.getAlternateRenderOptions() : style;
        Matrix4f base = new Matrix4f((Matrix4fc)matrices.peek().getPositionMatrix()).translate((float)(-camera.x), (float)(-camera.y), (float)(-camera.z));
        RenderLayer fillLayer = RenderLayers.debugQuads();
        VertexConsumer fill = consumers.getBuffer(fillLayer);
        for (Matrix4f worldTransform : worldTransforms) {
            if (worldTransform == null) continue;
            InlineMeshRenderer.emitFilledMeshes(fill, meshes, positionOverrides, localTransforms, (Matrix4fc)new Matrix4f((Matrix4fc)base).mul((Matrix4fc)worldTransform), color);
        }
        consumers.draw(fillLayer);
        if (!renderStyle.isOutlineEnabled()) {
            return;
        }
        int outlineColor = renderStyle.resolveOutlineColor(color);
        RenderLayer outlineLayer = renderStyle.isDepthTestEnabled() ? RenderLayers.lines() : RenderLayers.linesTranslucent();
        VertexConsumer outline = consumers.getBuffer(outlineLayer);
        for (Matrix4f worldTransform : worldTransforms) {
            if (worldTransform == null) continue;
            InlineMeshRenderer.emitMeshOutlines(outline, meshes, positionOverrides, localTransforms, (Matrix4fc)new Matrix4f((Matrix4fc)base).mul((Matrix4fc)worldTransform), outlineColor, renderStyle.getClampedOutlineLineWidth());
        }
        consumers.draw(outlineLayer);
    }

    private static void emitTransformedOutlines(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Vec3d camera, List<InlineMesh> meshes, List<float[]> positionOverrides, List<Matrix4f> localTransforms, int color, List<SpatialTransform> transforms, ModelRenderOptions style) {
        if (!style.isOutlineEnabled()) {
            return;
        }
        RenderLayer layer = style.isDepthTestEnabled() ? RenderLayers.lines() : RenderLayers.linesTranslucent();
        VertexConsumer outline = consumers.getBuffer(layer);
        int outlineColor = style.resolveOutlineColor(color);
        for (SpatialTransform transform : transforms) {
            matrices.push();
            matrices.translate(transform.centerX() - camera.x, transform.centerY() - camera.y, transform.centerZ() - camera.z);
            InlineMeshRenderer.applyRotation(matrices, transform);
            matrices.scale(transform.scaleX(), transform.scaleY(), transform.scaleZ());
            InlineMeshRenderer.emitMeshOutlines(outline, meshes, positionOverrides, localTransforms, (Matrix4fc)matrices.peek().getPositionMatrix(), outlineColor, style.getClampedOutlineLineWidth());
            matrices.pop();
        }
        consumers.draw(layer);
    }

    private static void applyRotation(MatrixStack matrices, SpatialTransform transform) {
        if (transform.yawDegrees() != 0.0f) {
            matrices.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(transform.yawDegrees()));
        }
        if (transform.pitchDegrees() != 0.0f) {
            matrices.multiply((Quaternionfc)RotationAxis.POSITIVE_X.rotationDegrees(transform.pitchDegrees()));
        }
        if (transform.rollDegrees() != 0.0f) {
            matrices.multiply((Quaternionfc)RotationAxis.POSITIVE_Z.rotationDegrees(transform.rollDegrees()));
        }
    }

    private static List<SpatialTransform> visibleTransforms(List<SpatialTransform> transforms) {
        Frustum frustum = RenderProjection.frustum();
        if (frustum == null) {
            return transforms.stream().filter(Objects::nonNull).toList();
        }
        ArrayList<SpatialTransform> visible = new ArrayList<SpatialTransform>(transforms.size());
        for (SpatialTransform transform : transforms) {
            if (transform == null) continue;
            double radius = Math.max(Math.max(Math.abs(transform.scaleX()), Math.abs(transform.scaleY())), Math.abs(transform.scaleZ()));
            Box bounds = new Box(transform.centerX() - radius, transform.centerY() - radius, transform.centerZ() - radius, transform.centerX() + radius, transform.centerY() + radius, transform.centerZ() + radius);
            if (!frustum.isVisible(bounds)) continue;
            visible.add(transform);
        }
        return visible;
    }

    private static void emitFilledMeshes(VertexConsumer vertices, List<InlineMesh> meshes, List<float[]> positionOverrides, List<Matrix4f> localTransforms, Matrix4fc baseMatrix, int fallbackColor) {
        for (int meshIndex = 0; meshIndex < meshes.size(); ++meshIndex) {
            InlineMesh mesh = meshes.get(meshIndex);
            if (mesh == null) continue;
            float[] positions = InlineMeshRenderer.positions(mesh, positionOverrides, meshIndex);
            int[] indices = mesh.getIntType2();
            if (positions == null || indices == null) continue;
            Matrix4f matrix = InlineMeshRenderer.localMatrix(baseMatrix, localTransforms, meshIndex);
            int index = 0;
            while (index + 2 < indices.length) {
                InlineMeshRenderer.emitVertex(vertices, (Matrix4fc)matrix, positions, mesh.getFloatType4(), indices[index], fallbackColor);
                InlineMeshRenderer.emitVertex(vertices, (Matrix4fc)matrix, positions, mesh.getFloatType4(), indices[index + 1], fallbackColor);
                InlineMeshRenderer.emitVertex(vertices, (Matrix4fc)matrix, positions, mesh.getFloatType4(), indices[index + 2], fallbackColor);
                InlineMeshRenderer.emitVertex(vertices, (Matrix4fc)matrix, positions, mesh.getFloatType4(), indices[index + 2], fallbackColor);
                index += 3;
            }
        }
    }

    private static void emitMeshOutlines(VertexConsumer vertices, List<InlineMesh> meshes, List<float[]> positionOverrides, List<Matrix4f> localTransforms, Matrix4fc baseMatrix, int color, float lineWidth) {
        for (int meshIndex = 0; meshIndex < meshes.size(); ++meshIndex) {
            InlineMesh mesh = meshes.get(meshIndex);
            if (mesh == null) continue;
            float[] positions = InlineMeshRenderer.positions(mesh, positionOverrides, meshIndex);
            int[] edges = mesh.getIntType();
            if (positions == null) continue;
            if (edges == null || edges.length < 2) {
                edges = InlineMeshRenderer.triangleEdges(mesh.getIntType2());
            }
            Matrix4f matrix = InlineMeshRenderer.localMatrix(baseMatrix, localTransforms, meshIndex);
            int index = 0;
            while (index + 1 < edges.length) {
                InlineMeshRenderer.emitLine(vertices, (Matrix4fc)matrix, positions, edges[index], edges[index + 1], color, lineWidth);
                index += 2;
            }
        }
    }

    private static float[] positions(InlineMesh mesh, List<float[]> overrides, int index) {
        float[] override;
        if (overrides != null && index < overrides.size() && (override = overrides.get(index)) != null && override.length >= 3) {
            return override;
        }
        return mesh.getFloatType();
    }

    private static Matrix4f localMatrix(Matrix4fc base, List<Matrix4f> transforms, int index) {
        Matrix4f matrix = new Matrix4f(base);
        if (transforms != null && index < transforms.size() && transforms.get(index) != null) {
            matrix.mul((Matrix4fc)transforms.get(index));
        }
        return matrix;
    }

    private static void emitVertex(VertexConsumer vertices, Matrix4fc matrix, float[] positions, float[] colors, int vertexIndex, int fallbackColor) {
        int offset = vertexIndex * 3;
        if (offset < 0 || offset + 2 >= positions.length) {
            return;
        }
        vertices.vertex(matrix, positions[offset], positions[offset + 1], positions[offset + 2]).color(InlineMeshRenderer.vertexColor(colors, vertexIndex, fallbackColor));
    }

    private static int vertexColor(float[] colors, int vertexIndex, int fallbackColor) {
        if (colors == null) {
            return fallbackColor;
        }
        int offset = vertexIndex * 4;
        if (offset + 3 >= colors.length) {
            return fallbackColor;
        }
        int red = Math.round(Math.clamp(colors[offset], 0.0f, 1.0f) * 255.0f);
        int green = Math.round(Math.clamp(colors[offset + 1], 0.0f, 1.0f) * 255.0f);
        int blue = Math.round(Math.clamp(colors[offset + 2], 0.0f, 1.0f) * 255.0f);
        int alpha = Math.round(Math.clamp(colors[offset + 3], 0.0f, 1.0f) * 255.0f);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static void emitLine(VertexConsumer vertices, Matrix4fc matrix, float[] positions, int first, int second, int color, float width) {
        int a = first * 3;
        int b = second * 3;
        if (a < 0 || b < 0 || a + 2 >= positions.length || b + 2 >= positions.length) {
            return;
        }
        float dx = positions[b] - positions[a];
        float dy = positions[b + 1] - positions[a + 1];
        float dz = positions[b + 2] - positions[a + 2];
        float length = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 0.0f) {
            dx /= length;
            dy /= length;
            dz /= length;
        }
        vertices.vertex(matrix, positions[a], positions[a + 1], positions[a + 2]).color(color).normal(dx, dy, dz).lineWidth(width);
        vertices.vertex(matrix, positions[b], positions[b + 1], positions[b + 2]).color(color).normal(dx, dy, dz).lineWidth(width);
    }

    private static int[] triangleEdges(int[] triangles) {
        if (triangles == null) {
            return new int[0];
        }
        int[] edges = new int[triangles.length / 3 * 6];
        int target = 0;
        int index = 0;
        while (index + 2 < triangles.length) {
            int a = triangles[index];
            int b = triangles[index + 1];
            int c = triangles[index + 2];
            edges[target++] = a;
            edges[target++] = b;
            edges[target++] = b;
            edges[target++] = c;
            edges[target++] = c;
            edges[target++] = a;
            index += 3;
        }
        return edges;
    }
}

