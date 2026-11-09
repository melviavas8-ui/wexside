package ru.wexside.misc;

import net.minecraft.client.gl.Framebuffer;

public final class ActiveFramebufferContext {
    private static Framebuffer framebuffer2;

    private ActiveFramebufferContext() {
    }

    public static Framebuffer getFramebuffer() {
        return framebuffer2;
    }

    public static void update() {
        framebuffer2 = null;
    }

    public static void setFramebuffer(Framebuffer iIllIIiilI2) {
        framebuffer2 = iIllIIiilI2;
    }
}

