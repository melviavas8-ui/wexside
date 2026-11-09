package ru.wexside.event;

import ru.wexside.event.Event;

public record KeyReleasedEvent(int key, int scancode) implements Event
{
}

