package ru.wexside.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
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
import ru.wexside.render.HandRenderPipelines;

public final class HandAuraCompositor {
    private static final Vector4f WHITE_COLOR_MODULATOR;
    private static final RenderPipeline pipeline;
    private boolean initialized;
    private static final Matrix4f IDENTITY_TEXTURE_MATRIX;
    private static final Vector3f ZERO_MODEL_OFFSET;
    private static final int UNIFORM_BUFFER_BYTES = 64;
    private GpuBuffer dummyVertexBuffer;
    private GpuBuffer uniformBuffer;
    private ByteBuffer uniformStagingBuffer;

    public void close() {
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
        this.uniformStagingBuffer = MemoryUtil.memAlloc((int)64);
        ByteBuffer byteBuffer = MemoryUtil.memAlloc((int)4);
        byteBuffer.putInt(0);
        byteBuffer.flip();
        this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "wexside:hand_aura_dummy_vertex", 32, byteBuffer);
        MemoryUtil.memFree((Buffer)byteBuffer);
        this.initialized = true;
    }

    public void composite(GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, int n, int n2, float f, int n3, int n4, int n5, boolean bl, boolean bl2, float f2, float f3, float f4) {
        this.ensureInitialized();
        this.writeUniformData(n, n2, f, n3, n4, n5, bl, bl2, f2, f3, f4);
        int n6 = this.uniformStagingBuffer.remaining();
        if (this.uniformBuffer == null || this.uniformBuffer.size() < (long)n6) {
            if (this.uniformBuffer != null) {
                this.uniformBuffer.close();
            }
            this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "wexside:hand_aura_uniform", 136, (long)n6);
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.writeToBuffer(this.uniformBuffer.slice(), this.uniformStagingBuffer);
        GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write((Matrix4fc)RenderSystem.getModelViewMatrix(), (Vector4fc)WHITE_COLOR_MODULATOR, (Vector3fc)ZERO_MODEL_OFFSET, (Matrix4fc)IDENTITY_TEXTURE_MATRIX);
        GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "wexside:hand_aura_composite_pass", gpuTextureView, OptionalInt.empty());){
            HandRenderPipelines.ensureCompiled(pipeline);
            renderPass.setPipeline(pipeline);
            renderPass.setVertexBuffer(0, this.dummyVertexBuffer);
            renderPass.bindTexture("MaskSampler", gpuTextureView2, gpuSampler);
            renderPass.bindTexture("GlowSampler", gpuTextureView3, gpuSampler);
            RenderSystem.bindDefaultUniforms((RenderPass)renderPass);
            renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
            renderPass.setUniform("HandAuraData", this.uniformBuffer);
            renderPass.draw(0, 6);
        }
    }

    private void writeUniformData(int n, int n2, float f, int n3, int n4, int n5, boolean bl, boolean bl2, float f2, float f3, float f4) {
        this.uniformStagingBuffer.clear();
        this.uniformStagingBuffer.putFloat(n);
        this.uniformStagingBuffer.putFloat(n2);
        this.uniformStagingBuffer.putFloat(f);
        this.uniformStagingBuffer.putFloat(n3);
        this.uniformStagingBuffer.putFloat((float)(n4 >> 16 & 0xFF) / 255.0f);
        this.uniformStagingBuffer.putFloat((float)(n4 >> 8 & 0xFF) / 255.0f);
        this.uniformStagingBuffer.putFloat((float)(n4 & 0xFF) / 255.0f);
        this.uniformStagingBuffer.putFloat((float)(n4 >>> 24 & 0xFF) / 255.0f);
        this.uniformStagingBuffer.putFloat((float)(n5 >> 16 & 0xFF) / 255.0f);
        this.uniformStagingBuffer.putFloat((float)(n5 >> 8 & 0xFF) / 255.0f);
        this.uniformStagingBuffer.putFloat((float)(n5 & 0xFF) / 255.0f);
        this.uniformStagingBuffer.putFloat((float)(n5 >>> 24 & 0xFF) / 255.0f);
        this.uniformStagingBuffer.putFloat(f2);
        this.uniformStagingBuffer.putFloat(f3);
        this.uniformStagingBuffer.putFloat(f4);
        this.uniformStagingBuffer.putFloat((bl ? 1.0f : 0.0f) + (bl2 ? 2.0f : 0.0f));
        this.uniformStagingBuffer.flip();
    }

    static {
        pipeline = HandRenderPipelines.AURA_COMPOSITE;
        WHITE_COLOR_MODULATOR = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
        ZERO_MODEL_OFFSET = new Vector3f(0.0f, 0.0f, 0.0f);
        IDENTITY_TEXTURE_MATRIX = new Matrix4f();
    }
}
