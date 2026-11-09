package ru.wexside.render;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;

public final class HudIconRenderer {
    private static final float VANILLA_ITEM_SIZE = 16.0f;

    private HudIconRenderer() {
    }

    public static void drawItem(DrawContext context, ItemStack stack, int x, int y, int size) {
        if (context == null || stack == null || stack.isEmpty() || size <= 0) {
            return;
        }
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate((float)x, (float)y);
        float scale = (float)size / 16.0f;
        matrices.scale(scale, scale);
        context.drawItem(stack, 0, 0);
        matrices.popMatrix();
    }

    public static void drawPlayerHead(DrawContext context, SkinTextures skin, int x, int y, int size) {
        if (context == null || skin == null || skin.body() == null || size <= 0) {
            return;
        }
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin.body().texturePath(), x, y, 8.0f, 8.0f, size, size, 8, 8, 64, 64);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin.body().texturePath(), x, y, 40.0f, 8.0f, size, size, 8, 8, 64, 64);
    }
}

