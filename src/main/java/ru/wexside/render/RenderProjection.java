package ru.wexside.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector4f;
import ru.wexside.render.RenderFrameState;

public final class RenderProjection {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    private RenderProjection() {
    }

    public static Matrix4f viewProjectionMatrix() {
        return new Matrix4f((Matrix4fc)RenderFrameState.projectionMatrix).mul((Matrix4fc)RenderFrameState.viewMatrix);
    }

    public static Frustum frustum() {
        Frustum frustum = new Frustum(RenderFrameState.viewMatrix, RenderFrameState.projectionMatrix);
        Vec3d camera = RenderProjection.cameraPosition();
        frustum.setPosition(camera.x, camera.y, camera.z);
        return frustum;
    }

    public static boolean isVisible(Entity entity, Frustum frustum) {
        return frustum == null || frustum.isVisible(entity.getBoundingBox());
    }

    public static Vector2f projectEntityLabel(Entity entity, Matrix4f viewProjection) {
        Box box = entity.getBoundingBox();
        return RenderProjection.project((box.minX + box.maxX) * 0.5, box.maxY + 0.25, (box.minZ + box.maxZ) * 0.5, viewProjection);
    }

    public static Vector2f projectEntityCenter(Entity entity, Matrix4f viewProjection) {
        Box box = entity.getBoundingBox();
        return RenderProjection.project((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5, viewProjection);
    }

    public static Vector2f project(Vec3d position) {
        return RenderProjection.project(position.x, position.y, position.z, RenderProjection.viewProjectionMatrix());
    }

    public static Vector2f project(double x, double y, double z, Matrix4f viewProjection) {
        Vec3d camera = RenderProjection.cameraPosition();
        Vector4f clip = new Vector4f((float)(x - camera.x), (float)(y - camera.y), (float)(z - camera.z), 1.0f);
        viewProjection.transform(clip);
        if (clip.w <= 0.001f) {
            return null;
        }
        float normalizedX = clip.x / clip.w;
        float normalizedY = clip.y / clip.w;
        if (normalizedX < -1.2f || normalizedX > 1.2f || normalizedY < -1.2f || normalizedY > 1.2f) {
            return null;
        }
        float width = CLIENT.getWindow().getScaledWidth();
        float height = CLIENT.getWindow().getScaledHeight();
        return new Vector2f((normalizedX * 0.5f + 0.5f) * width, (0.5f - normalizedY * 0.5f) * height);
    }

    public static float tickProgress() {
        return CLIENT.getRenderTickCounter().getTickProgress(false);
    }

    private static Vec3d cameraPosition() {
        Vec3d captured = RenderFrameState.cameraPosition;
        return captured != null ? captured : RenderProjection.CLIENT.gameRenderer.getCamera().getCameraPos();
    }
}

