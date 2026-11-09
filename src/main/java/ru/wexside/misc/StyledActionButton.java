package ru.wexside.misc;

import java.util.function.IntSupplier;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiInteractionState;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseHitTest;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class StyledActionButton
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
MouseHitTest {
    private float value;
    private final String string2;
    private final IntSupplier intSupplier;
    private final Runnable runnable;
    private final float value2;
    private final IntSupplier intSupplier2;
    public static final float value3 = 14.0f;
    private final float value4;
    private final float value5;

    public StyledActionButton(String string, IntSupplier intSupplier, IntSupplier intSupplier2, Runnable runnable) {
        super(new GuiBounds(0.0f, 0.0f, 0.0f, 14.0f));
        this.value4 = 7.0f;
        this.value2 = 6.5f;
        this.value5 = 0.1f;
        this.string2 = string;
        this.intSupplier = intSupplier;
        this.intSupplier2 = intSupplier2;
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
        if (n3 != 0 || !this.getBounds().contains(n, n2)) {
            return false;
        }
        this.runnable.run();
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        GuiInteractionState guiInteractionState = GuiInteractionState.getInstance();
        this.value = FrameInterpolator.lerpTowards(this.value, this.containsMouse(guiInteractionState.getScaledMouseX(), guiInteractionState.getScaledMouseY()) ? 1.0f : 0.0f, 20.0f);
        int n = ColorUtils.lerp(this.intSupplier.getAsInt(), ColorUtils.lerp(this.intSupplier.getAsInt(), -1, 0.1f), this.value);
        drawApi.drawRoundedRectangle(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, n);
        float f2 = FontRegistry.font2.textWidth(this.string2, 6.5f);
        float f3 = FontRegistry.font2.textHeight(this.string2, 6.5f);
        FontRegistry.font2.drawText(matrix4f, drawApi, this.string2, bounds2.getX() + (bounds2.getWidth() - f2) / 2.0f, bounds2.getY() + (bounds2.getHeight() - f3) / 2.0f, 6.5f, this.intSupplier2.getAsInt());
        return bounds2.getY() + bounds2.getHeight();
    }

    @Override
    public boolean containsMouse(int n, int n2) {
        GuiBounds bounds2 = this.getBounds();
        float f = this.getAbsoluteX();
        float f2 = this.getAbsoluteY();
        return (float)n >= f && (float)n <= f + bounds2.getWidth() && (float)n2 >= f2 && (float)n2 <= f2 + bounds2.getHeight();
    }
}

