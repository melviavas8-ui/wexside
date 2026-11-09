package ru.wexside.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

final class OpenGlStateSnapshot {
    private static final int TRACKED_TEXTURE_UNITS = 4;
    private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    private final int renderbuffer = GL11.glGetInteger((int)36007);
    private final int vertexArray = GL11.glGetInteger((int)34229);
    private final int arrayBuffer = GL11.glGetInteger((int)34964);
    private final int program = GL11.glGetInteger((int)35725);
    private final int activeTexture = GL11.glGetInteger((int)34016);
    private final int elementArrayBuffer = GL11.glGetInteger((int)34965);
    private final int blendSourceRgb = GL11.glGetInteger((int)32969);
    private final int blendDestinationRgb = GL11.glGetInteger((int)32968);
    private final int blendSourceAlpha = GL11.glGetInteger((int)32971);
    private final int blendDestinationAlpha = GL11.glGetInteger((int)32970);
    private final int blendEquationRgb = GL11.glGetInteger((int)32777);
    private final int blendEquationAlpha = GL11.glGetInteger((int)34877);
    private final int[] textureBindings = new int[4];
    private final int[] samplerBindings = new int[4];
    private final int[] viewport = new int[4];
    private final int[] scissorBox = new int[4];
    private final boolean depthTest = GL11.glIsEnabled((int)2929);
    private final boolean cullFace = GL11.glIsEnabled((int)2884);
    private final boolean scissorTest = GL11.glIsEnabled((int)3089);
    private final boolean blend = GL11.glIsEnabled((int)3042);
    private final boolean depthMask = GL11.glGetBoolean((int)2930);

    private OpenGlStateSnapshot() {
        GL11.glGetIntegerv((int)2978, (int[])this.viewport);
        GL11.glGetIntegerv((int)3088, (int[])this.scissorBox);
        for (int unit = 0; unit < 4; ++unit) {
            this.textureBindings[unit] = GL30.glGetIntegeri((int)32873, (int)unit);
            this.samplerBindings[unit] = GL30.glGetIntegeri((int)35097, (int)unit);
        }
    }

    static OpenGlStateSnapshot capture() {
        return new OpenGlStateSnapshot();
    }

    void restore() {
        // Direct GL compositors bypass Mojang's cached state. Synchronize it with
        // the real bindings before restoring, otherwise a cached no-op can leave
        // a compositor FBO, texture or enabled test active for the next GPU pass.
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING));
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.readFramebuffer);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
        GL30.glBindRenderbuffer((int)36161, (int)this.renderbuffer);
        GL30.glBindVertexArray((int)this.vertexArray);
        GL15.glBindBuffer((int)34962, (int)this.arrayBuffer);
        GL15.glBindBuffer((int)34963, (int)this.elementArrayBuffer);
        GL20.glUseProgram((int)this.program);
        GlStateManager._activeTexture(GL11.glGetInteger(GL13Constants.ACTIVE_TEXTURE));
        for (int unit = 0; unit < TRACKED_TEXTURE_UNITS; ++unit) {
            GlStateManager._activeTexture((int)(33984 + unit));
            GlStateManager._bindTexture(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
            GlStateManager._bindTexture((int)this.textureBindings[unit]);
            GL33.glBindSampler((int)unit, (int)this.samplerBindings[unit]);
        }
        GlStateManager._activeTexture((int)this.activeTexture);
        GlStateManager._viewport((int)this.viewport[0], (int)this.viewport[1], (int)this.viewport[2], (int)this.viewport[3]);
        GlStateManager._scissorBox((int)this.scissorBox[0], (int)this.scissorBox[1], (int)this.scissorBox[2], (int)this.scissorBox[3]);
        OpenGlStateSnapshot.setDepthTest(GL11.glIsEnabled(GL11.GL_DEPTH_TEST));
        OpenGlStateSnapshot.setDepthTest(this.depthTest);
        OpenGlStateSnapshot.setCull(GL11.glIsEnabled(GL11.GL_CULL_FACE));
        OpenGlStateSnapshot.setCull(this.cullFace);
        OpenGlStateSnapshot.setScissor(GL11.glIsEnabled(GL11.GL_SCISSOR_TEST));
        OpenGlStateSnapshot.setScissor(this.scissorTest);
        OpenGlStateSnapshot.setBlend(GL11.glIsEnabled(GL11.GL_BLEND));
        OpenGlStateSnapshot.setBlend(this.blend);
        GlStateManager._depthMask(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
        GlStateManager._depthMask((boolean)this.depthMask);
        GL20.glBlendEquationSeparate((int)this.blendEquationRgb, (int)this.blendEquationAlpha);
        GlStateManager._blendFuncSeparate(GL11.glGetInteger(32969), GL11.glGetInteger(32968), GL11.glGetInteger(32971), GL11.glGetInteger(32970));
        GlStateManager._blendFuncSeparate((int)this.blendSourceRgb, (int)this.blendDestinationRgb, (int)this.blendSourceAlpha, (int)this.blendDestinationAlpha);
    }

    private static void setDepthTest(boolean enabled) {
        if (enabled) {
            GlStateManager._enableDepthTest();
        } else {
            GlStateManager._disableDepthTest();
        }
    }

    private static void setCull(boolean enabled) {
        if (enabled) {
            GlStateManager._enableCull();
        } else {
            GlStateManager._disableCull();
        }
    }

    private static void setScissor(boolean enabled) {
        if (enabled) {
            GlStateManager._enableScissorTest();
        } else {
            GlStateManager._disableScissorTest();
        }
    }

    private static void setBlend(boolean enabled) {
        if (enabled) {
            GlStateManager._enableBlend();
        } else {
            GlStateManager._disableBlend();
        }
    }

    private static final class GL15Constants {
        private static final int ARRAY_BUFFER_BINDING = 34964;
        private static final int ELEMENT_ARRAY_BUFFER_BINDING = 34965;

        private GL15Constants() {
        }
    }

    private static final class GL13Constants {
        private static final int ACTIVE_TEXTURE = 34016;

        private GL13Constants() {
        }
    }
}

