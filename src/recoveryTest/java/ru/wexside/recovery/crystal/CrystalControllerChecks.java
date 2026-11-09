package ru.wexside.recovery.crystal;

import java.lang.reflect.Field;
import java.util.Random;
import ru.wexside.util.Angle;

/** Plain JVM checks of proven patched construction, release and quantizer arithmetic. */
public final class CrystalControllerChecks {
    private static Object field(Object instance, String name) throws ReflectiveOperationException {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void set(Object instance, String name, Object value) throws ReflectiveOperationException {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    private static final class CountingRandom extends Random {
        int integerCalls;
        int doubleCalls;
        @Override public int nextInt(int bound) {
            if (bound != 8) throw new AssertionError("Original segmented bound");
            integerCalls++;
            return 0;
        }
        @Override public double nextDouble() { doubleCalls++; return 0.5; }
    }

    public static void main(String[] args) throws Exception {
        Random random = new Random(17L);
        CrystalRotationStrategyFactory factory = name -> {
            throw new AssertionError("Construction/release must not manufacture a strategy");
        };
        RichCrystalRotationController controller = new RichCrystalRotationController(factory, random);
        if (field(controller, "random") != random || field(controller, "strategyFactory") != factory) {
            throw new AssertionError("Explicit factory/random identity");
        }
        if (!field(controller, "continuousViewYaw").equals(90.0f)
                || !field(controller, "viewPitch").equals(90.0f)
                || !field(controller, "viewUpdatedThisTick").equals(true)) {
            throw new AssertionError("Patched factory initialization");
        }
        CrystalRotationRequest initial = controller.getCurrentRequest();
        if (initial == null || !initial.equals(CrystalRotationRequest.idle()) || initial.hasTarget()
                || initial.hasCorrection() || initial.changeViewLook()) throw new AssertionError("Idle request");
        if (controller.isUnwrittenRequestSentinel()) throw new AssertionError("Idle object is distinct from unwritten null sentinel");
        if (field(controller, "UNWRITTEN_REQUEST_SENTINEL") != null
                || field(controller, "UNWRITTEN_STRATEGY_NAME") != null) throw new AssertionError("Proven unwritten JVM defaults");
        set(controller, "activeAngle", new Angle(15.0f, 20.0f));
        set(controller, "previousAngle", new Angle(10.0f, 15.0f));
        set(controller, "tickAngleSnapshot", new Angle(12.0f, 18.0f));
        controller.release();
        if (controller.hasActiveAngle() || controller.getTickAngleSnapshot() != null
                || field(controller, "previousAngle") != null || controller.getCurrentRequest() == initial
                || !controller.getCurrentRequest().equals(CrystalRotationRequest.idle())) {
            throw new AssertionError("Release clears retained angles and supplies a fresh idle request");
        }

        CountingRandom steps = new CountingRandom();
        if (CrystalMouseQuantizer.quantize(2.0f, 0.01f, 0.5, steps) != 2.0f
                || steps.integerCalls != 0 || steps.doubleCalls != 0) throw new AssertionError("Zero rounded steps consume no Random");
        float result = CrystalMouseQuantizer.quantize(0.0f, 1.0f, 0.5, steps);
        if (Math.abs(result - 1.05f) > 0.000001f || steps.integerCalls != 1 || steps.doubleCalls != 2) {
            throw new AssertionError("Seven mouse steps in three segments: " + result);
        }
        CountingRandom reverse = new CountingRandom();
        if (Math.abs(CrystalMouseQuantizer.quantize(0.0f, -1.0f, 0.5, reverse) + 1.05f) > 0.000001f) {
            throw new AssertionError("Signed mouse steps");
        }
        if (CrystalMouseQuantizer.quantize(2.0f, 1.0f, -1.0, new CountingRandom()) != 3.0f) {
            throw new AssertionError("Nonpositive step preserves original direct-add branch");
        }
        PatchedCrystalRotationStrategyFactory patched = new PatchedCrystalRotationStrategyFactory();
        CrystalRotationStrategy simple = patched.create("Simple");
        CrystalRotationStrategy legit = patched.create("Legit");
        if (simple == legit || patched.create("Nav") != legit || patched.create(null) != legit
                || patched.create("Neuro") != legit || patched.create("Spooky-T") != legit) {
            throw new AssertionError("Exact supplied two-instance factory and fallback routing");
        }
        patched.initializeStrategies();
        if (patched.create("Simple") == simple || patched.create("Legit") == legit) {
            throw new AssertionError("Factory initialization constructs fresh strategy instances");
        }
        Angle approached = PatchedCrystalRotationStrategy.approach(new Angle(0.0f, 0.0f), new Angle(90.0f, 0.0f));
        if (approached.yawDegrees() != 45.0f || approached.pitchDegrees() != 0.0f
                || PatchedCrystalRotationStrategy.wrap(180.0f) != -180.0f) {
            throw new AssertionError("Patched strategy 45-degree approach and wrapping");
        }
        var state = new ru.wexside.util.RotationState(null, null, null, new Angle(10.0f, 20.0f),
                null, null, 0, 0, 1.0f, true);
        var noTarget = new CrystalRotationRequest(null, null, new Angle(80.0f, 70.0f),
                CrystalRotationStrength.HARD, CrystalMovementCorrection.NONE, false);
        var strategyResult = simple.step(state, noTarget);
        if (strategyResult.ready() || !strategyResult.angle().equals(state.playerAngle())) {
            throw new AssertionError("A request without a target returns toward player and cannot be ready");
        }
        System.out.println("CRYSTAL CONTROLLER CHECKS PASS");
    }
}
