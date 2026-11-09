package ru.wexside.misc;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.input.BindInput;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.setting.BindSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.setting.SettingComponent;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class BindSettingComponent
extends SettingComponent
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private boolean enabled;
    private float value = 1.0f;
    private float value2 = 7.0f;
    private float value3 = 6.0f;
    private float preferredHeight = 12.0f;
    private final String string2;
    private float value5 = 5.0f;
    private float value6;

    public BindSettingComponent(BindSetting bindSetting) {
        super(new GuiBounds(0.0f, 0.0f, 0.0f, 12.0f), bindSetting);
        this.string2 = "...";
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
                this.beginBindingCapture();
                return true;
            }
            return false;
        }
        if (n3 == 0 || n3 == 1) {
            if (bl) {
                this.endBindingCapture();
                return true;
            }
            return false;
        }
        this.captureMouseBinding(n3);
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        String string = this.enabled ? "..." : ((BindSetting)this.getSetting()).getKeyDisplayName();
        float f2 = bounds2.getY() + (bounds2.getHeight() - FontRegistry.font4.textHeight(string, this.value3)) / 2.0f;
        this.value6 = FrameInterpolator.lerpTowards(this.value6, this.enabled ? 1.0f : 0.0f, 30.0f);
        int n = ColorUtils.lerp(ThemeColors.borderPrimary(), ThemeColors.accent(), this.value6);
        int n2 = ColorUtils.lerp(ThemeColors.controlFill(), ThemeColors.accentTint(), this.value6);
        int n3 = ColorUtils.lerp(ThemeColors.textSecondary(), ThemeColors.accent(), this.value6);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), this.value2, this.value, n2, n);
        FontRegistry.font4.drawText(matrix4f, drawApi, string, bounds2.getX() + this.value5, f2, this.value3, n3);
        return bounds2.getY() + bounds2.getHeight();
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        if (!this.enabled) {
            return;
        }
        if (n3 == 0 || n3 == 1) {
            if (!this.getBounds().contains(n, n2)) {
                this.endBindingCapture();
            }
            return;
        }
        this.captureMouseBinding(n3);
    }

    @Override
    public void update2() {
        this.endBindingCapture();
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
            this.clearBinding();
            return true;
        }
        this.captureKeyboardBinding(n);
        return true;
    }

    @Override
    public float getPreferredWidth() {
        String string = this.enabled ? "..." : ((BindSetting)this.getSetting()).getKeyDisplayName();
        return this.value5 + FontRegistry.font4.textWidth(string, this.value3) + this.value5;
    }

    private void captureKeyboardBinding(int n) {
        ((BindSetting)this.getSetting()).setBindInput(BindInput.keyboard(n));
        this.endBindingCapture();
    }

    private void captureMouseBinding(int n) {
        ((BindSetting)this.getSetting()).setBindInput(BindInput.mouse(n));
        this.endBindingCapture();
    }

    private void clearBinding() {
        ((BindSetting)this.getSetting()).setBindInput(BindInput.unbound());
        this.endBindingCapture();
    }

    private void beginBindingCapture() {
        this.enabled = true;
    }

    private void endBindingCapture() {
        this.enabled = false;
    }

    @Override
    public float getPreferredHeight() {
        return this.preferredHeight;
    }
}

