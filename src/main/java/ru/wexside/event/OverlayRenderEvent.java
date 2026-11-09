package ru.wexside.event;

import ru.wexside.event.CancellableEvent;
import ru.wexside.event.OverlayType;

public final class OverlayRenderEvent
extends CancellableEvent {
    private final OverlayType type;

    public OverlayRenderEvent(OverlayType type) {
        this.type = type;
    }

    public OverlayType type() {
        return this.type;
    }
}

