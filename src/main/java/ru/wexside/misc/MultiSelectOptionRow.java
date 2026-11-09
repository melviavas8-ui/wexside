package ru.wexside.misc;

import org.joml.Matrix4f;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.CompactOptionRow;
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

public class MultiSelectOptionRow
extends CompactOptionRow
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
MouseHitTest {
    public MultiSelectOptionRow(String string, String string2, float f) {
        super(string, string2, f);
    }

    public MultiSelectOptionRow(String string, String string2) {
        super(string, string2);
    }

    @Override
    protected float getSelectionIconFontSize() {
        return 5.5f;
    }

    @Override
    protected void renderSelectionIndicator(Matrix4f matrix4f, GuiDrawApi drawApi, GuiBounds bounds2) {
        float f = this.getSelectionProgress();
        float f2 = 7.5f;
        float f3 = bounds2.getX() + bounds2.getWidth() - 12.0f;
        float f4 = bounds2.getY() + (bounds2.getHeight() - f2) / 2.0f;
        int n = ColorUtils.lerp(ThemeColors.borderStrong(), ThemeColors.accent(), f);
        drawApi.drawRoundedRectangle(matrix4f, f3, f4, f2, f2, 4.0f, n);
        float f5 = this.getSelectionIconFontSize();
        int n2 = ColorUtils.withAlpha(ThemeColors.backgroundControl(), 255.0f * f);
        float f6 = f3 + (f2 - FontRegistry.font3.textWidth("T", f5)) / 2.0f;
        float f7 = f4 + (f2 - FontRegistry.font3.textHeight("T", f5)) / 2.0f;
        FontRegistry.font3.drawText(matrix4f, drawApi, "T", f6, f7, f5, n2);
    }
}

