package ru.wexside.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.WexSideClient;
import ru.wexside.event.AttackWindowEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.TickPhase;

@Mixin(value={MinecraftClient.class})
public abstract class MinecraftAttackWindowMixin {
    @Inject(method={"handleInputEvents"}, at={@At(value="HEAD")})
    private void beforeInputActions(CallbackInfo callback) {
        this.postAttackWindow(TickPhase.PRE);
    }

    @Inject(method={"handleInputEvents"}, at={@At(value="RETURN")})
    private void afterInputActions(CallbackInfo callback) {
        this.postAttackWindow(TickPhase.POST);
    }

    private void postAttackWindow(TickPhase phase) {
        EventBus events = WexSideClient.getEventBus();
        if (events != null) {
            events.post(new AttackWindowEvent(phase));
        }
    }
}

