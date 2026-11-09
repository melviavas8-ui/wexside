package ru.wexside.misc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public interface ResourceData {
    public String getPath();

    default public String readUtf8() {
        return new String(this.readBytes(), StandardCharsets.UTF_8);
    }

    default public void writeTo(ByteBuffer buffer) {
        buffer.put(this.readBytes());
    }

    default public byte[] readBytes() {
        try (InputStream stream = this.openStream();){
            if (stream == null) {
                throw new IllegalStateException("Resource stream is null: " + this.getPath());
            }
            return stream.readAllBytes();
        }
        catch (IOException exception) {
            throw new IllegalStateException("Failed to read resource: " + this.getPath(), exception);
        }
    }

    public InputStream openStream();

    default public ByteBuffer toDirectByteBuffer() {
        byte[] bytes = this.readBytes();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes);
        return buffer.flip();
    }
}

