package ru.wexside.util;

import net.minecraft.util.math.Vec3d;

/** Arithmetic regression checks against the recovered original bytecode operations. */
public final class ProjectileRecoveryChecks {
    private static int assertions;

    public static void main(String[] args) {
        Vec3d crossbow = ProjectileKinematics.launchVelocity(new Vec3d(0.0, 0.0, 1.0), 3.15, Vec3d.ZERO);
        vector("stationary crossbow forward launch", crossbow, 0.0, 0.0, 3.15);
        require(crossbow.lengthSquared() > 9.0, "a stationary crossbow must retain forward launch speed");

        Vec3d moving = ProjectileKinematics.launchVelocity(new Vec3d(0.6, 0.0, 0.8), 2.5,
                new Vec3d(0.2, -0.1, -0.3));
        vector("inherited motion is additive", moving, 1.7, -0.1, 1.7);

        Vec3d initial = new Vec3d(0.5, 0.01, -0.25);
        Vec3d afterDrag = ProjectileKinematics.integrateVelocity(initial, 0.8, 0.05, false);
        Vec3d beforeDrag = ProjectileKinematics.integrateVelocity(initial, 0.8, 0.05, true);
        vector("drag then gravity", afterDrag, 0.4, -0.042, -0.2);
        vector("gravity then drag", beforeDrag, 0.4, -0.032, -0.2);
        require(afterDrag.y < 0.0 && beforeDrag.y < 0.0, "gravity must turn small upward motion downward");
        require(beforeDrag.y > afterDrag.y, "pre-drag gravity must also be attenuated by drag");

        // Independent closed-form oracle for repeated original dmul, dneg, dadd arithmetic.
        // For drag-first: y(n) = y(0)*d^n - g*(1-d^n)/(1-d).
        // For gravity-first, the geometric gravity term is additionally multiplied by d.
        for (boolean gravityFirst : new boolean[]{false, true}) {
            Vec3d velocity = new Vec3d(1.0, 0.4, -0.5);
            double drag = 0.98;
            double gravity = 0.04;
            for (int tick = 1; tick <= 40; tick++) {
                velocity = ProjectileKinematics.integrateVelocity(velocity, drag, gravity, gravityFirst);
                double power = Math.pow(drag, tick);
                double gravitySum = gravity * (1.0 - power) / (1.0 - drag);
                double expectedY = 0.4 * power - gravitySum * (gravityFirst ? drag : 1.0);
                vector("closed-form trajectory tick " + tick + " gravityFirst=" + gravityFirst,
                        velocity, power, expectedY, -0.5 * power);
            }
        }
        vector("zero drag, gravity last", ProjectileKinematics.integrateVelocity(initial, 0.0, 0.05, false), 0, -0.05, 0);
        vector("zero drag, gravity first", ProjectileKinematics.integrateVelocity(initial, 0.0, 0.05, true), 0, 0, 0);
        System.out.println("ProjectileRecoveryChecks PASS: " + assertions + " assertions");
    }

    private static void vector(String scenario, Vec3d actual, double x, double y, double z) {
        near(scenario + " x", actual.x, x);
        near(scenario + " y", actual.y, y);
        near(scenario + " z", actual.z, z);
    }

    private static void near(String scenario, double actual, double expected) {
        assertions++;
        if (!Double.isFinite(actual) || Math.abs(actual - expected) > 1.0E-10) {
            throw new AssertionError(scenario + ": expected " + expected + ", actual " + actual);
        }
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
