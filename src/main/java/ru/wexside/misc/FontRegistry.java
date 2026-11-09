package ru.wexside.misc;

import ru.wexside.misc.MsdfFontBuilder;
import ru.wexside.util.MsdfFontRenderer;

public final class FontRegistry {
    public static final MsdfFontRenderer regularText = FontRegistry.load("sf_pro_display_medium");
    public static final MsdfFontRenderer smallText = FontRegistry.load("inter_semibold");
    public static final MsdfFontRenderer brandText = FontRegistry.load("Maison_Neue_Extended_Bold");
    public static final MsdfFontRenderer icons = FontRegistry.load("icons");
    public static final MsdfFontRenderer semiboldText = FontRegistry.load("inter_semibold");
    public static final MsdfFontRenderer cosmeticIcons = FontRegistry.load("cosmetics_icons");
    public static final MsdfFontRenderer boldText = FontRegistry.load("inter_bold");
    public static final MsdfFontRenderer headingText = FontRegistry.load("inter_display_bold");
    public static final MsdfFontRenderer overlayText = regularText;
    public static final MsdfFontRenderer font2 = regularText;
    public static final MsdfFontRenderer font5 = smallText;
    public static final MsdfFontRenderer font8 = brandText;
    public static final MsdfFontRenderer font3 = icons;
    public static final MsdfFontRenderer font4 = semiboldText;
    public static final MsdfFontRenderer font10 = cosmeticIcons;
    public static final MsdfFontRenderer font6 = boldText;
    public static final MsdfFontRenderer font7 = headingText;
    public static final MsdfFontRenderer font9 = overlayText;

    private FontRegistry() {
    }

    private static MsdfFontRenderer load(String name) {
        String root = "/fonts/msdf/";
        return MsdfFontBuilder.getMsdfFontBuilder().jsonPath(root + name + ".json").atlasPath(root + "atlas/" + name + ".png").name(name).getMsdfFontRenderer();
    }
}

