package ru.wexside.event;

import ru.wexside.event.Event;
import ru.wexside.event.EventListener;

public interface EventBus {
    public <T extends Event> void subscribe(Class<T> var1, EventListener<? super T> var2, int var3);

    public <T extends Event> void subscribe(Class<T> var1, EventListener<? super T> var2);

    public <T extends Event> void post(T var1);
}

