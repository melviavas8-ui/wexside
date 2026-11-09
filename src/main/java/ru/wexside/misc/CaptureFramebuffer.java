package ru.wexside.misc;

import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.GlTexture;

public final class CaptureFramebuffer
extends Framebuffer {
    public CaptureFramebuffer() {
        super("wex/vanilla-capture", true);
    }

    public int getColorAttachmentGlId() {
        int n;
        GpuTexture gpuTexture = this.getColorAttachment();
        if (gpuTexture instanceof GlTexture) {
            GlTexture iIiIIllIl2 = (GlTexture)gpuTexture;
            n = iIiIIllIl2.getGlId();
        } else {
            n = 0;
        }
        return n;
    }

    public void ensureSize(int n, int n2) {
        if (n <= 0 || n2 <= 0) {
            return;
        }
        if (this.textureWidth == n && this.textureHeight == n2 && this.getColorAttachment() != null) {
            return;
        }
        this.resize(n, n2);
    }
}

