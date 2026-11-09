package ru.wexside.util;

import java.nio.ByteBuffer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import ru.wexside.misc.UnsafeMemoryAccess;
import ru.wexside.render.GuiDrawMode;
import ru.wexside.util.NativeBuffer;
import ru.wexside.util.UnsafeAccess;
import sun.misc.Unsafe;

public final class GuiVertexBuffer {
    static final int VERTEX_STRIDE_BYTES = 86;
    private final Vector4f vector4f = new Vector4f();
    private int vertexCount = 0;
    private final NativeBuffer nativeBuffer2;
    private int indexCount = 0;
    private final NativeBuffer nativeBuffer;

    public GuiVertexBuffer() {
        Unsafe unsafe = UnsafeAccess.get();
        UnsafeMemoryAccess unsafeMemoryAccess = new UnsafeMemoryAccess(unsafe);
        this.nativeBuffer = new NativeBuffer(unsafeMemoryAccess, 65536L);
        this.nativeBuffer2 = new NativeBuffer(unsafeMemoryAccess, 16384L);
    }

    public void clear() {
        this.vertexCount = 0;
        this.indexCount = 0;
        this.nativeBuffer.reset();
        this.nativeBuffer2.reset();
    }

    public void appendQuad(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, float f13, float f14, float f15, float f16, float f17, float f18, float f19, int n, int n2, int n3, int n4, int n5, int n6, GuiDrawMode guiDrawMode) {
        Vector4f vector4f = matrix4f.transform(this.vector4f.set(f, f2, 0.0f, 1.0f));
        float f20 = vector4f.x;
        float f21 = vector4f.y;
        Vector4f vector4f2 = matrix4f.transform(this.vector4f.set(f + f3, f2 + f4, 0.0f, 1.0f));
        float f22 = vector4f2.x - f20;
        float f23 = vector4f2.y - f21;
        float f24 = Math.max(f14, 0.0f);
        float f25 = f24 * 0.5f;
        float f26 = this.computeAntialiasPadding(guiDrawMode, f3, f22, f24);
        float f27 = this.computeAntialiasPadding(guiDrawMode, f4, f23, f24);
        float f28 = f25 + f26;
        float f29 = f25 + f27;
        float f30 = f - f28;
        float f31 = f2 - f29;
        float f32 = f3 + f28 * 2.0f;
        float f33 = f4 + f29 * 2.0f;
        this.appendVertex(matrix4f, f30, f31 + f33, f20, f21, f22, f23, f5, f8, f9, f10, f11, f12, n2, n, f13, f14, f15, f16, f17, f18, f19, n6, guiDrawMode);
        this.appendVertex(matrix4f, f30 + f32, f31 + f33, f20, f21, f22, f23, f7, f8, f9, f10, f11, f12, n3, n, f13, f14, f15, f16, f17, f18, f19, n6, guiDrawMode);
        this.appendVertex(matrix4f, f30 + f32, f31, f20, f21, f22, f23, f7, f6, f9, f10, f11, f12, n4, n, f13, f14, f15, f16, f17, f18, f19, n6, guiDrawMode);
        this.appendVertex(matrix4f, f30, f31, f20, f21, f22, f23, f5, f6, f9, f10, f11, f12, n5, n, f13, f14, f15, f16, f17, f18, f19, n6, guiDrawMode);
        this.appendTriangleIndices(this.vertexCount, this.vertexCount + 1, this.vertexCount + 2);
        this.appendTriangleIndices(this.vertexCount, this.vertexCount + 2, this.vertexCount + 3);
        this.vertexCount += 4;
        this.indexCount += 6;
    }

