package ru.wexside.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.BannerResultGuiElementRenderer;
import net.minecraft.client.gui.render.BookModelGuiElementRenderer;
import net.minecraft.client.gui.render.EntityGuiElementRenderer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.OversizedItemGuiElementRenderer;
import net.minecraft.client.gui.render.PlayerSkinGuiElementRenderer;
import net.minecraft.client.gui.render.ProfilerChartGuiElementRenderer;
import net.minecraft.client.gui.render.SignGuiElementRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.joml.Quaternionfc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import ru.wexside.WexSideClient;
import ru.wexside.misc.ActiveFramebufferContext;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.CaptureFramebuffer;
import ru.wexside.misc.GuiRenderTargetAccessor;
import ru.wexside.render.IconAtlasEntry;
import ru.wexside.util.GlRenderStateSnapshot;
import ru.wexside.util.GuiDrawApi;

public final class OffscreenRenderManager {
    private GuiRenderer guiRenderer;
    private ProjectionMatrix2 itemProjectionMatrix;
    private GuiRenderState guiRenderState;
    private RenderDispatcher renderDispatcher;
    private int copyReadFramebufferId;
    private final CaptureFramebuffer captureFramebuffer = new CaptureFramebuffer();
    private static final int slot2 = 18;
    private static final int slot3 = 16;
    private VertexConsumerProvider.Immediate immediate2;

    public void bakeIcons(List<BakedIconEntry> list) {
        int n3;
        int n4;
        int n5;
        if (list.isEmpty()) {
            return;
        }
        Window window = MinecraftClient.getInstance().getWindow();
        int n = window.getFramebufferWidth();
        int n2 = window.getFramebufferHeight();
        if (n <= 0 || n2 <= 0) {
            return;
        }
        int n6 = Math.max(1, window.getScaleFactor());
        int n7 = list.size();
        int[] nArray = new int[n7];
        int[] nArray2 = new int[n7];
        int n8 = 0;
        for (n5 = 0; n5 < n7; ++n5) {
            BakedIconEntry icon = list.get(n5);
            n4 = icon.baseSize() * n6;
            n3 = icon.requestedSize() > 0 ? Math.max(1, Math.round((float)icon.requestedSize() / (float)n4)) : 1;
            nArray2[n5] = icon.baseSize() * n3;
            nArray[n5] = n4 * n3;
            n8 = Math.max(n8, nArray[n5]);
            icon.texture().ensureSize(nArray[n5]);
        }
        if (n8 <= 0) {
            return;
        }
        n5 = Math.max(1, n / n8);
        int n9 = n8 / n6;
        int columns = n5;
        int logicalCellSize = n9;
        this.captureGui(this.captureFramebuffer, n, n2, trimToWidth -> {
            for (int i = 0; i < n7; ++i) {
                ((BakedIconEntry)list.get(i)).renderer().render((DrawContext)trimToWidth, i % columns * logicalCellSize, i / columns * logicalCellSize, nArray2[i]);
            }
        });
        n4 = this.captureFramebuffer.textureHeight;
        for (n3 = 0; n3 < n7; ++n3) {
            IconAtlasEntry renderPipeline10 = list.get(n3).texture();
            if (!renderPipeline10.isAllocated()) {
                renderPipeline10.setBakeState(n6, false);
                continue;
            }
            int n10 = n3 % n5 * n8;
            int n11 = n4 - n3 / n5 * n8 - nArray[n3];
            this.copyCaptureToIcon(renderPipeline10, nArray[n3], n10, n11);
            renderPipeline10.setBakeState(n6, true);
        }
    }

