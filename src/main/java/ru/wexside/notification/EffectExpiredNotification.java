package ru.wexside.notification;

import java.util.List;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import ru.wexside.misc.ThemeColors;
import ru.wexside.notification.NotificationCategory;
import ru.wexside.notification.NotificationFactory;
import ru.wexside.notification.NotificationPart;
import ru.wexside.notification.NotificationToast;

public record EffectExpiredNotification(RegistryEntry<StatusEffect> effect) implements NotificationFactory
{
    @Override
    public NotificationCategory category() {
        return NotificationCategory.EFFECT_EXPIRED;
    }

    @Override
    public NotificationToast create(long durationMillis) {
        String name = ((StatusEffect)this.effect.value()).getName().getString();
        return new NotificationToast(this.category(), List.of(this.category(), this.effect), "E", ThemeColors::hudTextPrimary, List.of(NotificationPart.text("Эффект ", ThemeColors::hudTextPrimary), NotificationPart.text(name, ThemeColors::accent), NotificationPart.text(" закончился", ThemeColors::hudTextPrimary)), durationMillis);
    }
}

