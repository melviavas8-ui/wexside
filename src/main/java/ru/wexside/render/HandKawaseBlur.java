package ru.wexside.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.gl.GpuSampler;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryUtil;
import ru.wexside.render.HandBlurRegion;
import ru.wexside.render.HandRenderPipelines;

public class HandKawaseBlur {
    private ByteBuffer uniformStagingBuffer;
    private int sourceHeight = 0;
    private int[] downWidths;
    private static final int MAX_LEVELS = 8;
    private int[] upWidths;
    private static final Vector4f WHITE_COLOR_MODULATOR;
    private GpuTexture[] upTextures;
    private static final RenderPipeline downPipeline;
    private static final int UNIFORM_STAGING_CAPACITY_BYTES = 32;
    private int[] upHeights;
    private GpuTexture[] downTextures;
    private GpuTextureView[] upViews;
    private GpuBuffer uniformBuffer;
    private GpuTextureView[] downViews;
    private static final RenderPipeline upPipeline;
    private HandBlurRegion outputClip;
    private int[] downHeights;
    private boolean initialized = false;
    private static final Matrix4f IDENTITY_TEXTURE_MATRIX;
    private int sourceWidth = 0;
    private GpuBuffer dummyVertexBuffer;
    private static final Vector3f ZERO_MODEL_OFFSET;

    public void close() {
        this.outputClip = null;
        this.closePyramid();
        if (this.uniformBuffer != null) {
            this.uniformBuffer.close();
            this.uniformBuffer = null;
        }
        if (this.dummyVertexBuffer != null) {
            this.dummyVertexBuffer.close();
            this.dummyVertexBuffer = null;
        }
        if (this.uniformStagingBuffer != null) {
            MemoryUtil.memFree((Buffer)this.uniformStagingBuffer);
            this.uniformStagingBuffer = null;
        }
        this.initialized = false;
    }

