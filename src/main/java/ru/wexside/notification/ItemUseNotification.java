package ru.wexside.notification;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.text.TextColor;
import ru.wexside.misc.ThemeColors;
import ru.wexside.notification.NotificationCategory;
import ru.wexside.notification.NotificationFactory;
import ru.wexside.notification.NotificationPart;
import ru.wexside.notification.NotificationToast;

public final class ItemUseNotification
implements NotificationFactory {
    private final String playerName;
    private final boolean self;
    private final ItemStack stack;
    private final UseAction action;

    public ItemUseNotification(String playerName, boolean self, ItemStack stack, UseAction action) {
        this.playerName = playerName == null ? "" : playerName;
        this.self = self;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.action = action == null ? UseAction.NONE : action;
    }

    @Override
    public NotificationCategory category() {
        return this.self ? NotificationCategory.SELF_ITEM_USE : NotificationCategory.OTHER_ITEM_USE;
    }

    @Override
    public NotificationToast create(long durationMillis) {
        ArrayList<NotificationPart> parts = new ArrayList<NotificationPart>();
        parts.add(NotificationPart.text((this.self ? "Вы использовали " : this.playerName + " использовал "), ThemeColors::hudTextPrimary));
        parts.add(NotificationPart.text(this.stack.getName().getString(), this::itemNameColor));
        StatusEffectInstance effect = this.firstPotionEffect();
        if (effect != null) {
            parts.add(NotificationPart.text(", получив эффект " + ((StatusEffect)effect.getEffectType().value()).getName().getString(), ThemeColors::hudTextPrimary));
        }
        List<Object> key = this.self ? null : List.of(this.category(), this.playerName);
        return new NotificationToast(this.category(), key, "+", () -> -1, parts, durationMillis);
    }

    private StatusEffectInstance firstPotionEffect() {
        if (this.action != UseAction.EAT && this.action != UseAction.DRINK) {
            return null;
        }
        PotionContentsComponent potion = (PotionContentsComponent)this.stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potion == null) {
            return null;
        }
        return potion.getEffects().iterator().hasNext() ? (StatusEffectInstance)potion.getEffects().iterator().next() : null;
    }

    private int itemNameColor() {
        TextColor color = this.stack.getName().getStyle().getColor();
        return color == null ? ThemeColors.accent() : 0xFF000000 | color.getRgb();
    }
}

