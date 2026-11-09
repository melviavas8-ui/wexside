package ru.wexside.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.module.render.AmbientModule;
import ru.wexside.util.ColorUtils;

@Mixin(value={FogRenderer.class})
public abstract class FogRendererMixin {
    @Inject(method={"getFogColor"}, at={@At(value="RETURN")}, cancellable=true)
    private void wexside$applyConfiguredFogColor(Camera camera, float tickProgress, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> callback) {
        if (!AmbientModule.isEnabled4()) {
            return;
        }
        float[] color = ColorUtils.toNormalizedRgba(AmbientModule.getIntType2());
        callback.setReturnValue(new Vector4f(color[0], color[1], color[2], 1.0f));
    }
}

