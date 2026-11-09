package ru.wexside.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.AbstractTexture;

public final class FramebufferTextureAdapter
extends AbstractTexture
implements AutoCloseable {
    @Override
    public void close() {
        this.glTexture = null;
        this.glTextureView = null;
        this.sampler = null;
    }

    public void bind(Framebuffer framebuffer) {
        this.glTexture = framebuffer.getColorAttachment();
        this.glTextureView = framebuffer.getColorAttachmentView();
        this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
    }
}

