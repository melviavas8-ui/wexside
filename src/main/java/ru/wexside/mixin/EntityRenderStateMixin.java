package ru.wexside.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.render.HologramImpostorRenderer;
import ru.wexside.util.NameplateRenderer;

@Mixin(value={EntityRenderer.class})
public abstract class EntityRenderStateMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method={"updateRenderState"}, at={@At(value="TAIL")})
    private void wexside$hideVanillaNameplate(T entity, S state, float tickProgress, CallbackInfo callback) {
        if (NameplateRenderer.hasCustomNameplate(entity)) {
            ((EntityRenderState)state).displayName = null;
            ((EntityRenderState)state).nameLabelPos = null;
        }
    }

    @Inject(method={"renderLabelIfPresent"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$captureHologramLabel(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo callback) {
        if (HologramImpostorRenderer.captureEntityLabel(state, matrices, camera)) {
            callback.cancel();
        }
    }
}

