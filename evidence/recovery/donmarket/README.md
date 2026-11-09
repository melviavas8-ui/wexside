> Publication note: these are the original isolated-analysis notes. Delivered code is in `src/recovery/java`; tests are in `src/recoveryTest/java`. Use the root Gradle `check` task. Paths and standalone commands below describe the analysis workspace, not additional release setup.

# Retained DonMarket module bridge from temp_dump

Recovered isolated Java21 behavior: src/main/java/ru/wexside/recovery/donmarket/DonMarketRunnerBehavior.java. Nothing was registered or copied into staging.

## Evidence and identity

Original owner wexside/liIiIlIiIiIiIil is an automation module bridge for a DonMarket runner. The controller owner IillliliIilill has an original ConstantDynamic string that decodes to donmarket_runner. Its bootstrap liIillIlIlliIil.string and key routine were disassembled and translated offline: 4-hex-digit units XOR seed-dependent32-bit rotations, UTF-16 char narrowing. This owner is strong functional-role evidence; it does NOT recover the module's original ID/display name/category/default settings. Overlay labels independently decode Start/Pause/Continue/Stop in Russian. See decoded-role-evidence.json and reconciliation.json.

The two temp_dump copies have identical module hashes. The main JAR module differs in exactly five bodies: controller tick factory99 instructions vs1, click16 vs2, type11 vs2, render12 vs1, input11 vs2. Exact hashes, sizes, per-method fingerprints/references and all25 original method-to-readable mappings are in reconciliation.json. No matching named module/controller/session/overlay/rule exists in the propagated mapping; reference shared setting/class shape does not justify Timer/FastPlace identities.

## Recovered behavior

Tick always ticks the overlay first, even disabled or paused. If the controller is absent, it tests session then coordinator and returns if either is unavailable. It rereads both providers for construction, supplies live enabled&&!paused, rule, click-delay and budget callbacks, and supplies a completion callback that ONLY disables enabled. A new controller ticks immediately in the creation tick. Existing controllers tick without consulting the providers. Original flattened state constants and successors are recorded in the independent test oracle; original tick bci60..225 is retained in module-temp.javap.txt.

Rule creation reads the two integer-valued settings each time, widens the first to long, and supplies Set.of(). The first is a minimum rate threshold and the second bounds quoted units, established by the surviving rule helper's comparisons against offer.rate() and its positive quantity. Click-delay identity follows controller ctor fifth callback -> field illliIilI -> getAsInt widened to long -> Math.max -> click(page,slot,long). Units/default are not guessed.

Pause toggles the flag and invokes controller.pause only on entry to paused. Resuming has no separate controller call. Start while disabled clears pause then enables; start while enabled toggles pause. Stop disables, clears pause and resets an existing controller. Completion disables without clearing pause or resetting.

Static click/render require instance AND controller. Type/input require only instance. Enabled/paused are not additional input gates. This asymmetry is preserved. Static INSTANCE installation is explicit because its original initializer is missing.

The scattered dispatch selectors were checked: register selector1 -> IllilillIiIllil.lliIilllI -> original IilllIIilII; tick selector2 -> iIiIilllI -> original IllllIIilII; start selector0 -> liiIilllI -> original IlIllIIilII; rule selector0 -> lIiIilllI -> original iilllIIilII; controller selector2 -> iliIilllI -> original lIIllIIilII. Callback/selector wrappers were consolidated, not left as no-op placeholders. Discarded numeric protection scaffolding is not reproduced as game behavior.

## Exact recovery boundaries

The surviving public constructor only forwards event bus and caller-supplied metadata to Module. It never assigns its final settings, overlay or settings panel fields, its unused numeric constant fields, or INSTANCE. Those missing bodies are not fabricated: Metadata, Settings, InitialState, provider functions, a working Overlay and a working ControllerFactory are mandatory caller inputs. These interfaces are explicit dependencies, not fake controllers or an imitation of the underlying market transaction engine. The controller/session/transaction implementation and the overlay's drawing implementation were not ported by this scoped bridge recovery.

The hidden-setting helper is retained as HiddenNumberRecipe plus a required functional factory. It preserves id, label used as both name and description, supplied range/value/steps, original int option0, visibility supplier false. The exact meaning of the original steps(double) and integer formatting option is not assumed to match the current named NumberSettingBuilder, whose retained initialization differs. In particular this recovery does not substitute invented precision/multiplier/defaults. The factory must implement the known original builder contract if wired later.

## Validation

PASS62 assertions on the production isolated class. Tests cover all8 existing-controller/session/coordinator branches using an independently interpreted original flattened branch table; exact creation/provider-read order; live rule/config callbacks; paused/disabled ticking; pause/resume/stop/completion differences; all input gates; matrix identity forwarding; tick registration and hidden-setting recipe. No target class/initializer or game client was executed.

Run from workspace root:

```powershell
& 'C:/Program Files/Java/jdk-25.0.3/bin/javac.exe' '@work/full-recovery/temp-module-recovery/javac.args'
& 'C:/Program Files/Java/jdk-25.0.3/bin/java.exe' '@work/full-recovery/temp-module-recovery/java.args'
```

The isolated source uses only JDK and the same JOML1.10.8 dependency already used by the staged project. It does not depend on unresolved obfuscated classes. Main staging and flight outputs were not changed.
