package ru.wexside.notification;

import ru.wexside.notification.NotificationCategory;

public interface NotificationPreferences {
    public boolean isCategoryVisible(NotificationCategory var1);

    public boolean isSoundEnabled(NotificationCategory var1);

    public float soundVolume();

    public boolean isEnabled();
}

