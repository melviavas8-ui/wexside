package ru.wexside.item;

import java.util.Locale;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import ru.wexside.item.ItemBadgeCategory;

public record ItemBadge(Text label, ItemBadgeCategory category) {
    public static ItemBadge fromStack(ItemStack stack) {
        ItemBadgeCategory category;
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Text label = stack.getName();
        String normalizedName = label.getString().toLowerCase(Locale.ROOT);
        if (ItemBadge.containsAny(normalizedName, "талисман", "talisman", "амулет", "amulet")) {
            category = ItemBadgeCategory.TALISMAN;
        } else if (ItemBadge.containsAny(normalizedName, "сфера", "sphere", "шар", "orb")) {
            category = ItemBadgeCategory.SPHERE;
        } else {
            return null;
        }
        return new ItemBadge((Text)label.copy(), category);
    }

    private static boolean containsAny(String value, String ... markers) {
        for (String marker : markers) {
            if (!value.contains(marker)) continue;
            return true;
        }
        return false;
    }
}

