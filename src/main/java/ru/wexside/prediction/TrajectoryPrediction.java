package ru.wexside.prediction;

import java.util.List;
import net.minecraft.util.math.Vec3d;
import ru.wexside.prediction.ProjectileImpact;

public record TrajectoryPrediction(List<Vec3d> points, ProjectileImpact impact, int flightTicks) {
}

