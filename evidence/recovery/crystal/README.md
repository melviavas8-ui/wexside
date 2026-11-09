> Publication note: these are the original isolated-analysis notes. Delivered code is in `src/recovery/java`; tests are in `src/recoveryTest/java`. Use the root Gradle `check` task. Paths and standalone commands below describe the analysis workspace, not additional release setup.

# CrystalAura retained source: frozen isolated package

## Accepted source scope

`src/main/java/ru/wexside/recovery/crystal` contains 20 compilable named Java sources. This recovers the retained CrystalAura module bodies, selector-dispatched callbacks, rich controller bodies, request/event contracts, inventory/aim/lease helpers, exact mouse quantizer, and the supplied patched strategy/factory. Nothing was copied into staging main sources or registered as a default module. The package is suitable for an unregistered Gradle recovery source set using the project's normal named Yarn/Loom dependencies; it requires no separate source remapping step.

Exact inspected artifact: `wexside-decrypt-1.21.11.jar`, SHA-256 `340b0c8f77e020807ee994e3c9e8877950221c87ecf58ac900d4719d8c25a747`. This artifact already includes repair patches. Reproducing its patched factory/strategy is not a claim to have restored erased pre-patch constructors or original rotation policies.

## What is executable

- `CrystalAuraModule`: all retained runtime method bodies and all six original callback registrations; four setting objects, registration order and module metadata are required constructor inputs. The retained rotation controller is supplied explicitly. Original constructor timing through the superclass virtual initialize call is preserved. The module does not invent setting defaults, IDs, captions or key policies.
- `RichCrystalRotationController`: retained packet tracking, correction cooldown, request ownership, strategy transitions, background requests, view rotation, counters, mouse quantization and release logic. Handler methods are exposed for a host to bind after verifying event phases. No guessed registration order/phase is inserted.
- The no-argument rich-controller constructor reproduces **supplied GuiRepair factory effects**: new patched strategy factory, new Random, fresh idle request, view yaw/pitch 90, viewUpdatedThisTick true. The overload accepts explicit factory and Random inputs. Neither uses Unsafe.
- `PatchedCrystalRotationStrategyFactory` reproduces supplied IiIiIiliIiIllil: distinct Simple/Legit strategy instances, exact runtime-name aliases, unknown/null names falling back to Legit. The original ignored init context is not invented as a recovered configuration type; the port exposes the retained map-reset body as initializeStrategies().
- `PatchedCrystalRotationStrategy` is the supplied llIlllIlllIiIl body: wrapped yaw, 45-degree vector approach, pitch clamp, strict less-than-two-degree convergence, target and attackReady gates. Its two lifecycle methods contain only return in the supplied bytecode; the empty port bodies are proven bodies, not placeholders.
- `CrystalMouseQuantizer` preserves the sensitivity formula, integer mouse steps, segmented float rounding and Random-consumption order. Explicit sensitivity overload permits arithmetic testing without a game instance.

## Exact-only anomalies retained

The rich controller's static strategy-name and request sentinel fields have **no writes** and its supplied class initializer only returns. Their Java null initializers model the proven JVM defaults, not undecoded constants. The ordinary patched factory writes a nonnull fresh idle request to the current-request instance field; that object remains distinct from the null static sentinel.

The controller's apply-and-test branch discards its RotationApplyResult, pushes literal null and compares it with initialized nonnull enum IDLE. It therefore returns false in the supplied artifact. The port retains this condition explicitly; it does not substitute result.ready(). The original controller can dereference its unwritten null request sentinel in navigation/state construction paths. No guessed repair or invented null normalization was added. These defects prevent an assertion of usable gameplay behavior merely from a successful compilation.

`throw null` statements remaining after flattened switch loops are original impossible-state fallback instructions. They are not missing method implementations. Original conditional null returns are also retained.

## Evidence

- `selector-resolution.json`: all 22 module selector wrappers resolved to retained local bodies.
- `controller-selector-resolution.json`: 20 rich-controller selector routes, with bridge/branch evidence. The three additional bridge routes are recorded in `IlIillIlIlIllil.full.javap.txt` and `liliIlliliIlIil.full.javap.txt`.
- `IlIiIillIlIllil.full.javap.txt`: complete controller bytecode; literal-null/IDLE comparison is in iiiIliilI, the unwritten-field evidence is its return-only class initializer and lack of putstatic for the sentinels.
- `IiIiIiliIiIllil.full.javap.txt`, `llIlllIlllIiIl.full.javap.txt`: exact supplied patched factory and strategy bodies. `gui-repair.javap.txt` contains ensureHudServices initialization and auraRuntimeName aliases.
- Numeric constants are decoded offline in decoded-numeric-constants.json, inventory-helper-constants.json, controller-decoded-constants.json and quantizer-decoded-constants.json, with corresponding decoder scripts. No original class initializer was invoked for decoding.
- `remaining-item-field-mappings.json`: original vanilla item registration strings identify the supported netherite item constants; no item identity guessed from a field shape.
- `module-semantic-symbols.json` and `controller-semantic-symbols.json`: evidence-backed readable symbol translations. Controller attack/hit counter names follow the original RotationState constructor field order/labels.
- `frozen-source-manifest.json`: SHA-256 manifest of all 20 production sources and three check sources.

## Verification

JDK 25 javac --release 21 compiled all 20 production sources and the three checks against the staged named runtime classpath. Final compile exit code 0; `compile-controller-final.log` is empty. All three ordinary JVM main checks passed:

1. `CrystalRotationLeaseChecks.java`: identity-only ownership, mismatched-release clearing, clear-before-callback, exactly-once callback.
2. `CrystalAuraConstructionChecks.java`: supplied setting values and registration order retained; duplicate/incomplete setting registration rejected.
3. `CrystalControllerChecks.java`: explicit factory/random identities, observed patched initialization, unwritten sentinels, fresh idle release/reset, segmented quantizer arithmetic and Random consumption, signed/nonpositive-step behavior, supplied factory routing/fresh instances, patched approach/wrapping and no-target readiness behavior.

Machine-readable results: `verification-results.json`. Checks are in package ru.wexside.recovery.crystal; root can copy them into its recovery test source set. In-world event ordering, game-state rotation, live target readiness and end-to-end placement/attack are not tested by these plain JVM checks. No original protected JAR code or native library was executed.

## Remaining boundaries before registration

1. Original four BooleanSetting metadata/defaults/key policies and registration order are absent per initializer audit. Supply them explicitly; do not register a guessed default module.
2. CrystalUpdateEvent (original IlllllIlIillIil) has no constructor caller in the complete binary reference index. Its exact producer is unresolved. No fabricated tick post is included.
3. BeforeInputHandlingEvent is proven at MinecraftClient.handleInputEvents HEAD; cancellable BeforeItemUseEvent at doItemUse HEAD. These optional event types are not posted by existing staging mixins. Root must review/apply original-equivalent host bindings rather than silently map them to lower-information existing events.
4. CrystalBlockInteractEvent preserves the original Block plus BlockPos data. Existing staging BlockInteractEvent lacks the position; `block-interact-position.proposed.patch` is an unapplied compatible alternative. Do not map away the position.
5. Incoming/outgoing/world-reset controller handler bodies survive, but controller constructor registration phases/order are erased. No speculative listener registrations are provided. Original incoming packet posting location/order requires host validation before equivalence with staging IncomingPacketEvent can be claimed.
6. Rich controller and module are compiled optional recovery source, not a claim of complete source/gameplay equivalence. Supplied patched factory/strategy behavior is distinguished from unknown pre-patch initialization/policies throughout.
