package ru.wexside.notification;

import ru.wexside.notification.NotificationCategory;
import ru.wexside.notification.NotificationToast;

public interface NotificationFactory {
    default public String soundId() {
        return null;
    }

    public NotificationCategory category();

    public NotificationToast create(long var1);
}

