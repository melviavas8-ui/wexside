package ru.wexside.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.texture.GlTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL46;
import ru.wexside.render.hand.HandEnergyShaderMode;
import ru.wexside.render.hand.HandMaterialShaderMode;

/** Real GPU readback checks with a known rectangular hand-mask fixture. */
public final class HandRenderChecks {
    private static final int SIZE = 64;
    private static int assertions;

    public static void run() {
        RenderSystem.assertOnRenderThread();
        OpenGlStateSnapshot previous = OpenGlStateSnapshot.capture();
        List<GpuTextureView> views = new ArrayList<>();
        List<GpuTexture> textures = new ArrayList<>();
        HandMaskDiff maskPass = new HandMaskDiff();
        HandMaterialSource sourcePass = new HandMaterialSource();
        HandMaterialCompositor materialPass = new HandMaterialCompositor();
        HandKawaseBlur blurPass = new HandKawaseBlur();
        HandAuraCompositor auraPass = new HandAuraCompositor();
        try {
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            GpuTextureView before = texture("before", TextureFormat.RGBA8, textures, views);
            GpuTextureView after = texture("after", TextureFormat.RGBA8, textures, views);
            GpuTextureView depthBefore = texture("depth-before", TextureFormat.DEPTH32, textures, views);
            GpuTextureView depthAfter = texture("depth-after", TextureFormat.DEPTH32, textures, views);
            GpuTextureView mask = texture("mask", TextureFormat.RGBA8, textures, views);
            GpuTextureView source = texture("source", TextureFormat.RGBA8, textures, views);
            GpuTextureView output = texture("output", TextureFormat.RGBA8, textures, views);
            encoder.clearColorAndDepthTextures(before.texture(), 0xFF000000, depthBefore.texture(), 1.0);
            encoder.clearColorAndDepthTextures(after.texture(), 0xFF000000, depthAfter.texture(), 1.0);
            encoder.clearColorAndDepthTextures(after.texture(), 0xFFFFFFFF, depthAfter.texture(), 0.5, 20, 20, 24, 24);
            maskPass.renderMask(mask, before, after, depthBefore, depthAfter, SIZE, SIZE);
            ByteBuffer maskPixels = pixels(mask);
            require(channel(maskPixels, 32, 32, 3) > 240, "Mask contains the changed rectangle");
            require(channel(maskPixels, 2, 2, 3) == 0, "Mask excludes unchanged background");
            GpuTextureView blurred = blurPass.blur(mask, SIZE, SIZE, 3, 2.5f);
            require(blurred != null, "Blur output exists");
            ByteBuffer blurPixels = pixels(blurred);
            require(channel(blurPixels, blurred.texture().getWidth(0), 9, 16, 3) > 0, "Kawase spreads the mask beyond its original edge");

            sourcePass.renderSource(source, after, mask, SIZE, SIZE);
            for (HandMaterialShaderMode mode : HandMaterialShaderMode.values()) {
                encoder.clearColorTexture(output.texture(), 0xFF000000);
                materialPass.apply(output, source, depthAfter, before, depthBefore, SIZE, SIZE,
                    0xFFFF0000, 0xFF0000FF, false, mode.getShaderModeId(), 0, 0, SIZE, SIZE, 0.5f, 1.0f);
                require(sum(pixels(output), 0) > 0, "Material renders: " + mode);
            }
            encoder.clearColorTexture(output.texture(), 0xFF000000);
            materialPass.apply(output, source, depthAfter, before, depthBefore, SIZE, SIZE,
                0xFF0000FF, 0xFF0000FF, false, HandMaterialShaderMode.FILL.getShaderModeId(), 0, 0, SIZE, SIZE, 0.5f, 1.0f);
            long blueMaterial = sum(pixels(output), 2);
            require(blueMaterial > sum(pixels(output), 0), "Fill responds to configured blue color");

            for (HandEnergyShaderMode mode : HandEnergyShaderMode.values()) {
                encoder.clearColorTexture(output.texture(), 0xFF000000);
                ByteBuffer cleared = pixels(output);
                require(sum(cleared, 0) + sum(cleared, 1) + sum(cleared, 2) == 0, "GPU clear remains correct after direct GL material passes");
                auraPass.composite(output, mask, blurred, SIZE, SIZE, 0.5f, mode.getShaderModeId(),
                    0xFFFF0000, 0xFFFF0000, false, false, 0.2f, 0.1f, 0.4f);
                ByteBuffer redPixels = pixels(output);
                long red = sum(redPixels, 0);
                if (red == 0) {
                    System.out.println("HAND GPU DEBUG: mode=" + mode + ", mask=" + sum(maskPixels, 0)
                        + ", blur=" + sum(blurPixels, 0) + ", output=" + red + ", green=" + sum(redPixels, 1)
                        + ", blue=" + sum(redPixels, 2) + ", alpha=" + sum(redPixels, 3)
                        + ", blend=" + GL46.glIsEnabled(GL46.GL_BLEND) + ", scissor=" + GL46.glIsEnabled(GL46.GL_SCISSOR_TEST)
                        + ", error=" + GL46.glGetError());
                    try {
                        var field = HandAuraCompositor.class.getDeclaredField("uniformStagingBuffer");
                        field.setAccessible(true);
                        ByteBuffer packed = (ByteBuffer) field.get(auraPass);
                        float[] values = new float[16];
                        for (int i = 0; i < values.length; i++) values[i] = packed.getFloat(i * 4);
                        System.out.println("HAND GPU UNIFORMS: " + packed.order() + " " + java.util.Arrays.toString(values));
                    } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                }
                require(red > 0, "Energy mode renders: " + mode);
                encoder.clearColorTexture(output.texture(), 0xFF000000);
                auraPass.composite(output, mask, blurred, SIZE, SIZE, 0.5f, mode.getShaderModeId(),
                    0xFF0000FF, 0xFF0000FF, false, false, 0.2f, 0.1f, 0.4f);
                ByteBuffer bluePixels = pixels(output);
                require(sum(bluePixels, 2) > sum(bluePixels, 0), "Energy responds to configured color: " + mode);
            }
            require(GL46.glGetError() == GL46.GL_NO_ERROR, "No OpenGL error after hand passes");
            System.out.println("HAND RENDER CHECKS PASS: " + assertions + " assertions; real GPU mask, blur, four materials and three energy modes with pixel readback");
        } finally {
            auraPass.close();
            blurPass.close();
            materialPass.close();
            sourcePass.close();
            maskPass.close();
            for (int i = views.size() - 1; i >= 0; --i) views.get(i).close();
            for (int i = textures.size() - 1; i >= 0; --i) textures.get(i).close();
            previous.restore();
        }
    }

