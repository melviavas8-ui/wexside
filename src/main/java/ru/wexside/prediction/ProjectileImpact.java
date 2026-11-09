package ru.wexside.prediction;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public record ProjectileImpact(Vec3d position, Vec3d velocityDirection, Direction face, Entity hitEntity) {
}

