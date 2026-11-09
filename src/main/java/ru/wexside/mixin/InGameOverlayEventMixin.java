package ru.wexside.mixin;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.WexSideClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.OverlayRenderEvent;
import ru.wexside.event.OverlayType;

@Mixin(value={InGameOverlayRenderer.class})
public abstract class InGameOverlayEventMixin {
    @Shadow
    private ItemStack floatingItem;

    @Inject(method={"renderFloatingItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void allowTotemAnimationCancellation(MatrixStack matrices, float tickDelta, OrderedRenderCommandQueue queue, CallbackInfo callback) {
        if (this.floatingItem != null && this.floatingItem.isOf(Items.TOTEM_OF_UNDYING) && this.isCancelled(OverlayType.TOTEM)) {
            callback.cancel();
        }
    }

    @Shadow
    private static void renderInWallOverlay(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        throw new AssertionError();
    }

    @Shadow
    private static void renderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite) {
        throw new AssertionError();
    }

    @Redirect(method={"renderOverlays"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/InGameOverlayRenderer;renderInWallOverlay(Lnet/minecraft/client/texture/Sprite;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"))
    private void renderBlockOverlayConditionally(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        if (!this.isCancelled(OverlayType.BLOCK)) {
            InGameOverlayEventMixin.renderInWallOverlay(sprite, matrices, vertexConsumers);
        }
    }

    @Redirect(method={"renderOverlays"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/InGameOverlayRenderer;renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/texture/Sprite;)V"))
    private void renderFireOverlayConditionally(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite) {
        if (!this.isCancelled(OverlayType.FIRE)) {
            InGameOverlayEventMixin.renderFireOverlay(matrices, vertexConsumers, sprite);
        }
    }

    private boolean isCancelled(OverlayType type) {
        EventBus events = WexSideClient.getEventBus();
        if (events == null) {
            return false;
        }
        OverlayRenderEvent event = new OverlayRenderEvent(type);
        events.post(event);
        return event.isCancelled();
    }
}

