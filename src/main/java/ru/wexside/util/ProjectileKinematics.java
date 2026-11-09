package ru.wexside.util;

import net.minecraft.util.math.Vec3d;

/** Shared launch and velocity arithmetic recovered from the original bytecode. */
final class ProjectileKinematics {
    private ProjectileKinematics() {}

    static Vec3d launchVelocity(Vec3d direction, double speed, Vec3d inheritedMotion) {
        // Original llIillllIlIllil.iililllil, bci 75-88: scale, then Vec3d addition.
        return direction.multiply(speed).add(inheritedMotion);
    }

    static Vec3d integrateVelocity(Vec3d velocity, double drag, double gravity, boolean gravityBeforeDrag) {
        // Original motion lambdas use dneg then Vec3d.iiIIililI(DDD), whose xyz operations are dadd.
        // lililllil bci 11-31 applies gravity first; its bci 35-55 branch applies drag first.
        return gravityBeforeDrag
                ? velocity.add(0.0, -gravity, 0.0).multiply(drag)
                : velocity.multiply(drag).add(0.0, -gravity, 0.0);
    }
}
