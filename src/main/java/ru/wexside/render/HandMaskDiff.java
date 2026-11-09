package ru.wexside.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.FloatBuffer;
import net.minecraft.client.texture.GlTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL46;
import ru.wexside.misc.ClasspathResource;
import ru.wexside.misc.ResourceResolver;
import ru.wexside.misc.ShaderUniformWriter;
import ru.wexside.render.GlShaderProgram;
import ru.wexside.render.OpenGlStateSnapshot;

public final class HandMaskDiff {
    private static final int VERTEX_FLOAT_COUNT = 5;
    private boolean initialized;
    private int vertexBuffer;
    private ShaderUniformWriter depthBeforeSamplerUniform;
    private int vertexArray;
    private static final int QUAD_VERTEX_COUNT = 6;
    private ShaderUniformWriter afterSamplerUniform;
    private static final int VERTEX_STRIDE_BYTES = 20;
    private static final long QUAD_BUFFER_BYTES = 120L;
    private GlShaderProgram program;
    private ShaderUniformWriter beforeSamplerUniform;
    private ShaderUniformWriter screenSizeUniform;
    private ShaderUniformWriter depthAfterSamplerUniform;
    private int framebuffer;
    private int quadWidth = -1;
    private int quadHeight = -1;

    public void close() {
        if (this.vertexArray != 0) {
            GL46.glDeleteVertexArrays((int)this.vertexArray);
            this.vertexArray = 0;
        }
        if (this.vertexBuffer != 0) {
            GL46.glDeleteBuffers((int)this.vertexBuffer);
            this.vertexBuffer = 0;
        }
        if (this.framebuffer != 0) {
            GL46.glDeleteFramebuffers((int)this.framebuffer);
            this.framebuffer = 0;
        }
        if (this.program != null) {
            this.program.close();
            this.program = null;
        }
        this.quadWidth = -1;
        this.quadHeight = -1;
        this.initialized = false;
    }

    private void ensureInitialized() {
        if (this.initialized) {
            return;
        }
        ResourceResolver resourceResolver = new ResourceResolver("/assets/wexside/shaders/chams/", ClasspathResource::new);
        ResourceResolver resourceResolver2 = new ResourceResolver("/assets/wexside/shaders/handaura/", ClasspathResource::new);
        this.program = new GlShaderProgram(resourceResolver2.resolve("mask_diff.frag"), resourceResolver.resolve("chams_screen.vert"));
        this.screenSizeUniform = this.program.registerUniform("uScreenSize");
        this.beforeSamplerUniform = this.program.registerUniform("BeforeSampler");
        this.afterSamplerUniform = this.program.registerUniform("AfterSampler");
        this.depthBeforeSamplerUniform = this.program.registerUniform("DepthBeforeSampler");
        this.depthAfterSamplerUniform = this.program.registerUniform("DepthAfterSampler");
        this.program.getProgramId();
        this.vertexArray = GL46.glGenVertexArrays();
        this.vertexBuffer = GL46.glGenBuffers();
        GL46.glBindVertexArray((int)this.vertexArray);
        GL46.glBindBuffer((int)34962, (int)this.vertexBuffer);
        GL46.glBufferData((int)34962, (long)120L, (int)35048);
        GL46.glEnableVertexAttribArray((int)0);
        GL46.glVertexAttribPointer((int)0, (int)3, (int)5126, (boolean)false, (int)20, (long)0L);
        GL46.glEnableVertexAttribArray((int)2);
        GL46.glVertexAttribPointer((int)2, (int)2, (int)5126, (boolean)false, (int)20, (long)12L);
        GL46.glBindBuffer((int)34962, (int)0);
        GL46.glBindVertexArray((int)0);
        this.framebuffer = GL46.glGenFramebuffers();
        if (this.framebuffer == 0) {
            throw new IllegalStateException("Failed to create mask diff framebuffer");
        }
        this.initialized = true;
    }

