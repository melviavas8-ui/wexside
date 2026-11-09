package ru.wexside.misc;

import ru.wexside.misc.WaypointType;

public record Waypoint(String name, int x, int y, int z, WaypointType type) {
    public Waypoint {
        type = type == null ? WaypointType.WAYPOINT : type;
    }

    public Waypoint(String name, int x, int y, int z) {
        this(name, x, y, z, WaypointType.WAYPOINT);
    }
}

