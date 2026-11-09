package ru.wexside.schedule;

import java.util.Arrays;

public record ScheduledEvent(String name, int[] minutesOfDay) {
    public ScheduledEvent {
        minutesOfDay = (int[])minutesOfDay.clone();
        Arrays.sort(minutesOfDay);
    }

    public int[] minutesOfDay() {
        return (int[])this.minutesOfDay.clone();
    }
}

