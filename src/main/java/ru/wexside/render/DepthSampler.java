package ru.wexside.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;
import ru.wexside.misc.ClientChat;
import ru.wexside.prediction.PotionImpactMarker;
import ru.wexside.render.ClientRenderPipelines;
import ru.wexside.render.RenderCamera;
import ru.wexside.render.RenderProjection;

public final class DepthSampler {
    private static final float value = 2.5f;
    private static final int MAX_POTION_MARKERS = 8;
    private GpuBufferSlice gpuBufferSlice;
    private static final int UNIFORM_BUFFER_SIZE = 336;
    private static final int UNIFORM_BUFFER_USAGE = 136;
    private GpuBuffer gpuBuffer;
    private GpuBuffer gpuBuffer2;
    private static final float OUTLINE_ALPHA = 0.85f;
    private static final float FILL_ALPHA = 0.5f;
    private final Matrix4f matrix4f = new Matrix4f();
    private static final AtomicBoolean ERROR_REPORTED = new AtomicBoolean();

    public void renderPotionDecals(List<PotionImpactMarker> list) {
        block22: {
            if (list.isEmpty()) {
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            Framebuffer iIllIIiilI2 = mc.getFramebuffer();
            if (iIllIIiilI2 == null || iIllIIiilI2.getColorAttachmentView() == null || iIllIIiilI2.getDepthAttachmentView() == null) {
                return;
            }
            Vec3d vec = RenderCamera.position();
            if (vec == null) {
                return;
            }
            int n = Math.min(list.size(), 8);
            float f = 3.0f;
            this.matrix4f.set((Matrix4fc)RenderProjection.viewProjectionMatrix()).invert();
            try {
                this.ensureUniformBuffer();
                CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                try (MemoryStack memoryStack = MemoryStack.stackPush();){
                    int n2;
                    Std140Builder std140Builder = Std140Builder.onStack((MemoryStack)memoryStack, (int)336).putMat4f((Matrix4fc)this.matrix4f).putVec4((float)n, 0.5f, 0.85f, 2.5f);
                    for (n2 = 0; n2 < 8; ++n2) {
                        if (n2 < n) {
                            Vec3d vec2 = list.get(n2).position();
                            std140Builder.putVec4((float)(vec2.x - vec.x), (float)(vec2.y - vec.y), (float)(vec2.z - vec.z), f);
                            continue;
                        }
                        std140Builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                    }
                    for (n2 = 0; n2 < 8; ++n2) {
                        if (n2 < n) {
                            int n3 = list.get(n2).color();
                            std140Builder.putVec4((float)(n3 >> 16 & 0xFF) / 255.0f, (float)(n3 >> 8 & 0xFF) / 255.0f, (float)(n3 & 0xFF) / 255.0f, 1.0f);
                            continue;
                        }
                        std140Builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                    }
                    commandEncoder.writeToBuffer(this.gpuBufferSlice, std140Builder.get());
                }
                GpuBuffer vertexBuffer = this.getFullscreenQuadBuffer();
                RenderSystem.ShapeIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer((VertexFormat.DrawMode)VertexFormat.DrawMode.QUADS);
                GpuBuffer gpuBuffer = indexBuffer.getIndexBuffer(6);
                try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "wex/potion-decal", iIllIIiilI2.getColorAttachmentView(), OptionalInt.empty());){
                    renderPass.setPipeline(ClientRenderPipelines.POTION_DECAL);
                    RenderSystem.bindDefaultUniforms((RenderPass)renderPass);
                    renderPass.setUniform("PotionDecal", this.gpuBufferSlice);
                    renderPass.setVertexBuffer(0, vertexBuffer);
                    renderPass.setIndexBuffer(gpuBuffer, indexBuffer.getIndexType());
                    renderPass.bindTexture("DepthSampler", iIllIIiilI2.getDepthAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                    renderPass.drawIndexed(0, 0, 6, 1);
                }
            }
            catch (Throwable throwable) {
                if (!ERROR_REPORTED.compareAndSet(false, true)) break block22;
                Throwable throwable2 = throwable.getCause() != null ? throwable.getCause() : throwable;
                String string = String.valueOf(throwable2);
                ClientChat.send("PotionDecal error: " + string);
            }
        }
    }

    private GpuBuffer getFullscreenQuadBuffer() {
        if (this.gpuBuffer2 != null) {
            return this.gpuBuffer2;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(48).order(ByteOrder.nativeOrder());
        byteBuffer.putFloat(-1.0f).putFloat(-1.0f).putFloat(0.0f);
        byteBuffer.putFloat(1.0f).putFloat(-1.0f).putFloat(0.0f);
        byteBuffer.putFloat(1.0f).putFloat(1.0f).putFloat(0.0f);
        byteBuffer.putFloat(-1.0f).putFloat(1.0f).putFloat(0.0f);
        byteBuffer.flip();
        this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "wex/potion-decal-quad", 32, byteBuffer);
        return this.gpuBuffer2;
    }

    private void ensureUniformBuffer() {
        if (this.gpuBuffer == null) {
            this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "wex/potion-decal-ubo", 136, 336L);
            this.gpuBufferSlice = this.gpuBuffer.slice();
        }
    }
}

