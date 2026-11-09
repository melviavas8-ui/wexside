package ru.wexside.notification;

import java.util.List;
import net.minecraft.item.ItemStack;
import ru.wexside.notification.NotificationCategory;
import ru.wexside.notification.NotificationFactory;
import ru.wexside.notification.NotificationPart;
import ru.wexside.notification.NotificationToast;

public final class ItemNotification
implements NotificationFactory {
    private final ItemStack stack;

    public ItemNotification(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public ItemStack stack() {
        return this.stack;
    }

    @Override
    public NotificationCategory category() {
        return NotificationCategory.ITEM;
    }

    @Override
    public NotificationToast create(long durationMillis) {
        return new NotificationToast(this.category(), this.stack, "i", () -> -1, List.of(NotificationPart.text(this.stack.getName().getString(), () -> -1)), durationMillis);
    }
}

