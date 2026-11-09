package ru.wexside.render;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.texture.GlTexture;
import ru.wexside.misc.CaptureFramebuffer;
import ru.wexside.misc.GlowEspSettings;
import ru.wexside.render.GlowOutlineCompositor;

final class GlowEspPreviewEffect {
    private final GlowOutlineCompositor compositor = new GlowOutlineCompositor();
    private final long startedAtMillis = System.currentTimeMillis();
    private SimpleFramebuffer gradientFramebuffer;
    private SimpleFramebuffer horizontalOutlineFramebuffer;
    private SimpleFramebuffer outlineFramebuffer;
    private int width;
    private int height;

    GlowEspPreviewEffect() {
    }

    int render(CaptureFramebuffer sourceFramebuffer, int sourceWidth, int sourceHeight, GlowEspSettings settings) {
        if (sourceFramebuffer == null || settings == null || sourceWidth <= 0 || sourceHeight <= 0) {
            return 0;
        }
        GpuTextureView sourceView = sourceFramebuffer.getColorAttachmentView();
        if (sourceView == null) {
            return 0;
        }
        int effectWidth = Math.max(1, sourceWidth / 2);
        int effectHeight = Math.max(1, sourceHeight / 2);
        this.ensureFramebuffers(effectWidth, effectHeight);
        GlowEspPreviewEffect.clearFramebuffer(this.gradientFramebuffer);
        GlowEspPreviewEffect.clearFramebuffer(this.horizontalOutlineFramebuffer);
        GlowEspPreviewEffect.clearFramebuffer(this.outlineFramebuffer);
        GpuTextureView gradientView = this.gradientFramebuffer.getColorAttachmentView();
        GpuTextureView horizontalOutlineView = this.horizontalOutlineFramebuffer.getColorAttachmentView();
        GpuTextureView outlineView = this.outlineFramebuffer.getColorAttachmentView();
        if (gradientView == null || horizontalOutlineView == null || outlineView == null) {
            return 0;
        }
        float elapsed = (float)(System.currentTimeMillis() - this.startedAtMillis) / 700.0f;
        this.compositor.applyGradient(gradientView, sourceView, effectWidth, effectHeight, settings.getColor(), false, 0.0f, 0.0f, effectWidth, effectHeight, elapsed);
        float outlineSize = Math.max(1.0f, (float)Math.round(settings.getRadius() * 0.5f));
        this.compositor.applyOutline(horizontalOutlineView, gradientView, sourceView, effectWidth, effectHeight, outlineSize, 1.0f, 0.0f, false);
        this.compositor.applyOutline(outlineView, horizontalOutlineView, gradientView, effectWidth, effectHeight, outlineSize, 0.0f, 1.0f, true);
        return GlowEspPreviewEffect.getColorTextureId(this.outlineFramebuffer);
    }

    int getWidth() {
        return this.width;
    }

    int getHeight() {
        return this.height;
    }

    void releaseFramebuffers() {
        if (this.gradientFramebuffer != null) {
            this.gradientFramebuffer.delete();
            this.gradientFramebuffer = null;
        }
        if (this.horizontalOutlineFramebuffer != null) {
            this.horizontalOutlineFramebuffer.delete();
            this.horizontalOutlineFramebuffer = null;
        }
        if (this.outlineFramebuffer != null) {
            this.outlineFramebuffer.delete();
            this.outlineFramebuffer = null;
        }
        this.width = 0;
        this.height = 0;
    }

    private void ensureFramebuffers(int width, int height) {
        if (this.gradientFramebuffer != null && this.width == width && this.height == height) {
            return;
        }
        this.releaseFramebuffers();
        this.gradientFramebuffer = new SimpleFramebuffer("wexside_glow_preview_gradient", width, height, false);
        this.horizontalOutlineFramebuffer = new SimpleFramebuffer("wexside_glow_preview_outline_temp", width, height, false);
        this.outlineFramebuffer = new SimpleFramebuffer("wexside_glow_preview_outline", width, height, false);
        this.width = width;
        this.height = height;
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

    private static void clearFramebuffer(SimpleFramebuffer framebuffer) {
        GpuTexture texture = framebuffer.getColorAttachment();
        if (texture != null) {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.clearColorTexture(texture, 0);
        }
    }
}

