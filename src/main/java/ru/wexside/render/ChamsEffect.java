package ru.wexside.render;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import ru.wexside.render.ChamsCompositor;
import ru.wexside.render.RenderFrameState;
import ru.wexside.render.RenderTargetOverrideScope;
import ru.wexside.render.ScreenRegion;

public final class ChamsEffect {
    private int framebufferWidth;
    private VertexConsumerProvider.Immediate primaryVertexConsumers;
    private GpuTextureView sceneDepthView;
    private boolean effectRendered;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private BufferAllocator secondaryBufferAllocator;
    private int framebufferHeight;
    private final ChamsCompositor compositor = new ChamsCompositor();
    private GpuTextureView maskDepthView;
    private float elapsedTime;
    private EntityRenderManager entityRenderer;
    private boolean framePrepared;
    private boolean framebuffersCleared;
    private OutlineVertexConsumerProvider outlineVertexConsumers;
    private SimpleFramebuffer effectFramebuffer;
    private final long startedAtMillis = System.currentTimeMillis();
    private GpuTextureView effectView;
    private Framebuffer mainFramebuffer;
    private final Matrix4f viewProjectionMatrix = new Matrix4f();
    private BufferAllocator primaryBufferAllocator;
    private VertexConsumerProvider.Immediate secondaryVertexConsumers;
    private SimpleFramebuffer maskFramebuffer;
    private RenderDispatcher renderDispatcher;
    private GpuTextureView maskColorView;
    private final CameraRenderState cameraRenderState;
    private final Vector4f clipSpacePosition = new Vector4f();
    private GpuTextureView sceneColorView;
    private Vec3d cameraPosition;
    private OrderedRenderCommandQueueImpl renderQueue;

