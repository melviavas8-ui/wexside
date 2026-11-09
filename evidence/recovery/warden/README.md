> Publication note: these are the original isolated-analysis notes. Delivered code is in `src/recovery/java`; tests are in `src/recoveryTest/java`. Use the root Gradle `check` task. Paths and standalone commands below describe the analysis workspace, not additional release setup.

# AutoWarden Retained-Body Recovery — Frozen

This package contains **12 compile-checked Java source files and one test file**, tracing **57 retained behavior methods**. `build.ps1` compiles against the project's current named Minecraft/runtime classpath and passes **28 boundary checks**. The module is **not registered**. This is not a complete AutoWarden implementation or a claim that every supplied helper is recovered.

Original input: `work/input/Wexside-Launcher/wexside-decrypt-1.21.11.jar`, SHA-256 `340b0c8f77e020807ee994e3c9e8877950221c87ecf58ac900d4719d8c25a747`.

## Files To Integrate

Copy only `src/main/java/ru/wexside/recovery/warden/` and, for checks, `src/test/java/ru/wexside/recovery/warden/`. `mapped-java/`, `raw-java/`, `mapped-classes/` and `analysis-classes.jar` are analysis evidence, **not accepted or runnable recovered source**. `frozen-source-sha256.json` records every frozen Java file. `coverage.json` records the counts.

| Source | Retained Behavior |
|---|---|
| `WardenState` | Original SAVE, TAKE, COLLECT, ESCAPE declaration order. |
| `WardenLootLevel` | Original labels; LOW=1.5, MEDIUM=1.0, HIGH=0.8; truncating integer scale; unmatched label returns MEDIUM. That fallback is retained code, not an invented setting default. |
| `WardenAnarchyRoute` | Home exclusion, preserved duplicates, original shuffle, round-robin targets and readiness. |
| `WardenConsumableWindow` | Entire target-specific deferral decision, reset and timing policy: 10-second cooldown eligibility, 15-second window, duration/food/health/distance thresholds. |
| `WardenLootFilter` | Entire retained medium/high exclusion policy and `PublicBukkitValues` / `minecraft:ftid` / `potion-popper` matching. Requires an explicit mode supplier. |
| `WardenSupplyFailures` | Entire one/ten-failure policy and one-second counting interval. Clock is an explicit dependency. |
| `WardenDeathTracker` | Entire game-message death/killer extraction and one-shot consumption. Constructor retains IncomingPacketEvent binding; the BooleanSupplier association is supplied explicitly. |
| `WardenThreatTracker` | Warden distance, sound, facing/movement, anger and remembered-position bodies. Clock/player inputs are explicit; original event wiring is not registered by this package. |
| `WardenFarmRegion` | Entire horizontal/volume containment and inset clamping policy. Retained Overworld bounds: X[-2070,-1921], Z[-2076,-1929], volume Y[-60,-35]. |
| `WardenQueries` | Ten extracted game/decision queries, including exact squared-distance comparisons, collision/candle checks, inventory slot search, farm parsing and chest orbit. |
| `WardenCoordinator` | State-boundary effects, chest approach, stall watchdog, interaction delay, command/container pulses and the complete retained collection-to-escape decision. Required action/target interfaces have no default implementations. |
| `WardenSettings` | Explicit six-setting input contract. No original captions, IDs, defaults, visibility or constructor associations are fabricated. |

`method-evidence.json` links all 57 traced behaviors to exact original owner/name/descriptor and indexed fingerprints. `warden-constant-arrays.json`, `loot-constant-arrays.json`, `consumable-window-constant-arrays.json`, and `../initializer-audit/warden-mode-labels.json` retain offline decoding evidence. `ConstantVm.java` interprets an arithmetic/array opcode whitelist and never loads or initializes a target class.

## Exact Erased-Body Boundary

The supplied AutoWarden constructor initializes only its ArrayDeque and invokes Module's constructor. The six setting-object writes and the helper-reference initialization graph are absent; see `../initializer-audit/README.md`, `initializer-recovery-analysis.md`, and `raw-target-references.jsonl`. Settings and initial state therefore remain explicit inputs. Actual helper consumers/constructors are preserved in the evidence; an unestablished supplier association is not silently replaced with GuiRepair's guesses.

The following **six action/request/builder types contain zero fields and exactly one public `()V` constructor**, whose five-byte Code invokes only Object's constructor:

