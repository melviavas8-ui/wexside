package ru.wexside.render;

import com.mojang.blaze3d.opengl.GlStateManager;
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

public final class GlowOutlineCompositor
implements AutoCloseable {
    private static final int VERTEX_STRIDE = 20;
    private static final int VERTEX_COUNT = 6;
    private static final int VERTEX_BUFFER_SIZE = 120;
    private GlShaderProgram gradientShader;
    private ShaderUniformWriter gradientScreenSizeUniform;
    private ShaderUniformWriter gradientTextureUniform;
    private ShaderUniformWriter gradientLocationUniform;
    private ShaderUniformWriter gradientRectangleSizeUniform;
    private ShaderUniformWriter gradientTimeUniform;
    private ShaderUniformWriter gradientEnabledUniform;
    private ShaderUniformWriter primaryColorUniform;
    private ShaderUniformWriter secondaryColorUniform;
    private GlShaderProgram outlineShader;
    private ShaderUniformWriter outlineScreenSizeUniform;
    private ShaderUniformWriter outlineInputUniform;
    private ShaderUniformWriter outlineCheckTextureUniform;
    private ShaderUniformWriter outlineDirectionUniform;
    private ShaderUniformWriter outlineSizeUniform;
    private ShaderUniformWriter outlineUseCheckUniform;
    private GlShaderProgram presentShader;
    private ShaderUniformWriter presentScreenSizeUniform;
    private ShaderUniformWriter presentTextureUniform;
    private int framebufferId;
    private int vertexArrayId;
    private int vertexBufferId;
    private int quadWidth = -1;
    private int quadHeight = -1;
    private FloatBuffer quadVertices;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void applyOutline(GpuTextureView outputView, GpuTextureView inputView, GpuTextureView checkView, int width, int height, float outlineSize, float directionX, float directionY, boolean useCheckTexture) {
        RenderSystem.assertOnRenderThread();
        int outputTextureId = GlowOutlineCompositor.getTextureId(outputView);
        int inputTextureId = GlowOutlineCompositor.getTextureId(inputView);
        int checkTextureId = GlowOutlineCompositor.getTextureId(checkView);
        if (outputTextureId == 0 || inputTextureId == 0 || width <= 0 || height <= 0) {
            return;
        }
        this.ensureInitialized();
        this.updateFullscreenQuad(width, height);
        OpenGlStateSnapshot previousState = OpenGlStateSnapshot.capture();
        try {
            this.attachOutputTexture(outputTextureId);
            GlStateManager._viewport((int)0, (int)0, (int)width, (int)height);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            GlStateManager._disableCull();
            GlStateManager._disableScissorTest();
            GlStateManager._disableBlend();
            this.outlineShader.bind();
            this.outlineScreenSizeUniform.set(width, height);
            this.outlineInputUniform.set(0);
            this.outlineCheckTextureUniform.set(1);
            this.outlineDirectionUniform.set(directionX, directionY);
            this.outlineSizeUniform.set(outlineSize);
            this.outlineUseCheckUniform.set(useCheckTexture ? 1 : 0);
            GlowOutlineCompositor.bindTexture(33984, inputTextureId);
            GlowOutlineCompositor.bindTexture(33985, checkTextureId);
            this.drawFullscreenQuad();
        }
        finally {
            previousState.restore();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void applyGradient(GpuTextureView outputView, GpuTextureView inputView, int width, int height, int color, boolean useGradient, float rectangleX, float rectangleY, float rectangleWidth, float rectangleHeight, float time) {
        RenderSystem.assertOnRenderThread();
        int outputTextureId = GlowOutlineCompositor.getTextureId(outputView);
        int inputTextureId = GlowOutlineCompositor.getTextureId(inputView);
        if (outputTextureId == 0 || inputTextureId == 0 || width <= 0 || height <= 0) {
            return;
        }
        this.ensureInitialized();
        this.updateFullscreenQuad(width, height);
        OpenGlStateSnapshot previousState = OpenGlStateSnapshot.capture();
        try {
            this.attachOutputTexture(outputTextureId);
            GlStateManager._viewport((int)0, (int)0, (int)width, (int)height);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            GlStateManager._disableCull();
            GlStateManager._disableScissorTest();
            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate((int)773, (int)1, (int)773, (int)1);
            float red = (float)(color >> 16 & 0xFF) / 255.0f;
            float green = (float)(color >> 8 & 0xFF) / 255.0f;
            float blue = (float)(color & 0xFF) / 255.0f;
            float alpha = (float)(color >>> 24 & 0xFF) / 255.0f;
            this.gradientShader.bind();
            this.gradientScreenSizeUniform.set(width, height);
            this.gradientTextureUniform.set(0);
            this.gradientLocationUniform.set(rectangleX, rectangleY);
            this.gradientRectangleSizeUniform.set(rectangleWidth, rectangleHeight);
            this.gradientTimeUniform.set(time);
            this.gradientEnabledUniform.set(useGradient ? 1 : 0);
            this.primaryColorUniform.set(red, green, blue, alpha);
            this.secondaryColorUniform.set(red * 0.2f, green * 0.2f, blue * 0.2f, alpha);
            GlowOutlineCompositor.bindTexture(33984, inputTextureId);
            this.drawFullscreenQuad();
        }
        finally {
            previousState.restore();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void present(GpuTextureView outputView, GpuTextureView inputView, int width, int height) {
        RenderSystem.assertOnRenderThread();
        int outputTextureId = GlowOutlineCompositor.getTextureId(outputView);
        int inputTextureId = GlowOutlineCompositor.getTextureId(inputView);
        if (outputTextureId == 0 || inputTextureId == 0 || width <= 0 || height <= 0) {
            return;
        }
        this.ensureInitialized();
        this.updateFullscreenQuad(width, height);
        OpenGlStateSnapshot previousState = OpenGlStateSnapshot.capture();
        try {
            this.attachOutputTexture(outputTextureId);
            GlStateManager._viewport((int)0, (int)0, (int)width, (int)height);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            GlStateManager._disableCull();
            GlStateManager._disableScissorTest();
            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate((int)770, (int)771, (int)1, (int)771);
            this.presentShader.bind();
            this.presentScreenSizeUniform.set(width, height);
            this.presentTextureUniform.set(0);
            GlowOutlineCompositor.bindTexture(33984, inputTextureId);
            this.drawFullscreenQuad();
        }
        finally {
            previousState.restore();
        }
    }

    private void ensureInitialized() {
        if (this.gradientShader != null) {
            return;
        }
        ResourceResolver resources = new ResourceResolver("/assets/wexside/shaders/glowesp/", ClasspathResource::new);
        this.gradientShader = new GlShaderProgram(resources.resolve("glowesp_gradient.frag"), resources.resolve("glowesp_screen.vert"));
        this.gradientScreenSizeUniform = this.gradientShader.registerUniform("uScreenSize");
        this.gradientTextureUniform = this.gradientShader.registerUniform("uTexture");
        this.gradientLocationUniform = this.gradientShader.registerUniform("uLocation");
        this.gradientRectangleSizeUniform = this.gradientShader.registerUniform("uRectSize");
        this.gradientTimeUniform = this.gradientShader.registerUniform("uTime");
        this.gradientEnabledUniform = this.gradientShader.registerUniform("uGradient");
        this.primaryColorUniform = this.gradientShader.registerUniform("uColor1");
        this.secondaryColorUniform = this.gradientShader.registerUniform("uColor2");
        this.outlineShader = new GlShaderProgram(resources.resolve("glowesp_outline.frag"), resources.resolve("glowesp_screen.vert"));
        this.outlineScreenSizeUniform = this.outlineShader.registerUniform("uScreenSize");
        this.outlineInputUniform = this.outlineShader.registerUniform("uTextureIn");
        this.outlineCheckTextureUniform = this.outlineShader.registerUniform("uTextureCheck");
        this.outlineDirectionUniform = this.outlineShader.registerUniform("uDirection");
        this.outlineSizeUniform = this.outlineShader.registerUniform("uSize");
        this.outlineUseCheckUniform = this.outlineShader.registerUniform("uUseCheck");
        this.presentShader = new GlShaderProgram(resources.resolve("glowesp_present.frag"), resources.resolve("glowesp_screen.vert"));
        this.presentScreenSizeUniform = this.presentShader.registerUniform("uScreenSize");
        this.presentTextureUniform = this.presentShader.registerUniform("uTexture");
        this.vertexArrayId = GL46.glGenVertexArrays();
        this.vertexBufferId = GL46.glGenBuffers();
        GL46.glBindVertexArray((int)this.vertexArrayId);
        GL46.glBindBuffer((int)34962, (int)this.vertexBufferId);
        GL46.glBufferData((int)34962, (long)120L, (int)35048);
        GL46.glEnableVertexAttribArray((int)0);
        GL46.glVertexAttribPointer((int)0, (int)3, (int)5126, (boolean)false, (int)20, (long)0L);
        GL46.glEnableVertexAttribArray((int)2);
        GL46.glVertexAttribPointer((int)2, (int)2, (int)5126, (boolean)false, (int)20, (long)12L);
        GL46.glBindBuffer((int)34962, (int)0);
        GL46.glBindVertexArray((int)0);
        this.framebufferId = GL46.glGenFramebuffers();
        if (this.framebufferId == 0) {
            throw new IllegalStateException("Failed to create glow ESP framebuffer");
        }
    }

    private void updateFullscreenQuad(int width, int height) {
        if (this.quadWidth == width && this.quadHeight == height) {
            return;
        }
        if (this.quadVertices == null) {
            this.quadVertices = BufferUtils.createFloatBuffer((int)30);
        }
        this.quadVertices.clear();
        GlowOutlineCompositor.putVertex(this.quadVertices, 0.0f, 0.0f, 0.0f, 1.0f);
        GlowOutlineCompositor.putVertex(this.quadVertices, width, 0.0f, 1.0f, 1.0f);
        GlowOutlineCompositor.putVertex(this.quadVertices, width, height, 1.0f, 0.0f);
        GlowOutlineCompositor.putVertex(this.quadVertices, 0.0f, 0.0f, 0.0f, 1.0f);
        GlowOutlineCompositor.putVertex(this.quadVertices, width, height, 1.0f, 0.0f);
        GlowOutlineCompositor.putVertex(this.quadVertices, 0.0f, height, 0.0f, 0.0f);
        this.quadVertices.flip();
        GL46.glBindBuffer((int)34962, (int)this.vertexBufferId);
        GL46.glBufferSubData((int)34962, (long)0L, (FloatBuffer)this.quadVertices);
        GL46.glBindBuffer((int)34962, (int)0);
        this.quadWidth = width;
        this.quadHeight = height;
    }

    private static void putVertex(FloatBuffer vertices, float x, float y, float u, float v) {
        vertices.put(x).put(y).put(0.0f).put(u).put(v);
    }

    private void attachOutputTexture(int textureId) {
        GL46.glBindFramebuffer((int)36160, (int)this.framebufferId);
        GL46.glFramebufferTexture((int)36160, (int)36064, (int)textureId, (int)0);
        GL46.glFramebufferTexture((int)36160, (int)36096, (int)0, (int)0);
        if (GL46.glCheckFramebufferStatus((int)36160) != 36053) {
            throw new IllegalStateException("Failed to assemble glow ESP target");
        }
    }

    private static int getTextureId(GpuTextureView view) {
        GlTexture glTexture;
        if (view == null || view.isClosed()) {
            return 0;
        }
        GpuTexture texture = view.texture();
        return texture instanceof GlTexture && !(glTexture = (GlTexture)texture).isClosed() ? glTexture.getGlId() : 0;
    }

    private static void bindTexture(int textureUnit, int textureId) {
        GlStateManager._activeTexture((int)textureUnit);
        GL46.glBindSampler((int)(textureUnit - 33984), (int)0);
        GlStateManager._bindTexture((int)textureId);
        GL46.glTexParameteri((int)3553, (int)10241, (int)9729);
        GL46.glTexParameteri((int)3553, (int)10240, (int)9729);
    }

    private void drawFullscreenQuad() {
        GL46.glBindVertexArray((int)this.vertexArrayId);
        GL46.glDrawArrays((int)4, (int)0, (int)6);
        GL46.glBindVertexArray((int)0);
    }

    @Override
    public void close() {
        if (this.vertexArrayId != 0) {
            GL46.glDeleteVertexArrays((int)this.vertexArrayId);
            this.vertexArrayId = 0;
        }
        if (this.vertexBufferId != 0) {
            GL46.glDeleteBuffers((int)this.vertexBufferId);
            this.vertexBufferId = 0;
        }
        if (this.framebufferId != 0) {
            GL46.glDeleteFramebuffers((int)this.framebufferId);
            this.framebufferId = 0;
        }
        if (this.gradientShader != null) {
            this.gradientShader.close();
            this.gradientShader = null;
        }
        if (this.outlineShader != null) {
            this.outlineShader.close();
            this.outlineShader = null;
        }
        if (this.presentShader != null) {
            this.presentShader.close();
            this.presentShader = null;
        }
        this.quadWidth = -1;
        this.quadHeight = -1;
    }
}

