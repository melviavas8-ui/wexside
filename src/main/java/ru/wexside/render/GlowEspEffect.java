package ru.wexside.render;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.List;
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
import ru.wexside.render.GlowCompositeMode;
import ru.wexside.render.GlowOutlineCompositor;
import ru.wexside.render.RenderFrameState;
import ru.wexside.render.RenderTargetOverrideScope;

public final class GlowEspEffect {
    private GpuTextureView maskDepthView;
    private int sceneWidth;
    private GpuTextureView outlineView;
    private GpuTextureView sceneColorView;
    private Vec3d cameraPosition;
    private boolean framebuffersCleared;
    private SimpleFramebuffer outlineFramebuffer;
    private SimpleFramebuffer horizontalOutlineFramebuffer;
    private EntityRenderManager entityRenderer;
    private GpuTextureView horizontalOutlineView;
    private final GlowOutlineCompositor compositor;
    private int sceneHeight;
    private final Matrix4f viewProjectionMatrix;
    private final long startedAtMillis;
    private GpuTextureView maskColorView;
    private float elapsedTime;
    private int effectWidth;
    private RenderDispatcher renderDispatcher;
    private SimpleFramebuffer maskFramebuffer;
    private BufferAllocator auxiliaryBufferAllocator;
    private GpuTextureView gradientView;
    private int effectHeight;
    private OutlineVertexConsumerProvider outlineVertexConsumers;
    private boolean framePrepared;
    private final CameraRenderState cameraRenderState;
    private SimpleFramebuffer gradientFramebuffer;
    private VertexConsumerProvider.Immediate auxiliaryVertexConsumers;
    private VertexConsumerProvider.Immediate entityVertexConsumers;
    private final Vector4f clipSpacePosition;
    private BufferAllocator entityBufferAllocator;
    private OrderedRenderCommandQueueImpl renderQueue;
    private Framebuffer mainFramebuffer;
    private boolean maskRendered;
    private final MinecraftClient client = MinecraftClient.getInstance();

    public GlowEspEffect() {
        this.compositor = new GlowOutlineCompositor();
        this.startedAtMillis = System.currentTimeMillis();
        this.viewProjectionMatrix = new Matrix4f();
        this.clipSpacePosition = new Vector4f();
        this.cameraRenderState = new CameraRenderState();
        this.cameraPosition = Vec3d.ZERO;
    }

    private void clearFramebuffer(SimpleFramebuffer framebuffer7) {
        if (framebuffer7 == null || framebuffer7.getColorAttachment() == null) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        GpuTexture gpuTexture = framebuffer7.getColorAttachment();
        GpuTexture gpuTexture2 = framebuffer7.getDepthAttachment();
        if (gpuTexture2 != null) {
            commandEncoder.clearColorAndDepthTextures(gpuTexture, 0, gpuTexture2, 1.0);
        } else {
            commandEncoder.clearColorTexture(gpuTexture, 0);
        }
    }

    public boolean prepareFrame() {
        if (this.client.world == null) {
            this.resetFrameState();
            return false;
        }
        this.initializeFrame();
        if (!this.framePrepared || this.sceneColorView == null || this.maskColorView == null || this.maskDepthView == null || this.gradientView == null || this.horizontalOutlineView == null || this.outlineView == null) {
            this.clearFrameState();
            return false;
        }
        this.clearEffectFramebuffers();
        if (!this.framebuffersCleared) {
            this.clearFrameState();
            return false;
        }
        return true;
    }

