package ru.wexside.misc;

import ru.wexside.misc.NativeMemoryAccess;
import ru.wexside.misc.NativeMemoryAllocator;
import ru.wexside.misc.UnsafeNativeMemoryAccess;
import sun.misc.Unsafe;

public class UnsafeMemoryAccess
implements NativeMemoryAccess,
NativeMemoryAllocator,
UnsafeNativeMemoryAccess {
    private final Unsafe unsafe;

    public UnsafeMemoryAccess(Unsafe unsafe) {
        this.unsafe = unsafe;
    }

    @Override
    public void free(long l) {
        this.unsafe.freeMemory(l);
    }

    @Override
    public void putLong(long l, long l2) {
        this.unsafe.putLong(l, l2);
    }

    @Override
    public int getUnsignedShort(long l) {
        return this.unsafe.getShort(l) & 0xFFFF;
    }

    @Override
    public void putFloat(long l, float f) {
        this.unsafe.putFloat(l, f);
    }

    @Override
    public long reallocateMemory(long l, long l2, long l3) {
        return this.unsafe.reallocateMemory(l, l3);
    }

    @Override
    public long getLong(long l) {
        return this.unsafe.getLong(l);
    }

    @Override
    public void putInt(long l, int n) {
        this.unsafe.putInt(l, n);
    }

    @Override
    public int getInt(long l) {
        return this.unsafe.getInt(l);
    }

    @Override
    public long allocate(long l) {
        return this.unsafe.allocateMemory(l);
    }

    @Override
    public void copyMemory(long l, long l2, long l3) {
        this.unsafe.copyMemory(l, l2, l3);
    }

    @Override
    public void putByte(long l, int n) {
        this.unsafe.putByte(l, (byte)n);
    }

    @Override
    public void putShort(long l, int n) {
        this.unsafe.putShort(l, (short)n);
    }

    @Override
    public int getUnsignedByte(long l) {
        return this.unsafe.getByte(l) & 0xFF;
    }
}

