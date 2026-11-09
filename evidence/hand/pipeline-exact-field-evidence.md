# Exact hand pipeline field identities

All three questioned replacements are established directly from original class initialization, with no shader-shape assumption needed. No production source edits or target execution.

| Original field | Exact initialized value | Original bytecode evidence |
|---|---|---|
| RenderPipelines owner wexside/IlllliIlIiIilil.lIIiilIIiiI | Snippet containing only DynamicTransforms:UNIFORM_BUFFER and Projection:UNIFORM_BUFFER | <clinit> bci10 creates empty snippet array;14 builder;17..23 first uniform;26..32 second;35 buildSnippet;38 PUTSTATIC target. |
| VertexFormats owner wexside/lillllliIiliIll.illllIiliIiI | EMPTY vertex format | <clinit> bci0 VertexFormat.builder();3 Builder.build();6 PUTSTATIC target, with no element additions. Named Minecraft VertexFormats.EMPTY has the identical builder/build/write expression at bci0/3/6. |
| UniformType owner wexside/IlIillliIiIilil.illllliIIIiI | UNIFORM_BUFFER, enum ordinal0, metadata string ubo | <clinit> bci4 loads decoded UNIFORM_BUFFER;7 pushes0;8 loads decoded ubo;11 invokes enum constructor;14 PUTSTATIC target. Surviving constructor delegates name/ordinal to Enum and stores the metadata. Named UniformType.UNIFORM_BUFFER initializes from the same name/ordinal/ubo tuple. |

The two uniform names were decoded from the exact holder fields loaded at original snippet bci17 and26:7jeh7rSgvY7uG8cqVVTQcy7dB41.ZVjLB60HJks3P3EWvkraNWolWFDNfX -> DynamicTransforms, HFu7VEJWjzjAQmmISeLI8CnJq31.lsX5fkTz8Xun9AptmCtnjimOCVgCbk -> Projection. Enum name and metadata likewise decode from sKOFLC3b2mVt1hhemIA4ZrunL15.Ieo0InhJfWhG3SRhjQmtlX29kwYVhA and SKyKlMkRjg98v1syTCwF9AOiC50.MrMZg1xvIjFbabMBPEcNljeDgQlCSh. Encrypted LDC values, holder write instructions and decoded results are retained in pipeline-exact-field-evidence.json.

Additional confirmation: original hand pipeline draw-mode field com/mojang/blaze3d/vertex/VertexFormat$class_5596.field_27379 initializes at bci68..82 with decoded enum name TRIANGLES, ordinal4 and constructor data3,3,false. This matches the selected DrawMode.TRIANGLES rather than merely assuming triangles from the full-screen shader.

Original javap evidence is saved in review-IlllliIlIiIilil.javap.txt, review-lillllliIiliIll.javap.txt, review-IlIillliIiIilil.javap.txt, review-drawmode-original.javap.txt and five review-holder-*.javap.txt files. Named API comparison javap files are review-named-VertexFormats.javap.txt, review-named-UniformType.javap.txt, review-named-RenderPipelines.javap.txt and review-named-VertexFormat$DrawMode.javap.txt, obtained from the project's named Minecraft1.21.11 merged jar. Named RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET also contains exactly those two uniforms and no additional properties.

Verdict: production's explicit two-uniform builder, VertexFormats.EMPTY, UniformType.UNIFORM_BUFFER and DrawMode.TRIANGLES are supported by actual original initialization bodies. The source getter/cache synchronization remain separately identified compatibility adaptations.
