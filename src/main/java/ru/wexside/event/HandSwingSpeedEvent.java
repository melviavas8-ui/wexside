package ru.wexside.event;

import ru.wexside.event.CancellableEvent;
import ru.wexside.event.Event;

public final class HandSwingSpeedEvent
extends CancellableEvent
implements Event {
    private float speedMultiplier;

    public void setSpeedMultiplier(float speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public float getSpeedMultiplier() {
        return this.speedMultiplier;
    }
}

