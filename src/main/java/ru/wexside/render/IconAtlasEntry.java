package ru.wexside.render;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL30;
import ru.wexside.render.IconAtlas;

public final class IconAtlasEntry {
    private int framebufferId;
    private boolean baked;
    private boolean nearestFiltering;
    private int iconSize;
    private int atlasGeneration = -1;
    private int atlasSlotIndex = -1;
    private IconAtlas atlas;
    private int textureId;
    private float renderScale = -1.0f;

    public IconAtlasEntry() {
    }

    public IconAtlasEntry(IconAtlas renderPipeline12) {
        this.atlas = renderPipeline12;
    }

    public IconAtlasEntry(boolean bl) {
        this.nearestFiltering = bl;
    }

    public boolean needsBake(float f) {
        return !this.baked || f != this.renderScale;
    }

    public void invalidateBake() {
        this.baked = false;
        this.renderScale = -1.0f;
    }

    public boolean isAllocated() {
        if (this.atlas != null) {
            return this.hasCurrentAtlasSlot() && this.atlas.getTextureId() != 0 && this.iconSize > 0;
        }
        return this.textureId != 0 && this.iconSize > 0;
    }

    public int getIconSize() {
        return this.iconSize;
    }

    public int getAtlasY() {
        return this.atlas != null && this.hasCurrentAtlasSlot() ? this.atlas.getSlotY(this.atlasSlotIndex) : 0;
    }

    public int getTextureSize() {
        return this.atlas != null ? this.atlas.getTextureSize() : this.iconSize;
    }

    public void close() {
        if (this.atlas != null) {
            if (this.hasCurrentAtlasSlot()) {
                this.atlas.releaseSlot(this.atlasSlotIndex);
            }
            this.atlasSlotIndex = -1;
            this.atlasGeneration = -1;
            this.iconSize = 0;
            this.baked = false;
            this.renderScale = -1.0f;
            return;
        }
        if (this.framebufferId != 0) {
            GL30.glDeleteFramebuffers((int)this.framebufferId);
            this.framebufferId = 0;
        }
        if (this.textureId != 0) {
            GL30.glDeleteTextures((int)this.textureId);
            this.textureId = 0;
        }
        this.iconSize = 0;
    }

    public int getTextureId() {
        return this.atlas != null ? this.atlas.getTextureId() : this.textureId;
    }

    public int getAtlasX() {
        return this.atlas != null && this.hasCurrentAtlasSlot() ? this.atlas.getSlotX(this.atlasSlotIndex) : 0;
    }

    public float getMaxU() {
        return this.atlas == null ? 1.0f : (float)(this.getAtlasX() + this.iconSize) / (float)this.atlas.getTextureSize();
    }

    public boolean isBaked() {
        return this.baked;
    }

    private void allocateAtlasRegion(int n) {
        if (n <= 0) {
            return;
        }
        this.atlas.ensureIconSize(n);
        if (this.atlasSlotIndex >= 0 && this.atlasGeneration != this.atlas.getGeneration()) {
            this.atlasSlotIndex = -1;
        }
        if (this.atlasSlotIndex < 0) {
            this.atlasSlotIndex = this.atlas.allocateSlot();
            if (this.atlasSlotIndex < 0) {
                this.iconSize = 0;
                return;
            }
            this.atlasGeneration = this.atlas.getGeneration();
            this.baked = false;
            this.renderScale = -1.0f;
        }
        this.iconSize = n;
    }

    public boolean usesNearestFiltering() {
        return this.nearestFiltering;
    }

    public float getMaxV() {
        return this.atlas == null ? 1.0f : (float)(this.getAtlasY() + this.iconSize) / (float)this.atlas.getTextureSize();
    }

    public float getRenderScale() {
        return this.renderScale;
    }

    private boolean hasCurrentAtlasSlot() {
        return this.atlasSlotIndex >= 0 && this.atlasGeneration == this.atlas.getGeneration();
    }

    public int getFramebufferId() {
        return this.atlas != null ? this.atlas.getFramebufferId() : this.framebufferId;
    }

    public float getMinU() {
        return this.atlas == null ? 0.0f : (float)this.getAtlasX() / (float)this.atlas.getTextureSize();
    }

    public float getMinV() {
        return this.atlas == null ? 0.0f : (float)this.getAtlasY() / (float)this.atlas.getTextureSize();
    }

    public int getAtlasGeneration() {
        return this.atlasGeneration;
    }

    public int getAtlasSlotIndex() {
        return this.atlasSlotIndex;
    }

    public IconAtlas getAtlas() {
        return this.atlas;
    }

    public void setBakeState(float f, boolean bl) {
        this.renderScale = f;
        this.baked = bl;
    }

    public void ensureSize(int n) {
        if (this.atlas != null) {
            this.allocateAtlasRegion(n);
            return;
        }
        if (n <= 0 || n == this.iconSize && this.textureId != 0) {
            return;
        }
        this.close();
        int n2 = GL30.glGetInteger((int)32873);
        int n3 = GL30.glGetInteger((int)36006);
        int n4 = this.nearestFiltering ? 9728 : 9729;
        this.textureId = GL30.glGenTextures();
        GL30.glBindTexture((int)3553, (int)this.textureId);
        GL30.glTexParameteri((int)3553, (int)10241, (int)n4);
        GL30.glTexParameteri((int)3553, (int)10240, (int)n4);
        GL30.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL30.glTexParameteri((int)3553, (int)10243, (int)33071);
        int n5 = GL30.glGetInteger((int)35055);
        GL30.glBindBuffer((int)35052, (int)0);
        GL30.glTexImage2D((int)3553, (int)0, (int)32856, (int)n, (int)n, (int)0, (int)6408, (int)5121, (ByteBuffer)null);
        GL30.glBindBuffer((int)35052, (int)n5);
        this.framebufferId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer((int)36160, (int)this.framebufferId);
        GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)3553, (int)this.textureId, (int)0);
        int n6 = GL30.glCheckFramebufferStatus((int)36160);
        if (n6 != 36053) {
            int n7 = n6;
            throw new IllegalStateException("Icon framebuffer is incomplete: " + n7);
        }
        GL30.glBindFramebuffer((int)36160, (int)n3);
        GL30.glBindTexture((int)3553, (int)n2);
        this.iconSize = n;
    }
}

