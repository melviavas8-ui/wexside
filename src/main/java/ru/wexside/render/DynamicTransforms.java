package ru.wexside.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.misc.SpatialTransform;
import ru.wexside.util.InlineMesh;

public final class DynamicTransforms {
    public void renderTransformedMeshes(List<InlineMesh> meshes, Vec3d cameraPosition, int fillColor, int outlineColor, List<SpatialTransform> transforms, boolean drawOutline, float outlineWidth, float animationTime, int animationMode) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || meshes == null || meshes.isEmpty() || cameraPosition == null || transforms == null || transforms.isEmpty()) {
            return;
        }
        int animatedFillColor = this.animateColor(fillColor, animationTime, animationMode);
        int animatedOutlineColor = this.animateColor(outlineColor, animationTime, animationMode);
        BufferBuilder fillBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        BufferBuilder outlineBuffer = drawOutline ? Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR) : null;
        for (SpatialTransform transform : transforms) {
            Matrix4f modelMatrix = this.createModelMatrix(transform, cameraPosition);
            for (InlineMesh mesh : meshes) {
                this.emitTriangles(fillBuffer, modelMatrix, mesh, animatedFillColor);
                if (outlineBuffer == null) continue;
                this.emitOutline(outlineBuffer, modelMatrix, mesh, animatedOutlineColor);
            }
        }
        this.draw(RenderLayers.debugQuads(), fillBuffer);
        if (outlineBuffer != null) {
            this.draw(RenderLayers.lines(), outlineBuffer);
        }
    }

    private Matrix4f createModelMatrix(SpatialTransform transform, Vec3d cameraPosition) {
        return new Matrix4f().translate((float)(transform.centerX() - cameraPosition.x), (float)(transform.centerY() - cameraPosition.y), (float)(transform.centerZ() - cameraPosition.z)).rotateY((float)Math.toRadians(transform.yawDegrees())).rotateX((float)Math.toRadians(transform.pitchDegrees())).rotateZ((float)Math.toRadians(transform.rollDegrees())).scale(transform.scaleX(), transform.scaleY(), transform.scaleZ());
    }

    private void emitTriangles(BufferBuilder output, Matrix4f matrix, InlineMesh mesh, int color) {
        float[] positions = mesh.getFloatType();
        if (positions == null || positions.length < 3) {
            return;
        }
        int[] indices = mesh.getIntType2();
        float[] vertexColors = mesh.getFloatType4();
        if (indices == null) {
            for (int vertex = 0; vertex < positions.length / 3; ++vertex) {
                this.emitVertex(output, matrix, positions, vertex, color, vertexColors);
            }
            return;
        }
        for (int vertex : indices) {
            this.emitVertex(output, matrix, positions, vertex, color, vertexColors);
        }
    }

    private void emitOutline(BufferBuilder output, Matrix4f matrix, InlineMesh mesh, int color) {
        float[] positions = mesh.getFloatType();
        int[] edges = mesh.getIntType();
        if (positions == null || edges == null) {
            return;
        }
        for (int vertex : edges) {
            this.emitVertex(output, matrix, positions, vertex, color, null);
        }
    }

    private void emitVertex(BufferBuilder output, Matrix4f matrix, float[] positions, int vertex, int color, float[] vertexColors) {
        int positionOffset = vertex * 3;
        if (positionOffset < 0 || positionOffset + 2 >= positions.length) {
            return;
        }
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        int alpha = color >>> 24;
        int colorOffset = vertex * 4;
        if (vertexColors != null && colorOffset + 3 < vertexColors.length) {
            red = this.multiplyColor(red, vertexColors[colorOffset]);
            green = this.multiplyColor(green, vertexColors[colorOffset + 1]);
            blue = this.multiplyColor(blue, vertexColors[colorOffset + 2]);
            alpha = this.multiplyColor(alpha, vertexColors[colorOffset + 3]);
        }
        output.vertex((Matrix4fc)matrix, positions[positionOffset], positions[positionOffset + 1], positions[positionOffset + 2]).color(red, green, blue, alpha);
    }

    private int multiplyColor(int channel, float multiplier) {
        return Math.clamp((long)Math.round((float)channel * multiplier), 0, 255);
    }

    private int animateColor(int color, float time, int mode) {
        if (mode <= 0) {
            return color;
        }
        float frequency = switch (mode) {
            case 1 -> 0.65f;
            case 2 -> 1.15f;
            case 3 -> 1.75f;
            case 4 -> 0.9f;
            default -> 2.25f;
        };
        float opacity = 0.72f + 0.28f * (float)Math.sin(time * frequency);
        int alpha = this.multiplyColor(color >>> 24, opacity);
        return color & 0xFFFFFF | alpha << 24;
    }

    private void draw(RenderLayer layer, BufferBuilder buffer) {
        layer.draw(buffer.end());
    }
}