* `wexside/llIlIllllillIl`
* `wexside/liIiIiIiIiIlll`
* `wexside/IllillIilililil`
* `wexside/liliIiIlllIllil`
* `wexside/liIiIillliIiIil`
* `wexside/lllilililllilil`

Proof: `erased-action-class-proof.json` and `evidence/erased-action-classes.javap.txt`. This is not an intended abstract interface/no-op hook: the retained consumers allocate action objects, load operational parameters, discard them and pass null requests to the arbiter.

`unconstructed-allocations.json` identifies **seven NEW sites across five classes** where the allocated owner's constructor is never invoked anywhere in the same method. This is a conservative lower bound, not a substitute for JVM verification:

| Original Owner.Method | Allocated Type(s) |
|---|---|
| `llIillIillliIl.iiIIliiilII` | `llIlIllllillIl`, `liIiIiIiIiIlll` |
| `IillliliIillIil.click` | `IllillIilililil`, `liliIiIlllIllil` |
| `IillllIlIiIllil.iIIiIiIlIl` | `IllillIilililil` |
| `liIiIililillll.IiiIiIIll` | `liIiIiIiIiIlll` |
| `llIiIillliIlIil.IIliliII` | `liliIiIlllIllil` |

The exact AutoWarden hand-clearing body is in `../initializer-audit/auto-warden.javap.txt`, method `iiIIliiilII(Lwexside/llIillIillliIl;Lwexside/IlliliIiIilil;)Z`. Offsets 367–375 and439–453 retain NEW/DUP/argument loads followed by POPs/ASTORE instead of constructor calls. Offsets 455–539 discard action, owner, HIGH priority, FAST/SILENT policy, SKIP_IF_BUSY and validity-supplier operands and store null. Offsets 547/558 submit that null local. Some request policy is thus recoverable, while the actuator/builder implementations and erased calls are not present in those types. No replacement actuator is invented here.

The full start/stop/tick/save/take/collect integration and helper implementations outside this package remain analysis source in `mapped-java/`; they are not counted as accepted runnable recovery. In particular, callers must provide actual implementations for `WardenCoordinator.Actions`/`Targets`, an exact setting/supplier wiring, and the original threat-tracker tick hook (`wexside/IlllllIlIillIil`) plus IncomingPacketEvent delivery before enabling a module. These are explicit remaining integration/dependency boundaries, not empty implementation bodies.

## Alternate-Copy Audit

`alternative-copy-audit.json` covers every standalone `.class`/`.java` under `work/input`, including `Wexside-Launcher/tools` and `temp_dump`, plus every class/source entry in every recursively nested input JAR/ZIP: **215 standalone classes, 906 Java files, 80 archives (46 nested), 32,930 archived classes; zero read/parse errors**. No Java declaration or alternate class definition of the six erased action/request/builder types was found.

One earlier AutoWarden class exists in `temp_dump/wexside/llIillIillliIl.class`: 56,843 bytes, SHA-256 `a92e9030899cdd5fa288b36fb4211824fbfca04e94cbb9ac62768e4126957709`. It has the same 100 fields and 124 methods. `temp-dump-comparison.json` and `evidence/temp-dump-normalized.diff` compare all 124 resolved javap method bodies: **110 are identical after constant-pool normalization; the other 14 differ only at 20 instructions redirecting direct helper invokevirtual calls through GuiRepair invokestatic wrappers in the main JAR**. The 25-byte module constructor is identical after constant-pool resolution. The erased action section remains erased. This alternate supplies useful direct helper owner/name/descriptor evidence, not the missing constructors/builders/settings.

The new launcher RAR's main JAR was independently hash-confirmed identical by root; its ancillary copies were independently compared by root. This audit makes no claim about artifacts outside the enumerated supplied input tree.

## Verification And Freeze

Run `& work/full-recovery/warden-recovery/build.ps1` from the workspace. JDK25.0.3 and the current `project/build/recovery/runtime-classpath.txt` are used. `verification.txt`: **28 recovered-policy boundary checks passed**. Checks cover scale/truncation, label fallback, route properties, exact window thresholds/time boundaries, supply-failure timing, state-effect order, repeated-state behavior, inventory escape thresholds and consecutive anger ticks. Game input/output behavior and erased actuator execution were not tested or claimed.

Java sources/test are frozen for root integration. No shared main sources, mixins, registration, or default module inventory were edited. `@kazah_teams` branding is preserved.
