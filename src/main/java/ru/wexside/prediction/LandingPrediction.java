package ru.wexside.prediction;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public record LandingPrediction(Vec3d position, int flightTicks, ItemStack icon) {
}

