package ru.wexside.render.world;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;

public final class WorldLineRenderer {
    private WorldLineRenderer() {
    }

    public static void draw(List<Segment> segments) {
        if (segments == null || segments.isEmpty()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPosition = camera.getCameraPos();
        Matrix4f matrix = new Matrix4f().rotation((Quaternionfc)camera.getRotation());
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (Segment segment : segments) {
            WorldLineRenderer.vertex(buffer, matrix, cameraPosition, segment.start(), segment.startColor());
            WorldLineRenderer.vertex(buffer, matrix, cameraPosition, segment.end(), segment.endColor());
        }
        RenderLayers.lines().draw(buffer.end());
    }

    public static void drawEntityBox(Entity entity, int color) {
        if (entity == null) {
            return;
        }
        Box box = entity.getBoundingBox().expand(0.025);
        Vec3d p000 = new Vec3d(box.minX, box.minY, box.minZ);
        Vec3d p001 = new Vec3d(box.minX, box.minY, box.maxZ);
        Vec3d p010 = new Vec3d(box.minX, box.maxY, box.minZ);
        Vec3d p011 = new Vec3d(box.minX, box.maxY, box.maxZ);
        Vec3d p100 = new Vec3d(box.maxX, box.minY, box.minZ);
        Vec3d p101 = new Vec3d(box.maxX, box.minY, box.maxZ);
        Vec3d p110 = new Vec3d(box.maxX, box.maxY, box.minZ);
        Vec3d p111 = new Vec3d(box.maxX, box.maxY, box.maxZ);
        ArrayList<Segment> lines = new ArrayList<Segment>(12);
        WorldLineRenderer.add(lines, p000, p001, color);
        WorldLineRenderer.add(lines, p001, p101, color);
        WorldLineRenderer.add(lines, p101, p100, color);
        WorldLineRenderer.add(lines, p100, p000, color);
        WorldLineRenderer.add(lines, p010, p011, color);
        WorldLineRenderer.add(lines, p011, p111, color);
        WorldLineRenderer.add(lines, p111, p110, color);
        WorldLineRenderer.add(lines, p110, p010, color);
        WorldLineRenderer.add(lines, p000, p010, color);
        WorldLineRenderer.add(lines, p001, p011, color);
        WorldLineRenderer.add(lines, p100, p110, color);
        WorldLineRenderer.add(lines, p101, p111, color);
        WorldLineRenderer.draw(lines);
    }

    private static void add(List<Segment> lines, Vec3d start, Vec3d end, int color) {
        lines.add(new Segment(start, end, color, color));
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3d camera, Vec3d point, int color) {
        buffer.vertex((Matrix4fc)matrix, (float)(point.x - camera.x), (float)(point.y - camera.y), (float)(point.z - camera.z)).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24 & 0xFF);
    }

    public record Segment(Vec3d start, Vec3d end, int startColor, int endColor) {
    }
}