    public void composite(float outlineSize, GlowCompositeMode compositeMode) {
        if (!this.maskRendered || this.sceneColorView == null) {
            this.clearFrameState();
            return;
        }
        float scaledOutlineSize = Math.max(1.0f, (float)Math.round(outlineSize * 0.5f));
        this.compositor.applyOutline(this.horizontalOutlineView, this.gradientView, this.maskColorView, this.effectWidth, this.effectHeight, scaledOutlineSize, 1.0f, 0.0f, false);
        this.compositor.applyOutline(this.outlineView, this.horizontalOutlineView, this.gradientView, this.effectWidth, this.effectHeight, scaledOutlineSize, 0.0f, 1.0f, true);
        if (compositeMode.drawsInnerGlow()) {
            this.compositor.present(this.sceneColorView, this.gradientView, this.sceneWidth, this.sceneHeight);
        }
        if (compositeMode.drawsOuterGlow()) {
            this.compositor.present(this.sceneColorView, this.outlineView, this.sceneWidth, this.sceneHeight);
        }
        this.clearFrameState();
    }

    private void clearEffectFramebuffers() {
        if (this.framebuffersCleared) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            this.clearFramebuffer(this.maskFramebuffer);
            this.clearFramebuffer(this.gradientFramebuffer);
            this.clearFramebuffer(this.horizontalOutlineFramebuffer);
            this.clearFramebuffer(this.outlineFramebuffer);
            this.framebuffersCleared = true;
        }
    }

    public void resetFrameState() {
        this.clearFrameState();
    }

    public void releaseFramebuffers() {
        this.clearFrameState();
        if (this.maskFramebuffer != null) {
            this.maskFramebuffer.delete();
        }
        if (this.gradientFramebuffer != null) {
            this.gradientFramebuffer.delete();
        }
        if (this.horizontalOutlineFramebuffer != null) {
            this.horizontalOutlineFramebuffer.delete();
        }
        if (this.outlineFramebuffer != null) {
            this.outlineFramebuffer.delete();
        }
        this.maskFramebuffer = null;
        this.gradientFramebuffer = null;
        this.horizontalOutlineFramebuffer = null;
        this.outlineFramebuffer = null;
    }

    private void ensureFramebuffers() {
        this.maskFramebuffer = this.ensureFramebufferSize(this.maskFramebuffer, "wexside_glow_esp_mask", this.effectWidth, this.effectHeight, true);
        this.gradientFramebuffer = this.ensureFramebufferSize(this.gradientFramebuffer, "wexside_glow_esp_gradient", this.effectWidth, this.effectHeight, false);
        this.horizontalOutlineFramebuffer = this.ensureFramebufferSize(this.horizontalOutlineFramebuffer, "wexside_glow_esp_outline_temp", this.effectWidth, this.effectHeight, false);
        this.outlineFramebuffer = this.ensureFramebufferSize(this.outlineFramebuffer, "wexside_glow_esp_outline", this.effectWidth, this.effectHeight, false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean renderEntities(MatrixStack matrices2, List<? extends Entity> list, float f, int n, boolean bl) {
        if (matrices2 == null || list == null || list.isEmpty() || !this.framePrepared || !this.framebuffersCleared || this.client.world == null) {
            return false;
        }
        boolean bl2 = false;
        MemoryStack memoryStack = MemoryStack.stackPush();
        RenderSystem.getModelViewStack().pushMatrix();
        try (RenderTargetOverrideScope ignored = RenderTargetOverrideScope.use(this.maskColorView, this.maskDepthView);){
            for (Entity class_12972 : list) {
                if (class_12972 == null || !class_12972.isAlive() || !this.isEntityVisible(class_12972, f)) continue;
                bl2 |= this.renderEntity(matrices2, class_12972, f);
            }
            if (bl2) {
                this.renderDispatcher.render();
                this.renderDispatcher.endLayeredCustoms();
                this.entityVertexConsumers.draw();
                this.auxiliaryVertexConsumers.draw();
                this.outlineVertexConsumers.draw();
                this.renderQueue.onNextFrame();
            }
        }
        finally {
            RenderSystem.getModelViewStack().popMatrix();
            memoryStack.close();
            this.renderQueue.clear();
        }
        if (!bl2) {
            return false;
        }
        this.compositor.applyGradient(this.gradientView, this.maskColorView, this.effectWidth, this.effectHeight, n, bl, 0.0f, 0.0f, this.effectWidth, this.effectHeight, this.elapsedTime);
        this.maskRendered = true;
        return true;
    }

    public void close() {
        this.releaseFramebuffers();
        if (this.renderDispatcher != null) {
            this.renderDispatcher.close();
        }
        if (this.entityBufferAllocator != null) {
            this.entityBufferAllocator.close();
        }
        if (this.auxiliaryBufferAllocator != null) {
            this.auxiliaryBufferAllocator.close();
        }
        this.renderDispatcher = null;
        this.entityBufferAllocator = null;
        this.auxiliaryBufferAllocator = null;
        this.entityVertexConsumers = null;
        this.auxiliaryVertexConsumers = null;
        this.outlineVertexConsumers = null;
        this.renderQueue = null;
        this.compositor.close();
    }

    private boolean isEntityVisible(Entity entity2, float f) {
        if (this.sceneWidth <= 0 || this.sceneHeight <= 0) {
            return false;
        }
        Box box = entity2.getBoundingBox().expand(0.65, 0.4, 0.65).offset(entity2.getLerpedPos(f).subtract(entity2.getX(), entity2.getY(), entity2.getZ()));
        double d = Double.POSITIVE_INFINITY;
        double d2 = Double.POSITIVE_INFINITY;
        double d3 = Double.NEGATIVE_INFINITY;
        double d4 = Double.NEGATIVE_INFINITY;
        boolean bl = false;
        for (int i = 0; i < 2; ++i) {
            double d5 = i == 0 ? box.minX : box.maxX;
            for (int j = 0; j < 2; ++j) {
                double d6 = j == 0 ? box.minY : box.maxY;
                for (int k = 0; k < 2; ++k) {
                    double d7 = k == 0 ? box.minZ : box.maxZ;
                    this.clipSpacePosition.set((float)(d5 - this.cameraPosition.x), (float)(d6 - this.cameraPosition.y), (float)(d7 - this.cameraPosition.z), 1.0f);
                    this.viewProjectionMatrix.transform(this.clipSpacePosition);
                    float f2 = this.clipSpacePosition.w();
                    if (f2 <= 0.0f) continue;
                    float f3 = this.clipSpacePosition.x() / f2;
                    float f4 = this.clipSpacePosition.y() / f2;
                    if (!Float.isFinite(f3) || !Float.isFinite(f4)) continue;
                    double d8 = ((double)f3 * 0.5 + 0.5) * (double)this.sceneWidth;
                    double d9 = (1.0 - ((double)f4 * 0.5 + 0.5)) * (double)this.sceneHeight;
                    d = Math.min(d, d8);
                    d2 = Math.min(d2, d9);
                    d3 = Math.max(d3, d8);
                    d4 = Math.max(d4, d9);
                    bl = true;
                }
            }
        }
        if (!bl) {
            return false;
        }
        return d3 - d >= 1.0 && d4 - d2 >= 1.0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean renderEntity(MatrixStack matrices2, Entity entity2, float f) {
        EntityRenderState renderState = this.entityRenderer.getAndUpdateRenderState(entity2, f);
        if (renderState == null) {
            return false;
        }
        this.prepareRenderState(renderState);
        matrices2.push();
        try {
            this.entityRenderer.render(renderState, this.cameraRenderState, renderState.x - this.cameraPosition.x, renderState.y - this.cameraPosition.y, renderState.z - this.cameraPosition.z, matrices2, (OrderedRenderCommandQueue)this.renderQueue);
        }
        finally {
            matrices2.pop();
        }
        return true;
    }

    private void updateCameraState(Camera camera2) {
        this.cameraRenderState.initialized = true;
        this.cameraRenderState.pos = camera2.getCameraPos();
        this.cameraRenderState.blockPos = camera2.getBlockPos();
        this.cameraRenderState.entityPos = this.cameraRenderState.pos;
        this.cameraRenderState.orientation = camera2.getRotation();
    }

    private void initializeFrame() {
        this.framePrepared = false;
        this.framebuffersCleared = false;
        this.maskRendered = false;
        this.mainFramebuffer = this.client.getFramebuffer();
        if (this.mainFramebuffer == null || this.mainFramebuffer.getColorAttachmentView() == null) {
            return;
        }
        this.ensureRenderDispatcher();
        this.sceneWidth = this.mainFramebuffer.textureWidth;
        this.sceneHeight = this.mainFramebuffer.textureHeight;
        this.effectWidth = Math.max(1, this.sceneWidth / 2);
        this.effectHeight = Math.max(1, this.sceneHeight / 2);
        this.ensureFramebuffers();
        Camera camera2 = this.client.gameRenderer.getCamera();
        this.entityRenderer = this.client.getEntityRenderDispatcher();
        if (camera2 == null || this.entityRenderer == null) {
            this.clearFrameState();
            return;
        }
        this.sceneColorView = this.mainFramebuffer.getColorAttachmentView();
        this.maskColorView = this.maskFramebuffer.getColorAttachmentView();
        this.maskDepthView = this.maskFramebuffer.getDepthAttachmentView();
        this.gradientView = this.gradientFramebuffer.getColorAttachmentView();
        this.horizontalOutlineView = this.horizontalOutlineFramebuffer.getColorAttachmentView();
        this.outlineView = this.outlineFramebuffer.getColorAttachmentView();
        if (this.sceneColorView == null || this.maskColorView == null || this.maskDepthView == null || this.gradientView == null || this.horizontalOutlineView == null || this.outlineView == null) {
            this.clearFrameState();
            return;
        }
        this.entityRenderer.configure(camera2, this.client.getCameraEntity());
        this.updateCameraState(camera2);
        this.cameraPosition = camera2.getCameraPos();
        this.viewProjectionMatrix.set((Matrix4fc)RenderFrameState.projectionMatrix).mul((Matrix4fc)RenderFrameState.viewMatrix);
        this.elapsedTime = (float)(System.currentTimeMillis() - this.startedAtMillis) / 700.0f;
        this.framePrepared = true;
    }

    private void clearFrameState() {
        this.framePrepared = false;
        this.framebuffersCleared = false;
        this.maskRendered = false;
        this.mainFramebuffer = null;
        this.sceneColorView = null;
        this.maskColorView = null;
        this.maskDepthView = null;
        this.gradientView = null;
        this.horizontalOutlineView = null;
        this.outlineView = null;
        this.entityRenderer = null;
        this.cameraPosition = Vec3d.ZERO;
        this.sceneWidth = 0;
        this.sceneHeight = 0;
        this.effectWidth = 0;
        this.effectHeight = 0;
        this.elapsedTime = 0.0f;
    }

    private SimpleFramebuffer ensureFramebufferSize(SimpleFramebuffer framebuffer7, String string, int n, int n2, boolean bl) {
        if (framebuffer7 == null) {
            return new SimpleFramebuffer(string, n, n2, bl);
        }
        if (framebuffer7.textureWidth != n || framebuffer7.textureHeight != n2) {
            framebuffer7.resize(n, n2);
        }
        return framebuffer7;
    }

    private void ensureRenderDispatcher() {
        if (this.renderDispatcher != null) {
            return;
        }
        this.entityBufferAllocator = new BufferAllocator(0x100000);
        this.auxiliaryBufferAllocator = new BufferAllocator(262144);
        this.entityVertexConsumers = VertexConsumerProvider.immediate((BufferAllocator)this.entityBufferAllocator);
        this.auxiliaryVertexConsumers = VertexConsumerProvider.immediate((BufferAllocator)this.auxiliaryBufferAllocator);
        this.outlineVertexConsumers = new OutlineVertexConsumerProvider();
        this.renderQueue = new OrderedRenderCommandQueueImpl();
        this.renderDispatcher = new RenderDispatcher(this.renderQueue, this.client.getBlockRenderManager(), this.entityVertexConsumers, this.client.getAtlasManager(), this.outlineVertexConsumers, this.auxiliaryVertexConsumers, this.client.textRenderer);
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
}

