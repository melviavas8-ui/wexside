package ru.wexside.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.module.render.SeeInvisiblesModule;

@Mixin(value={LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin {
    @Shadow
    public abstract Identifier getTexture(LivingEntityRenderState var1);

    @Inject(method={"getRenderLayer"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$useTranslucentLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderLayer> callback) {
        if (SeeInvisiblesModule.isEnabled() && state.invisible) {
            callback.setReturnValue(RenderLayers.itemEntityTranslucentCull(this.getTexture(state)));
        }
    }

    @Inject(method={"getMixColor"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$applyInvisibleAlpha(LivingEntityRenderState state, CallbackInfoReturnable<Integer> callback) {
        if (SeeInvisiblesModule.isEnabled() && state.invisible) {
            callback.setReturnValue(SeeInvisiblesModule.getIntType());
        }
    }
}

