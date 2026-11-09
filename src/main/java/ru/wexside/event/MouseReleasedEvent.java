package ru.wexside.event;

import ru.wexside.event.Event;

public record MouseReleasedEvent(int button) implements Event
{
}

