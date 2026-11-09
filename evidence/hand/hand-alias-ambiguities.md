# Hand aliases: unresolved members

188 evidence-backed aliases cover 196 opaque declarations across the seven selected classes. No source, config or staging file was modified. `hand-alias-evidence.json` records the role evidence for each alias. Names already meaningful in the supplied CFR source were left unchanged.

Eight unresolved declarations, all in original owner `wexside/IlllllllIlIiIil` / HandMaterialRenderer:

| Field | Setter | Proven behavior |
|---|---|---|
| lIiiiIIIIl | ilIlIIlll(int) | Stores clamped integer [1,8]; no read outside its declaration/assignment. ShaderHands caller passes3 or2. |
| lIiliIIIIl | iiiiIiIliI(float) | Stores supplied float; no consumer. ShaderHands caller passes2.2+scale*1.15 or1.4+scale*0.8. |
| lllIiIIIIl | IiIilIlll(float) | Stores supplied float; no consumer. Caller passes0.12 or0.92. |
| lIliiIIIIl | iiIlIIlll(float) | Stores supplied float; no consumer. Caller passes0.14 or0.08. |

Calling these blur iterations, blur radius, refraction or reflection would be plausible speculation, not a role proven by these retained bodies. All eight therefore remain explicit unresolved names rather than fabricated semantic aliases. Original setter caller evidence is retained in caller-shaderhands.javap.txt.

Other review observations: HandMaterialCompositor's rectangle helper computes viewport intersection but returns an uninitialized local in CFR, so its alias clipRectangle describes the surviving computation; it does not claim the missing return construction was restored. In the retained material renderer, already-named reflect and saturation are also write-only; setter aliases preserve those existing names without claiming a shader effect.

Direct shader evidence resolves energy boolean flags: HandAuraCompositor packs gradient as motion.w bit1 and fillHands as bit2. Actual hand_aura_composite.frag reads gradient with mod(motion.w,2)>0.5 and fillHands with motion.w>=2. Uniform aliases come directly from registerUniform literals, GL handles from allocation/bind/delete operations, and texture roles from createTexture resource labels.
