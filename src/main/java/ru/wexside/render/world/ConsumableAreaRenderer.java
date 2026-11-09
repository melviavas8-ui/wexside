package ru.wexside.render.world;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import ru.wexside.util.ColorUtils;

public final class ConsumableAreaRenderer {
    private static final int CYLINDER_SEGMENTS = 64;

    private ConsumableAreaRenderer() {
    }

    public static void renderBox(Box box, int fillStart, int fillEnd, int outlineStart, int outlineEnd, long timeMillis) {
        RenderFrame frame = ConsumableAreaRenderer.beginFrame();
        if (frame == null) {
            return;
        }
        int fillColor = ConsumableAreaRenderer.animatedColor(fillStart, fillEnd, timeMillis);
        int outlineColor = ConsumableAreaRenderer.animatedColor(outlineStart, outlineEnd, timeMillis);
        BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        ConsumableAreaRenderer.filledBox(fill, frame, box, fillColor);
        ConsumableAreaRenderer.outlinedBox(lines, frame, box, outlineColor);
        RenderLayers.debugQuads().draw(fill.end());
        RenderLayers.lines().draw(lines.end());
    }

    public static void renderCylinder(double x, double y, double z, double radius, double height, int fillStart, int fillEnd, int outlineStart, int outlineEnd, long timeMillis) {
        RenderFrame frame = ConsumableAreaRenderer.beginFrame();
        if (frame == null) {
            return;
        }
        int fillColor = ConsumableAreaRenderer.animatedColor(fillStart, fillEnd, timeMillis);
        int outlineColor = ConsumableAreaRenderer.animatedColor(outlineStart, outlineEnd, timeMillis);
        BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int segment = 0; segment < 64; ++segment) {
            double a = Math.PI * 2 * (double)segment / 64.0;
            double b = Math.PI * 2 * (double)(segment + 1) / 64.0;
            double x1 = x + Math.cos(a) * radius;
            double z1 = z + Math.sin(a) * radius;
            double x2 = x + Math.cos(b) * radius;
            double z2 = z + Math.sin(b) * radius;
            ConsumableAreaRenderer.quad(fill, frame, x, y, z, x1, y, z1, x2, y, z2, x, y, z, fillColor);
            ConsumableAreaRenderer.quad(fill, frame, x, y + height, z, x, y + height, z, x2, y + height, z2, x1, y + height, z1, fillColor);
            ConsumableAreaRenderer.quad(fill, frame, x1, y, z1, x1, y + height, z1, x2, y + height, z2, x2, y, z2, fillColor);
            ConsumableAreaRenderer.line(lines, frame, x1, y, z1, x2, y, z2, outlineColor);
            ConsumableAreaRenderer.line(lines, frame, x1, y + height, z1, x2, y + height, z2, outlineColor);
            if ((segment & 7) != 0) continue;
            ConsumableAreaRenderer.line(lines, frame, x1, y, z1, x1, y + height, z1, outlineColor);
        }
        RenderLayers.debugQuads().draw(fill.end());
        RenderLayers.lines().draw(lines.end());
    }

    private static RenderFrame beginFrame() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }
        Camera camera = client.gameRenderer.getCamera();
        return new RenderFrame(new Matrix4f().rotation((Quaternionfc)camera.getRotation()), camera.getCameraPos());
    }

    private static int animatedColor(int start, int end, long timeMillis) {
        float phase = (float)((Math.sin((double)timeMillis * 0.003) + 1.0) * 0.5);
        return ColorUtils.lerp(start, end, phase);
    }

    private static void filledBox(BufferBuilder out, RenderFrame frame, Box box, int color) {
        ConsumableAreaRenderer.quad(out, frame, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
        ConsumableAreaRenderer.quad(out, frame, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ, color);
        ConsumableAreaRenderer.quad(out, frame, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ, color);
        ConsumableAreaRenderer.quad(out, frame, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
        ConsumableAreaRenderer.quad(out, frame, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, color);
        ConsumableAreaRenderer.quad(out, frame, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ, color);
    }

    private static void outlinedBox(BufferBuilder out, RenderFrame frame, Box box, int color) {
        ConsumableAreaRenderer.line(out, frame, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, color);
        ConsumableAreaRenderer.line(out, frame, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, color);
        ConsumableAreaRenderer.line(out, frame, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
        ConsumableAreaRenderer.line(out, frame, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, color);
        ConsumableAreaRenderer.line(out, frame, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, color);
        ConsumableAreaRenderer.line(out, frame, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
        ConsumableAreaRenderer.line(out, frame, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
        ConsumableAreaRenderer.line(out, frame, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, color);
        ConsumableAreaRenderer.line(out, frame, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, color);
        ConsumableAreaRenderer.line(out, frame, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, color);
        ConsumableAreaRenderer.line(out, frame, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, color);
        ConsumableAreaRenderer.line(out, frame, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
    }

    private static void quad(BufferBuilder out, RenderFrame frame, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, int color) {
        ConsumableAreaRenderer.vertex(out, frame, x1, y1, z1, color);
        ConsumableAreaRenderer.vertex(out, frame, x2, y2, z2, color);
        ConsumableAreaRenderer.vertex(out, frame, x3, y3, z3, color);
        ConsumableAreaRenderer.vertex(out, frame, x4, y4, z4, color);
    }

    private static void line(BufferBuilder out, RenderFrame frame, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        ConsumableAreaRenderer.vertex(out, frame, x1, y1, z1, color);
        ConsumableAreaRenderer.vertex(out, frame, x2, y2, z2, color);
    }

    private static void vertex(BufferBuilder out, RenderFrame frame, double x, double y, double z, int color) {
        Vec3d camera = frame.cameraPosition();
        out.vertex((Matrix4fc)frame.matrix(), (float)(x - camera.x), (float)(y - camera.y), (float)(z - camera.z)).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24 & 0xFF);
    }

    private record RenderFrame(Matrix4f matrix, Vec3d cameraPosition) {
    }
}

