package ru.wexside.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.WexSideClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.OverlayRenderEvent;
import ru.wexside.event.OverlayType;
import ru.wexside.module.hud.CrosshairModule;
import ru.wexside.module.hud.HotbarModule;

@Mixin(value={InGameHud.class})
public abstract class InGameHudEventMixin {
    @Inject(method={"renderCrosshair"}, at={@At(value="HEAD")}, cancellable=true)
    private void replaceVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callback) {
        if (CrosshairModule.isEnabled3()) {
            callback.cancel();
        }
    }

    @Inject(method={"renderHotbar"}, at={@At(value="HEAD")}, cancellable=true)
    private void replaceVanillaHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callback) {
        if (HotbarModule.isEnabled()) {
            callback.cancel();
        }
    }

    @Inject(method={"renderBossBarHud"}, at={@At(value="HEAD")}, cancellable=true)
    private void beforeBossBars(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callback) {
        this.cancelIfRequested(OverlayType.BOSS_BAR, callback);
    }

    @Inject(method={"renderStatusEffectOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private void beforeStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callback) {
        this.cancelIfRequested(OverlayType.STATUS_EFFECTS, callback);
    }

    @Inject(method={"renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void beforeScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callback) {
        this.cancelIfRequested(OverlayType.SCOREBOARD, callback);
    }

    private void cancelIfRequested(OverlayType type, CallbackInfo callback) {
        EventBus events = WexSideClient.getEventBus();
        if (events == null) {
            return;
        }
        OverlayRenderEvent event = new OverlayRenderEvent(type);
        events.post(event);
        if (event.isCancelled()) {
            callback.cancel();
        }
    }
}

