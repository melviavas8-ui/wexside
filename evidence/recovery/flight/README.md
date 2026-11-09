> Publication note: these are the original isolated-analysis notes. Delivered code is in `src/recovery/java`; tests are in `src/recoveryTest/java`. Use the root Gradle `check` task. Paths and standalone commands below describe the analysis workspace, not additional release setup.

# Retained flight behavior recovery

This directory contains readable implementations of the surviving numeric and control-flow bodies for original `wexside/IlIlIllllililil` (FireworkBoost) and `wexside/liIiIlIlliIilil` (ElytraMotion). It does not claim that the removed constructors, action builders, settings defaults or Minecraft integration have been recovered. No staged source was edited.

## Source and verification

- `src/main/java/ru/wexside/recovery/flight/FireworkBoostBehavior.java`: folded-yaw/pitch profile lookup, four exact preset arrays, custom profile path, event speed selection, and the original FireworkRocketEntity velocity consumer.
- `src/main/java/ru/wexside/recovery/flight/ElytraMotionBehavior.java`: exact retained glide/target checks, freeze/key state transitions, 500ms rocket cadence, local-only travel cancellation and rocket-selection workflow with a required inventory integration interface.
- `src/test/java/ru/wexside/recovery/flight/FlightRecoveryChecks.java`: 74 assertions covering every preset bucket, angle boundaries, max horizontal/vertical rule, consumer arithmetic, target/freeze branches, interval boundaries and request routing.

Compile/run from workspace cwd with the installed JDK (Java 21 target):
```
& 'C:\Program Files\Java\jdk-25.0.3\bin\javac.exe' --release 21 -encoding UTF-8 -d work/full-recovery/flight-recovery/classes work/full-recovery/flight-recovery/src/main/java/ru/wexside/recovery/flight/FireworkBoostBehavior.java work/full-recovery/flight-recovery/src/main/java/ru/wexside/recovery/flight/ElytraMotionBehavior.java work/full-recovery/flight-recovery/src/test/java/ru/wexside/recovery/flight/FlightRecoveryChecks.java
& 'C:\Program Files\Java\jdk-25.0.3\bin\java.exe' -cp work/full-recovery/flight-recovery/classes ru.wexside.recovery.flight.FlightRecoveryChecks
```
Actual result: javac exit 0; `FlightRecoveryChecks PASS: 74 assertions`, java exit 0. Sources use Java standard library only and compile independently of, or alongside, staged project classes. They are callable behavior kernels, not fabricated registered modules. Game launch/module acceptance remains INCONCLUSIVE.

## Firework evidence

`firework.javap.txt` is direct original javap output. Numeric helper `liilIlllIII` has been evaluated entirely offline by `decode_numeric.py`, including the original shared 64-entry seed array, 32-bit overflow/unsigned shifts, int/long arrays and decode loops. No target class or initializer was loaded. Unknown static references cause failure; only the two proven boolean initialization flags use specified JVM-default false. `numeric-constants.json` contains all decoded fields and float/double bit conversions.

Recovered constants: angle bucket 5 degrees, folds 90/180/45 degrees; preset array length9; custom setting helper range1.5..2.5, original steps argument0.01 and precision-like argument2. The semantic role of the original builder `steps(double)` and obfuscated precision setter is not assumed equivalent to a staged builder API; these arguments remain named as retained arguments in the source.

`iIIlIlllIII` bci128-145 checks BooleanSetting.enabled. bci155-180 returns if local player absent. bci183-206 gets local **yaw** (wrapped with MathHelper.wrapDegrees) and absolute local **pitch**, then passes each to the triangular fold helper `IlilIlllIII` bci56-140. It is not pitch-only. Actual wrapper lllIIlllIII passes selector3 to IlIilillIiIlIil.ilillIIIl, which reaches IlIillliIiIllil.iillIIllI and then original IlilIlllIII; the actual helper body has been inspected.

Mode labels decode to exact strings ` B-HVH` (leading space retained), `RW`, and `Custom`. Handler bci217-266 selects B-HVH tables, bci276-325 selects RW, and bci335-356 uses custom setting arrays for any other mode value. Visibility helper lIilIlllIII is true only for exact `Custom`. At bci366-411 it raises horizontal speed to vertical speed if vertical>horizontal, then writes first and second event floats respectively. The original event class `llliIiIiliIllil` and its two float setters/getters have been inspected (`boost-event.txt`).

