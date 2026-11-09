package ru.wexside.render;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

public final class IconAtlas {
    private int textureSize;
    private final Deque<Integer> freeSlots = new ArrayDeque<Integer>();
    private int cellSize;
    private int framebufferId;
    private int columns;
    private int textureId;
    private int nextSlotIndex;
    private int iconSize;
    private static final int CELL_PADDING = 2;
    private final boolean nearestFiltering;
    private int generation;
    private final int capacity;

    public IconAtlas(int n, boolean bl) {
        this.capacity = Math.max(1, n);
        this.nearestFiltering = bl;
    }

    public int allocateSlot() {
        Integer n = this.freeSlots.poll();
        if (n != null) {
            return n;
        }
        if (this.nextSlotIndex >= this.capacity) {
            return -1;
        }
        return this.nextSlotIndex++;
    }

    public void releaseSlot(int n) {
        if (n >= 0 && n < this.nextSlotIndex) {
            this.freeSlots.push(n);
        }
    }

    public int getSlotX(int n) {
        return n % this.columns * this.cellSize;
    }

    public int getTextureSize() {
        return this.textureSize;
    }

    public int getGeneration() {
        return this.generation;
    }

    public void ensureIconSize(int n) {
        if (n <= 0 || n == this.iconSize && this.textureId != 0) {
            return;
        }
        this.close();
        this.iconSize = n;
        this.cellSize = n + 2;
        this.columns = (int)Math.ceil(Math.sqrt(this.capacity));
        this.textureSize = this.columns * this.cellSize;
        this.createTextureAndFramebuffer();
        ++this.generation;
    }

    public int getSlotY(int n) {
        return n / this.columns * this.cellSize;
    }

    public int getFramebufferId() {
        return this.framebufferId;
    }

    public void close() {
        if (this.framebufferId != 0) {
            GL30.glDeleteFramebuffers((int)this.framebufferId);
            this.framebufferId = 0;
        }
        if (this.textureId != 0) {
            GL30.glDeleteTextures((int)this.textureId);
            this.textureId = 0;
        }
        this.iconSize = 0;
        this.cellSize = 0;
        this.columns = 0;
        this.textureSize = 0;
        this.nextSlotIndex = 0;
        this.freeSlots.clear();
    }

    private void createTextureAndFramebuffer() {
        int n = GL30.glGetInteger((int)32873);
        int n2 = GL30.glGetInteger((int)36006);
        float[] fArray = new float[4];
        GL30.glGetFloatv((int)3106, (float[])fArray);
        int n3 = this.nearestFiltering ? 9728 : 9729;
        this.textureId = GL30.glGenTextures();
        GL30.glBindTexture((int)3553, (int)this.textureId);
        GL30.glTexParameteri((int)3553, (int)10241, (int)n3);
        GL30.glTexParameteri((int)3553, (int)10240, (int)n3);
        GL30.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL30.glTexParameteri((int)3553, (int)10243, (int)33071);
        int n4 = GL30.glGetInteger((int)35055);
        GL30.glBindBuffer((int)35052, (int)0);
        GL30.glTexImage2D((int)3553, (int)0, (int)32856, (int)this.textureSize, (int)this.textureSize, (int)0, (int)6408, (int)5121, (ByteBuffer)null);
        GL30.glBindBuffer((int)35052, (int)n4);
        this.framebufferId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer((int)36160, (int)this.framebufferId);
        GL30.glFramebufferTexture2D((int)36160, (int)36064, (int)3553, (int)this.textureId, (int)0);
        int n5 = GL30.glCheckFramebufferStatus((int)36160);
        if (n5 != 36053) {
            int n6 = n5;
            throw new IllegalStateException("Icon atlas framebuffer is incomplete: " + n6);
        }
        boolean bl = GL30.glIsEnabled((int)3089);
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer((int)4);
        GL30.glGetBooleanv((int)3107, (ByteBuffer)byteBuffer);
        GL30.glDisable((int)3089);
        GL30.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GL30.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        GL30.glClear((int)16384);
        GL30.glColorMask((byteBuffer.get(0) != 0 ? 1 : 0) != 0, (byteBuffer.get(1) != 0 ? 1 : 0) != 0, (byteBuffer.get(2) != 0 ? 1 : 0) != 0, (byteBuffer.get(3) != 0 ? 1 : 0) != 0);
        if (bl) {
            GL30.glEnable((int)3089);
        }
        GL30.glClearColor((float)fArray[0], (float)fArray[1], (float)fArray[2], (float)fArray[3]);
        GL30.glBindFramebuffer((int)36160, (int)n2);
        GL30.glBindTexture((int)3553, (int)n);
    }

    public int getTextureId() {
        return this.textureId;
    }
}

