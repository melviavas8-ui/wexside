package ru.wexside.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.TextDisplayEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wexside.module.misc.HologramOptimizerModule;
import ru.wexside.render.FramebufferTextureAdapter;

public final class HologramImpostorRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HologramImpostorRenderer.class);
    private static final Identifier CACHE_TEXTURE_ID = Identifier.of((String)"wexside", (String)"hologram_impostor");
    private static final int MAX_CACHE_ENTRIES = 256;
    private static final int MAX_TEXTURE_SIZE = 2048;
    private static final int FULL_BRIGHT_LIGHT = 0xF000F0;
    private static final Matrix4f DRAW_MATRIX = new Matrix4f();
    private static final Map<CacheKey, CachedImpostor> CACHE = new HashMap<CacheKey, CachedImpostor>();
    private static final List<RenderRequest> REQUESTS = new ArrayList<RenderRequest>();
    private static final FramebufferTextureAdapter CACHE_TEXTURE = new FramebufferTextureAdapter();
    private static ProjectionMatrix2 projection;
    private static VertexConsumerProvider.Immediate offscreenConsumers;
    private static boolean textureRegistered;
    private static boolean failed;

    private HologramImpostorRenderer() {
    }

    public static boolean captureEntityLabel(EntityRenderState state, MatrixStack matrices, CameraRenderState camera) {
        if (!HologramImpostorRenderer.canCapture() || state == null || matrices == null || camera == null) {
            return false;
        }
        Text text = state.displayName;
        Vec3d labelPosition = state.nameLabelPos;
        if (text == null || labelPosition == null || !HologramImpostorRenderer.isHologramEntity(state.entityType)) {
            return false;
        }
        if (state.squaredDistanceToCamera > HologramOptimizerModule.maxDistanceSquared()) {
            return true;
        }
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int width = Math.max(1, textRenderer.getWidth((StringVisitable)text));
        int height = 10;
        int background = HologramImpostorRenderer.defaultBackgroundColor();
        Matrix4f transform = new Matrix4f((Matrix4fc)matrices.peek().getPositionMatrix()).translate((float)labelPosition.x, (float)labelPosition.y + 0.5f, (float)labelPosition.z).rotate((Quaternionfc)camera.orientation).scale(0.025f, -0.025f, 0.025f).translate((float)(-width) / 2.0f, 0.0f, 0.0f);
        TextDrawer drawer = (renderer, consumers) -> renderer.draw(text, 0.0f, 0.0f, -1, false, DRAW_MATRIX, consumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        REQUESTS.add(new RenderRequest(transform, new CacheKey(text, background, 0), width, height, background, 255, true, drawer));
        return true;
    }

    public static boolean captureTextDisplay(TextDisplayEntityRenderState state, MatrixStack matrices, float tickProgress) {
        if (!HologramImpostorRenderer.canCapture() || state == null || matrices == null) {
            return false;
        }
        DisplayEntity.TextDisplayEntity.TextLines textLines = state.textLines;
        DisplayEntity.TextDisplayEntity.Data data = state.data;
        if (textLines == null || textLines.lines().isEmpty() || data == null) {
            return false;
        }
        if (state.squaredDistanceToCamera > HologramOptimizerModule.maxDistanceSquared()) {
            return true;
        }
        int opacity = data.textOpacity().lerp(tickProgress) & 0xFF;
        if (opacity == 0) {
            return true;
        }
        byte flags = data.flags();
        int background = (flags & 4) != 0 ? HologramImpostorRenderer.defaultBackgroundColor() : data.backgroundColor().lerp(tickProgress);
        int width = Math.max(1, textLines.width());
        int height = Math.max(1, textLines.lines().size() * 10);
        Matrix4f transform = new Matrix4f((Matrix4fc)matrices.peek().getPositionMatrix()).rotate((float)Math.PI, 0.0f, 1.0f, 0.0f).scale(-0.025f).translate(1.0f - (float)width / 2.0f, -((float)height - 1.0f), 0.0f);
        DisplayEntity.TextDisplayEntity.TextAlignment alignment = DisplayEntity.TextDisplayEntity.getAlignment((byte)flags);
        boolean shadow = (flags & 1) != 0;
        List<DisplayEntity.TextDisplayEntity.TextLine> lines = List.copyOf(textLines.lines());
        TextDrawer drawer = (renderer, consumers) -> {
            float y = 0.0f;
            for (DisplayEntity.TextDisplayEntity.TextLine line : lines) {
                float x = HologramImpostorRenderer.alignedX(alignment, width, line.width());
                renderer.draw(line.contents(), x, y, -1, shadow, DRAW_MATRIX, consumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
                y += 10.0f;
            }
        };
        boolean seeThrough = (flags & 2) != 0;
        REQUESTS.add(new RenderRequest(transform, new CacheKey(textLines, background, flags), width, height, background, opacity, seeThrough, drawer));
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void flush() {
        if (failed || !HologramOptimizerModule.isEnabled()) {
            HologramImpostorRenderer.clear();
            return;
        }
        if (REQUESTS.isEmpty()) {
            return;
        }
        try {
            HologramImpostorRenderer.registerTexture();
            for (RenderRequest request : REQUESTS) {
                CACHE.computeIfAbsent(request.key(), ignored -> HologramImpostorRenderer.createImpostor(request));
            }
            VertexConsumerProvider.Immediate consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
            for (RenderRequest request : REQUESTS) {
                CachedImpostor cached = CACHE.get(request.key());
                if (cached == null) continue;
                HologramImpostorRenderer.drawCached(consumers, request, cached);
            }
            HologramImpostorRenderer.evictUnusedEntries();
        }
        catch (Throwable throwable) {
            failed = true;
            LOGGER.warn("Hologram impostor renderer disabled after a rendering failure", throwable);
            HologramImpostorRenderer.clear();
        }
        finally {
            REQUESTS.clear();
        }
    }

    public static void clear() {
        for (CachedImpostor cached : CACHE.values()) {
            cached.framebuffer().delete();
        }
        CACHE.clear();
        REQUESTS.clear();
        failed = false;
    }

    private static boolean canCapture() {
        return !failed && HologramOptimizerModule.isEnabled() && RenderSystem.tryGetDevice() != null;
    }

    private static boolean isHologramEntity(EntityType<?> type) {
        return type == EntityType.ARMOR_STAND || type == EntityType.ITEM || type == EntityType.ITEM_DISPLAY;
    }

    private static void registerTexture() {
        if (textureRegistered) {
            return;
        }
        MinecraftClient.getInstance().getTextureManager().registerTexture(CACHE_TEXTURE_ID, (AbstractTexture)CACHE_TEXTURE);
        textureRegistered = true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static CachedImpostor createImpostor(RenderRequest request) {
        int quality;
        for (quality = HologramOptimizerModule.qualityScale(); quality > 1 && ((long)request.width() * (long)quality > 2048L || (long)request.height() * (long)quality > 2048L); --quality) {
        }
        SimpleFramebuffer framebuffer = new SimpleFramebuffer("wexside_hologram_impostor", request.width() * quality, request.height() * quality, true);
        boolean complete = false;
        try {
            CachedImpostor cachedImpostor;
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(framebuffer.getColorAttachment(), 0, framebuffer.getDepthAttachment(), 1.0);
            VertexConsumerProvider.Immediate consumers = HologramImpostorRenderer.offscreenConsumers();
            GpuBufferSlice projectionSlice = HologramImpostorRenderer.projection().set((float)request.width(), (float)request.height());
            RenderSystem.backupProjectionMatrix();
            try {
                RenderSystem.setProjectionMatrix((GpuBufferSlice)projectionSlice, (ProjectionType)ProjectionType.ORTHOGRAPHIC);
                Matrix4fStack modelView = RenderSystem.getModelViewStack();
                modelView.pushMatrix();
                try {
                    modelView.identity();
                    GpuTextureView previousColor = RenderSystem.outputColorTextureOverride;
                    GpuTextureView previousDepth = RenderSystem.outputDepthTextureOverride;
                    RenderSystem.outputColorTextureOverride = framebuffer.getColorAttachmentView();
                    RenderSystem.outputDepthTextureOverride = framebuffer.getDepthAttachmentView();
                    try {
                        HologramImpostorRenderer.drawBackground(consumers, request.width(), request.height(), request.backgroundColor());
                        request.drawer().draw(MinecraftClient.getInstance().textRenderer, (VertexConsumerProvider)consumers);
                        consumers.draw();
                    }
                    finally {
                        RenderSystem.outputColorTextureOverride = previousColor;
                        RenderSystem.outputDepthTextureOverride = previousDepth;
                    }
                }
                finally {
                    modelView.popMatrix();
                }
            }
            finally {
                RenderSystem.restoreProjectionMatrix();
            }
            complete = true;
            CachedImpostor cachedImpostor2 = cachedImpostor = new CachedImpostor((Framebuffer)framebuffer, request.width(), request.height());
            return cachedImpostor2;
        }
        finally {
            if (!complete) {
                framebuffer.delete();
            }
        }
    }

    private static void drawBackground(VertexConsumerProvider.Immediate consumers, int width, int height, int color) {
        if (color >>> 24 == 0) {
            return;
        }
        RenderLayer layer = RenderLayers.debugQuads();
        VertexConsumer vertices = consumers.getBuffer(layer);
        vertices.vertex((Matrix4fc)DRAW_MATRIX, 0.0f, 0.0f, 0.01f).color(color);
        vertices.vertex((Matrix4fc)DRAW_MATRIX, 0.0f, (float)height, 0.01f).color(color);
        vertices.vertex((Matrix4fc)DRAW_MATRIX, (float)width, (float)height, 0.01f).color(color);
        vertices.vertex((Matrix4fc)DRAW_MATRIX, (float)width, 0.0f, 0.01f).color(color);
        consumers.draw(layer);
    }

    private static void drawCached(VertexConsumerProvider.Immediate consumers, RenderRequest request, CachedImpostor cached) {
        CACHE_TEXTURE.bind(cached.framebuffer());
        RenderLayer layer = request.seeThrough() ? RenderLayers.entityTranslucent((Identifier)CACHE_TEXTURE_ID) : RenderLayers.entityCutoutNoCull((Identifier)CACHE_TEXTURE_ID);
        VertexConsumer vertices = consumers.getBuffer(layer);
        Matrix4f transform = request.transform();
        int color = request.alpha() << 24 | 0xFFFFFF;
        float width = cached.width();
        float height = cached.height();
        HologramImpostorRenderer.vertex(vertices, transform, 0.0f, 0.0f, 0.0f, 1.0f, color);
        HologramImpostorRenderer.vertex(vertices, transform, width, 0.0f, 1.0f, 1.0f, color);
        HologramImpostorRenderer.vertex(vertices, transform, width, height, 1.0f, 0.0f, color);
        HologramImpostorRenderer.vertex(vertices, transform, 0.0f, 0.0f, 0.0f, 1.0f, color);
        HologramImpostorRenderer.vertex(vertices, transform, width, height, 1.0f, 0.0f, color);
        HologramImpostorRenderer.vertex(vertices, transform, 0.0f, height, 0.0f, 0.0f, color);
        consumers.draw(layer);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float u, float v, int color) {
        vertices.vertex((Matrix4fc)matrix, x, y, 0.0f).texture(u, v).color(color);
    }

    private static void evictUnusedEntries() {
        if (CACHE.size() <= 256) {
            return;
        }
        HashSet<CacheKey> active = new HashSet<CacheKey>();
        for (RenderRequest request : REQUESTS) {
            active.add(request.key());
        }
        Iterator<Map.Entry<CacheKey, CachedImpostor>> iterator = CACHE.entrySet().iterator();
        while (CACHE.size() > 256 && iterator.hasNext()) {
            Map.Entry<CacheKey, CachedImpostor> entry = iterator.next();
            if (active.contains(entry.getKey())) continue;
            entry.getValue().framebuffer().delete();
            iterator.remove();
        }
    }

    private static ProjectionMatrix2 projection() {
        if (projection == null) {
            projection = new ProjectionMatrix2("wexside_hologram_projection", -1000.0f, 1000.0f, true);
        }
        return projection;
    }

    private static VertexConsumerProvider.Immediate offscreenConsumers() {
        if (offscreenConsumers == null) {
            offscreenConsumers = VertexConsumerProvider.immediate((BufferAllocator)new BufferAllocator(0x100000));
        }
        return offscreenConsumers;
    }

    private static int defaultBackgroundColor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null) {
            return 0;
        }
        return (int)(client.options.getTextBackgroundOpacity(0.25f) * 255.0f) << 24;
    }

    private static float alignedX(DisplayEntity.TextDisplayEntity.TextAlignment alignment, int lineWidth, int textWidth) {
        return switch (alignment) {
            default -> throw new MatchException(null, null);
            case DisplayEntity.TextDisplayEntity.TextAlignment.LEFT -> 0.0f;
            case DisplayEntity.TextDisplayEntity.TextAlignment.RIGHT -> lineWidth - textWidth;
            case DisplayEntity.TextDisplayEntity.TextAlignment.CENTER -> (float)(lineWidth - textWidth) / 2.0f;
        };
    }

    @FunctionalInterface
    private static interface TextDrawer {
        public void draw(TextRenderer var1, VertexConsumerProvider var2);
    }

    private record RenderRequest(Matrix4f transform, CacheKey key, int width, int height, int backgroundColor, int alpha, boolean seeThrough, TextDrawer drawer) {
    }

    private record CacheKey(Object content, int backgroundColor, int flags) {
    }

    private record CachedImpostor(Framebuffer framebuffer, float width, float height) {
    }
}