`firework-consumer.txt` proves the actual event is created only for a local boosted entity with both speeds initialized1.5. If both remain1.5, vanilla supplied velocity is preserved. Otherwise the consumer uses first speed for X/Z and second speed for Y, with component formula old + look*0.1 + (look*speed-old)*0.5. This formula, not a guessed generic speed multiplier, is in the recovered source.

## Elytra evidence

Actual `check()` uses selector1 in IlIilillIiIlIil.IilIilIIl, which calls IlllliIiIiIllil.ilIliIllI and then original liililIilII. The result is false for missing target/player, otherwise **not** (enabled AND player gliding AND target gliding) AND distance<threshold. Wrapper selectors and their actual intermediary callees are recorded in `selector-traces.json`; full disassemblies are retained.

Original iiililIilII bci132-165 returns with no state write when player absent. Disabled bci190-201 clears freeze. Non-gliding bci223-228 clears freeze. When local is gliding, check=true sets the controlled GameOptions key false and freeze=true; check=false sets key true and freeze=false. The exact identity of original GameOptions.ilIliliiiI was not established, so the caller supplies a real `KeyControl`; it is not relabeled jump/forward without evidence.

Automatic-firework option is original llIlIiIiilII. With a nonnull aura target and elapsed time>=500ms, it invokes rocket attempt and stores current timestamp even if the attempt finds no usable inventory. Target resolution occurs twice, once in check and again before automatic firework; source preserves these independent supplier calls. Target source is the AttackAuraModule lookup/helper, established through member candidates and owner references, not a misleading bundled module label.

Original lilIilIilII cancels only an event whose entity is the local player while freeze is true. requestRocket's retained path prefers offhand FIREWORK_ROCKET, else finds a rocket inventory slot, returns if absent, and reaches inventory request submission. Original constants include40 and500. The API deliberately calls the40 value `retainedArgument`; its erased action-constructor parameter role has not been established.

## Decoded enum/string evidence

`decode_labels.py` traces referenced original enum labels through their holder assignments and decodes the retained teGM0Rro3NgL0yQBhCP28UZ8n algorithm offline. `decoded-labels.json` records exact encoded strings, holder fields, enum field names and decoded values. Original priority labels: LOW/NORMAL/HIGH/CRITICAL. Original timing labels: FAST/SILENT/LEGIT. Original conflict labels: REPLACE/QUEUE/SKIP_IF_BUSY. The real retained path uses HIGH/FAST/SKIP_IF_BUSY and owner `elytra_motion`.

The actual enum/holder classes followed here contain encrypted literal assignments, not CONSTANT_Dynamic bootstrap strings. The condy bootstrap used by other recovered enums must not be applied to the wrong layer merely because their names look alike. This pass decodes the actual referenced label representation; it makes no claim that a nonexistent condy was evaluated.

## Precise remaining boundaries

1. Both original module constructors only call the superclass; settings fields and their original initialized defaults are absent. API construction requires enabled flags, mode/custom profile values, threshold and initial freeze/timestamp explicitly. These are configuration snapshots; callers supply updated snapshots when settings change.
2. Original action classes IiIllillliIllil, liIiIiIiIiIlll and lillIiIlIlIlll are zero-field/no-arg shells, lacking the expected interface/constructor bodies. The retained caller has NEW/DUP/POP sequences and discarded builder operands; there is no intact original inventory-action object to reconstruct faithfully. `RocketInventory` is a required abstract integration boundary, not a no-op implementation or manufactured task. The tests use an observable spy only.
3. Original task timing/conflict enums do not match the simplified staged ClickPolicy/TaskFlag inventory model automatically. The source preserves decoded strings at the port; integration must recover or explicitly map the original scheduling semantics rather than silently substituting staged SILENT/SKIP_IF_PRESENT.
4. EventBus registration/mixin hooks, settings labels/defaults/ids, module display metadata and controlled key identity are not recovered here. These files must not be advertised as complete working original modules.
5. Surviving functions, numeric tables and branches are implemented; unknown constructor/action logic is neither zeroed nor replaced with arbitrary metadata. Original malformed class bytecode cannot itself act as a runtime oracle, so checks compare extracted numeric golden values, independently simplified velocity arithmetic and explicit control-flow branch expectations.
