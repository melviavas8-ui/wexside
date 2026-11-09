package ru.wexside.util;

import java.util.Objects;
import org.joml.Matrix4f;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.IconPlacement;
import ru.wexside.misc.ThemeColors;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class HudIconStyle {
    private final String string2;
    private final float value;
    private final float value2;
    private final float value3;
    private final String string3;

    public HudIconStyle(String string) {
        this.string2 = "h";
        this.value = 6.0f;
        this.value3 = 5.0f;
        this.value2 = 2.0f;
        this.string3 = string;
    }

    public String iconGlyph() {
        return this.string3;
    }

    public float getContentHeight() {
        return Math.max(FontRegistry.font3.textHeight("h", 6.0f), FontRegistry.font4.textHeight(this.string3, 5.0f));
    }

    public void renderRightAligned(Matrix4f matrix4f, GuiDrawApi drawApi, float f, float f2, float f3) {
        this.render(matrix4f, drawApi, f, f2, f3, IconPlacement.ICON_RIGHT);
    }

    public float getContentWidth() {
        float f = FontRegistry.font3.textWidth("h", 6.0f);
        float f2 = FontRegistry.font4.textWidth(this.string3, 5.0f);
        return f + 2.0f + f2;
    }

    public float getIconTextGap() {
        Objects.requireNonNull(this);
        return 2.0f;
    }

    public String getString() {
        Objects.requireNonNull(this);
        return "h";
    }

    public float getLabelSize() {
        Objects.requireNonNull(this);
        return 5.0f;
    }

    public void render(Matrix4f matrix4f, GuiDrawApi drawApi, float f, float f2, float f3, IconPlacement iconPlacement) {
        float f4;
        float f5;
        int n = ColorUtils.lerp(ThemeColors.textMuted(), ThemeColors.textSecondary(), f3);
        float f6 = FontRegistry.font3.textWidth("h", 6.0f);
        float f7 = FontRegistry.font3.textHeight("h", 6.0f);
        float f8 = FontRegistry.font4.textWidth(this.string3, 5.0f);
        float f9 = FontRegistry.font4.textHeight(this.string3, 5.0f);
        if (iconPlacement == IconPlacement.ICON_LEFT) {
            f5 = f;
            f4 = f5 + f6 + 2.0f;
        } else {
            f4 = f - f8;
            f5 = f4 - 2.0f - f6;
        }
        float f10 = f2 + (f9 - f7) / 2.0f;
        FontRegistry.font3.drawGlyphText(matrix4f, drawApi, "h", f5, f10, 6.0f, n);
        FontRegistry.font4.drawText(matrix4f, drawApi, this.string3, f4, f2, 5.0f, n);
    }

    public float getIconSize() {
        Objects.requireNonNull(this);
        return 6.0f;
    }
}

