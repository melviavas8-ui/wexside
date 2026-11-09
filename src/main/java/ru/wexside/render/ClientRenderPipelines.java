package ru.wexside.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class ClientRenderPipelines {
    private static final VertexFormatElement POSITION_2F = ClientRenderPipelines.element(7, VertexFormatElement.Type.FLOAT, 2);
    private static final VertexFormatElement MESH_POSITION_2F = ClientRenderPipelines.element(8, VertexFormatElement.Type.FLOAT, 2);
    private static final VertexFormatElement MESH_SIZE_2F = ClientRenderPipelines.element(9, VertexFormatElement.Type.FLOAT, 2);
    private static final VertexFormatElement TEXTURE_COORDINATE_2F = ClientRenderPipelines.element(10, VertexFormatElement.Type.FLOAT, 2);
    private static final VertexFormatElement CORNER_RADII_4F = ClientRenderPipelines.element(11, VertexFormatElement.Type.FLOAT, 4);
    private static final VertexFormatElement FILL_COLOR_4UB = ClientRenderPipelines.element(12, VertexFormatElement.Type.UBYTE, 4);
    private static final VertexFormatElement OUTLINE_COLOR_4UB = ClientRenderPipelines.element(13, VertexFormatElement.Type.UBYTE, 4);
    private static final VertexFormatElement OUTLINE_THICKNESS_1F = ClientRenderPipelines.element(14, VertexFormatElement.Type.FLOAT, 1);
    private static final VertexFormatElement EDGE_SOFTNESS_1F = ClientRenderPipelines.element(15, VertexFormatElement.Type.FLOAT, 1);
    private static final VertexFormatElement MSDF_EDGE_1F = ClientRenderPipelines.element(16, VertexFormatElement.Type.FLOAT, 1);
    private static final VertexFormatElement MSDF_TEXT_SIZE_1F = ClientRenderPipelines.element(17, VertexFormatElement.Type.FLOAT, 1);
    private static final VertexFormatElement MSDF_RANGE_1F = ClientRenderPipelines.element(18, VertexFormatElement.Type.FLOAT, 1);
    private static final VertexFormatElement MSDF_ATLAS_SIZE_2F = ClientRenderPipelines.element(19, VertexFormatElement.Type.FLOAT, 2);
    private static final VertexFormatElement TEXTURE_INDEX_1B = ClientRenderPipelines.element(20, VertexFormatElement.Type.BYTE, 1);
    private static final VertexFormatElement DRAW_MODE_1B = ClientRenderPipelines.element(21, VertexFormatElement.Type.BYTE, 1);
    public static final VertexFormat GUI_VERTEX_FORMAT = VertexFormat.builder().add("aPosition", POSITION_2F).add("aMeshPos", MESH_POSITION_2F).add("aMeshSize", MESH_SIZE_2F).add("aTexCoord", TEXTURE_COORDINATE_2F).add("aRadius", CORNER_RADII_4F).add("aColor", FILL_COLOR_4UB).add("aOutlineColor", OUTLINE_COLOR_4UB).add("aThickness", OUTLINE_THICKNESS_1F).add("aSoftness", EDGE_SOFTNESS_1F).add("aMsdfEdge", MSDF_EDGE_1F).add("aMsdfTextSize", MSDF_TEXT_SIZE_1F).add("aMsdfRange", MSDF_RANGE_1F).add("aMsdfAtlasSize", MSDF_ATLAS_SIZE_2F).add("aTexIndex", TEXTURE_INDEX_1B).add("aDrawMode", DRAW_MODE_1B).build();
    public static final RenderPipeline OVERLAY_PRESENT = RenderPipelines.register((RenderPipeline)ClientRenderPipelines.fullscreenPipeline("overlay_present", "dc/present").withSampler("OverlaySampler").withBlend(BlendFunction.TRANSLUCENT).build());
    public static final RenderPipeline MASK_CLEAR = RenderPipelines.register((RenderPipeline)ClientRenderPipelines.fullscreenPipeline("mask_clear", "dc/clear").withoutBlend().build());
    public static final RenderPipeline KAWASE_DOWNSAMPLE = RenderPipelines.register((RenderPipeline)ClientRenderPipelines.fullscreenPipeline("kawase_downsample", "dc/kawase").withFragmentShader(ClientRenderPipelines.id("dc/kawase_down")).withSampler("InSampler").withUniform("KawaseData", UniformType.UNIFORM_BUFFER).withoutBlend().build());
    public static final RenderPipeline KAWASE_UPSAMPLE = RenderPipelines.register((RenderPipeline)ClientRenderPipelines.fullscreenPipeline("kawase_upsample", "dc/kawase").withFragmentShader(ClientRenderPipelines.id("dc/kawase_up")).withSampler("InSampler").withUniform("KawaseData", UniformType.UNIFORM_BUFFER).withoutBlend().build());
    public static final RenderPipeline MASK_WRITER = RenderPipelines.register((RenderPipeline)ClientRenderPipelines.guiPipeline("mask_writer").withoutBlend().build());
    public static final RenderPipeline GUI_BATCH = RenderPipelines.register((RenderPipeline)ClientRenderPipelines.guiPipeline("gui_batch").withBlend(BlendFunction.TRANSLUCENT).build());
    public static final RenderPipeline POTION_DECAL = RenderPipelines.register((RenderPipeline)ClientRenderPipelines.fullscreenPipeline("potion_decal", "prediction/potion_decal").withSampler("DepthSampler").withUniform("PotionDecal", UniformType.UNIFORM_BUFFER).withBlend(BlendFunction.TRANSLUCENT).build());

    private ClientRenderPipelines() {
    }

    private static VertexFormatElement element(int id, VertexFormatElement.Type type, int count) {
        return VertexFormatElement.register((int)id, (int)0, (VertexFormatElement.Type)type, (VertexFormatElement.Usage)VertexFormatElement.Usage.GENERIC, (int)count);
    }

    private static RenderPipeline.Builder fullscreenPipeline(String name, String shader) {
        return RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(ClientRenderPipelines.id("pipeline/" + name)).withVertexShader(ClientRenderPipelines.id(shader)).withFragmentShader(ClientRenderPipelines.id(shader)).withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false);
    }

    private static RenderPipeline.Builder guiPipeline(String name) {
        RenderPipeline.Builder builder = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(ClientRenderPipelines.id("pipeline/" + name)).withVertexShader(ClientRenderPipelines.id("dc/core_dc")).withFragmentShader(ClientRenderPipelines.id("dc/core_dc")).withUniform("WexGlobals", UniformType.UNIFORM_BUFFER).withSampler("MaskTex").withVertexFormat(GUI_VERTEX_FORMAT, VertexFormat.DrawMode.QUADS).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false);
        for (int textureIndex = 0; textureIndex < 16; ++textureIndex) {
            builder.withSampler("textureSampler[" + textureIndex + "]");
        }
        return builder;
    }

    private static Identifier id(String path) {
        return Identifier.of((String)"wexside", (String)path);
    }
}

