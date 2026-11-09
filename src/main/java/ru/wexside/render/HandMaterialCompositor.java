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
import ru.wexside.render.ScissorRegion;

public final class HandMaterialCompositor {
    private int vertexBuffer;
    private ShaderUniformWriter maskTextureUniform;
    private ShaderUniformWriter rectSizeUniform;
    private boolean initialized;
    private ShaderUniformWriter radiusUniform;
    private static final int VERTEX_STRIDE_BYTES = 20;
    private GlShaderProgram program;
    private int quadWidth = -1;
    private int vertexArray;
    private ShaderUniformWriter locationUniform;
    private static final int VERTEX_FLOAT_COUNT = 5;
    private ShaderUniformWriter maskDepthTextureUniform;
    private ShaderUniformWriter sceneTextureUniform;
    private int framebuffer;
    private int quadHeight = -1;
    private ShaderUniformWriter colorUniform;
    private ShaderUniformWriter color2Uniform;
    private static final long QUAD_BUFFER_BYTES = 120L;
    private ShaderUniformWriter screenSizeUniform;
    private ShaderUniformWriter gradientUniform;
    private ShaderUniformWriter timeUniform;
    private ShaderUniformWriter modeUniform;
    private static final int QUAD_VERTEX_COUNT = 6;

    public void apply(GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuTextureView gpuTextureView4, GpuTextureView gpuTextureView5, int n, int n2, int n3, int n4, boolean bl, int n5, float f, float f2, float f3, float f4, float f5, float f6) {
        RenderSystem.assertOnRenderThread();
        int n6 = this.textureViewId(gpuTextureView);
        int n7 = this.textureViewId(gpuTextureView2);
        int n8 = this.textureViewId(gpuTextureView3);
        int n9 = this.textureViewId(gpuTextureView4);
        int n10 = this.textureViewId(gpuTextureView5);
        if (n6 == 0 || n7 == 0 || n8 == 0 || n9 == 0 || n10 == 0 || n <= 0 || n2 <= 0) {
            return;
        }
        this.ensureInitialized();
        this.updateScreenQuad(n, n2);
        ScissorRegion scissorRegion = this.clipRectangle(n, n2, f, f2, f3, f4);
        if (scissorRegion == null) {
            return;
        }
        OpenGlStateSnapshot openGlStateSnapshot = OpenGlStateSnapshot.capture();
        try {
            this.attachDestination(n6);
            GL46.glViewport((int)0, (int)0, (int)n, (int)n2);
            GL46.glDisable((int)2929);
            GL46.glDepthMask((boolean)false);
            GL46.glDisable((int)2884);
            GL46.glEnable((int)3089);
            GL46.glScissor(scissorRegion.x(), scissorRegion.y(), scissorRegion.width(), scissorRegion.height());
            GL46.glEnable((int)3042);
            GL46.glBlendFuncSeparate((int)770, (int)771, (int)1, (int)771);
            this.program.bind();
            this.screenSizeUniform.set((float)n, (float)n2);
            this.maskTextureUniform.set(0);
            this.sceneTextureUniform.set(1);
            this.maskDepthTextureUniform.set(2);
            this.modeUniform.set(n5);
            this.gradientUniform.set(bl ? 1 : 0);
            this.setColorUniform(this.colorUniform, n3);
            this.setColorUniform(this.color2Uniform, n4);
            this.locationUniform.set(f, f2);
            this.rectSizeUniform.set(f3, f4);
            this.timeUniform.set(f5);
            this.radiusUniform.set(f6);
            this.bindTexture(33984, n7);
            this.bindTexture(33985, n9);
            this.bindTexture(33986, n8);
            this.bindTexture(33987, n10);
            GL46.glBindVertexArray((int)this.vertexArray);
            GL46.glDrawArrays((int)4, (int)0, (int)6);
            GL46.glBindVertexArray((int)0);
        }
        finally {
            openGlStateSnapshot.restore();
        }
    }

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
        this.program = new GlShaderProgram(resourceResolver2.resolve("glass_apply.frag"), resourceResolver.resolve("chams_screen.vert"));
        this.screenSizeUniform = this.program.registerUniform("uScreenSize");
        this.maskTextureUniform = this.program.registerUniform("uMaskTexture");
        this.sceneTextureUniform = this.program.registerUniform("uSceneTexture");
        this.maskDepthTextureUniform = this.program.registerUniform("uMaskDepthTexture");
        this.modeUniform = this.program.registerUniform("uMode");
        this.gradientUniform = this.program.registerUniform("uGradient");
        this.colorUniform = this.program.registerUniform("uColor");
        this.color2Uniform = this.program.registerUniform("uColor2");
        this.locationUniform = this.program.registerUniform("uLocation");
        this.rectSizeUniform = this.program.registerUniform("uRectSize");
        this.timeUniform = this.program.registerUniform("uTime");
        this.radiusUniform = this.program.registerUniform("uRadius");
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
            throw new IllegalStateException("Failed to create glass composite framebuffer");
        }
        this.initialized = true;
    }

    private void bindTexture(int n, int n2) {
        GL46.glActiveTexture((int)n);
        GL46.glBindTexture((int)3553, (int)n2);
    }

    private void setColorUniform(ShaderUniformWriter shaderUniformWriter, int n) {
        shaderUniformWriter.set((float)(n >> 16 & 0xFF) / 255.0f, (float)(n >> 8 & 0xFF) / 255.0f, (float)(n & 0xFF) / 255.0f, (float)(n >>> 24 & 0xFF) / 255.0f);
    }

    private void attachDestination(int n) {
        GL46.glBindFramebuffer((int)36160, (int)this.framebuffer);
        GL46.glFramebufferTexture((int)36160, (int)36064, (int)n, (int)0);
        GL46.glFramebufferTexture((int)36160, (int)36096, (int)0, (int)0);
        if (GL46.glCheckFramebufferStatus((int)36160) != 36053) {
            throw new IllegalStateException("Failed to assemble glass composite target");
        }
    }

    private int textureId(GpuTexture gpuTexture) {
        GlTexture glTexture;
        if (!(gpuTexture instanceof GlTexture) || (glTexture = (GlTexture)gpuTexture).isClosed()) {
            return 0;
        }
        return glTexture.getGlId();
    }

    private int textureViewId(GpuTextureView gpuTextureView) {
        if (gpuTextureView == null || gpuTextureView.isClosed()) {
            return 0;
        }
        return this.textureId(gpuTextureView.texture());
    }

    private ScissorRegion clipRectangle(int n, int n2, float f, float f2, float f3, float f4) {
        int n3 = Math.max(0, (int)Math.floor(f));
        int n4 = Math.max(0, (int)Math.floor(f2));
        int n5 = Math.min(n, (int)Math.ceil(f + f3));
        int n6 = Math.min(n2, (int)Math.ceil(f2 + f4));
        int n7 = n5 - n3;
        int n8 = n6 - n4;
        if (n7 <= 0 || n8 <= 0) {
            return null;
        }
        return new ScissorRegion(n3, n4, n7, n8);
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
}
