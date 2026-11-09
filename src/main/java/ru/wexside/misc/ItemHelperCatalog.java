package ru.wexside.misc;

import java.util.List;
import net.minecraft.item.Items;
import ru.wexside.misc.ItemHelperEntry;

public final class ItemHelperCatalog {
    public static final ItemHelperEntry CHORUS = new ItemHelperEntry("chorus", "Плод хоруса", Items.CHORUS_FRUIT, false);
    public static final ItemHelperEntry GOLDEN_APPLE = new ItemHelperEntry("gapple", "Золотое яблоко", Items.GOLDEN_APPLE, false);
    public static final ItemHelperEntry ENCHANTED_GOLDEN_APPLE = new ItemHelperEntry("enchanted_gapple", "Зачарованное золотое яблоко", Items.ENCHANTED_GOLDEN_APPLE, false);
    public static final ItemHelperEntry INSTANT_HEALING = new ItemHelperEntry("instant_healing", "Зелье исцеления", Items.POTION, true);
    public static final ItemHelperEntry SHIELD = new ItemHelperEntry("shield", "Щит", Items.SHIELD, false);
    public static final ItemHelperEntry CROSSBOW = new ItemHelperEntry("crossbow", "Арбалет", Items.CROSSBOW, false);
    public static final ItemHelperEntry MILK = new ItemHelperEntry("milk", "Молоко", Items.MILK_BUCKET, false);
    public static final List<ItemHelperEntry> list = List.of(CHORUS, GOLDEN_APPLE, ENCHANTED_GOLDEN_APPLE, INSTANT_HEALING, SHIELD, CROSSBOW, MILK);
    public static final ItemHelperEntry itemHelperEntry2 = CHORUS;
    public static final ItemHelperEntry itemHelperEntry = GOLDEN_APPLE;
    public static final ItemHelperEntry itemHelperEntry3 = ENCHANTED_GOLDEN_APPLE;
    public static final ItemHelperEntry itemHelperEntry4 = INSTANT_HEALING;
    public static final ItemHelperEntry itemHelperEntry5 = SHIELD;
    public static final ItemHelperEntry itemHelperEntry6 = CROSSBOW;
    public static final ItemHelperEntry itemHelperEntry7 = MILK;

    private ItemHelperCatalog() {
    }
}

