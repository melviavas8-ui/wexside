package ru.wexside.misc;

public interface NativeMemoryAccess {
    public void putLong(long var1, long var3);

    public int getUnsignedShort(long var1);

    public void putFloat(long var1, float var3);

    public long getLong(long var1);

    public void putInt(long var1, int var3);

    public int getInt(long var1);

    public void copyMemory(long var1, long var3, long var5);

    public void putByte(long var1, int var3);

    public void putShort(long var1, int var3);

    public int getUnsignedByte(long var1);
}

