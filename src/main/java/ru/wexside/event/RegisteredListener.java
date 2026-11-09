package ru.wexside.event;

import ru.wexside.event.Event;
import ru.wexside.event.EventListener;

final class RegisteredListener<T extends Event> {
    private final EventListener<? super T> listener;
    private final int priority;

    RegisteredListener(EventListener<? super T> listener, int priority) {
        this.listener = listener;
        this.priority = priority;
    }

    int priority() {
        return this.priority;
    }

    void dispatch(Event event) {
        this.listener.onEvent((T)event);
    }
}

