package ru.wexside.misc;

import ru.wexside.misc.TextFieldStyle;
import ru.wexside.misc.ThemeColors;

public final class ExpandedTextFieldStyle
implements TextFieldStyle {
    @Override
    public float getFontSize() {
        return 6.0f;
    }

    @Override
    public int getFillColor() {
        return ThemeColors.controlFill();
    }

    @Override
    public int getBorderColor() {
        return ThemeColors.borderPrimary();
    }

    @Override
    public float getInputHeight() {
        return 14.0f;
    }

    @Override
    public float getPreferredHeight() {
        return 14.0f;
    }

    @Override
    public float getOutlineSize() {
        return 8.0f;
    }

    @Override
    public float getHorizontalPadding() {
        return 57.5f;
    }
}

