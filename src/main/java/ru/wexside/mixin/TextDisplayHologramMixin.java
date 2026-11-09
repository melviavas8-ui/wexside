package ru.wexside.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.client.render.entity.state.TextDisplayEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.render.HologramImpostorRenderer;

@Mixin(value={DisplayEntityRenderer.TextDisplayEntityRenderer.class})
public abstract class TextDisplayHologramMixin {
    @Inject(method={"render(Lnet/minecraft/client/render/entity/state/TextDisplayEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IF)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$captureTextDisplay(TextDisplayEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, float tickProgress, CallbackInfo callback) {
        if (HologramImpostorRenderer.captureTextDisplay(state, matrices, tickProgress)) {
            callback.cancel();
        }
    }
}

