package ru.wexside.event;

import ru.wexside.event.CancellableEvent;
import ru.wexside.event.Event;

public class PreAttackEvent
extends CancellableEvent
implements Event {
    private float yaw;

    public PreAttackEvent(float f) {
        this.yaw = f;
    }

    public void setYaw(float f) {
        this.yaw = f;
    }

    public float getYaw() {
        return this.yaw;
    }
}

