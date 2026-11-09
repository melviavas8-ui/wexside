package ru.wexside.misc;

public interface TextFieldStyle {
    public float getFontSize();

    public int getFillColor();

    public int getBorderColor();

    public float getInputHeight();

    public float getPreferredHeight();

    default public float getOutlineSize() {
        return 1.0f;
    }

    default public float getHorizontalPadding() {
        return 4.0f;
    }
}

