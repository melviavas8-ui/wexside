package ru.wexside.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.WexSideClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.HandSwingSpeedEvent;
import ru.wexside.event.PreAttackEvent;

@Mixin(value={LivingEntity.class})
public abstract class LivingEntityEventMixin {
    @Inject(method={"getHandSwingDuration"}, at={@At(value="HEAD")}, cancellable=true)
    private void applySwingDurationOverride(CallbackInfoReturnable<Integer> callback) {
        if ((Object)this != MinecraftClient.getInstance().player) {
            return;
        }
        EventBus events = WexSideClient.getEventBus();
        if (events == null) {
            return;
        }
        HandSwingSpeedEvent event = new HandSwingSpeedEvent();
        events.post(event);
        if (event.isCancelled()) {
            callback.setReturnValue(Math.max(1, Math.round(6.0f * event.getSpeedMultiplier())));
        }
    }

    @Inject(method={"swingHand(Lnet/minecraft/util/Hand;Z)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void beforeLocalHandSwing(Hand hand, boolean fromServer, CallbackInfo callback) {
        LivingEntity entity = (LivingEntity)((Object)this);
        if (entity != MinecraftClient.getInstance().player || fromServer) {
            return;
        }
        EventBus events = WexSideClient.getEventBus();
        if (events == null) {
            return;
        }
        PreAttackEvent event = new PreAttackEvent(entity.getYaw());
        events.post(event);
        if (event.isCancelled()) {
            callback.cancel();
        }
    }
}

