package ru.wexside.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.FloatBuffer;
import net.minecraft.client.texture.GlTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import ru.wexside.misc.ClasspathResource;
import ru.wexside.misc.ResourceResolver;
import ru.wexside.misc.ShaderUniformWriter;
import ru.wexside.render.GlShaderProgram;
import ru.wexside.render.OpenGlStateSnapshot;

public final class ColorCorrectionShader
implements AutoCloseable {
    private static final int VERTEX_STRIDE = 20;
    private static final int VERTEX_COUNT = 6;
    private static final int VERTEX_BUFFER_SIZE = 120;
    private GlShaderProgram shaderProgram;
    private ShaderUniformWriter screenSizeUniform;
    private ShaderUniformWriter sceneSamplerUniform;
    private ShaderUniformWriter contrastUniform;
    private ShaderUniformWriter saturationUniform;
    private ShaderUniformWriter brightnessUniform;
    private int framebufferId;
    private int vertexArrayId;
    private int vertexBufferId;
    private int quadWidth = -1;
    private int quadHeight = -1;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void apply(GpuTextureView outputView, GpuTextureView sceneView, int width, int height, float contrast, float saturation, float brightness) {
        RenderSystem.assertOnRenderThread();
        int outputTextureId = ColorCorrectionShader.getTextureId(outputView);
        int sceneTextureId = ColorCorrectionShader.getTextureId(sceneView);
        if (outputTextureId == 0 || sceneTextureId == 0 || width <= 0 || height <= 0) {
            return;
        }
        this.ensureInitialized();
        this.updateFullscreenQuad(width, height);
        OpenGlStateSnapshot previousState = OpenGlStateSnapshot.capture();
        try {
            this.attachOutputTexture(outputTextureId);
            GL11.glViewport((int)0, (int)0, (int)width, (int)height);
            GL11.glDisable((int)2929);
            GL11.glDepthMask((boolean)false);
            GL11.glDisable((int)2884);
            GL11.glDisable((int)3089);
            GL11.glDisable((int)3042);
            this.shaderProgram.bind();
            this.screenSizeUniform.set(width, height);
            this.sceneSamplerUniform.set(0);
            this.contrastUniform.set(contrast);
            this.saturationUniform.set(saturation);
            this.brightnessUniform.set(brightness);
            ColorCorrectionShader.bindTexture(33984, sceneTextureId);
            this.drawFullscreenQuad();
        }
        finally {
            previousState.restore();
        }
    }

    private void ensureInitialized() {
        if (this.shaderProgram != null) {
            return;
        }
        ResourceResolver resources = new ResourceResolver("/assets/wexside/shaders/colorcorrection/", ClasspathResource::new);
        this.shaderProgram = new GlShaderProgram(resources.resolve("colorcorrection.frag"), resources.resolve("colorcorrection.vert"));
        this.screenSizeUniform = this.shaderProgram.registerUniform("uScreenSize");
        this.sceneSamplerUniform = this.shaderProgram.registerUniform("uScene");
        this.contrastUniform = this.shaderProgram.registerUniform("uContrast");
        this.saturationUniform = this.shaderProgram.registerUniform("uSaturation");
        this.brightnessUniform = this.shaderProgram.registerUniform("uBrightness");
        this.vertexArrayId = GL30.glGenVertexArrays();
        this.vertexBufferId = GL15.glGenBuffers();
        GL30.glBindVertexArray((int)this.vertexArrayId);
        GL15.glBindBuffer((int)34962, (int)this.vertexBufferId);
        GL15.glBufferData((int)34962, (long)120L, (int)35048);
        GL20.glEnableVertexAttribArray((int)0);
        GL20.glVertexAttribPointer((int)0, (int)3, (int)5126, (boolean)false, (int)20, (long)0L);
        GL20.glEnableVertexAttribArray((int)2);
        GL20.glVertexAttribPointer((int)2, (int)2, (int)5126, (boolean)false, (int)20, (long)12L);
        GL15.glBindBuffer((int)34962, (int)0);
        GL30.glBindVertexArray((int)0);
        this.framebufferId = GL30.glGenFramebuffers();
        if (this.framebufferId == 0) {
            throw new IllegalStateException("Failed to create color correction framebuffer");
        }
    }

    private void updateFullscreenQuad(int width, int height) {
        if (this.quadWidth == width && this.quadHeight == height) {
            return;
        }
        FloatBuffer vertices = BufferUtils.createFloatBuffer((int)30);
        ColorCorrectionShader.putVertex(vertices, 0.0f, 0.0f, 0.0f, 1.0f);
        ColorCorrectionShader.putVertex(vertices, width, 0.0f, 1.0f, 1.0f);
        ColorCorrectionShader.putVertex(vertices, width, height, 1.0f, 0.0f);
        ColorCorrectionShader.putVertex(vertices, 0.0f, 0.0f, 0.0f, 1.0f);
        ColorCorrectionShader.putVertex(vertices, width, height, 1.0f, 0.0f);
        ColorCorrectionShader.putVertex(vertices, 0.0f, height, 0.0f, 0.0f);
        vertices.flip();
        GL15.glBindBuffer((int)34962, (int)this.vertexBufferId);
        GL15.glBufferSubData((int)34962, (long)0L, (FloatBuffer)vertices);
        GL15.glBindBuffer((int)34962, (int)0);
        this.quadWidth = width;
        this.quadHeight = height;
    }

    private static void putVertex(FloatBuffer vertices, float x, float y, float u, float v) {
        vertices.put(x).put(y).put(0.0f).put(u).put(v);
    }

    private void attachOutputTexture(int textureId) {
        GL30.glBindFramebuffer((int)36160, (int)this.framebufferId);
        GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)3553, (int)textureId, (int)0);
        GL30.glFramebufferTexture2D((int)36160, (int)36096, (int)3553, (int)0, (int)0);
        if (GL30.glCheckFramebufferStatus((int)36160) != 36053) {
            throw new IllegalStateException("Failed to assemble color correction target");
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
        GL13.glActiveTexture((int)textureUnit);
        GL11.glBindTexture((int)3553, (int)textureId);
    }

    private void drawFullscreenQuad() {
        GL30.glBindVertexArray((int)this.vertexArrayId);
        GL11.glDrawArrays((int)4, (int)0, (int)6);
        GL30.glBindVertexArray((int)0);
    }

    @Override
    public void close() {
        if (this.vertexArrayId != 0) {
            GL30.glDeleteVertexArrays((int)this.vertexArrayId);
            this.vertexArrayId = 0;
        }
        if (this.vertexBufferId != 0) {
            GL15.glDeleteBuffers((int)this.vertexBufferId);
            this.vertexBufferId = 0;
        }
        if (this.framebufferId != 0) {
            GL30.glDeleteFramebuffers((int)this.framebufferId);
            this.framebufferId = 0;
        }
        if (this.shaderProgram != null) {
            this.shaderProgram.close();
            this.shaderProgram = null;
        }
        this.quadWidth = -1;
        this.quadHeight = -1;
    }
}

