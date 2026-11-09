package ru.wexside.event;

import ru.wexside.event.DamageType;
import ru.wexside.event.Event;

public final class DamageEvent
implements Event {
    private final DamageType type;

    public DamageEvent(DamageType type) {
        this.type = type;
    }

    public DamageType type() {
        return this.type;
    }
}

