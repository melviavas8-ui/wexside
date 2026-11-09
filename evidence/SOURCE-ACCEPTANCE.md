# Source Acceptance

Verdict: **PARTIAL EXACT RECOVERY; BUILDABLE GRADLE PROJECT**. Missing code was documented according to the exact-only decision. This is not a claim of fully recovered, behaviorally equivalent source for every original feature.

| Requirement | Status | Evidence / scope |
|---|---|---|
| Complete delivered Gradle structure | PASS | Wrapper, pinned build, 876 main Java files, 35 retained Java files, tests and 195 main resources. |
| Binary-owned resource inventory | PASS, scoped | All 180 owned assets are accounted for; nine incompatible variants are retained separately. See recovered-resources.json. |
| Exact original completeness | FAIL / PARTIAL | Physically erased action bodies, initialization and metadata remain absent. See RECOVERY-REPORT.md and recovery/warden proofs. |
| Fully semantic names throughout | INCONCLUSIVE | 1287 automated baseline rename records, hand aliases and named retained members; generic accessor contracts and some locals remain. |
| No additional source remapping | PASS | Source tree and 911-file sources JAR are named Yarn; remapSourcesJar SKIPPED. Standard dependency mapping/remapJar remain. |
| Fresh source build | PASS | fresh-clean.log and fresh-verification.log; external dependency cache reused. |
| JVM / ASM data flow | PASS | 1028/1028 own classes: 974 main + 54 retained, without application class initialization. |
| Static member linkage | PASS, scoped | 66681 references, zero findings. This does not prove every access rule, reflection path or behavior. |
| Source client / core / GPU smoke | PASS, limited | 122 entries, 623 settings, 148 config entries; timer, menu/popup, brightness/config, 19 real GPU readback assertions, GUI and clean exit. |
| Native window title | PASS | window-title.json observes @kazah_teams on the fresh test process. |
| Every module / worlds / reconnect / long stability / FPS | INCONCLUSIVE | 0/122 full module gates; no raw frame-time or soak data. |

Release mod SHA-256: `c05dd91732e7a149c075c286aa73b4b82417feef7dd8deaaf497c003e0743baf`. Full pending acceptance checklist/report is separately supplied in the sibling `acceptance` directory; its consistency audit is not a functional result. The optional recovery source set is compiled/verified but is not automatically registered or included in the installable mod.
