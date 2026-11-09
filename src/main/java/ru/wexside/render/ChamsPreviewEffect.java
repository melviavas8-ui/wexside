package ru.wexside.render;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.texture.GlTexture;
import ru.wexside.misc.CaptureFramebuffer;
import ru.wexside.render.ChamsCompositor;
import ru.wexside.render.ChamsSettings;

final class ChamsPreviewEffect {
    private final ChamsCompositor compositor = new ChamsCompositor();
    private final long startedAtMillis = System.currentTimeMillis();
    private SimpleFramebuffer sceneFramebuffer;
    private SimpleFramebuffer effectFramebuffer;
    private int width;
    private int height;

    ChamsPreviewEffect() {
    }

    int render(CaptureFramebuffer sourceFramebuffer, int width, int height, int previewSize, ChamsSettings settings) {
        if (sourceFramebuffer == null || settings == null || width <= 0 || height <= 0 || previewSize <= 0 || !settings.isVisibleFillEnabled() && !settings.isHiddenFillEnabled()) {
            return 0;
        }
        GpuTextureView sourceColorView = sourceFramebuffer.getColorAttachmentView();
        GpuTextureView sourceDepthView = sourceFramebuffer.getDepthAttachmentView();
        if (sourceColorView == null || sourceDepthView == null) {
            return 0;
        }
        this.ensureFramebuffers(width, height);
        ChamsPreviewEffect.clearFramebuffer(this.effectFramebuffer);
        ChamsPreviewEffect.clearFramebuffer(this.sceneFramebuffer);
        GpuTextureView sceneDepthView = this.sceneFramebuffer.getDepthAttachmentView();
        GpuTextureView effectView = this.effectFramebuffer.getColorAttachmentView();
        if (sceneDepthView == null || effectView == null) {
            return 0;
        }
        float elapsed = (float)(System.currentTimeMillis() - this.startedAtMillis) / 700.0f;
        this.compositor.composite(effectView, sourceColorView, sourceDepthView, sourceColorView, sceneDepthView, width, height, settings.getVisibleColor(), settings.getHiddenColor(), settings.isVisibleFillEnabled(), settings.isHiddenFillEnabled(), settings.getMaterialModeIndex(), 0.0f, height - previewSize, previewSize, previewSize, elapsed);
        return ChamsPreviewEffect.getColorTextureId(this.effectFramebuffer);
    }

    void releaseFramebuffers() {
        if (this.sceneFramebuffer != null) {
            this.sceneFramebuffer.delete();
            this.sceneFramebuffer = null;
        }
        if (this.effectFramebuffer != null) {
            this.effectFramebuffer.delete();
            this.effectFramebuffer = null;
        }
        this.width = 0;
        this.height = 0;
    }

    private void ensureFramebuffers(int width, int height) {
        if (this.sceneFramebuffer != null && this.effectFramebuffer != null && this.width == width && this.height == height) {
            return;
        }
        this.releaseFramebuffers();
        this.sceneFramebuffer = new SimpleFramebuffer("wexside_chams_preview_scene", width, height, true);
        this.effectFramebuffer = new SimpleFramebuffer("wexside_chams_preview_effect", width, height, false);
        this.width = width;
        this.height = height;
    }

    private static void clearFramebuffer(SimpleFramebuffer framebuffer) {
        GpuTexture colorTexture = framebuffer.getColorAttachment();
        if (colorTexture == null) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        GpuTexture depthTexture = framebuffer.getDepthAttachment();
        if (depthTexture != null) {
            commandEncoder.clearColorAndDepthTextures(colorTexture, 0, depthTexture, 1.0);
        } else {
            commandEncoder.clearColorTexture(colorTexture, 0);
        }
    }

    private static int getColorTextureId(SimpleFramebuffer framebuffer) {
        int n;
        GpuTexture texture = framebuffer.getColorAttachment();
        if (texture instanceof GlTexture) {
            GlTexture glTexture = (GlTexture)texture;
            n = glTexture.getGlId();
        } else {
            n = 0;
        }
        return n;
    }
}

