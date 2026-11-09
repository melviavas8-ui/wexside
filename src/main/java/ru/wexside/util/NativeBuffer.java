package ru.wexside.util;

import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;
import ru.wexside.misc.NativeMemoryAccess;
import ru.wexside.misc.UnsafeNativeMemoryAccess;

public class NativeBuffer
implements AutoCloseable,
NativeMemoryAccess {
    private long positionBytes;
    private long address;
    private final UnsafeNativeMemoryAccess memoryAccess;
    private long capacityBytes;

    public NativeBuffer(UnsafeNativeMemoryAccess callback51Impl, long l) {
        this.memoryAccess = callback51Impl;
        this.address = callback51Impl.allocate(l);
        this.capacityBytes = l;
    }

    public void reset() {
        this.positionBytes = 0L;
    }

    @Override
    public void close() {
        this.memoryAccess.free(this.address);
    }

    @Override
    public void putLong(long l, long l2) {
        this.memoryAccess.putLong(l, l2);
    }

    @Override
    public int getUnsignedShort(long l) {
        return this.memoryAccess.getUnsignedShort(l);
    }

    @Override
    public void putFloat(long l, float f) {
        this.memoryAccess.putFloat(l, f);
    }

    public void advancePosition(long l) {
        this.positionBytes += l;
    }

    @Override
    public long getLong(long l) {
        return this.memoryAccess.getLong(l);
    }

    @Override
    public void putInt(long l, int n) {
        this.memoryAccess.putInt(l, n);
    }

    @Override
    public int getInt(long l) {
        return this.memoryAccess.getInt(l);
    }

    @Override
    public void copyMemory(long l, long l2, long l3) {
        this.memoryAccess.copyMemory(l, l2, l3);
    }

    @Override
    public void putByte(long l, int n) {
        this.memoryAccess.putByte(l, n);
    }

    public void ensureRemainingCapacity(long l) {
        long l2 = this.positionBytes + l;
        if (l2 > this.capacityBytes) {
            long l3 = Math.max(this.capacityBytes * 2L, l2);
            this.address = this.memoryAccess.reallocateMemory(this.address, this.capacityBytes, l3);
            this.capacityBytes = l3;
        }
    }

    public ByteBuffer getByteBuffer() {
        return this.asByteBuffer((int)this.positionBytes);
    }

    public long currentAddress() {
        return this.address + this.positionBytes;
    }

    @Override
    public void putShort(long l, int n) {
        this.memoryAccess.putShort(l, n);
    }

    public long writtenBytes() {
        return this.positionBytes;
    }

    public ByteBuffer getByteBuffer2() {
        return MemoryUtil.memByteBuffer((long)this.address, (int)((int)this.positionBytes));
    }

    public ByteBuffer asByteBuffer(int n) {
        return MemoryUtil.memByteBuffer((long)this.address, (int)n);
    }

    @Override
    public int getUnsignedByte(long l) {
        return this.memoryAccess.getUnsignedByte(l);
    }
}