    private void initializeRenderResources(MinecraftClient mc) {
        VertexConsumerProvider.Immediate iIiIliiliI2;
        if (this.guiRenderer != null) {
            return;
        }
        this.guiRenderState = new GuiRenderState();
        BufferBuilderStorage bufferBuilderStorage = new BufferBuilderStorage(1);
        this.immediate2 = iIiIliiliI2 = bufferBuilderStorage.getEntityVertexConsumers();
        AtlasManager iliIilliII2 = mc.getAtlasManager();
        OrderedRenderCommandQueueImpl orderedRenderCommandQueueImpl = new OrderedRenderCommandQueueImpl();
        this.renderDispatcher = new RenderDispatcher(orderedRenderCommandQueueImpl, mc.getBlockRenderManager(), iIiIliiliI2, iliIilliII2, bufferBuilderStorage.getOutlineVertexConsumers(), bufferBuilderStorage.getEffectVertexConsumers(), mc.textRenderer);
        this.guiRenderer = new GuiRenderer(this.guiRenderState, iIiIliiliI2, (OrderedRenderCommandQueue)orderedRenderCommandQueueImpl, this.renderDispatcher, List.of(new OversizedItemGuiElementRenderer(iIiIliiliI2), new ProfilerChartGuiElementRenderer(iIiIliiliI2), new BookModelGuiElementRenderer(iIiIliiliI2), new SignGuiElementRenderer(iIiIliiliI2, (SpriteHolder)iliIilliII2), new BannerResultGuiElementRenderer(iIiIliiliI2, (SpriteHolder)iliIilliII2), new PlayerSkinGuiElementRenderer(iIiIliiliI2), new EntityGuiElementRenderer(iIiIliiliI2, mc.getEntityRenderDispatcher())));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderGuiIntoCapture(MinecraftClient mc, CaptureFramebuffer captureFramebuffer) {
        int n;
        GpuBufferSlice gpuBufferSlice = RenderSystem.getShaderFog();
        GpuTextureView gpuTextureView = RenderSystem.outputColorTextureOverride;
        GpuTextureView gpuTextureView2 = RenderSystem.outputDepthTextureOverride;
        GpuBufferSlice gpuBufferSlice2 = RenderSystem.getShaderFog();
        GpuBufferSlice gpuBufferSlice3 = RenderSystem.getShaderLights();
        RenderSystem.backupProjectionMatrix();
        GlRenderStateSnapshot glRenderStateSnapshot = GlRenderStateSnapshot.captureRenderState();
        boolean bl = GL11.glGetBoolean((int)2930);
        int n2 = GL11.glGetInteger((int)2932);
        int[] nArray = new int[12];
        try {
            ActiveFramebufferContext.setFramebuffer(captureFramebuffer);
            RenderSystem.outputColorTextureOverride = captureFramebuffer.getColorAttachmentView();
            RenderSystem.outputDepthTextureOverride = captureFramebuffer.getDepthAttachmentView();
            this.clearItemAtlas();
            GlStateManager._activeTexture((int)GL11.glGetInteger((int)34016));
            for (n = 0; n < nArray.length; ++n) {
                GlStateManager._activeTexture((int)(33984 + n));
                nArray[n] = GL11.glGetInteger((int)32873);
                GlStateManager._bindTexture((int)nArray[n]);
            }
            GlStateManager._activeTexture((int)33984);
            this.guiRenderer.render(gpuBufferSlice);
            this.guiRenderer.incrementFrame();
        }
        finally {
            for (n = 0; n < nArray.length; ++n) {
                GlStateManager._activeTexture((int)(33984 + n));
                GlStateManager._bindTexture((int)nArray[n]);
            }
            GlStateManager._activeTexture((int)33984);
            ActiveFramebufferContext.update();
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.setShaderFog((GpuBufferSlice)gpuBufferSlice2);
            RenderSystem.setShaderLights((GpuBufferSlice)gpuBufferSlice3);
            RenderSystem.outputColorTextureOverride = gpuTextureView;
            RenderSystem.outputDepthTextureOverride = gpuTextureView2;
            glRenderStateSnapshot.restore();
            GL11.glDepthMask((boolean)bl);
            GL11.glDepthFunc((int)n2);
        }
    }

    private void flushHudRenderer() {
        GuiDrawApi drawApi = WexSideClient.getHudRenderer();
        if (drawApi != null) {
            drawApi.finishIfIdle();
        }
    }

    private void copyCaptureToIcon(IconAtlasEntry renderPipeline10, int n, int n2, int n3) {
        if (this.captureFramebuffer.getColorAttachmentGlId() <= 0) {
            return;
        }
        int n4 = GL30.glGetInteger((int)36010);
        int n5 = GL30.glGetInteger((int)36006);
        if (this.copyReadFramebufferId == 0) {
            this.copyReadFramebufferId = GL30.glGenFramebuffers();
        }
        GL30.glBindFramebuffer((int)36008, (int)this.copyReadFramebufferId);
        GL30.glFramebufferTexture2D((int)36008, (int)36064, (int)3553, (int)this.captureFramebuffer.getColorAttachmentGlId(), (int)0);
        GL30.glBindFramebuffer((int)36009, (int)renderPipeline10.getFramebufferId());
        int n6 = renderPipeline10.getAtlasX();
        int n7 = renderPipeline10.getAtlasY();
        boolean bl = GL30.glIsEnabled((int)3089);
        GL30.glDisable((int)3089);
        GL30.glBlitFramebuffer((int)n2, (int)n3, (int)(n2 + n), (int)(n3 + n), (int)n6, (int)n7, (int)(n6 + n), (int)(n7 + n), (int)16384, (int)9728);
        if (bl) {
            GL30.glEnable((int)3089);
        }
        GL30.glBindFramebuffer((int)36008, (int)n4);
        GL30.glBindFramebuffer((int)36009, (int)n5);
    }

    public void close() {
        if (this.guiRenderer != null) {
            this.guiRenderer.close();
            this.guiRenderer = null;
        }
        if (this.renderDispatcher != null) {
            this.renderDispatcher.close();
            this.renderDispatcher = null;
        }
        this.immediate2 = null;
        if (this.itemProjectionMatrix != null) {
            this.itemProjectionMatrix.close();
            this.itemProjectionMatrix = null;
        }
        if (this.copyReadFramebufferId != 0) {
            GL30.glDeleteFramebuffers((int)this.copyReadFramebufferId);
            this.copyReadFramebufferId = 0;
        }
        this.captureFramebuffer.delete();
    }

    private void clearItemAtlas() {
        GuiRenderTargetAccessor renderer = (GuiRenderTargetAccessor)this.guiRenderer;
        GpuTexture gpuTexture = renderer.getItemAtlasTexture();
        GpuTexture gpuTexture2 = renderer.getItemAtlasDepthTexture();
        if (gpuTexture == null || gpuTexture2 == null) {
            return;
        }
        renderer.getRenderedItems().clear();
        renderer.setItemAtlasX(0);
        renderer.setItemAtlasY(0);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(gpuTexture, 0, gpuTexture2, 1.0);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void captureModel(CaptureFramebuffer captureFramebuffer, int n, int n2, float f, float f2, float f3, float f4, float f5, BiConsumer<MatrixStack, VertexConsumerProvider.Immediate> biConsumer) {
        if (biConsumer == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        captureFramebuffer.ensureSize(n, n2);
        if (captureFramebuffer.getColorAttachment() == null) {
            return;
        }
        this.flushHudRenderer();
        this.initializeRenderResources(mc);
        if (this.itemProjectionMatrix == null) {
            this.itemProjectionMatrix = new ProjectionMatrix2("wex-capture-item", -1000.0f, 1000.0f, true);
        }
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(captureFramebuffer.getColorAttachment(), 0, captureFramebuffer.getDepthAttachment(), 1.0);
        float f6 = mc.getWindow().getScaleFactor();
        GpuBufferSlice gpuBufferSlice = RenderSystem.getShaderFog();
        GpuTextureView gpuTextureView = RenderSystem.outputColorTextureOverride;
        GpuTextureView gpuTextureView2 = RenderSystem.outputDepthTextureOverride;
        GpuBufferSlice gpuBufferSlice2 = RenderSystem.getShaderFog();
        RenderSystem.backupProjectionMatrix();
        GlRenderStateSnapshot glRenderStateSnapshot = GlRenderStateSnapshot.captureRenderState();
        boolean bl = GL11.glGetBoolean((int)2930);
        int n3 = GL11.glGetInteger((int)2932);
        try {
            RenderSystem.outputColorTextureOverride = captureFramebuffer.getColorAttachmentView();
            RenderSystem.outputDepthTextureOverride = captureFramebuffer.getDepthAttachmentView();
            RenderSystem.setProjectionMatrix((GpuBufferSlice)this.itemProjectionMatrix.set((float)n / f6, (float)n2 / f6), (ProjectionType)ProjectionType.ORTHOGRAPHIC);
            RenderSystem.setShaderFog((GpuBufferSlice)gpuBufferSlice);
            MatrixStack matrices2 = new MatrixStack();
            matrices2.translate(f, f2, 0.0f);
            matrices2.scale(f3, -f3, f3);
            matrices2.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(f5));
            matrices2.translate(0.0f, f4, 0.0f);
            biConsumer.accept(matrices2, this.immediate2);
        }
        finally {
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.setShaderFog((GpuBufferSlice)gpuBufferSlice2);
            RenderSystem.outputColorTextureOverride = gpuTextureView;
            RenderSystem.outputDepthTextureOverride = gpuTextureView2;
            glRenderStateSnapshot.restore();
            GL11.glDepthMask((boolean)bl);
            GL11.glDepthFunc((int)n3);
        }
    }

    public void captureGui(CaptureFramebuffer captureFramebuffer, int n, int n2, Consumer<DrawContext> consumer) {
        MinecraftClient mc = MinecraftClient.getInstance();
        captureFramebuffer.ensureSize(n, n2);
        if (captureFramebuffer.getColorAttachment() == null) {
            return;
        }
        this.flushHudRenderer();
        this.initializeRenderResources(mc);
        this.guiRenderState.clear();
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(captureFramebuffer.getColorAttachment(), 0, captureFramebuffer.getDepthAttachment(), 1.0);
        consumer.accept(new DrawContext(mc, this.guiRenderState, 0, 0));
        this.renderGuiIntoCapture(mc, captureFramebuffer);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void captureItem(CaptureFramebuffer captureFramebuffer, int n, int n2, ItemStack stack, ItemDisplayContext itemDisplayContext, float f, float f2, float f3, float f4, float f5) {
        if (stack.isEmpty()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        captureFramebuffer.ensureSize(n, n2);
        if (captureFramebuffer.getColorAttachment() == null) {
            return;
        }
        this.flushHudRenderer();
        this.initializeRenderResources(mc);
        if (this.itemProjectionMatrix == null) {
            this.itemProjectionMatrix = new ProjectionMatrix2("wex-capture-item", -1000.0f, 1000.0f, true);
        }
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(captureFramebuffer.getColorAttachment(), 0, captureFramebuffer.getDepthAttachment(), 1.0);
        ItemRenderState itemState = new ItemRenderState();
        mc.getItemModelManager().clearAndUpdate(itemState, stack, itemDisplayContext, (World)mc.world, null, 0);
        float f6 = mc.getWindow().getScaleFactor();
        GpuBufferSlice gpuBufferSlice = RenderSystem.getShaderFog();
        GpuTextureView gpuTextureView = RenderSystem.outputColorTextureOverride;
        GpuTextureView gpuTextureView2 = RenderSystem.outputDepthTextureOverride;
        GpuBufferSlice gpuBufferSlice2 = RenderSystem.getShaderFog();
        RenderSystem.backupProjectionMatrix();
        GlRenderStateSnapshot glRenderStateSnapshot = GlRenderStateSnapshot.captureRenderState();
        GpuBufferSlice gpuBufferSlice3 = RenderSystem.getShaderLights();
        boolean bl = GL11.glGetBoolean((int)2930);
        int n3 = GL11.glGetInteger((int)2932);
        int[] nArray = new int[12];
        try {
            RenderSystem.outputColorTextureOverride = captureFramebuffer.getColorAttachmentView();
            RenderSystem.outputDepthTextureOverride = captureFramebuffer.getDepthAttachmentView();
            RenderSystem.setProjectionMatrix((GpuBufferSlice)this.itemProjectionMatrix.set((float)n / f6, (float)n2 / f6), (ProjectionType)ProjectionType.ORTHOGRAPHIC);
            RenderSystem.setShaderFog((GpuBufferSlice)gpuBufferSlice);
            mc.gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ITEMS_3D);
            GlStateManager._activeTexture((int)GL11.glGetInteger((int)34016));
            for (int i = 0; i < nArray.length; ++i) {
                GlStateManager._activeTexture((int)(33984 + i));
                nArray[i] = GL11.glGetInteger((int)32873);
                GlStateManager._bindTexture((int)nArray[i]);
            }
            GlStateManager._activeTexture((int)33984);
            MatrixStack matrices2 = new MatrixStack();
            matrices2.translate(f, f2, 0.0f);
            matrices2.scale(f3, -f3, f3);
            if (f5 != 0.0f) {
                matrices2.multiply((Quaternionfc)RotationAxis.POSITIVE_X.rotationDegrees(f5));
            }
            matrices2.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(f4));
            OrderedRenderCommandQueueImpl orderedRenderCommandQueueImpl = this.renderDispatcher.getQueue();
            itemState.render(matrices2, (OrderedRenderCommandQueue)orderedRenderCommandQueueImpl, 0xF000F0, OverlayTexture.DEFAULT_UV, 0);
            this.renderDispatcher.render();
            this.immediate2.draw();
        }
        finally {
            for (int i = 0; i < nArray.length; ++i) {
                GlStateManager._activeTexture((int)(33984 + i));
                GlStateManager._bindTexture((int)nArray[i]);
            }
            GlStateManager._activeTexture((int)33984);
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.setShaderFog((GpuBufferSlice)gpuBufferSlice2);
            RenderSystem.setShaderLights((GpuBufferSlice)gpuBufferSlice3);
            RenderSystem.outputColorTextureOverride = gpuTextureView;
            RenderSystem.outputDepthTextureOverride = gpuTextureView2;
            glRenderStateSnapshot.restore();
            GL11.glDepthMask((boolean)bl);
            GL11.glDepthFunc((int)n3);
        }
    }
}

