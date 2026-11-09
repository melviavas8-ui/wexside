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
import ru.wexside.render.ScissorRegion;

public final class ChamsCompositor
implements AutoCloseable {
    private static final int VERTEX_STRIDE = 20;
    private static final int VERTEX_COUNT = 6;
    private static final int VERTEX_BUFFER_SIZE = 120;
    private GlShaderProgram compositeShader;
    private ShaderUniformWriter compositeScreenSizeUniform;
    private ShaderUniformWriter maskTextureUniform;
    private ShaderUniformWriter sceneTextureUniform;
    private ShaderUniformWriter maskDepthTextureUniform;
    private ShaderUniformWriter sceneDepthTextureUniform;
    private ShaderUniformWriter materialModeUniform;
    private ShaderUniformWriter visibleColorUniform;
    private ShaderUniformWriter hiddenColorUniform;
    private ShaderUniformWriter visibleEnabledUniform;
    private ShaderUniformWriter hiddenEnabledUniform;
    private ShaderUniformWriter rectangleLocationUniform;
    private ShaderUniformWriter rectangleSizeUniform;
    private ShaderUniformWriter timeUniform;
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
    public void composite(GpuTextureView outputView, GpuTextureView maskView, GpuTextureView maskDepthView, GpuTextureView sceneView, GpuTextureView sceneDepthView, int width, int height, int visibleColor, int hiddenColor, boolean renderVisible, boolean renderHidden, int materialMode, float rectangleX, float rectangleY, float rectangleWidth, float rectangleHeight, float time) {
        RenderSystem.assertOnRenderThread();
        int outputTextureId = ChamsCompositor.getTextureId(outputView);
        int maskTextureId = ChamsCompositor.getTextureId(maskView);
        int maskDepthTextureId = ChamsCompositor.getTextureId(maskDepthView);
        int sceneTextureId = ChamsCompositor.getTextureId(sceneView);
        int sceneDepthTextureId = ChamsCompositor.getTextureId(sceneDepthView);
        if (outputTextureId == 0 || maskTextureId == 0 || maskDepthTextureId == 0 || sceneTextureId == 0 || sceneDepthTextureId == 0 || width <= 0 || height <= 0 || !renderVisible && !renderHidden) {
            return;
        }
        this.ensureInitialized();
        this.updateFullscreenQuad(width, height);
        ScissorRegion scissor = ChamsCompositor.calculateScissorRegion(width, height, rectangleX, rectangleY, rectangleWidth, rectangleHeight);
        if (scissor == null) {
            return;
        }
        OpenGlStateSnapshot previousState = OpenGlStateSnapshot.capture();
        try {
            this.attachOutputTexture(outputTextureId);
            GlStateManager._viewport((int)0, (int)0, (int)width, (int)height);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask((boolean)false);
            GlStateManager._disableCull();
            GlStateManager._enableScissorTest();
            GlStateManager._scissorBox((int)scissor.x(), (int)scissor.y(), (int)scissor.width(), (int)scissor.height());
            GlStateManager._disableBlend();
            this.compositeShader.bind();
            this.compositeScreenSizeUniform.set(width, height);
            this.maskTextureUniform.set(0);
            this.sceneTextureUniform.set(1);
            this.maskDepthTextureUniform.set(2);
            this.sceneDepthTextureUniform.set(3);
            this.materialModeUniform.set(materialMode);
            this.visibleEnabledUniform.set(renderVisible ? 1 : 0);
            this.hiddenEnabledUniform.set(renderHidden ? 1 : 0);
            ChamsCompositor.setColor(this.visibleColorUniform, visibleColor);
            ChamsCompositor.setColor(this.hiddenColorUniform, hiddenColor);
            this.rectangleLocationUniform.set(rectangleX, rectangleY);
            this.rectangleSizeUniform.set(rectangleWidth, rectangleHeight);
            this.timeUniform.set(time);
            ChamsCompositor.bindTexture(33984, maskTextureId);
            ChamsCompositor.bindTexture(33985, sceneTextureId);
            ChamsCompositor.bindTexture(33986, maskDepthTextureId);
            ChamsCompositor.bindTexture(33987, sceneDepthTextureId);
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
        int outputTextureId = ChamsCompositor.getTextureId(outputView);
        int inputTextureId = ChamsCompositor.getTextureId(inputView);
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
            ChamsCompositor.bindTexture(33984, inputTextureId);
            this.drawFullscreenQuad();
        }
        finally {
            previousState.restore();
        }
    }

    private void ensureInitialized() {
        if (this.compositeShader != null) {
            return;
        }
        ResourceResolver resources = new ResourceResolver("/assets/wexside/shaders/chams/", ClasspathResource::new);
        this.compositeShader = new GlShaderProgram(resources.resolve("chams_apply.frag"), resources.resolve("chams_screen.vert"));
        this.compositeScreenSizeUniform = this.compositeShader.registerUniform("uScreenSize");
        this.maskTextureUniform = this.compositeShader.registerUniform("uMaskTexture");
        this.sceneTextureUniform = this.compositeShader.registerUniform("uSceneTexture");
        this.maskDepthTextureUniform = this.compositeShader.registerUniform("uMaskDepthTexture");
        this.sceneDepthTextureUniform = this.compositeShader.registerUniform("uSceneDepthTexture");
        this.materialModeUniform = this.compositeShader.registerUniform("uMode");
        this.visibleColorUniform = this.compositeShader.registerUniform("uColor");
        this.hiddenColorUniform = this.compositeShader.registerUniform("uHiddenColor");
        this.visibleEnabledUniform = this.compositeShader.registerUniform("uVisibleEnabled");
        this.hiddenEnabledUniform = this.compositeShader.registerUniform("uHiddenEnabled");
        this.rectangleLocationUniform = this.compositeShader.registerUniform("uLocation");
        this.rectangleSizeUniform = this.compositeShader.registerUniform("uRectSize");
        this.timeUniform = this.compositeShader.registerUniform("uTime");
        this.presentShader = new GlShaderProgram(resources.resolve("chams_present.frag"), resources.resolve("chams_screen.vert"));
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
            throw new IllegalStateException("Failed to create chams composite framebuffer");
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
        ChamsCompositor.putVertex(this.quadVertices, 0.0f, 0.0f, 0.0f, 1.0f);
        ChamsCompositor.putVertex(this.quadVertices, width, 0.0f, 1.0f, 1.0f);
        ChamsCompositor.putVertex(this.quadVertices, width, height, 1.0f, 0.0f);
        ChamsCompositor.putVertex(this.quadVertices, 0.0f, 0.0f, 0.0f, 1.0f);
        ChamsCompositor.putVertex(this.quadVertices, width, height, 1.0f, 0.0f);
        ChamsCompositor.putVertex(this.quadVertices, 0.0f, height, 0.0f, 0.0f);
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
            throw new IllegalStateException("Failed to assemble chams composite target");
        }
    }

    private static ScissorRegion calculateScissorRegion(int framebufferWidth, int framebufferHeight, float x, float y, float width, float height) {
        int left = Math.max(0, (int)Math.floor(x));
        int bottom = Math.max(0, (int)Math.floor(y));
        int right = Math.min(framebufferWidth, (int)Math.ceil(x + width));
        int top = Math.min(framebufferHeight, (int)Math.ceil(y + height));
        int scissorWidth = right - left;
        int scissorHeight = top - bottom;
        return scissorWidth <= 0 || scissorHeight <= 0 ? null : new ScissorRegion(left, bottom, scissorWidth, scissorHeight);
    }

    private static void setColor(ShaderUniformWriter uniform, int color) {
        uniform.set((float)(color >> 16 & 0xFF) / 255.0f, (float)(color >> 8 & 0xFF) / 255.0f, (float)(color & 0xFF) / 255.0f, (float)(color >>> 24 & 0xFF) / 255.0f);
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
        GlStateManager._bindTexture((int)textureId);
        GL46.glBindSampler((int)(textureUnit - 33984), (int)0);
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
        if (this.compositeShader != null) {
            this.compositeShader.close();
            this.compositeShader = null;
        }
        if (this.presentShader != null) {
            this.presentShader.close();
            this.presentShader = null;
        }
        this.quadWidth = -1;
        this.quadHeight = -1;
    }
}

