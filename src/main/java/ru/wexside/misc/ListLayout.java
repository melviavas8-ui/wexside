package ru.wexside.misc;

import org.joml.Matrix4f;

public interface ListLayout {
    public float getItemHeight(int var1);

    public int getItemCount();

    public float renderItem(int var1, Matrix4f var2, float var3, float var4, float var5);
}

