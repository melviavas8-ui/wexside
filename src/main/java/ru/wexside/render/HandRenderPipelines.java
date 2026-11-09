package ru.wexside.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import ru.wexside.misc.ClasspathResource;

/** Three pipeline recipes recovered from the binary's dedicated hand pipeline registry. */
public final class HandRenderPipelines {
    private static final BlendFunction REPLACE = new BlendFunction(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ONE, DestFactor.ZERO);
    public static final RenderPipeline KAWASE_DOWN = blur("hand_aura_kawase_down");
    public static final RenderPipeline KAWASE_UP = blur("hand_aura_kawase_up");
    public static final RenderPipeline AURA_COMPOSITE = base("hand_aura_composite", "hand_aura_composite")
        .withUniform("HandAuraData", UniformType.UNIFORM_BUFFER)
        .withSampler("MaskSampler").withSampler("GlowSampler")
        .withBlend(BlendFunction.ADDITIVE).build();

    private HandRenderPipelines() {}

    private static RenderPipeline blur(String name) {
        return base(name, "kawase").withUniform("KawaseData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0").withBlend(REPLACE).build();
    }

    private static RenderPipeline.Builder base(String name, String shader) {
        return RenderPipeline.builder()
            .withLocation("wexside/pipeline/" + name)
            .withVertexShader(Identifier.of("wexside", "shaders/handaura/" + shader + ".vert"))
            .withFragmentShader(Identifier.of("wexside", "shaders/handaura/" + shader + ".frag"))
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false);
    }

    public static void ensureCompiled(RenderPipeline pipeline) {
        // These original identifiers include their real .vert/.frag paths, rather than vanilla suffix conventions.
        var compiled = RenderSystem.getDevice().precompilePipeline(pipeline, (id, type) ->
            new ClasspathResource("/assets/" + id.getNamespace() + "/" + id.getPath()).readUtf8());
        if (!compiled.isValid()) throw new IllegalStateException("Hand pipeline compilation failed: " + pipeline.getLocation());
    }
}
