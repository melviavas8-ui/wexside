package ru.wexside.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.LavaFogModifier;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.module.render.NoRenderModule;

@Mixin(value={LavaFogModifier.class})
public abstract class LavaFogModifierMixin {
    @Inject(method={"applyStartEndModifier"}, at={@At(value="TAIL")})
    private void wexside$removeLavaFog(FogData fog, Camera camera, ClientWorld world, float viewDistance, RenderTickCounter tickCounter, CallbackInfo callback) {
        if (!NoRenderModule.isUnderLavaDisabled()) {
            return;
        }
        fog.environmentalStart = 1.0E7f;
        fog.renderDistanceStart = 1.0E7f;
        fog.environmentalEnd = 1.0E8f;
        fog.renderDistanceEnd = 1.0E8f;
        fog.skyEnd = 1.0E8f;
        fog.cloudEnd = 1.0E8f;
    }
}

