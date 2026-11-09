package ru.wexside.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.WexSideClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.MovementSlowdownEvent;

@Mixin(value={ClientPlayerEntity.class})
public abstract class ClientPlayerMovementEventMixin {
    @Inject(method={"shouldSlowDown"}, at={@At(value="HEAD")}, cancellable=true)
    private void allowSlowdownCancellation(CallbackInfoReturnable<Boolean> callback) {
        EventBus events = WexSideClient.getEventBus();
        if (events == null) {
            return;
        }
        MovementSlowdownEvent event = new MovementSlowdownEvent();
        events.post(event);
        if (event.isCancelled()) {
            callback.setReturnValue(false);
        }
    }
}

