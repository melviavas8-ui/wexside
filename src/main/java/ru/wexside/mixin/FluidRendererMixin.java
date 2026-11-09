package ru.wexside.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.module.render.NoRenderModule;

@Mixin(value={FluidRenderer.class})
public abstract class FluidRendererMixin {
    @Inject(method={"render"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$hideWorldLava(BlockRenderView world, BlockPos position, VertexConsumer vertices, BlockState blockState, FluidState fluidState, CallbackInfo callback) {
        if (NoRenderModule.isWorldLavaDisabled() && fluidState.isIn(FluidTags.LAVA)) {
            callback.cancel();
        }
    }
}

