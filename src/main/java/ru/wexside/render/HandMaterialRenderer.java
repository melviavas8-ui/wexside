package ru.wexside.render;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import ru.wexside.render.HandMaskDiff;
import ru.wexside.render.HandMaterialCompositor;
import ru.wexside.render.HandMaterialSource;
import ru.wexside.render.hand.HandMaterialShaderMode;

public final class HandMaterialRenderer {
    private GpuTexture depthBeforeTexture;
    private boolean enabled = false;
    private GpuTextureView depthAfterView;
    private HandMaterialCompositor compositor;
    private GpuTexture sceneAfterTexture;
    private GpuTextureView maskView;
    private boolean reflect = true;
    private final MinecraftClient client;
    private GpuTextureView sceneBeforeView;
    private GpuTexture maskTexture;
    private float radius = 1.0f;
    private GpuTexture depthAfterTexture;
    private static HandMaterialRenderer instance;
    private float saturation = 1.0f;
    private boolean initialized = false;
    private int unusedMaterialStepCount = 4;
    private int secondaryColor = 0;
    private int frameWidth = 0;
    private HandMaterialShaderMode mode;
    private float unusedMaterialScalarA = 0.3f;
    private HandMaterialSource sourceRenderer;
    private float unusedMaterialScalarB = 0.1f;
    private int frameHeight = 0;
    private float unusedMaterialScalarC = 6.0f;
    private GpuTexture sceneBeforeTexture;
    private GpuTexture sourceTexture;
    private int primaryColor = 0;
    private boolean gradientEnabled = false;
    private final long animationStartMillis = System.currentTimeMillis();
    private GpuTextureView depthBeforeView;
    private GpuTextureView sceneAfterView;
    private HandMaskDiff maskRenderer;
    private boolean capturePending = false;
    private GpuTextureView sourceView;

    private HandMaterialRenderer() {
        this.mode = HandMaterialShaderMode.GLASS;
        this.client = MinecraftClient.getInstance();
    }

    public static HandMaterialRenderer getInstance() {
        if (instance == null) {
            instance = new HandMaterialRenderer();
        }
        return instance;
    }

    public void close() {
        this.releaseResources();
    }

    private void ensureInitialized() {
        if (this.initialized) {
            return;
        }
        if (this.compositor != null) {
            this.compositor.close();
        }
        if (this.sourceRenderer != null) {
            this.sourceRenderer.close();
        }
        if (this.maskRenderer != null) {
            this.maskRenderer.close();
        }
        this.compositor = new HandMaterialCompositor();
        this.sourceRenderer = new HandMaterialSource();
        this.maskRenderer = new HandMaskDiff();
        this.frameWidth = 0;
        this.frameHeight = 0;
        this.initialized = true;
    }

    public boolean isCapturePending() {
        return this.capturePending;
    }