    private void ensureInitialized() {
        if (this.initialized) {
            return;
        }
        this.uniformStagingBuffer = MemoryUtil.memAlloc((int)32);
        ByteBuffer byteBuffer = MemoryUtil.memAlloc((int)4);
        byteBuffer.putInt(0);
        byteBuffer.flip();
        this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "wexside:hand_aura_kawase_dummy_vertex", 32, byteBuffer);
        MemoryUtil.memFree((Buffer)byteBuffer);
        this.downTextures = new GpuTexture[8];
        this.downViews = new GpuTextureView[8];
        this.upTextures = new GpuTexture[8];
        this.upViews = new GpuTextureView[8];
        this.downWidths = new int[8];
        this.downHeights = new int[8];
        this.upWidths = new int[8];
        this.upHeights = new int[8];
        this.initialized = true;
    }

    public HandBlurRegion getOutputClip() {
        return this.outputClip;
    }

    private void resizePyramid(int n, int n2) {
        if (n == this.sourceWidth && n2 == this.sourceHeight) {
            return;
        }
        this.closePyramid();
        int n3 = n;
        int n4 = n2;
        for (int i = 0; i < 8; ++i) {
            n3 = Math.max(1, n3 / 2);
            n4 = Math.max(1, n4 / 2);
            int n5 = i;
            int n6 = n3;
            int n7 = n4;
            this.downWidths[i] = n6;
            this.downHeights[i] = n7;
            this.upWidths[i] = n6;
            this.upHeights[i] = n7;
            this.downTextures[i] = RenderSystem.getDevice().createTexture(() -> "wexside:hand_aura_kawase_down_" + n5, 13, TextureFormat.RGBA8, n6, n7, 1, 1);
            this.downViews[i] = RenderSystem.getDevice().createTextureView(this.downTextures[i]);
            this.upTextures[i] = RenderSystem.getDevice().createTexture(() -> "wexside:hand_aura_kawase_up_" + n5, 13, TextureFormat.RGBA8, n6, n7, 1, 1);
            this.upViews[i] = RenderSystem.getDevice().createTextureView(this.upTextures[i]);
        }
        this.sourceWidth = n;
        this.sourceHeight = n2;
    }

    private void writeUniformData(int n, int n2, float f) {
        this.uniformStagingBuffer.clear();
        this.uniformStagingBuffer.putFloat(n);
        this.uniformStagingBuffer.putFloat(n2);
        this.uniformStagingBuffer.putFloat(f);
        this.uniformStagingBuffer.putFloat(0.0f);
        this.uniformStagingBuffer.flip();
    }

    public GpuTextureView blur(GpuTextureView gpuTextureView, int n, int n2, int n3, float f, HandBlurRegion handBlurRegion) {
        int n4;
        int n5;
        this.outputClip = null;
        if (gpuTextureView == null) {
            return null;
        }
        this.ensureInitialized();
        this.resizePyramid(n, n2);
        n3 = Math.min(n3, 8);
        if (n3 < 1) {
            n3 = 1;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        GpuTextureView gpuTextureView2 = gpuTextureView;
        int n6 = n;
        int n7 = n2;
        HandBlurRegion handBlurRegion2 = handBlurRegion;
        float f2 = Math.abs(f) * 0.5f + 1.0f;
        for (n5 = 0; n5 < n3; ++n5) {
            int n8 = this.downWidths[n5];
            n4 = this.downHeights[n5];
            if (handBlurRegion2 != null) {
                handBlurRegion2 = handBlurRegion2.scale(n6, n7, n8, n4, f2);
            }
            this.writeUniformData(n6, n7, f);
            int n9 = this.uniformStagingBuffer.remaining();
            if (this.uniformBuffer == null || this.uniformBuffer.size() < (long)n9) {
                if (this.uniformBuffer != null) {
                    this.uniformBuffer.close();
                }
                this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "wexside:hand_aura_kawase_uniform", 136, (long)n9);
            }
            commandEncoder.writeToBuffer(this.uniformBuffer.slice(), this.uniformStagingBuffer);
            GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write((Matrix4fc)RenderSystem.getModelViewMatrix(), (Vector4fc)WHITE_COLOR_MODULATOR, (Vector3fc)ZERO_MODEL_OFFSET, (Matrix4fc)IDENTITY_TEXTURE_MATRIX);
            int n10 = n5;
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "wexside:hand_aura_kawase_down_pass_" + n10, this.downViews[n5], handBlurRegion == null ? OptionalInt.empty() : OptionalInt.of(0));){
                HandRenderPipelines.ensureCompiled(downPipeline);
                renderPass.setPipeline(downPipeline);
                if (handBlurRegion2 != null) {
                    handBlurRegion2.apply(renderPass);
                }
                renderPass.setVertexBuffer(0, this.dummyVertexBuffer);
                renderPass.bindTexture("Sampler0", gpuTextureView2, gpuSampler);
                RenderSystem.bindDefaultUniforms((RenderPass)renderPass);
                renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                renderPass.setUniform("KawaseData", this.uniformBuffer);
                renderPass.draw(0, 6);
            }
            finally {
                if (handBlurRegion != null) {
                    GlStateManager._disableScissorTest();
                }
            }
            gpuTextureView2 = this.downViews[n5];
            n6 = n8;
            n7 = n4;
        }
        for (n5 = n3 - 1; n5 >= 0; --n5) {
            if (handBlurRegion2 != null) {
                handBlurRegion2 = handBlurRegion2.scale(n6, n7, this.upWidths[n5], this.upHeights[n5], f2);
            }
            this.writeUniformData(n6, n7, f);
            commandEncoder.writeToBuffer(this.uniformBuffer.slice(), this.uniformStagingBuffer);
            GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write((Matrix4fc)RenderSystem.getModelViewMatrix(), (Vector4fc)WHITE_COLOR_MODULATOR, (Vector3fc)ZERO_MODEL_OFFSET, (Matrix4fc)IDENTITY_TEXTURE_MATRIX);
            final int upPassIndex = n5;
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "wexside:hand_aura_kawase_up_pass_" + upPassIndex, this.upViews[n5], handBlurRegion == null ? OptionalInt.empty() : OptionalInt.of(0));){
                HandRenderPipelines.ensureCompiled(upPipeline);
                renderPass.setPipeline(upPipeline);
                if (handBlurRegion2 != null) {
                    handBlurRegion2.apply(renderPass);
                }
                renderPass.setVertexBuffer(0, this.dummyVertexBuffer);
                renderPass.bindTexture("Sampler0", gpuTextureView2, gpuSampler);
                RenderSystem.bindDefaultUniforms((RenderPass)renderPass);
                renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                renderPass.setUniform("KawaseData", this.uniformBuffer);
                renderPass.draw(0, 6);
            }
            finally {
                if (handBlurRegion != null) {
                    GlStateManager._disableScissorTest();
                }
            }
            gpuTextureView2 = this.upViews[n5];
            n6 = this.upWidths[n5];
            n7 = this.upHeights[n5];
        }
        if (handBlurRegion2 != null) {
            this.outputClip = handBlurRegion2.scale(n6, n7, n, n2, 1.0f);
        }
        return gpuTextureView2;
    }

    public GpuTextureView blur(GpuTextureView gpuTextureView, int n, int n2, int n3, float f) {
        return this.blur(gpuTextureView, n, n2, n3, f, null);
    }

    private void closePyramid() {
        this.sourceWidth = 0;
        this.sourceHeight = 0;
        if (this.downViews != null) {
            for (int i = 0; i < 8; ++i) {
                if (this.downViews[i] != null) {
                    this.downViews[i].close();
                    this.downViews[i] = null;
                }
                if (this.downTextures[i] != null) {
                    this.downTextures[i].close();
                    this.downTextures[i] = null;
                }
                if (this.upViews[i] != null) {
                    this.upViews[i].close();
                    this.upViews[i] = null;
                }
                if (this.upTextures[i] == null) continue;
                this.upTextures[i].close();
                this.upTextures[i] = null;
            }
        }
    }

    static {
        downPipeline = HandRenderPipelines.KAWASE_DOWN;
        upPipeline = HandRenderPipelines.KAWASE_UP;
        WHITE_COLOR_MODULATOR = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
        ZERO_MODEL_OFFSET = new Vector3f(0.0f, 0.0f, 0.0f);
        IDENTITY_TEXTURE_MATRIX = new Matrix4f();
    }
}