    public void appendTriangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int n, int n2, int n3, int n4) {
        float f5;
        float f6;
        float f7;
        float f8;
        float f9;
        Vector4f vector4f = matrix4f.transform(this.vector4f.set(f, f2, 0.0f, 1.0f));
        float f10 = vector4f.x;
        float f11 = vector4f.y;
        Vector4f vector4f2 = matrix4f.transform(this.vector4f.set(f + f3, f2 + f4, 0.0f, 1.0f));
        float f12 = vector4f2.x - f10;
        float f13 = vector4f2.y - f11;
        float f14 = f;
        float f15 = f2;
        float f16 = f + f3;
        float f17 = f2;
        float f18 = f + f3;
        float f19 = f2 + f4;
        float f20 = f;
        float f21 = f2 + f4;
        float f22 = switch (n4 & 3) {
            default -> {
                f9 = f14;
                f8 = f15;
                f7 = f16;
                f6 = f17;
                f5 = f18;
                yield f19;
            }
            case 1 -> {
                f9 = f14;
                f8 = f15;
                f7 = f18;
                f6 = f19;
                f5 = f20;
                yield f21;
            }
            case 2 -> {
                f9 = f16;
                f8 = f17;
                f7 = f18;
                f6 = f19;
                f5 = f20;
                yield f21;
            }
            case 3 -> {
                f9 = f14;
                f8 = f15;
                f7 = f16;
                f6 = f17;
                f5 = f20;
                yield f21;
            }
        };
        this.appendVertex(matrix4f, f9, f8, f10, f11, f12, f13, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, n, 0, 0.0f, 0.75f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, GuiDrawMode.COLOR);
        this.appendVertex(matrix4f, f7, f6, f10, f11, f12, f13, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, n2, 0, 0.0f, 0.75f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, GuiDrawMode.COLOR);
        this.appendVertex(matrix4f, f5, f22, f10, f11, f12, f13, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, n3, 0, 0.0f, 0.75f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, GuiDrawMode.COLOR);
        this.appendTriangleIndices(this.vertexCount, this.vertexCount + 1, this.vertexCount + 2);
        this.vertexCount += 3;
        this.indexCount += 3;
    }

    public int getIndexCount() {
        return this.indexCount;
    }

    public ByteBuffer getByteBuffer() {
        return this.nativeBuffer.getByteBuffer2();
    }

    public ByteBuffer getByteBuffer2() {
        return this.nativeBuffer2.getByteBuffer2();
    }

    private void appendTriangleIndices(int n, int n2, int n3) {
        this.nativeBuffer2.ensureRemainingCapacity(12L);
        long l = this.nativeBuffer2.currentAddress();
        this.nativeBuffer2.putInt(l, n);
        this.nativeBuffer2.putInt(l + 4L, n2);
        this.nativeBuffer2.putInt(l + 8L, n3);
        this.nativeBuffer2.advancePosition(12L);
    }

    private void writeRgbaBytes(long l, int n) {
        this.nativeBuffer.putByte(l, n >>> 16 & 0xFF);
        this.nativeBuffer.putByte(l + 1L, n >>> 8 & 0xFF);
        this.nativeBuffer.putByte(l + 2L, n & 0xFF);
        this.nativeBuffer.putByte(l + 3L, n >>> 24 & 0xFF);
    }

    public ByteBuffer getByteBuffer3() {
        return this.nativeBuffer2.getByteBuffer();
    }

    private void appendVertex(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, int n, int n2, float f13, float f14, float f15, float f16, float f17, float f18, float f19, int n3, GuiDrawMode guiDrawMode) {
        Vector4f vector4f = matrix4f.transform(this.vector4f.set(f, f2, 0.0f, 1.0f));
        this.nativeBuffer.ensureRemainingCapacity(86L);
        long l = this.nativeBuffer.currentAddress();
        this.nativeBuffer.putFloat(l, vector4f.x);
        this.nativeBuffer.putFloat(l + 4L, vector4f.y);
        this.nativeBuffer.putFloat(l + 8L, f3);
        this.nativeBuffer.putFloat(l + 12L, f4);
        this.nativeBuffer.putFloat(l + 16L, f5);
        this.nativeBuffer.putFloat(l + 20L, f6);
        this.nativeBuffer.putFloat(l + 24L, f7);
        this.nativeBuffer.putFloat(l + 28L, f8);
        this.nativeBuffer.putFloat(l + 32L, f9);
        this.nativeBuffer.putFloat(l + 36L, f10);
        this.nativeBuffer.putFloat(l + 40L, f11);
        this.nativeBuffer.putFloat(l + 44L, f12);
        this.writeRgbaBytes(l + 48L, n);
        this.writeRgbaBytes(l + 52L, n2);
        this.nativeBuffer.putFloat(l + 56L, f13);
        this.nativeBuffer.putFloat(l + 60L, f14);
        this.nativeBuffer.putFloat(l + 64L, f15);
        this.nativeBuffer.putFloat(l + 68L, f16);
        this.nativeBuffer.putFloat(l + 72L, f17);
        this.nativeBuffer.putFloat(l + 76L, f18);
        this.nativeBuffer.putFloat(l + 80L, f19);
        this.nativeBuffer.putByte(l + 84L, n3);
        this.nativeBuffer.putByte(l + 85L, guiDrawMode.shaderId());
        this.nativeBuffer.advancePosition(86L);
    }

    public ByteBuffer getByteBuffer4() {
        return this.nativeBuffer.getByteBuffer();
    }

    private float computeAntialiasPadding(GuiDrawMode guiDrawMode, float f, float f2, float f3) {
        if (!this.supportsAntialiasPadding(guiDrawMode)) {
            return 0.0f;
        }
        float f4 = Math.abs(f) > 1.0E-4f ? Math.abs(f2 / f) : 1.0f;
        float f5 = 1.0f / Math.max(f4, 1.0f);
        if (guiDrawMode == GuiDrawMode.COLOR) {
            float f6 = Math.abs(f2);
            if (f3 <= 0.0f && f6 >= 1.0f) {
                return 0.0f;
            }
            return f5;
        }
        return f5;
    }

    private boolean supportsAntialiasPadding(GuiDrawMode guiDrawMode) {
        return guiDrawMode == GuiDrawMode.COLOR || guiDrawMode == GuiDrawMode.ROUNDED_RECTANGLE || guiDrawMode == GuiDrawMode.ROUNDED_TEXTURE || guiDrawMode == GuiDrawMode.BLURRED_ROUNDED_RECTANGLE || guiDrawMode == GuiDrawMode.GRADIENT_ROUNDED_RECTANGLE || guiDrawMode == GuiDrawMode.CIRCLE || guiDrawMode == GuiDrawMode.RING_SECTOR || guiDrawMode == GuiDrawMode.SHADOW || guiDrawMode == GuiDrawMode.ROUNDED_SHADOW || guiDrawMode == GuiDrawMode.SHIMMER_HIGHLIGHT;
    }

    public int getVertexCount() {
        return this.vertexCount;
    }
}

