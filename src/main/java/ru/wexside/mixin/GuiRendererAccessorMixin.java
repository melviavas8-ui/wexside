package ru.wexside.mixin;

import com.mojang.blaze3d.textures.GpuTexture;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.wexside.misc.ActiveFramebufferContext;
import ru.wexside.misc.GuiRenderTargetAccessor;

@Mixin(value={GuiRenderer.class})
public abstract class GuiRendererAccessorMixin
implements GuiRenderTargetAccessor {
    @Shadow
    private GpuTexture itemAtlasTexture;
    @Shadow
    private GpuTexture itemAtlasDepthTexture;
    @Shadow
    private Map<Object, ?> renderedItems;
    @Shadow
    private int itemAtlasX;
    @Shadow
    private int itemAtlasY;

    @Override
    public GpuTexture getItemAtlasDepthTexture() {
        return this.itemAtlasDepthTexture;
    }

    @Override
    public GpuTexture getItemAtlasTexture() {
        return this.itemAtlasTexture;
    }

    @Override
    public Map<Object, ?> getRenderedItems() {
        return this.renderedItems;
    }

    @Override
    public void setItemAtlasX(int x) {
        this.itemAtlasX = x;
    }

    @Override
    public void setItemAtlasY(int y) {
        this.itemAtlasY = y;
    }

    @Redirect(method={"renderPreparedDraws"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/MinecraftClient;getFramebuffer()Lnet/minecraft/client/gl/Framebuffer;"))
    private Framebuffer wexside$useCaptureFramebuffer(MinecraftClient client) {
        Framebuffer capture = ActiveFramebufferContext.getFramebuffer();
        return capture == null ? client.getFramebuffer() : capture;
    }
}

