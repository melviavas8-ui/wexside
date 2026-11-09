package ru.wexside.render;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import ru.wexside.render.HandAuraCompositor;
import ru.wexside.render.HandKawaseBlur;
import ru.wexside.render.HandMaskDiff;
import ru.wexside.render.hand.HandEnergyShaderMode;

public final class HandEnergyRenderer {
    private HandAuraCompositor compositor;
    private boolean offhandPositionValid;
    private HandKawaseBlur blurRenderer;
    private int motionSampleCount;
    private float effectRadius = 1.0f;
    private GpuTexture sceneBeforeTexture;
    private float motionSumX;
    private float motionStrength;
    private float motionSumY;
    private int frameWidth;
    private final long animationStartMillis = System.currentTimeMillis();
    private int frameHeight;
    private boolean initialized;
    private float motionDirectionX;
    private final Vector3f previousOffhandPosition;
    private final MinecraftClient client;
    private GpuTextureView sceneAfterView;
    private boolean diffAfterCaptured;
    private GpuTexture sceneAfterTexture;
    private GpuTextureView diffAfterView;
    private int primaryColor = -1125978881;
    private GpuTextureView depthBeforeView;
    private GpuTextureView sceneBeforeView;
    private boolean fillHands;
    private float motionDirectionY;
    private final Vector3f previousMainHandPosition;
    private boolean capturePending;
    private boolean gradientEnabled;
    private GpuTextureView maskView;
    private GpuTexture diffAfterTexture;
    private HandEnergyShaderMode mode = HandEnergyShaderMode.STATIC;
    private HandMaskDiff maskRenderer;
    private static HandEnergyRenderer instance;
    private GpuTexture maskTexture;
    private GpuTexture depthBeforeTexture;
    private boolean mainHandPositionValid;
    private boolean enabled;
    private int secondaryColor = -1125978881;
    private final Vector3f currentHandPosition;

    private HandEnergyRenderer() {
        this.previousMainHandPosition = new Vector3f();
        this.previousOffhandPosition = new Vector3f();
        this.currentHandPosition = new Vector3f();
        this.client = MinecraftClient.getInstance();
    }

