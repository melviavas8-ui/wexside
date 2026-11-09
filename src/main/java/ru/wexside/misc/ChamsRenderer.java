package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.ModelEspSettings;
import ru.wexside.model.esp.EspTargetClassifier;
import ru.wexside.render.ChamsEffect;
import ru.wexside.render.ChamsSettings;
import ru.wexside.render.RenderCamera;
import ru.wexside.util.EspFeatureRegistry;

public final class ChamsRenderer {
    private static final int MODEL_ESP_MATERIAL_MODE = 5;
    private static volatile ChamsRenderer instance;
    private final EspFeatureRegistry espFeatures;
    private final ChamsEffect chamsEffect;

    public ChamsRenderer(EventBus eventBus, EspFeatureRegistry espFeatures) {
        this.espFeatures = espFeatures;
        this.chamsEffect = new ChamsEffect();
        instance = this;
        eventBus.subscribe(WorldRenderEvent.class, this::renderPlayerChams);
        eventBus.subscribe(WorldSessionEvent.class, this::onWorldSessionChanged);
    }

    private void onWorldSessionChanged(WorldSessionEvent event) {
        this.chamsEffect.releaseFramebuffers();
    }

    private ChamsSettings getSettings(PlayerEntity player) {
        return this.espFeatures.getChamsSettings(EspTargetClassifier.relation((Entity)player));
    }

    private ModelEspSettings getModelSettings(PlayerEntity player) {
        return this.espFeatures.getModelEspSettings(EspTargetClassifier.relation((Entity)player));
    }

    private boolean hasEnabledEffects() {
        return this.espFeatures.hasEnabledChams() || this.espFeatures.hasEnabledModelEsp();
    }

    private void renderPlayerChams(WorldRenderEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!this.hasEnabledEffects() || mc.world == null || mc.player == null || RenderCamera.position() == null) {
            this.chamsEffect.resetFrameState();
            return;
        }
        boolean framePrepared = false;
        boolean renderedAnyPlayer = false;
        for (PlayerEntity player : mc.world.getPlayers()) {
            ModelEspSettings modelSettings;
            if (!player.isAlive() || player == mc.player) continue;
            ChamsSettings chamsSettings = this.getSettings(player);
            if (chamsSettings != null && chamsSettings.isEnabled()) {
                boolean renderVisibleParts = chamsSettings.isVisibleFillEnabled();
                boolean renderHiddenParts = chamsSettings.isHiddenFillEnabled();
                if (renderVisibleParts || renderHiddenParts) {
                    if (!framePrepared) {
                        this.chamsEffect.prepareFrame();
                        framePrepared = true;
                    }
                    this.chamsEffect.renderEntity(event.getMatrices(), (Entity)player, event.getFloatType(), chamsSettings.getVisibleColor(), chamsSettings.getHiddenColor(), renderVisibleParts, renderHiddenParts, chamsSettings.getMaterialModeIndex());
                    renderedAnyPlayer = true;
                }
            }
            if ((modelSettings = this.getModelSettings(player)) == null || !modelSettings.isEnabled() || !modelSettings.isModelStyle()) continue;
            if (!framePrepared) {
                this.chamsEffect.prepareFrame();
                framePrepared = true;
            }
            this.chamsEffect.renderEntity(event.getMatrices(), (Entity)player, event.getFloatType(), modelSettings.getOutlineColor(), modelSettings.getFillColor(), true, modelSettings.isFillEnabled(), 5);
            renderedAnyPlayer = true;
        }
        if (!renderedAnyPlayer) {
            this.chamsEffect.resetFrameState();
        }
    }

    public static void presentPendingChams() {
        ChamsRenderer renderer = instance;
        if (renderer == null || !renderer.hasEnabledEffects()) {
            return;
        }
        renderer.chamsEffect.present();
    }
}

