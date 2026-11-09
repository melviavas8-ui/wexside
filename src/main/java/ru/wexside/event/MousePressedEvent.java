package ru.wexside.event;

import ru.wexside.event.Event;

public record MousePressedEvent(int button) implements Event
{
}

