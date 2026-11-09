# Independent hand integration review

Scope: production Hand*.java, hand/HandAuraEffects.java, hand/HandMaterialMode.java, OpenGlStateSnapshot.java, and HandAuraModule options. Read-only review; no production source edits or GPU test rerun. GPU smoke04 PASS19 is the integration runner's reported result, not an independently rerun result here.

## Actionable exactness finding

Resolved P2: the reviewed revision of HandAuraModule.onHandRender called syncRenderers before both phases; root reports this has now been moved to PRE only. Original IlIlllllIiIiIl.llIIIlIilII calls lliIIlIilII only in PRE (bci21..22), then performs POST directly. Move syncRenderers into the BEFORE branch. The current extra POST configuration can change animated color samples between the frame's captures/composition and has no original callsite. Original POST order is otherwise preserved: energy captureDiffAfter if material+energy; material captureSceneAfter; material compositeHands; energy compositeHands.

## Confirmed retained behavior

- PRE captures energy then material. POST snapshots the unmaterialized hand diff before material composition, then draws energy last. These match original module bci25..97.
- Material option labels/order are Glass Hand, Fill, Bubble, Galaxy from original llIilillIlIill and decoded holder literals. They map to original material enum IDs GLASS2, FILL1, BUBBLE5, GALAXY6. Energy shader IDs AURA0, STATIC1, RIBBONS2 match original IiIlIiIlliIlIil. Static disables motion collection; the other modes enable it.
- All seven files in original-resources match production assets/wexside/shaders/handaura byte for byte, including restored glass_apply.frag and hand_aura_composite.frag. Pipeline shader paths, sampler/uniform literals, blend recipes, no depth test/write and no cull match named-bytecode/decompiled pipeline declarations.
- The seven recovered renderer bodies differ from named-source only in semantic naming, legal lambda capture repairs, explicit pipeline compilation and the two rectangle restorations. No replacement effect formulas/defaults were added to those retained bodies.

## Rectangle reconstruction evidence

Original material compositor clip helper IlilliliiI computes clamped floor x/y and ceil right/top, then positive width/height. At bci82 it NEWs original IllillIiliIlIil and DUPs; bci86,88,90,92 loads locals7,8,11,12 before stripped constructor POPs94..98. The surviving unique four-int constructor independently assigns arguments x,y,width,height in that order. Production new ScissorRegion(n3,n4,n7,n8) therefore restores the actual surviving operand tuple, not a guessed rectangle.

At original apply bci154..169, four identical aload23 / pop / iconst0 sequences replace erased rectangle int accessors before glScissor at170. The original rectangle exposes exactly x,y,width,height getters; the OpenGL contract fixes their argument order. Production getter restoration is strongly constrained by these surviving operands/type/getters. It is a recovery of erased calls, not a literal preservation of the deliberately zeroed input JAR. Evidence is saved in review-material-original.javap.txt and review-scissor-original.javap.txt.

## Compatibility changes requiring explicit provenance

OpenGlStateSnapshot's Mojang cache synchronization is not present in the original restore method (raw GL at bci0..171). The production snapshot also preserves extra bindings/state beyond that original class. This is an integration correction supported by the reported smoke03 stale-FBO failure and smoke04 pass, not recovered original behavior. Its read-current -> synchronize-cache -> restore-captured sequence correctly avoids cached no-op suppression for FBOs, texture bindings and test enables.

HandRenderPipelines.ensureCompiled and its custom resource source getter are likewise compatibility glue, not surviving original callsites. Exact original shader IDs include the real .vert/.frag path; the getter loads those exact recovered resources rather than inventing shader code.

The replacement pipeline snippet is now directly proven: original RenderPipelines.lIIiilIIiiI initializes an empty builder with exactly DynamicTransforms and Projection, both the enum value decoded as UNIFORM_BUFFER/ubo. Original VertexFormats.illllIiliIiI initializes builder().build() with no attributes, exactly matching named EMPTY. Original draw mode enum label independently decodes TRIANGLES. See pipeline-exact-field-evidence.md/json and original/named javap logs; the earlier inference limit is closed.

The facade omits original mode-specific material setters at sync method bci268..354. The target fields (four opaque material parameters, reflect and saturation) have no surviving consumer, so omission does not change the retained rendering math. It does change write-only configuration state and should not be described as reconstruction of their missing effect. The production unusedMaterialScalar* naming states the evidence limit honestly.
