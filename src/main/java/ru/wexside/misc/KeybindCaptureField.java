package ru.wexside.misc;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.input.BindInput;
import ru.wexside.input.InputBindings;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.KeybindDescriptor;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class KeybindCaptureField
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private float value;
    private boolean enabled;
    private final KeybindDescriptor callback10;
    private final String string2;

    public KeybindCaptureField(KeybindDescriptor callback10) {
        super(new GuiBounds(0.0f, 0.0f, 0.0f, 12.0f));
        this.string2 = "...";
        this.callback10 = callback10;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        boolean bl = this.getBounds().contains(n, n2);
        if (!this.enabled) {
            if (n3 == 0 && bl) {
                this.enabled = true;
                return true;
            }
            return false;
        }
        if (n3 == 0 || n3 == 1) {
            if (bl) {
                this.enabled = false;
                return true;
            }
            return false;
        }
        if (!bl) {
            return false;
        }
        this.callback10.setBindInput(BindInput.mouse(n3));
        this.enabled = false;
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        String string = this.getString();
        float f2 = bounds2.getY() + (bounds2.getHeight() - FontRegistry.font4.textHeight(string, 6.0f)) / 2.0f;
        this.value = FrameInterpolator.lerpTowards(this.value, this.enabled ? 1.0f : 0.0f, 30.0f);
        int n = ColorUtils.lerp(ThemeColors.borderPrimary(), ThemeColors.accent(), this.value);
        int n2 = ColorUtils.lerp(ThemeColors.controlFill(), ThemeColors.accentTint(), this.value);
        int n3 = ColorUtils.lerp(ThemeColors.textSecondary(), ThemeColors.accent(), this.value);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, 1.0f, n2, n);
        FontRegistry.font4.drawText(matrix4f, drawApi, string, bounds2.getX() + 5.0f, f2, 6.0f, n3);
        return bounds2.getY() + bounds2.getHeight();
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        if (!this.enabled) {
            return;
        }
        if (n3 == 0 || n3 == 1) {
            if (!this.getBounds().contains(n, n2)) {
                this.enabled = false;
            }
            return;
        }
        this.callback10.setBindInput(BindInput.mouse(n3));
        this.enabled = false;
    }

    @Override
    public void update2() {
        this.enabled = false;
        super.update2();
    }

    @Override
    public boolean onKeyPressed(int n) {
        if (!this.enabled) {
            return false;
        }
        if (n == 256) {
            return false;
        }
        if (n == 261 || n == 259) {
            this.callback10.setBindInput(BindInput.unbound());
            this.enabled = false;
            return true;
        }
        this.callback10.setBindInput(BindInput.keyboard(n));
        this.enabled = false;
        return true;
    }

    public float getPreferredWidth() {
        return 5.0f + FontRegistry.font4.textWidth(this.getString(), 6.0f) + 5.0f;
    }

    private String getString() {
        return this.enabled ? "..." : InputBindings.displayName(this.callback10.getBindInput());
    }

    public float getPreferredHeight() {
        return 12.0f;
    }
}

