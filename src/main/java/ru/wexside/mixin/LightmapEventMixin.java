package ru.wexside.mixin;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.WexSideClient;
import ru.wexside.event.BrightnessEvent;
import ru.wexside.event.EventBus;

@Mixin(value={LightmapTextureManager.class})
public abstract class LightmapEventMixin {
    @Inject(method={"getBrightness(FI)F"}, at={@At(value="RETURN")}, cancellable=true)
    private static void applyBrightnessOverride(float ambientLight, int lightLevel, CallbackInfoReturnable<Float> callback) {
        EventBus events = WexSideClient.getEventBus();
        if (events == null) {
            return;
        }
        BrightnessEvent event = new BrightnessEvent(callback.getReturnValueF());
        events.post(event);
        callback.setReturnValue(Float.valueOf(event.getBrightness()));
    }
}

