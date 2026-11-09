package ru.wexside.event;

import net.minecraft.entity.Entity;
import ru.wexside.event.CancellableEvent;
import ru.wexside.event.Event;

public class EntityAttackEvent
extends CancellableEvent
implements Event {
    private final Entity entity;

    public EntityAttackEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return this.entity;
    }
}