    public void setColor(int n) {
        this.setColors(n, n, false);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    private void closeTargets() {
        if (this.sceneBeforeView != null) {
            this.sceneBeforeView.close();
            this.sceneBeforeView = null;
        }
        if (this.sceneBeforeTexture != null) {
            this.sceneBeforeTexture.close();
            this.sceneBeforeTexture = null;
        }
        if (this.sceneAfterView != null) {
            this.sceneAfterView.close();
            this.sceneAfterView = null;
        }
        if (this.sceneAfterTexture != null) {
            this.sceneAfterTexture.close();
            this.sceneAfterTexture = null;
        }
        if (this.depthBeforeView != null) {
            this.depthBeforeView.close();
            this.depthBeforeView = null;
        }
        if (this.depthBeforeTexture != null) {
            this.depthBeforeTexture.close();
            this.depthBeforeTexture = null;
        }
        if (this.depthAfterView != null) {
            this.depthAfterView.close();
            this.depthAfterView = null;
        }
        if (this.depthAfterTexture != null) {
            this.depthAfterTexture.close();
            this.depthAfterTexture = null;
        }
        if (this.maskView != null) {
            this.maskView.close();
            this.maskView = null;
        }
        if (this.maskTexture != null) {
            this.maskTexture.close();
            this.maskTexture = null;
        }
        if (this.sourceView != null) {
            this.sourceView.close();
            this.sourceView = null;
        }
        if (this.sourceTexture != null) {
            this.sourceTexture.close();
            this.sourceTexture = null;
        }
    }

    private void resizeTargets(int n, int n2) {
        if (n == this.frameWidth && n2 == this.frameHeight && this.sceneBeforeTexture != null) {
            return;
        }
        this.closeTargets();
        this.sceneBeforeTexture = RenderSystem.getDevice().createTexture(() -> "wexside:glass_scene_before", 13, TextureFormat.RGBA8, n, n2, 1, 1);
        this.sceneBeforeView = RenderSystem.getDevice().createTextureView(this.sceneBeforeTexture);
        this.sceneAfterTexture = RenderSystem.getDevice().createTexture(() -> "wexside:glass_scene_after", 13, TextureFormat.RGBA8, n, n2, 1, 1);
        this.sceneAfterView = RenderSystem.getDevice().createTextureView(this.sceneAfterTexture);
        this.depthBeforeTexture = RenderSystem.getDevice().createTexture(() -> "wexside:glass_depth_before", 13, TextureFormat.DEPTH32, n, n2, 1, 1);
        this.depthBeforeView = RenderSystem.getDevice().createTextureView(this.depthBeforeTexture);
        this.depthAfterTexture = RenderSystem.getDevice().createTexture(() -> "wexside:glass_depth_after", 13, TextureFormat.DEPTH32, n, n2, 1, 1);
        this.depthAfterView = RenderSystem.getDevice().createTextureView(this.depthAfterTexture);
        this.maskTexture = RenderSystem.getDevice().createTexture(() -> "wexside:glass_mask", 13, TextureFormat.RGBA8, n, n2, 1, 1);
        this.maskView = RenderSystem.getDevice().createTextureView(this.maskTexture);
        this.sourceTexture = RenderSystem.getDevice().createTexture(() -> "wexside:glass_source", 13, TextureFormat.RGBA8, n, n2, 1, 1);
        this.sourceView = RenderSystem.getDevice().createTextureView(this.sourceTexture);
        this.frameWidth = n;
        this.frameHeight = n2;
    }

    public void setMode(HandMaterialShaderMode handMaterialShaderMode) {
        this.mode = handMaterialShaderMode == null ? HandMaterialShaderMode.GLASS : handMaterialShaderMode;
    }

    public void setSaturation(float f) {
        this.saturation = f;
    }

    public void releaseResources() {
        this.closeTargets();
        if (this.compositor != null) {
            this.compositor.close();
        }
        if (this.sourceRenderer != null) {
            this.sourceRenderer.close();
        }
        if (this.maskRenderer != null) {
            this.maskRenderer.close();
        }
        this.compositor = null;
        this.sourceRenderer = null;
        this.maskRenderer = null;
        this.frameWidth = 0;
        this.frameHeight = 0;
        this.initialized = false;
        this.capturePending = false;
    }

    public void setReflect(boolean bl) {
        this.reflect = bl;
    }

    public void setUnusedMaterialScalarB(float f) {
        this.unusedMaterialScalarB = f;
    }

    public void setRadius(float f) {
        this.radius = Math.max(0.6f, Math.min(1.8f, f));
    }

    public void setUnusedMaterialStepCount(int n) {
        this.unusedMaterialStepCount = Math.max(1, Math.min(8, n));
    }

    public void captureSceneAfter() {
        if (!this.enabled || !this.capturePending) {
            return;
        }
        Framebuffer framebuffer = this.client.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachment() == null) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), this.sceneAfterTexture, 0, 0, 0, 0, 0, this.frameWidth, this.frameHeight);
        if (framebuffer.getDepthAttachment() != null) {
            commandEncoder.copyTextureToTexture(framebuffer.getDepthAttachment(), this.depthAfterTexture, 0, 0, 0, 0, 0, this.frameWidth, this.frameHeight);
        }
    }

    public void captureSceneBefore() {
        if (!this.enabled) {
            return;
        }
        this.ensureInitialized();
        Framebuffer framebuffer = this.client.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachment() == null) {
            return;
        }
        int n = framebuffer.textureWidth;
        int n2 = framebuffer.textureHeight;
        this.resizeTargets(n, n2);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), this.sceneBeforeTexture, 0, 0, 0, 0, 0, n, n2);
        if (framebuffer.getDepthAttachment() != null) {
            commandEncoder.copyTextureToTexture(framebuffer.getDepthAttachment(), this.depthBeforeTexture, 0, 0, 0, 0, 0, n, n2);
        }
        this.capturePending = true;
    }

    public void compositeHands() {
        if (!this.enabled || !this.capturePending) {
            return;
        }
        Framebuffer framebuffer = this.client.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachment() == null) {
            this.capturePending = false;
            return;
        }
        this.maskRenderer.renderMask(this.maskView, this.sceneBeforeView, this.sceneAfterView, this.depthBeforeView, this.depthAfterView, this.frameWidth, this.frameHeight);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.clearColorTexture(this.sourceTexture, 0);
        this.sourceRenderer.renderSource(this.sourceView, this.sceneAfterView, this.maskView, this.frameWidth, this.frameHeight);
        this.compositor.apply(framebuffer.getColorAttachmentView(), this.sourceView, this.depthAfterView != null ? this.depthAfterView : this.depthBeforeView, this.sceneBeforeView, this.depthBeforeView != null ? this.depthBeforeView : this.depthAfterView, this.frameWidth, this.frameHeight, this.primaryColor, this.secondaryColor, this.gradientEnabled, this.mode.getShaderModeId(), 0.0f, 0.0f, this.frameWidth, this.frameHeight, (float)(System.currentTimeMillis() - this.animationStartMillis) / 700.0f, this.radius);
        this.capturePending = false;
    }

    public void setEnabled(boolean bl) {
        this.enabled = bl;
        if (bl) {
            this.ensureInitialized();
        }
    }

    public void setUnusedMaterialScalarC(float f) {
        this.unusedMaterialScalarC = f;
    }

    public void setUnusedMaterialScalarA(float f) {
        this.unusedMaterialScalarA = f;
    }

    public void setColors(int n, int n2, boolean bl) {
        this.primaryColor = n;
        this.secondaryColor = n2;
        this.gradientEnabled = bl;
    }
}
