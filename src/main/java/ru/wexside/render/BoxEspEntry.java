package ru.wexside.render;

import net.minecraft.util.math.Box;

public record BoxEspEntry(Box box, int color, boolean animated, boolean depthTest) {
}

