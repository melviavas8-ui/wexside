package ru.wexside.prediction;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public record ImpactQuad(Vec3d position, Direction face, int color) {
}

