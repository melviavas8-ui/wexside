package ru.wexside.prediction;

import net.minecraft.util.math.Vec3d;

@FunctionalInterface
public interface ProjectileMotionStep {
    public Vec3d apply(Vec3d var1, Vec3d var2);
}

