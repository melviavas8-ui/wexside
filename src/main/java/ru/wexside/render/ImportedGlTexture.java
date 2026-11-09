package ru.wexside.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.GlTextureView;

public final class ImportedGlTexture
extends GlTexture {
    private static final int USAGE_SAMPLED_AND_COPY_SOURCE = 5;
    private final GpuTextureView view = new ImportedGlTextureView(this);

    private ImportedGlTexture(int glId, int width, int height) {
        super(5, "wexside/imported_texture", TextureFormat.RGBA8, width, height, 1, 1, glId);
    }

    public static ImportedGlTexture wrap(int glId, int width, int height) {
        if (glId <= 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid imported texture");
        }
        return new ImportedGlTexture(glId, width, height);
    }

    public GpuTextureView view() {
        return this.view;
    }

    public void close() {
        this.view.close();
        super.close();
    }

    private static final class ImportedGlTextureView
    extends GlTextureView {
        private ImportedGlTextureView(GlTexture texture) {
            super(texture, 0, 1);
        }
    }
}

