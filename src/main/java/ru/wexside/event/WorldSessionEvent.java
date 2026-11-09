package ru.wexside.event;

import ru.wexside.event.Event;

public record WorldSessionEvent(Change change) implements Event
{

    public static enum Change {
        JOINED,
        DISCONNECTED;

    }
}

