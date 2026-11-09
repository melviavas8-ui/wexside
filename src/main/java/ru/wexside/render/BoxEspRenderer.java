package ru.wexside.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class BoxEspRenderer {
    private BoxEspRenderer() {
    }

    public static void fill(VertexConsumer consumer, Matrix4f matrix, Vec3d camera, Box box, int color) {
        float x0 = (float)(box.minX - camera.x);
        float y0 = (float)(box.minY - camera.y);
        float z0 = (float)(box.minZ - camera.z);
        float x1 = (float)(box.maxX - camera.x);
        float y1 = (float)(box.maxY - camera.y);
        float z1 = (float)(box.maxZ - camera.z);
        BoxEspRenderer.quad(consumer, matrix, color, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        BoxEspRenderer.quad(consumer, matrix, color, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        BoxEspRenderer.quad(consumer, matrix, color, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        BoxEspRenderer.quad(consumer, matrix, color, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        BoxEspRenderer.quad(consumer, matrix, color, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        BoxEspRenderer.quad(consumer, matrix, color, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
    }

    public static void outline(VertexConsumer consumer, Matrix4f matrix, Vec3d camera, Box box, int color) {
        float x0 = (float)(box.minX - camera.x);
        float y0 = (float)(box.minY - camera.y);
        float z0 = (float)(box.minZ - camera.z);
        float x1 = (float)(box.maxX - camera.x);
        float y1 = (float)(box.maxY - camera.y);
        float z1 = (float)(box.maxZ - camera.z);
        BoxEspRenderer.line(consumer, matrix, color, x0, y0, z0, x1, y0, z0);
        BoxEspRenderer.line(consumer, matrix, color, x1, y0, z0, x1, y0, z1);
        BoxEspRenderer.line(consumer, matrix, color, x1, y0, z1, x0, y0, z1);
        BoxEspRenderer.line(consumer, matrix, color, x0, y0, z1, x0, y0, z0);
        BoxEspRenderer.line(consumer, matrix, color, x0, y1, z0, x1, y1, z0);
        BoxEspRenderer.line(consumer, matrix, color, x1, y1, z0, x1, y1, z1);
        BoxEspRenderer.line(consumer, matrix, color, x1, y1, z1, x0, y1, z1);
        BoxEspRenderer.line(consumer, matrix, color, x0, y1, z1, x0, y1, z0);
        BoxEspRenderer.line(consumer, matrix, color, x0, y0, z0, x0, y1, z0);
        BoxEspRenderer.line(consumer, matrix, color, x1, y0, z0, x1, y1, z0);
        BoxEspRenderer.line(consumer, matrix, color, x1, y0, z1, x1, y1, z1);
        BoxEspRenderer.line(consumer, matrix, color, x0, y0, z1, x0, y1, z1);
    }

    public static void animatedOutline(VertexConsumer consumer, Matrix4f matrix, Vec3d camera, Box box, int color, long timeMillis) {
        float pulse = 0.65f + 0.35f * (float)Math.sin((double)timeMillis * 0.006);
        int alpha = Math.max(0, Math.min(255, Math.round((float)(color >>> 24 & 0xFF) * pulse)));
        BoxEspRenderer.outline(consumer, matrix, camera, box, color & 0xFFFFFF | alpha << 24);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, int color, float ... xyz) {
        for (int index = 0; index < xyz.length; index += 3) {
            consumer.vertex((Matrix4fc)matrix, xyz[index], xyz[index + 1], xyz[index + 2]).color(color);
        }
    }

    private static void line(VertexConsumer consumer, Matrix4f matrix, int color, float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float length = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0.0f) {
            return;
        }
        consumer.vertex((Matrix4fc)matrix, x0, y0, z0).color(color).normal(dx /= length, dy /= length, dz /= length).lineWidth(1.0f);
        consumer.vertex((Matrix4fc)matrix, x1, y1, z1).color(color).normal(dx, dy, dz).lineWidth(1.0f);
    }
}

