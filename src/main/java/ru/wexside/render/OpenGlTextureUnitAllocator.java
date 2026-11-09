package ru.wexside.render;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;
import ru.wexside.misc.TextureUnitAllocator;

public class OpenGlTextureUnitAllocator
implements TextureUnitAllocator {
    private final IntBuffer samplerUnits;
    private final Int2IntMap textureUnits = new Int2IntOpenHashMap();
    private static final int MAX_TEXTURE_UNITS = 16;
    private int textureUnitLimit = -1;
    private int nextTextureUnit = 8;
    private static final int FIRST_TEXTURE_UNIT = 8;

    public OpenGlTextureUnitAllocator() {
        this.samplerUnits = MemoryUtil.memAllocInt((int)16);
        for (int i = 0; i < 16; ++i) {
            this.samplerUnits.put(i);
        }
        this.samplerUnits.flip();
    }

    @Override
    public int bindTextureUnit(int n) {
        if (this.textureUnits.containsKey(n)) {
            return this.textureUnits.get(n);
        }
        if (this.nextTextureUnit >= this.getTextureUnitLimit()) {
            return -1;
        }
        int n2 = this.nextTextureUnit++;
        this.textureUnits.put(n, n2);
        GL33.glActiveTexture((int)(33984 + n2));
        GL33.glBindTexture((int)3553, (int)n);
        return n2;
    }

    public IntBuffer getIntBuffer() {
        return this.samplerUnits;
    }

    @Override
    public void resetTextureUnits() {
        this.textureUnits.clear();
        this.nextTextureUnit = 8;
    }

    private int getTextureUnitLimit() {
        if (this.textureUnitLimit < 0) {
            int n = GL11.glGetInteger((int)34930);
            this.textureUnitLimit = Math.max(9, Math.min(16, n));
        }
        return this.textureUnitLimit;
    }
}

