package ru.wexside.render;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import ru.wexside.item.ItemBadge;
import ru.wexside.render.BakedItemIcon;

public final class NameplateLayout {
    public final float centerX;
    public final float baselineY;
    public final MutableText label;
    public final float fontSize;
    public final float labelWidth;
    public boolean friend;
    public boolean showEnchantments;
    public List<ItemStack> equipment;
    public List<BakedItemIcon> equipmentIcons;
    public ItemBadge badge;

    public NameplateLayout(float centerX, float baselineY, MutableText label, float fontSize, float labelWidth) {
        this.centerX = centerX;
        this.baselineY = baselineY;
        this.label = label;
        this.fontSize = fontSize;
        this.labelWidth = labelWidth;
    }
}