    public static HandEnergyRenderer getInstance() {
        if (instance == null) {
            instance = new HandEnergyRenderer();
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
        if (this.blurRenderer != null) {
            this.blurRenderer.close();
        }
        if (this.compositor != null) {
            this.compositor.close();
        }
        if (this.maskRenderer != null) {
            this.maskRenderer.close();
        }
        this.blurRenderer = new HandKawaseBlur();
        this.compositor = new HandAuraCompositor();
        this.maskRenderer = new HandMaskDiff();
        this.frameWidth = 0;
        this.frameHeight = 0;
        this.initialized = true;
    }

    public void setColor(int n) {
        this.setColors(n, n, false);
    }

    private void updateMotion() {
        float f = this.motionSampleCount > 0 ? this.motionSumX / (float)this.motionSampleCount : 0.0f;
        float f2 = this.motionSampleCount > 0 ? this.motionSumY / (float)this.motionSampleCount : 0.0f;
        float f3 = (float)Math.sqrt(f * f + f2 * f2);
        float f4 = Math.min(f3 * 2.4f, 1.0f);
        if (f3 > 1.0E-4f) {
            f /= f3;
            f2 /= f3;
        } else {
            f = 0.0f;
            f2 = 0.0f;
        }
        this.motionDirectionX += (f - this.motionDirectionX) * 0.28f;
        this.motionDirectionY += (f2 - this.motionDirectionY) * 0.28f;
        this.motionStrength += (f4 - this.motionStrength) * 0.24f;
        if (this.motionSampleCount <= 0) {
            this.motionDirectionX *= 0.92f;
            this.motionDirectionY *= 0.92f;
            this.motionStrength *= 0.88f;
        }
    }

    private void resizeTargets(int n, int n2) {
        if (n == this.frameWidth && n2 == this.frameHeight && this.sceneBeforeTexture != null) {
            return;
        }
        this.closeTargets();
        this.sceneBeforeTexture = RenderSystem.getDevice().createTexture(() -> "wexside:hand_aura_scene_before", 13, TextureFormat.RGBA8, n, n2, 1, 1);
        this.sceneBeforeView = RenderSystem.getDevice().createTextureView(this.sceneBeforeTexture);
        this.sceneAfterTexture = RenderSystem.getDevice().createTexture(() -> "wexside:hand_aura_scene_after", 13, TextureFormat.RGBA8, n, n2, 1, 1);
        this.sceneAfterView = RenderSystem.getDevice().createTextureView(this.sceneAfterTexture);
        this.depthBeforeTexture = RenderSystem.getDevice().createTexture(() -> "wexside:hand_aura_depth_before", 13, TextureFormat.DEPTH32, n, n2, 1, 1);
        this.depthBeforeView = RenderSystem.getDevice().createTextureView(this.depthBeforeTexture);
        this.maskTexture = RenderSystem.getDevice().createTexture(() -> "wexside:hand_aura_mask", 13, TextureFormat.RGBA8, n, n2, 1, 1);
        this.maskView = RenderSystem.getDevice().createTextureView(this.maskTexture);
        this.frameWidth = n;
        this.frameHeight = n2;
    }

    private void ensureDiffAfterTarget() {
        if (this.diffAfterTexture != null) {
            return;
        }
        this.diffAfterTexture = RenderSystem.getDevice().createTexture(() -> "wexside:hand_aura_diff_after", 13, TextureFormat.RGBA8, this.frameWidth, this.frameHeight, 1, 1);
        this.diffAfterView = RenderSystem.getDevice().createTextureView(this.diffAfterTexture);
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
        if (this.maskView != null) {
            this.maskView.close();
            this.maskView = null;
        }
        if (this.maskTexture != null) {
            this.maskTexture.close();
            this.maskTexture = null;
        }
        if (this.diffAfterView != null) {
            this.diffAfterView.close();
            this.diffAfterView = null;
        }
        if (this.diffAfterTexture != null) {
            this.diffAfterTexture.close();
            this.diffAfterTexture = null;
        }
        this.diffAfterCaptured = false;
    }

    public void setEffectRadius(float f) {
        this.effectRadius = Math.max(0.6f, Math.min(1.8f, f));
    }

    public void setMode(HandEnergyShaderMode handEnergyShaderMode) {
        this.mode = handEnergyShaderMode == null ? HandEnergyShaderMode.STATIC : handEnergyShaderMode;
    }

    public void releaseResources() {
        this.closeTargets();
        if (this.blurRenderer != null) {
            this.blurRenderer.close();
            this.blurRenderer = null;
        }
        if (this.compositor != null) {
            this.compositor.close();
            this.compositor = null;
        }
        if (this.maskRenderer != null) {
            this.maskRenderer.close();
            this.maskRenderer = null;
        }
        this.frameWidth = 0;
        this.frameHeight = 0;
        this.initialized = false;
        this.capturePending = false;
        this.mainHandPositionValid = false;
        this.offhandPositionValid = false;
        this.motionSumX = 0.0f;
        this.motionSumY = 0.0f;
        this.motionSampleCount = 0;
        this.motionDirectionX = 0.0f;
        this.motionDirectionY = 0.0f;
        this.motionStrength = 0.0f;
    }

    public void recordHandMotion(Hand hand, MatrixStack matrixStack) {
        boolean bl;
        if (!this.enabled || matrixStack == null) {
            return;
        }
        matrixStack.peek().getPositionMatrix().transformPosition(0.0f, 0.0f, 0.0f, this.currentHandPosition);
        Vector3f vector3f = hand == Hand.MAIN_HAND ? this.previousMainHandPosition : this.previousOffhandPosition;
        boolean bl2 = bl = hand == Hand.MAIN_HAND ? this.mainHandPositionValid : this.offhandPositionValid;
        if (bl) {
            float f = this.currentHandPosition.x - vector3f.x;
            float f2 = this.currentHandPosition.y - vector3f.y;
            float f3 = 2.8f;
            this.motionSumX += -f * f3;
            this.motionSumY += -f2 * f3;
            ++this.motionSampleCount;
        }
        vector3f.set((Vector3fc)this.currentHandPosition);
        if (hand == Hand.MAIN_HAND) {
            this.mainHandPositionValid = true;
        } else {
            this.offhandPositionValid = true;
        }
    }

    public void setColors(int n, int n2, boolean bl) {
        this.primaryColor = n;
        this.secondaryColor = n2;
        this.gradientEnabled = bl;
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
        this.motionSumX = 0.0f;
        this.motionSumY = 0.0f;
        this.motionSampleCount = 0;
        this.capturePending = true;
    }

    public void captureDiffAfter() {
        if (!this.enabled || !this.capturePending) {
            return;
        }
        Framebuffer framebuffer = this.client.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachment() == null) {
            return;
        }
        this.ensureDiffAfterTarget();
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), this.diffAfterTexture, 0, 0, 0, 0, 0, this.frameWidth, this.frameHeight);
        this.diffAfterCaptured = true;
    }

    public void compositeHands() {
        GpuTextureView gpuTextureView;
        if (!this.enabled || !this.capturePending) {
            return;
        }
        Framebuffer framebuffer = this.client.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachment() == null) {
            this.capturePending = false;
            this.diffAfterCaptured = false;
            return;
        }
        if (this.diffAfterCaptured && this.diffAfterView != null) {
            gpuTextureView = this.diffAfterView;
        } else {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), this.sceneAfterTexture, 0, 0, 0, 0, 0, this.frameWidth, this.frameHeight);
            gpuTextureView = this.sceneAfterView;
        }
        this.diffAfterCaptured = false;
        this.maskRenderer.renderMask(this.maskView, this.sceneBeforeView, gpuTextureView, this.depthBeforeView, framebuffer.getDepthAttachmentView(), this.frameWidth, this.frameHeight);
        int n = this.effectRadius >= 1.45f ? 5 : (this.effectRadius <= 0.8f ? 3 : 4);
        float f = 2.2f + this.effectRadius * 1.4f;
        GpuTextureView gpuTextureView2 = this.blurRenderer.blur(this.maskView, this.frameWidth, this.frameHeight, n, f);
        if (gpuTextureView2 == null) {
            gpuTextureView2 = this.maskView;
        }
        float f2 = (float)(System.currentTimeMillis() - this.animationStartMillis) / 850.0f;
        this.updateMotion();
        this.compositor.composite(framebuffer.getColorAttachmentView(), this.maskView, gpuTextureView2, this.frameWidth, this.frameHeight, f2, this.mode.getShaderModeId(), this.primaryColor, this.secondaryColor, this.gradientEnabled, this.fillHands, this.motionDirectionX, this.motionDirectionY, this.motionStrength);
        this.capturePending = false;
    }

    public void setEnabled(boolean bl) {
        this.enabled = bl;
        if (bl) {
            this.ensureInitialized();
        }
    }

    public void setFillHands(boolean bl) {
        this.fillHands = bl;
    }
}
