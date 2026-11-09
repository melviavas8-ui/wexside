package ru.wexside.misc;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL33;
import ru.wexside.misc.DecodedImage;
import ru.wexside.misc.ResizableTexture;

public class OpenGlTexture
implements ResizableTexture {
    private final int height;
    private final int width;
    private final int textureId;

    OpenGlTexture(int n, int n2, int n3) {
        this.textureId = n;
        this.width = n2;
        this.height = n3;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    public int getTextureId() {
        return this.textureId;
    }

    @Override
    public void close() {
        GL33.glDeleteTextures((int)this.textureId);
    }

    public static OpenGlTexture create(DecodedImage decodedImage2, int n, int n2, int n3, int n4, boolean bl, int n5) {
        int n6 = GL33.glGetInteger((int)32873);
        int n7 = GL33.glGetInteger((int)3317);
        GL33.glBindTexture((int)3553, (int)n5);
        GL33.glPixelStorei((int)3312, (int)0);
        GL33.glPixelStorei((int)3313, (int)0);
        GL33.glPixelStorei((int)3314, (int)0);
        GL33.glPixelStorei((int)32878, (int)0);
        GL33.glPixelStorei((int)3315, (int)0);
        GL33.glPixelStorei((int)3316, (int)0);
        GL33.glPixelStorei((int)32877, (int)0);
        GL33.glPixelStorei((int)3317, (int)1);
        GL33.glTexParameteri((int)3553, (int)33084, (int)0);
        GL33.glTexParameteri((int)3553, (int)33085, (int)1000);
        GL33.glTexParameteri((int)3553, (int)10242, (int)n3);
        GL33.glTexParameteri((int)3553, (int)10243, (int)n4);
        GL33.glTexParameteri((int)3553, (int)10241, (int)n2);
        GL33.glTexParameteri((int)3553, (int)10240, (int)n);
        GL33.glTexImage2D((int)3553, (int)0, (int)32856, (int)decodedImage2.getWidth(), (int)decodedImage2.getHeight(), (int)0, (int)6408, (int)5121, (ByteBuffer)decodedImage2.getByteBuffer());
        if (bl) {
            GL33.glGenerateMipmap((int)3553);
        }
        GL33.glPixelStorei((int)3317, (int)n7);
        GL33.glBindTexture((int)3553, (int)n6);
        return new OpenGlTexture(n5, decodedImage2.getWidth(), decodedImage2.getHeight());
    }

    public void bindToUnit(int n) {
        GL33.glActiveTexture((int)(33984 + n));
        GL33.glBindTexture((int)3553, (int)this.textureId);
    }
}

