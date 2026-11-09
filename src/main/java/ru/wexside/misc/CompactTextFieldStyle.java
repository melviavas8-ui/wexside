package ru.wexside.misc;

import ru.wexside.misc.TextFieldStyle;
import ru.wexside.misc.ThemeColors;

public final class CompactTextFieldStyle
implements TextFieldStyle {
    @Override
    public float getFontSize() {
        return 5.0f;
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
        return 12.0f;
    }

    @Override
    public float getPreferredHeight() {
        return 12.0f;
    }
}

