package ru.wexside.event;

import ru.wexside.event.Event;

public record KeyPressedEvent(int key, int scancode) implements Event
{
}

