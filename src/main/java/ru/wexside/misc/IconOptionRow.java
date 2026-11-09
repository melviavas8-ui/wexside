package ru.wexside.misc;

import org.joml.Matrix4f;
import ru.wexside.misc.AbstractOptionRow;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseHitTest;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public class IconOptionRow
extends AbstractOptionRow
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
MouseHitTest {
    public IconOptionRow(String string, String string2) {
        super(string, string2);
    }

    public IconOptionRow(String string, String string2, float f) {
        super(string, string2, f);
    }

    @Override
    protected void renderSelectionIndicator(Matrix4f matrix4f, GuiDrawApi drawApi, GuiBounds bounds2) {
        float f = this.getSelectionProgress();
        if (f <= 0.01f) {
            return;
        }
        float f2 = this.getSelectionIconFontSize();
        int n = ColorUtils.withAlpha(ThemeColors.accent(), 255.0f * f);
        float f3 = bounds2.getX() + bounds2.getWidth() - 10.5f + (1.0f - f) * 2.0f;
        float f4 = bounds2.getY() + (bounds2.getHeight() - FontRegistry.font3.textHeight("T", f2)) / 2.0f;
        FontRegistry.font3.drawText(matrix4f, drawApi, "T", f3, f4, f2, n);
    }
}

