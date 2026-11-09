package ru.wexside.schedule;

import java.util.List;
import ru.wexside.schedule.ScheduledEvent;

public final class EventScheduleRegistry {
    private final List<ScheduledEvent> events = List.of(new ScheduledEvent("Air Drop", EventScheduleRegistry.timesEveryHours(0, 3)), new ScheduledEvent("Scrooge", new int[]{1140}), new ScheduledEvent("Secret Trader", EventScheduleRegistry.timesEveryHours(1, 3)), new ScheduledEvent("Mascot", new int[]{1200}), new ScheduledEvent("Competition", EventScheduleRegistry.timesEveryHours(2, 3)));

    public List<ScheduledEvent> events() {
        return this.events;
    }

    private static int[] timesEveryHours(int startHour, int intervalHours) {
        int count = 24 / intervalHours;
        int[] times = new int[count];
        for (int index = 0; index < count; ++index) {
            times[index] = (startHour + index * intervalHours) % 24 * 60;
        }
        return times;
    }
}

