package ru.wexside.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.WexSideClient;
import ru.wexside.event.AspectRatioEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.OverlayRenderEvent;
import ru.wexside.event.OverlayType;
import ru.wexside.misc.ChamsRenderer;
import ru.wexside.misc.GlowEspRenderer;
import ru.wexside.module.render.NoRenderModule;
import ru.wexside.render.ClientEventBridge;
import ru.wexside.render.HologramImpostorRenderer;
import ru.wexside.ui.WexsideScreen;

@Mixin(value={GameRenderer.class})
public abstract class GameRendererEventMixin {
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private float fovMultiplier;
    @Shadow
    private float lastFovMultiplier;

    @Shadow
    public abstract float getFarPlaneDistance();

    @Inject(method={"render"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift=At.Shift.BEFORE)})
    private void renderWexsideHud(RenderTickCounter tickCounter, boolean tick, CallbackInfo callback) {
        ClientEventBridge.renderHudAfterVanillaGui();
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift=At.Shift.AFTER)})
    private void renderWexsidePanel(RenderTickCounter tickCounter, boolean tick, CallbackInfo callback) {
        Screen class_4372 = this.client.currentScreen;
        if (class_4372 instanceof WexsideScreen) {
            WexsideScreen screen = (WexsideScreen)class_4372;
            screen.renderPanel(tickCounter.getDynamicDeltaTicks());
        }
    }

    @Inject(method={"renderWorld"}, at={@At(value="TAIL")})
    private void wexside$finishDeferredWorldEffects(RenderTickCounter tickCounter, CallbackInfo callback) {
        HologramImpostorRenderer.flush();
        ChamsRenderer.presentPendingChams();
        GlowEspRenderer.renderPendingGlow();
    }

    @Inject(method={"tiltViewWhenHurt"}, at={@At(value="HEAD")}, cancellable=true)
    private void allowHurtCameraCancellation(MatrixStack matrices, float tickDelta, CallbackInfo callback) {
        EventBus events = WexSideClient.getEventBus();
        if (events == null) {
            return;
        }
        OverlayRenderEvent event = new OverlayRenderEvent(OverlayType.CAMERA_HURT);
        events.post(event);
        if (event.isCancelled()) {
            callback.cancel();
        }
    }

    @Inject(method={"bobView"}, at={@At(value="HEAD")}, cancellable=true)
    private void disableViewBobbing(MatrixStack matrices, float tickDelta, CallbackInfo callback) {
        if (NoRenderModule.isViewBobbingDisabled()) {
            callback.cancel();
        }
    }

    @Inject(method={"updateFovMultiplier"}, at={@At(value="HEAD")}, cancellable=true)
    private void disableDynamicFov(CallbackInfo callback) {
        if (!NoRenderModule.isEnabled()) {
            return;
        }
        this.lastFovMultiplier = 1.0f;
        this.fovMultiplier = 1.0f;
        callback.cancel();
    }

    @Inject(method={"getBasicProjectionMatrix"}, at={@At(value="HEAD")}, cancellable=true)
    private void useConfiguredAspectRatio(float fov, CallbackInfoReturnable<Matrix4f> callback) {
        EventBus events = WexSideClient.getEventBus();
        if (events == null) {
            return;
        }
        float vanillaRatio = (float)this.client.getWindow().getFramebufferWidth() / (float)Math.max(1, this.client.getWindow().getFramebufferHeight());
        AspectRatioEvent event = new AspectRatioEvent(vanillaRatio);
        events.post(event);
        if (Float.compare(event.getAspectRatio(), vanillaRatio) == 0) {
            return;
        }
        callback.setReturnValue(new Matrix4f().perspective(fov * ((float)Math.PI / 180), event.getAspectRatio(), 0.05f, this.getFarPlaneDistance()));
    }
}

