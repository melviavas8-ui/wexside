package ru.wexside.misc;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.util.ColorUtils;

public final class ConsumablesEspSettings {
    private final BooleanSetting booleanSetting;
    public static final String string2 = "Пласт";
    private final BooleanSetting booleanSetting2;
    public static final String string3 = "Явная пыль";
    public static final String string4 = "Дезориентация";
    private final BooleanSetting booleanSetting3;
    public static final String TRAP_ITEM = "Трапка";
    private final MultiSelectSetting multiSelectSetting;
    public static final String[] string5 = new String[]{"Трапка", "Пласт", "Дезориентация", "Явная пыль", "Снежок заморозка"};
    private final ColorSetting colorSetting;
    public static final String string6 = "Снежок заморозка";
    private final MultiSelectSetting multiSelectSetting2;
    private final ColorSetting colorSetting2;

    public ConsumablesEspSettings(BooleanSetting booleanSetting, BooleanSetting toggle, MultiSelectSetting multiSelectSetting, ColorSetting colorSetting, BooleanSetting toggle2, MultiSelectSetting multi, ColorSetting colorSetting3) {
        this.booleanSetting3 = booleanSetting;
        this.booleanSetting2 = toggle;
        this.multiSelectSetting2 = multiSelectSetting;
        this.colorSetting = colorSetting;
        this.booleanSetting = toggle2;
        this.multiSelectSetting = multi;
        this.colorSetting2 = colorSetting3;
    }

    public boolean isActive() {
        return this.booleanSetting3.isEnabled();
    }

    private static String getItemOptionLabel(Item iiIilIIilI2) {
        if (iiIilIIilI2 == Items.NETHERITE_SCRAP) {
            return TRAP_ITEM;
        }
        if (iiIilIIilI2 == Items.PHANTOM_MEMBRANE) {
            return string2;
        }
        if (iiIilIIilI2 == Items.FIREWORK_STAR) {
            return string4;
        }
        if (iiIilIIilI2 == Items.SUGAR) {
            return string3;
        }
        if (iiIilIIilI2 == Items.SNOWBALL) {
            return string6;
        }
        return null;
    }

    public ColorSetting getLocalPlayerColor(Item iiIilIIilI2) {
        return this.colorSetting;
    }

    public boolean isLocalPlayerItemEnabled(Item iiIilIIilI2) {
        return ConsumablesEspSettings.isItemSelected(this.multiSelectSetting2, iiIilIIilI2);
    }

    private static boolean isItemSelected(MultiSelectSetting multiSelectSetting, Item iiIilIIilI2) {
        String string = ConsumablesEspSettings.getItemOptionLabel(iiIilIIilI2);
        return string != null && multiSelectSetting.getSelectedOptions().contains(string);
    }

    public boolean isActive2() {
        return this.booleanSetting2.isEnabled();
    }

    public static int sampleEffectColor(ColorSetting colorSetting, float f) {
        if (colorSetting.isAstolfoMode()) {
            return colorSetting.getEditingColor(f);
        }
        int n = colorSetting.getPrimaryColor();
        int n2 = colorSetting.isDoubleColorMode() ? colorSetting.getSecondaryColor() : n;
        return ColorUtils.lerp(n, n2, f);
    }

    public boolean isOtherPlayerItemEnabled(Item iiIilIIilI2) {
        return ConsumablesEspSettings.isItemSelected(this.multiSelectSetting, iiIilIIilI2);
    }

    public boolean isActive3() {
        return this.booleanSetting.isEnabled();
    }

    public ColorSetting getOtherPlayerColor(Item iiIilIIilI2) {
        return this.colorSetting2;
    }
}