    public ChamsEffect() {
        this.cameraRenderState = new CameraRenderState();
        this.cameraPosition = Vec3d.ZERO;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderEntityToMask(MatrixStack matrices2, Entity entity2, float f) {
        if (this.entityRenderer == null) {
            return;
        }
        EntityRenderState renderState = this.entityRenderer.getAndUpdateRenderState(entity2, f);
        if (renderState == null) {
            return;
        }
        this.prepareRenderState(renderState);
        boolean rendered = false;
        try {
            matrices2.push();
            try {
                this.entityRenderer.render(renderState, this.cameraRenderState, renderState.x - this.cameraPosition.x, renderState.y - this.cameraPosition.y, renderState.z - this.cameraPosition.z, matrices2, (OrderedRenderCommandQueue)this.renderQueue);
            }
            finally {
                matrices2.pop();
            }
            this.renderDispatcher.render();
            this.renderDispatcher.endLayeredCustoms();
            this.primaryVertexConsumers.draw();
            this.secondaryVertexConsumers.draw();
            this.outlineVertexConsumers.draw();
            this.renderQueue.onNextFrame();
            rendered = true;
        }
        finally {
            if (!rendered) {
                this.renderQueue.clear();
            }
        }
    }

    private void prepareRenderState(EntityRenderState renderState) {
        renderState.positionOffset = null;
        renderState.displayName = null;
        renderState.nameLabelPos = null;
        renderState.onFire = false;
        renderState.outlineColor = 0;
        renderState.invisible = false;
        renderState.shadowRadius = 0.0f;
        renderState.shadowPieces.clear();
    }

    private void updateCameraState(Camera camera2) {
        this.cameraRenderState.initialized = true;
        this.cameraRenderState.pos = camera2.getCameraPos();
        this.cameraRenderState.blockPos = camera2.getBlockPos();
        this.cameraRenderState.entityPos = this.cameraRenderState.pos;
        this.cameraRenderState.orientation = camera2.getRotation();
    }

    public void resetFrameState() {
        this.clearFrameState();
    }

    public void present() {
        if (!this.effectRendered) {
            this.clearFrameState();
            return;
        }
        if (this.mainFramebuffer == null || this.sceneColorView == null || this.effectView == null) {
            this.clearFrameState();
            return;
        }
        this.compositor.present(this.sceneColorView, this.effectView, this.framebufferWidth, this.framebufferHeight);
        this.clearFrameState();
    }

    private void ensureRenderDispatcher() {
        if (this.renderDispatcher != null) {
            return;
        }
        this.primaryBufferAllocator = new BufferAllocator(0x100000);
        this.secondaryBufferAllocator = new BufferAllocator(262144);
        this.primaryVertexConsumers = VertexConsumerProvider.immediate((BufferAllocator)this.primaryBufferAllocator);
        this.secondaryVertexConsumers = VertexConsumerProvider.immediate((BufferAllocator)this.secondaryBufferAllocator);
        this.outlineVertexConsumers = new OutlineVertexConsumerProvider();
        this.renderQueue = new OrderedRenderCommandQueueImpl();
        this.renderDispatcher = new RenderDispatcher(this.renderQueue, this.client.getBlockRenderManager(), this.primaryVertexConsumers, this.client.getAtlasManager(), this.outlineVertexConsumers, this.secondaryVertexConsumers, this.client.textRenderer);
    }

    public void releaseFramebuffers() {
        this.clearFrameState();
        if (this.maskFramebuffer != null) {
            this.maskFramebuffer.delete();
            this.maskFramebuffer = null;
        }
        if (this.effectFramebuffer != null) {
            this.effectFramebuffer.delete();
            this.effectFramebuffer = null;
        }
    }

    public void close() {
        this.releaseFramebuffers();
        if (this.renderDispatcher != null) {
            this.renderDispatcher.close();
            this.renderDispatcher = null;
        }
        if (this.primaryBufferAllocator != null) {
            this.primaryBufferAllocator.close();
            this.primaryBufferAllocator = null;
        }
        if (this.secondaryBufferAllocator != null) {
            this.secondaryBufferAllocator.close();
            this.secondaryBufferAllocator = null;
        }
        this.primaryVertexConsumers = null;
        this.secondaryVertexConsumers = null;
        this.outlineVertexConsumers = null;
        this.renderQueue = null;
        this.compositor.close();
    }

    private void clearFrameState() {
        this.framePrepared = false;
        this.framebuffersCleared = false;
        this.effectRendered = false;
        this.mainFramebuffer = null;
        this.sceneColorView = null;
        this.sceneDepthView = null;
        this.maskColorView = null;
        this.maskDepthView = null;
        this.effectView = null;
        this.entityRenderer = null;
        this.cameraPosition = Vec3d.ZERO;
        this.framebufferWidth = 0;
        this.framebufferHeight = 0;
        this.elapsedTime = 0.0f;
    }

    private ScreenRegion calculateScreenRegion(Entity entity2, float f) {
        double d;
        if (this.framebufferWidth <= 0 || this.framebufferHeight <= 0) {
            return null;
        }
        Box box = entity2.getBoundingBox().expand(0.65, 0.4, 0.65).offset(entity2.getLerpedPos(f).subtract(entity2.getX(), entity2.getY(), entity2.getZ()));
        double d2 = Double.POSITIVE_INFINITY;
        double d3 = Double.POSITIVE_INFINITY;
        double d4 = Double.NEGATIVE_INFINITY;
        double d5 = Double.NEGATIVE_INFINITY;
        boolean bl = false;
        for (int i = 0; i < 2; ++i) {
            double d6 = i == 0 ? box.minX : box.maxX;
            for (int j = 0; j < 2; ++j) {
                d = j == 0 ? box.minY : box.maxY;
                for (int k = 0; k < 2; ++k) {
                    double d7 = k == 0 ? box.minZ : box.maxZ;
                    this.clipSpacePosition.set((float)(d6 - this.cameraPosition.x), (float)(d - this.cameraPosition.y), (float)(d7 - this.cameraPosition.z), 1.0f);
                    this.viewProjectionMatrix.transform(this.clipSpacePosition);
                    if (this.clipSpacePosition.w() <= 0.0f) continue;
                    float f2 = this.clipSpacePosition.x() / this.clipSpacePosition.w();
                    float f3 = this.clipSpacePosition.y() / this.clipSpacePosition.w();
                    if (!Float.isFinite(f2) || !Float.isFinite(f3)) continue;
                    double d8 = ((double)f2 * 0.5 + 0.5) * (double)this.framebufferWidth;
                    double d9 = (1.0 - ((double)f3 * 0.5 + 0.5)) * (double)this.framebufferHeight;
                    d2 = Math.min(d2, d8);
                    d3 = Math.min(d3, d9);
                    d4 = Math.max(d4, d8);
                    d5 = Math.max(d5, d9);
                    bl = true;
                }
            }
        }
        if (!bl) {
            return null;
        }
        double d10 = d4 - d2;
        double d11 = d5 - d3;
        if (d10 < 1.0 || d11 < 1.0) {
            return null;
        }
        d = Math.max(20.0, d10 * 0.28);
        double d12 = Math.max(14.0, d11 * 0.18);
        d2 = Math.max(0.0, d2 - d);
        d3 = Math.max(0.0, d3 - d12);
        d4 = Math.min((double)this.framebufferWidth, d4 + d);
        d5 = Math.min((double)this.framebufferHeight, d5 + d12);
        d10 = d4 - d2;
        d11 = d5 - d3;
        if (d10 < 1.0 || d11 < 1.0) {
            return null;
        }
        return new ScreenRegion((float)d2, (float)((double)this.framebufferHeight - d5), (float)d10, (float)d11);
    }

    private float getElapsedTime() {
        return (float)(System.currentTimeMillis() - this.startedAtMillis) / 700.0f;
    }

    public void prepareFrame() {
        this.effectRendered = false;
        this.framePrepared = false;
        this.framebuffersCleared = false;
        this.mainFramebuffer = this.client.getFramebuffer();
        if (this.mainFramebuffer == null || this.mainFramebuffer.getColorAttachmentView() == null) {
            return;
        }
        this.ensureRenderDispatcher();
        this.framebufferWidth = this.mainFramebuffer.textureWidth;
        this.framebufferHeight = this.mainFramebuffer.textureHeight;
        this.ensureFramebuffers(this.framebufferWidth, this.framebufferHeight);
        Camera camera2 = this.client.gameRenderer.getCamera();
        this.entityRenderer = this.client.getEntityRenderDispatcher();
        if (camera2 == null || this.entityRenderer == null || this.maskFramebuffer == null || this.effectFramebuffer == null) {
            this.clearFrameState();
            return;
        }
        this.sceneColorView = this.mainFramebuffer.getColorAttachmentView();
        this.sceneDepthView = this.mainFramebuffer.getDepthAttachmentView();
        this.maskColorView = this.maskFramebuffer.getColorAttachmentView();
        this.maskDepthView = this.maskFramebuffer.getDepthAttachmentView();
        this.effectView = this.effectFramebuffer.getColorAttachmentView();
        if (this.sceneColorView == null || this.sceneDepthView == null || this.maskColorView == null || this.maskDepthView == null || this.effectView == null) {
            this.clearFrameState();
            return;
        }
        this.entityRenderer.configure(camera2, this.client.getCameraEntity());
        this.updateCameraState(camera2);
        this.cameraPosition = camera2.getCameraPos();
        this.viewProjectionMatrix.set((Matrix4fc)RenderFrameState.projectionMatrix).mul((Matrix4fc)RenderFrameState.viewMatrix);
        this.elapsedTime = this.getElapsedTime();
        this.framePrepared = true;
    }

    public void renderEntity(MatrixStack matrices2, Entity entity2, float f, int n, int n2, boolean bl, boolean bl2, int n3) {
        if (matrices2 == null || entity2 == null || !entity2.isAlive() || this.client.world == null) {
            return;
        }
        if (!bl && !bl2) {
            return;
        }
        if (!this.framePrepared) {
            this.prepareFrame();
        }
        if (!this.framePrepared || this.mainFramebuffer == null || this.sceneColorView == null || this.sceneDepthView == null || this.maskColorView == null || this.maskDepthView == null || this.effectView == null) {
            return;
        }
        ScreenRegion screenRegion = this.expandRegion(this.calculateScreenRegion(entity2, f));
        if (screenRegion == null) {
            return;
        }
        this.prepareEffectFramebuffer();
        if (!this.framebuffersCleared) {
            return;
        }
        this.clearFramebuffer(this.maskFramebuffer);
        try (MemoryStack memoryStack = MemoryStack.stackPush();
             RenderTargetOverrideScope ignored = RenderTargetOverrideScope.use(this.maskColorView, this.maskDepthView);){
            this.renderEntityToMask(matrices2, entity2, f);
        }
        this.compositor.composite(this.effectView, this.maskColorView, this.maskDepthView, this.sceneColorView, this.sceneDepthView, this.framebufferWidth, this.framebufferHeight, n, n2, bl, bl2, n3, screenRegion.x(), screenRegion.y(), screenRegion.width(), screenRegion.height(), this.elapsedTime);
        this.effectRendered = true;
    }

    private void prepareEffectFramebuffer() {
        if (this.framebuffersCleared) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            this.clearFramebuffer(this.effectFramebuffer);
            this.framebuffersCleared = true;
        }
    }

