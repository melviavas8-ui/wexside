package ru.wexside.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public final class RenderCamera {
    private RenderCamera() {
    }

    public static Vec3d position() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.gameRenderer.getCamera().getCameraPos();
    }
}

