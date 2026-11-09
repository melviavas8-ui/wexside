package ru.wexside.event;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ru.wexside.event.Event;

public final class TotemPopEvent
implements Event {
    private final Vec3d position;
    private final Entity entity;
    private final long time;

    public TotemPopEvent(Entity entity, Vec3d position, long time) {
        this.entity = entity;
        this.position = position;
        this.time = time;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public Vec3d toVec3d() {
        return this.position;
    }

    public long getTime() {
        return this.time;
    }
}