    private void ensureFramebuffers(int n, int n2) {
        int n3 = Math.max(n, 1);
        int n4 = Math.max(n2, 1);
        if (this.maskFramebuffer == null) {
            this.maskFramebuffer = new SimpleFramebuffer("wexside_chams_mask", n3, n4, true);
        } else if (this.maskFramebuffer.textureWidth != n || this.maskFramebuffer.textureHeight != n2) {
            this.maskFramebuffer.resize(n3, n4);
        }
        if (this.effectFramebuffer == null) {
            this.effectFramebuffer = new SimpleFramebuffer("wexside_chams_effect", n3, n4, true);
        } else if (this.effectFramebuffer.textureWidth != n || this.effectFramebuffer.textureHeight != n2) {
            this.effectFramebuffer.resize(n3, n4);
        }
    }

    private ScreenRegion expandRegion(ScreenRegion screenRegion) {
        if (screenRegion == null || this.framebufferWidth <= 0 || this.framebufferHeight <= 0) {
            return screenRegion;
        }
        float f = screenRegion.x();
        float f2 = screenRegion.y();
        float f3 = f + screenRegion.width();
        float f4 = f2 + screenRegion.height();
        boolean bl = f <= 52.0f || f3 >= (float)this.framebufferWidth - 52.0f;
        boolean bl2 = f2 <= 38.0f || f4 >= (float)this.framebufferHeight - 38.0f;
        float f5 = Math.max(52.0f, screenRegion.width() * 0.08f);
        float f6 = Math.max(38.0f, screenRegion.height() * 0.08f);
        if (bl) {
            f5 *= 1.75f;
        }
        if (bl2) {
            f6 *= 1.75f;
        }
        f = Math.max(0.0f, f - f5);
        f2 = Math.max(0.0f, f2 - f6);
        f3 = Math.min((float)this.framebufferWidth, f3 + f5);
        f4 = Math.min((float)this.framebufferHeight, f4 + f6);
        float f7 = f3 - f;
        float f8 = f4 - f2;
        if (f7 < 1.0f || f8 < 1.0f) {
            return screenRegion;
        }
        return new ScreenRegion(f, f2, f7, f8);
    }

    private void clearFramebuffer(SimpleFramebuffer framebuffer5) {
        if (framebuffer5 == null) {
            return;
        }
        GpuTexture gpuTexture = framebuffer5.getColorAttachment();
        if (gpuTexture == null) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        GpuTexture gpuTexture2 = framebuffer5.getDepthAttachment();
        if (gpuTexture2 != null) {
            commandEncoder.clearColorAndDepthTextures(gpuTexture, 0, gpuTexture2, 1.0);
        } else {
            commandEncoder.clearColorTexture(gpuTexture, 0);
        }
    }
}

