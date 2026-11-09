package ru.wexside.misc;

public interface NativeMemoryAllocator {
    public void free(long var1);

    public long reallocateMemory(long var1, long var3, long var5);

    public long allocate(long var1);
}

