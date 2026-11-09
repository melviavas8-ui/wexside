package ru.wexside.util;

import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.KeybindCaptureField;
import ru.wexside.misc.KeybindDescriptor;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.TextLayoutUtils;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.GuiDrawApi;

public final class KeybindRow
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final KeybindDescriptor callback10;
    private final KeybindCaptureField keybindCaptureField;

    public KeybindRow(KeybindDescriptor callback10) {
        super(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f));
        this.callback10 = callback10;
        this.keybindCaptureField = new KeybindCaptureField(callback10);
        this.addChild(this.keybindCaptureField);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        float f2;
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        float f3 = bounds2.getWidth();
        float f4 = this.measureRowHeight(f3);
        bounds2.setSize(f3, f4);
        float f5 = this.measureLabelHeight(f3);
        float f6 = bounds2.getY() + (f4 - f5) / 2.0f;
        FontRegistry.font2.drawText(matrix4f, drawApi, this.callback10.getString(), bounds2.getX(), f6, 6.75f, ThemeColors.textPrimary());
        if (this.hasDescription()) {
            f2 = FontRegistry.font2.textHeight(this.callback10.getString(), 6.75f);
            TextLayoutUtils.drawWrappedText(matrix4f, drawApi, this.callback10.getString2(), FontRegistry.font2, 6.25f, this.labelAvailableWidth(f3), bounds2.getX(), f6 + f2 + 1.5f, 1.0f, ThemeColors.textMuted());
        }
        f2 = this.keybindCaptureField.getPreferredWidth();
        float f7 = this.keybindCaptureField.getPreferredHeight();
        float f8 = bounds2.getX() + bounds2.getWidth() - f2 - 0.0f;
        float f9 = bounds2.getY() + (f4 - f7) / 2.0f;
        this.keybindCaptureField.getBounds().setPosition(f8, f9);
        this.keybindCaptureField.getBounds().setSize(f2, f7);
        this.keybindCaptureField.render(f, matrix4f);
        return bounds2.getY() + f4;
    }

    private boolean hasDescription() {
        return this.callback10.getString2() != null && !this.callback10.getString2().isBlank();
    }

    public float measureRowHeight(float f) {
        return Math.max(this.measureLabelHeight(f), this.keybindCaptureField.getPreferredHeight());
    }

    public float getDescriptionFontSize() {
        Objects.requireNonNull(this);
        return 6.25f;
    }

    public KeybindDescriptor getCallback10() {
        return this.callback10;
    }

    public float getRightMargin() {
        Objects.requireNonNull(this);
        return 0.0f;
    }

    public float getTitleDescriptionGap() {
        Objects.requireNonNull(this);
        return 1.5f;
    }

    private float labelAvailableWidth(float f) {
        return Math.max(0.0f, f - this.keybindCaptureField.getPreferredWidth() - 4.0f);
    }

    public float getEntrySpacing() {
        Objects.requireNonNull(this);
        return 4.0f;
    }

    private float measureLabelHeight(float f) {
        float f2 = FontRegistry.font2.textHeight(this.callback10.getString(), 6.75f);
        if (!this.hasDescription()) {
            return f2;
        }
        List<String> list = TextLayoutUtils.wrapText(this.callback10.getString2(), FontRegistry.font2, 6.25f, this.labelAvailableWidth(f));
        float f3 = FontRegistry.font2.textHeight("A", 6.25f);
        int n = Math.max(1, list.size());
        float f4 = (float)n * f3 + (float)Math.max(0, n - 1) * 1.0f;
        return f2 + 1.5f + f4;
    }

    public KeybindCaptureField getKeybindCaptureField() {
        return this.keybindCaptureField;
    }

    public float getLineSpacing() {
        Objects.requireNonNull(this);
        return 1.0f;
    }

    public float getTitleFontSize() {
        Objects.requireNonNull(this);
        return 6.75f;
    }
}

