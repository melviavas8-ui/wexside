package ru.wexside.misc;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;

public interface HandledScreenAccessor {
    public int getContainerX();

    public int getContainerY();

    public int getContainerWidth();

    public int getContainerHeight();

    public Slot getFocusedSlot();

    public void drawContainerBackground(DrawContext var1, float var2, int var3, int var4);
}

