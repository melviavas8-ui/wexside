package ru.wexside.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.module.render.AmbientModule;

@Mixin(value={SkyRendering.class})
public abstract class SkyRenderingMixin {
    @Inject(method={"updateRenderState"}, at={@At(value="TAIL")})
    private void wexside$applyConfiguredSkyColor(ClientWorld world, float tickProgress, Camera camera, SkyRenderState state, CallbackInfo callback) {
        if (AmbientModule.isEnabled5()) {
            state.skyColor = AmbientModule.getIntType();
        }
    }
}

