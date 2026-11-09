package ru.wexside.notification;

import java.util.function.IntSupplier;

public record NotificationPart(String text, IntSupplier color) {
    public NotificationPart {
        text = text == null ? "" : text;
    }

    public static NotificationPart text(String text, IntSupplier color) {
        return new NotificationPart(text, color);
    }
}

