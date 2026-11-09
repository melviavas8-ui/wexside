# Recovery Report

## Verdict

**Buildable Gradle source; partial exact recovery.** The original client JAR is not a dependency or hidden implementation. Delivered Java is already named; source remapping is disabled. Normal Loom dependency mapping and binary `remapJar` remain.

The supplied named reference and patched binary represent different revisions. The current source runtime has 122 entries (103 regular modules+19 ESP instances). The binary has 122 direct Module subclasses. Its bundled repair attempts 121 distinct module classes plus19 ESP entries. These are different inventories; their counts do not establish identity, completeness or working behavior.

## Recovered and repaired

- Symbol-resolved javac renames:1,287 records across two passes, plus two further FTSnap fields. All former `process`/numbered-process method declarations in main sources were resolved. Remaining generic accessors and ambiguous contracts prevent a claim of complete semantic naming. Hand renderer aliases and isolated retained-source names have separate audits.
- Projectile launch now adds inherited motion; gravity uses the original negative addition with the original drag/gravity ordering. The helper passes 261 regression assertions, including independent multi-step closed-form checks and red tests of the prior errors.
- Invalid configuration is parsed and validated before resetting live state. Prepared data is reused rather than reread. This is preflight protection, not a claim of rollback after every possible setting-reader failure.
- Potion popup rows are detached before rebuilding. Existing HeadHitter and Predictions source modules are registered; Predictions listener lambdas defer renderer access until after construction.
- Recovered vanilla hooks cover FastPlace delay, Timer scale after the duration operator, ItemScroller input and NoEntityTrace/OpenWalls raycast policies. Complete in-world acceptance of these features is not claimed.
- Actual hand mask/depth difference, material source/compositing, Kawase blur, motion and energy passes replace the reference's matrix-only stand-in. Exact option labels/IDs and 92 decoded strings are evidenced. The seven hand shader resources match the binary byte-for-byte. Stripped rectangle construction/getters were restored from surviving operand loads and the intact constructor contract.
- OpenGL state/cache synchronization and the custom shader-source getter are explicit compatibility adaptations to the vanilla named Minecraft dependency. They are tested corrections, not claims that these exact implementations survived in the original binary. GPU readback verifies mask, blur, four materials, three energy modes and color response.
- All 180 binary-owned assets are inventoried. 105 previously absent resources are included. Nine conflicting shader variants remain separately available instead of silently mixing renderer/resource revisions.

## Retained additional code

The custom `recovery` source set is compiled and checked by normal Gradle `build`. It is not automatically loaded as a second mod or placed in the installable mod JAR.

| Component | Included exact retained scope | Boundary |
|---|---|---|
| CrystalAura | Module bodies, ownership lease, requests, input/event contracts, controller and supplied-patch strategy/factory | Four setting metadata/defaults/order require explicit inputs; event producer/registration boundaries remain unestablished. Supplied-artifact null-sentinel defects are documented and preserved. |
| AutoWarden | Twelve helpers covering state policy, queries, farm bounds, loot, consumables, threat/death/supply handling and chest coordination | Removed action/request/builder bodies and unestablished initialization prevent complete automatic integration. Required actions are interfaces, not fake implementations. |
| FireworkBoost/ElytraMotion | Exact angle folding/preset tables, boost consumer arithmetic, key/freeze/rocket selection policy | Controlled-key identity and erased inventory-action construction require real integration inputs. |
| DonMarket | All 25 bridge methods reconciled; five real alternate bodies recovered from temp_dump; lifecycle and overlay input/tick order preserved | Settings, initialization and actual controller/overlay providers remain required inputs. Original branded module metadata is not inferred from its functional owner string. |

## Exact missing-data boundary

The input contains concrete removed code. At seven AutoWarden allocation sites across five classes, NEW/DUP and operands survive, but constructor invocations have been replaced with POPs; request creation eventually stores null. Six referenced action/request/builder types themselves contain zero fields and only a no-argument constructor, with no actuator behavior. Original setting assignments are also absent in the specifically audited AutoWarden/CrystalAura classes: zero writes to their ten setting-reference fields. This does not imply that all other original metadata is complete, or that their whole retained logic is absent.

The alternative-copy audit scanned 215 standalone classes, 906 Java files, 80 archives and 32,930 archived classes without errors. It found no alternate implementations of those six erased types. AutoWarden's alternate dump differs only in20direct-helper/GuiRepair redirects; its constructor and erased regions remain unchanged. Exact class names, bytecode offsets and hashes are in `recovery/warden` evidence.

The later `Wexside-Launcher.rar` contains the identical client JAR and identical tools/reference/runtime/dumps. Both launcher scripts differ: the batch changes argument forwarding; PowerShell changes library discovery/launch handling and enables `-Xverify:none`. These scripts supply no missing module bodies. The separate named source RAR was also compared and does not contain matching omitted modules.

No new implementations were invented for the removed actions, following the explicit exact-recovery preference. A matching artifact that actually retains the missing bodies/initialization would be needed to establish those originals. Empty input methods or JVM-default null fields are identified as input behavior, not described as completed features.

## Acceptance limits

Full per-module gameplay, lifecycle, combinations, long soak and frame-time acceptance remains INCONCLUSIVE (0/122 complete module gates). Supporting unit/CPU/GPU smoke checks are narrower evidence. Original names are not promised where only new semantic names are justified. There is no honest full-recovery percentage from the available inventory alone.
