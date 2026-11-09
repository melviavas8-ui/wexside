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

public final class HandMaterialSource {
    private static final long QUAD_BUFFER_BYTES = 120L;
    private ShaderUniformWriter sourceSamplerUniform;
    private boolean initialized;
    private ShaderUniformWriter screenSizeUniform;
    private int quadHeight = -1;
    private ShaderUniformWriter maskSamplerUniform;
    private int vertexArray;
    private int quadWidth = -1;
    private int vertexBuffer;
    private static final int VERTEX_STRIDE_BYTES = 20;
    private static final int QUAD_VERTEX_COUNT = 6;
    private int framebuffer;
    private static final int VERTEX_FLOAT_COUNT = 5;
    private GlShaderProgram program;

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
        this.program = new GlShaderProgram(resourceResolver2.resolve("hand_material_source.frag"), resourceResolver.resolve("chams_screen.vert"));
        this.screenSizeUniform = this.program.registerUniform("uScreenSize");
        this.sourceSamplerUniform = this.program.registerUniform("SourceSampler");
        this.maskSamplerUniform = this.program.registerUniform("MaskSampler");
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
            throw new IllegalStateException("Failed to create hand material source framebuffer");
        }
        this.initialized = true;
    }

    public void renderSource(GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, int n, int n2) {
        RenderSystem.assertOnRenderThread();
        int n3 = this.textureViewId(gpuTextureView);
        int n4 = this.textureViewId(gpuTextureView2);
        int n5 = this.textureViewId(gpuTextureView3);
        if (n3 == 0 || n4 == 0 || n5 == 0 || n <= 0 || n2 <= 0) {
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
            this.sourceSamplerUniform.set(0);
            this.maskSamplerUniform.set(1);
            this.bindTexture(33984, n4);
            this.bindTexture(33985, n5);
            GL46.glBindVertexArray((int)this.vertexArray);
            GL46.glDrawArrays((int)4, (int)0, (int)6);
            GL46.glBindVertexArray((int)0);
        }
        finally {
            openGlStateSnapshot.restore();
        }
    }

    private int textureViewId(GpuTextureView gpuTextureView) {
        if (gpuTextureView == null || gpuTextureView.isClosed()) {
            return 0;
        }
        return this.textureId(gpuTextureView.texture());
    }

    private void bindTexture(int n, int n2) {
        GL46.glActiveTexture((int)n);
        GL46.glBindTexture((int)3553, (int)n2);
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
}
