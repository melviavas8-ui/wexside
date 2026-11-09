package ru.wexside.mixin;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.module.render.AmbientModule;

@Mixin(value={World.class})
public abstract class WorldAmbientMixin {
    @Inject(method={"getTimeOfDay"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$useConfiguredTime(CallbackInfoReturnable<Long> callback) {
        if (AmbientModule.isEnabled2()) {
            callback.setReturnValue(AmbientModule.getLongType());
        }
    }
}

