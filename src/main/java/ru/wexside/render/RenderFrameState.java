package ru.wexside.render;

import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class RenderFrameState {
    public static volatile Vec3d cameraPosition;
    public static final Matrix4f projectionMatrix;
    public static final Matrix4f viewMatrix;

    private RenderFrameState() {
    }

    public static void update(Vec3d cameraPosition, Matrix4fc projection, Matrix4fc view) {
        RenderFrameState.cameraPosition = cameraPosition;
        projectionMatrix.set(projection);
        viewMatrix.set(view);
    }

    public static void clear() {
        cameraPosition = null;
        projectionMatrix.identity();
        viewMatrix.identity();
    }

    static {
        projectionMatrix = new Matrix4f();
        viewMatrix = new Matrix4f();
    }
}

