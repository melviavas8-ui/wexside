package ru.wexside.misc;

public interface MouseButtonHandler {
    public boolean onMousePressed(int var1, int var2, int var3);

    default public void onMouseReleased(int mouseX, int mouseY, int button) {
    }
}