    private int textureViewId(GpuTextureView gpuTextureView) {
        if (gpuTextureView == null || gpuTextureView.isClosed()) {
            return 0;
        }
        return this.textureId(gpuTextureView.texture());
    }

    private void updateScreenQuad(int n, int n2) {
        if (this.quadWidth == n && this.quadHeight == n2) {
            return;
        }
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer((int)30);
        floatBuffer.put(0.0f).put(0.0f).put(0.0f).put(0.0f).put(1.0f);
        floatBuffer.put(n).put(0.0f).put(0.0f).put(1.0f).put(1.0f);
        floatBuffer.put(n).put(n2).put(0.0f).put(1.0f).put(0.0f);
        floatBuffer.put(0.0f).put(0.0f).put(0.0f).put(0.0f).put(1.0f);
        floatBuffer.put(n).put(n2).put(0.0f).put(1.0f).put(0.0f);
        floatBuffer.put(0.0f).put(n2).put(0.0f).put(0.0f).put(0.0f);
        floatBuffer.flip();
        GL46.glBindBuffer((int)34962, (int)this.vertexBuffer);
        GL46.glBufferSubData((int)34962, (long)0L, (FloatBuffer)floatBuffer);
        GL46.glBindBuffer((int)34962, (int)0);
        this.quadWidth = n;
        this.quadHeight = n2;
    }

    private int textureId(GpuTexture gpuTexture) {
        GlTexture glTexture;
        if (!(gpuTexture instanceof GlTexture) || (glTexture = (GlTexture)gpuTexture).isClosed()) {
            return 0;
        }
        return glTexture.getGlId();
    }

    private void bindTexture(int n, int n2) {
        GL46.glActiveTexture((int)n);
        GL46.glBindTexture((int)3553, (int)n2);
    }

    public void renderMask(GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuTextureView gpuTextureView4, GpuTextureView gpuTextureView5, int n, int n2) {
        RenderSystem.assertOnRenderThread();
        int n3 = this.textureViewId(gpuTextureView);
        int n4 = this.textureViewId(gpuTextureView2);
        int n5 = this.textureViewId(gpuTextureView3);
        int n6 = this.textureViewId(gpuTextureView4);
        int n7 = this.textureViewId(gpuTextureView5);
        if (n3 == 0 || n4 == 0 || n5 == 0 || n6 == 0 || n7 == 0 || n <= 0 || n2 <= 0) {
            return;
        }
        this.ensureInitialized();
        this.updateScreenQuad(n, n2);
        OpenGlStateSnapshot openGlStateSnapshot = OpenGlStateSnapshot.capture();
        try {
            GL46.glBindFramebuffer((int)36160, (int)this.framebuffer);
            GL46.glFramebufferTexture((int)36160, (int)36064, (int)n3, (int)0);
            GL46.glFramebufferTexture((int)36160, (int)36096, (int)0, (int)0);
            if (GL46.glCheckFramebufferStatus((int)36160) != 36053) {
                return;
            }
            GL46.glViewport((int)0, (int)0, (int)n, (int)n2);
            GL46.glDisable((int)2929);
            GL46.glDepthMask((boolean)false);
            GL46.glDisable((int)2884);
            GL46.glDisable((int)3089);
            GL46.glDisable((int)3042);
            this.program.bind();
            this.screenSizeUniform.set((float)n, (float)n2);
            this.beforeSamplerUniform.set(0);
            this.afterSamplerUniform.set(1);
            this.depthBeforeSamplerUniform.set(2);
            this.depthAfterSamplerUniform.set(3);
            this.bindTexture(33984, n4);
            this.bindTexture(33985, n5);
            this.bindTexture(33986, n6);
            this.bindTexture(33987, n7);
            GL46.glBindVertexArray((int)this.vertexArray);
            GL46.glDrawArrays((int)4, (int)0, (int)6);
            GL46.glBindVertexArray((int)0);
        }
        finally {
            openGlStateSnapshot.restore();
        }
    }
}
