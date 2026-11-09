package ru.wexside.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import ru.wexside.misc.FrameRateLimiter;
import ru.wexside.misc.TextureHandle;

final class GaussianBlurTarget
implements TextureHandle,
AutoCloseable {
    final int[] textures = new int[2];
    final int[] framebuffers = new int[2];
    final FrameRateLimiter refreshLimiter = new FrameRateLimiter(144);
    int lastRenderedFrame = Integer.MIN_VALUE;
    int sourceGeneration = Integer.MIN_VALUE;
    float sigmaStart = -1.0f;
    float sigmaEnd = -1.0f;
    int width;
    int height;

    GaussianBlurTarget(int width, int height) {
        int previousTexture = GL30.glGetInteger((int)32873);
        int previousFramebuffer = GL30.glGetInteger((int)36006);
        for (int index = 0; index < this.textures.length; ++index) {
            this.textures[index] = GL11.glGenTextures();
            GL11.glBindTexture((int)3553, (int)this.textures[index]);
            GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
            GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
            GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
            GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
            this.framebuffers[index] = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer((int)36160, (int)this.framebuffers[index]);
            GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)3553, (int)this.textures[index], (int)0);
        }
        this.allocateStorage(width, height);
        GL11.glBindTexture((int)3553, (int)previousTexture);
        GL30.glBindFramebuffer((int)36160, (int)previousFramebuffer);
    }

    private void allocateStorage(int width, int height) {
        this.width = width;
        this.height = height;
        for (int index = 0; index < this.textures.length; ++index) {
            GL11.glBindTexture((int)3553, (int)this.textures[index]);
            GL11.glTexImage2D((int)3553, (int)0, (int)32856, (int)width, (int)height, (int)0, (int)6408, (int)5121, (long)0L);
            GL30.glBindFramebuffer((int)36160, (int)this.framebuffers[index]);
            int status = GL30.glCheckFramebufferStatus((int)36160);
            if (status == 36053) continue;
            throw new IllegalStateException("Gaussian blur framebuffer is incomplete: " + status);
        }
    }

    void resize(int width, int height) {
        if (this.width == width && this.height == height) {
            return;
        }
        int previousTexture = GL30.glGetInteger((int)32873);
        int previousFramebuffer = GL30.glGetInteger((int)36006);
        this.allocateStorage(width, height);
        GL11.glBindTexture((int)3553, (int)previousTexture);
        GL30.glBindFramebuffer((int)36160, (int)previousFramebuffer);
    }

    @Override
    public int getTextureId() {
        return this.textures[1];
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void close() {
        for (int index = 0; index < this.textures.length; ++index) {
            if (this.framebuffers[index] != 0) {
                GL30.glDeleteFramebuffers((int)this.framebuffers[index]);
                this.framebuffers[index] = 0;
            }
            if (this.textures[index] == 0) continue;
            GL11.glDeleteTextures((int)this.textures[index]);
            this.textures[index] = 0;
        }
    }
}

