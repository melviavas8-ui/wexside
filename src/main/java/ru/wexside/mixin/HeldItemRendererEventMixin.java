package ru.wexside.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.WexSideClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.FirstPersonItemTransformEvent;
import ru.wexside.event.FirstPersonSwingTransformEvent;
import ru.wexside.event.HandRenderEvent;
import ru.wexside.event.HandRenderPhase;

@Mixin(value={HeldItemRenderer.class})
public abstract class HeldItemRendererEventMixin {
    @Unique
    private Hand wexside$currentHand;
    @Unique
    private float wexside$currentEquipProgress;

    @Shadow
    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        throw new AssertionError();
    }

    @Inject(method={"renderFirstPersonItem"}, at={@At(value="HEAD")})
    private void beforeFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo callback) {
        this.wexside$currentHand = hand;
        this.wexside$currentEquipProgress = equipProgress;
        EventBus events = WexSideClient.getEventBus();
        if (events == null) {
            return;
        }
        FirstPersonItemTransformEvent event = new FirstPersonItemTransformEvent(matrices, stack, hand);
        events.post(event);
        float scale = event.getScale();
        if (scale != 1.0f) {
            matrices.scale(scale, scale, scale);
        }
    }

    @Redirect(method={"swingArm"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/render/item/HeldItemRenderer;applySwingOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"))
    private void replaceSwingTransform(HeldItemRenderer renderer, MatrixStack matrices, Arm arm, float swingProgress) {
        EventBus events = WexSideClient.getEventBus();
        if (events != null && this.wexside$currentHand != null) {
            FirstPersonSwingTransformEvent event = new FirstPersonSwingTransformEvent(matrices, this.wexside$currentHand, swingProgress, this.wexside$currentEquipProgress);
            events.post(event);
            if (event.isCancelled()) {
                return;
            }
        }
        this.applySwingOffset(matrices, arm, swingProgress);
    }

    @Inject(method={"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"}, at={@At(value="HEAD")})
    private void beforeHandRender(float tickDelta, MatrixStack matrices, OrderedRenderCommandQueue queue, ClientPlayerEntity player, int light, CallbackInfo callback) {
        EventBus events = WexSideClient.getEventBus();
        if (events != null) {
            events.post(new HandRenderEvent(HandRenderPhase.BEFORE, matrices, tickDelta));
        }
    }

    @Inject(method={"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"}, at={@At(value="RETURN")})
    private void afterHandRender(float tickDelta, MatrixStack matrices, OrderedRenderCommandQueue queue, ClientPlayerEntity player, int light, CallbackInfo callback) {
        EventBus events = WexSideClient.getEventBus();
        if (events != null) {
            events.post(new HandRenderEvent(HandRenderPhase.AFTER, matrices, tickDelta));
        }
    }
}