    private static GpuTextureView texture(String name, TextureFormat format, List<GpuTexture> textures, List<GpuTextureView> views) {
        GpuTexture texture = RenderSystem.getDevice().createTexture(() -> "hand-recovery-check/" + name, 13, format, SIZE, SIZE, 1, 1);
        textures.add(texture);
        GpuTextureView view = RenderSystem.getDevice().createTextureView(texture);
        views.add(view);
        return view;
    }

    private static ByteBuffer pixels(GpuTextureView view) {
        ByteBuffer pixels = BufferUtils.createByteBuffer(view.texture().getWidth(0) * view.texture().getHeight(0) * 4);
        GL46.glGetTextureImage(((GlTexture) view.texture()).getGlId(), 0, GL46.GL_RGBA, GL46.GL_UNSIGNED_BYTE, pixels);
        return pixels;
    }

    private static int channel(ByteBuffer pixels, int x, int y, int channel) { return pixels.get((y * SIZE + x) * 4 + channel) & 255; }
    private static int channel(ByteBuffer pixels, int width, int x, int y, int channel) { return pixels.get((y * width + x) * 4 + channel) & 255; }
    private static long sum(ByteBuffer pixels, int channel) {
        long sum = 0;
        for (int i = channel; i < pixels.capacity(); i += 4) sum += pixels.get(i) & 255;
        return sum;
    }
    private static void require(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError(label);
    }
}
