package ru.wexside.misc;

import java.util.List;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.TextLayoutUtils;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.GuiDrawApi;

public final class SectionHeader
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private String string5;
    private final String string6;
    private final String string7;
    public static final float value5 = 40.0f;
    private final String string8;

    public SectionHeader(String string, String string2, String string3, String string4, float f) {
        super(new GuiBounds(0.0f, 0.0f, f, 40.0f));
        this.string5 = string;
        this.string7 = string2;
        this.string6 = string3;
        this.string8 = string4;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        int n = ThemeColors.accent();
        FontRegistry.font4.drawText(matrix4f, drawApi, this.string5, bounds2.getX(), bounds2.getY(), 5.5f, n);
        float f2 = bounds2.getX() + FontRegistry.font4.textWidth(this.string5, 5.5f) + 1.5f;
        float f3 = bounds2.getY() + FontRegistry.font4.textHeight(this.string5, 5.5f) / 2.0f;
        FontRegistry.font3.drawGlyphText(matrix4f, drawApi, this.string7, f2, f3 - FontRegistry.font3.glyphCenterY(this.string7.charAt(0), 0.0f, 6.0f), 6.0f, n);
        FontRegistry.font4.drawText(matrix4f, drawApi, this.string6, bounds2.getX(), bounds2.getY() + 8.5f, 8.0f, ThemeColors.textPrimary());
        List<String> list = TextLayoutUtils.wrapText(this.string8, FontRegistry.font2, 6.5f, bounds2.getWidth());
        float f4 = bounds2.getY() + 22.0f;
        for (String string : list) {
            FontRegistry.font2.drawText(matrix4f, drawApi, string, bounds2.getX(), f4, 6.5f, ThemeColors.textMuted());
            f4 += 9.0f;
        }
        return bounds2.getY() + 40.0f;
    }

    public void setString(String string) {
        this.string5 = string;
    }
}

