package ru.wexside.util;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.input.InputBindings;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiInteractionState;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseHitTest;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public abstract class AbstractTextInput
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
MouseHitTest {
    public static final float HEIGHT = 11.5f;
    protected float visibilityAnimation = 1.0f;
    protected boolean mousePressed;
    protected final Runnable runnable;
    protected float clickAnimation;
    protected float hoverAnimation;

    protected AbstractTextInput(Runnable runnable) {
        super(new GuiBounds(0.0f, 0.0f, 0.0f, 11.5f));
        this.runnable = runnable;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (n3 != 0 || !this.isActive2() || !this.getBounds().contains(n, n2)) {
            return false;
        }
        this.mousePressed = true;
        if (this.runnable != null) {
            this.runnable.run();
        }
        return true;
    }

    @Override
    public final float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        GuiInteractionState guiInteractionState = GuiInteractionState.getInstance();
        boolean bl = this.isActive2();
        if (this.mousePressed && !InputBindings.isMouseButtonPressed(0)) {
            this.mousePressed = false;
        }
        this.visibilityAnimation = FrameInterpolator.lerpTowards(this.visibilityAnimation, bl ? 1.0f : 0.0f, 45.0f);
        this.hoverAnimation = FrameInterpolator.lerpTowards(this.hoverAnimation, bl && this.containsMouse(guiInteractionState.getScaledMouseX(), guiInteractionState.getScaledMouseY()) ? 1.0f : 0.0f, 20.0f);
        this.clickAnimation = FrameInterpolator.lerpTowards(this.clickAnimation, this.mousePressed ? 1.0f : 0.0f, 30.0f);
        bounds2.setSize(this.getWidth(), 11.5f);
        if (this.visibilityAnimation <= 0.01f) {
            return bounds2.getY() + 11.5f;
        }
        float f2 = Math.max(this.hoverAnimation, Math.max(this.clickAnimation, this.getActiveAnimation()));
        this.renderUnderlay(drawApi, matrix4f, bounds2, this.visibilityAnimation);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), 11.5f, 7.0f, 1.0f, this.backgroundColor(f2, this.visibilityAnimation), this.borderColor(f2, this.visibilityAnimation));
        this.renderInputContent(drawApi, matrix4f, bounds2, ColorUtils.withAlpha(ThemeColors.textSecondary(), 255.0f * this.visibilityAnimation), this.visibilityAnimation);
        return bounds2.getY() + 11.5f;
    }

    protected float getActiveAnimation() {
        return 0.0f;
    }

    protected int borderColor(float f, float f2) {
        return ColorUtils.withAlpha(ThemeColors.borderPrimary(), 255.0f * f2);
    }

    protected void renderUnderlay(GuiDrawApi drawApi, Matrix4f matrix4f, GuiBounds bounds2, float f) {
    }

    protected int backgroundColor(float f, float f2) {
        return ColorUtils.multiplyAlpha(ThemeColors.backgroundHover(), f * f2);
    }

    protected abstract void renderInputContent(GuiDrawApi var1, Matrix4f var2, GuiBounds var3, int var4, float var5);

    public abstract float getWidth();

    @Override
    public boolean containsMouse(int n, int n2) {
        GuiBounds bounds2 = this.getBounds();
        float f = this.getAbsoluteX();
        float f2 = this.getAbsoluteY();
        return (float)n >= f && (float)n <= f + bounds2.getWidth() && (float)n2 >= f2 && (float)n2 <= f2 + bounds2.getHeight();
    }

    @Override
    public boolean isActive2() {
        return true;
    }
}

