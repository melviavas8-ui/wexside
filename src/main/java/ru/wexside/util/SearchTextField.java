package ru.wexside.util;

import java.util.function.BooleanSupplier;
import org.joml.Matrix4f;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseHitTest;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.TextInputController;
import ru.wexside.misc.TextInputModel;
import ru.wexside.misc.TextLayoutUtils;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.AbstractTextInput;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public class SearchTextField
extends AbstractTextInput
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
MouseHitTest {
    public static final float value = 128.0f;
    private float activeAnimation;
    private float caretAnimation;
    private float horizontalScroll;
    private final boolean enabled;
    private final TextInputModel callback15;
    private final String string4;
    public static final int slot = 48;
    private final BooleanSupplier booleanSupplier;
    private final TextInputController textInputController;

    public SearchTextField(TextInputModel callback15, BooleanSupplier booleanSupplier, String string, boolean bl) {
        super((Runnable)null);
        this.callback15 = callback15;
        this.string4 = string;
        this.enabled = bl;
        this.booleanSupplier = booleanSupplier;
        this.textInputController = new TextInputController(callback15);
        this.getBounds().setSize(128.0f, 11.5f);
    }

    @Override
    public void update() {
        if (!this.isActive2()) {
            this.textInputController.blur();
        }
        this.textInputController.tick();
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (!this.isActive2()) {
            return false;
        }
        if (this.getBounds().contains(n, n2)) {
            this.callback15.setText("");
            return true;
        }
        return this.textInputController.onMousePressed(this.getBounds(), n, n2, n3);
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        this.textInputController.blurIfOutside(this.getBounds(), n, n2);
    }

    @Override
    public boolean onCharTyped(char c) {
        return this.textInputController.onCharTyped(c);
    }

    @Override
    public void update2() {
        this.textInputController.blur();
        super.update2();
    }

    @Override
    public boolean onKeyPressed(int n) {
        if (!this.textInputController.isFocused()) {
            return false;
        }
        if (n == 256 || n == 257 || n == 335) {
            this.textInputController.blur();
            return true;
        }
        return this.textInputController.onKeyPressed(n);
    }

    public void update4() {
        this.textInputController.focus();
    }

    private GuiBounds getClearButtonBounds() {
        if (!this.hasClearButton()) {
            return new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f);
        }
        GuiBounds bounds2 = super.getBounds();
        float f = FontRegistry.font3.textWidth("d", 6.0f) + 4.0f;
        return new GuiBounds(bounds2.getX() + bounds2.getWidth() - f, bounds2.getY(), f, 11.5f);
    }

    private boolean hasClearButton() {
        return this.enabled && !this.textInputController.getText().isEmpty();
    }

    @Override
    protected float getActiveAnimation() {
        this.activeAnimation = FrameInterpolator.lerpTowards(this.activeAnimation, this.textInputController.isFocused() ? 1.0f : 0.0f, 25.0f);
        return this.activeAnimation;
    }

    @Override
    protected int borderColor(float f, float f2) {
        return ColorUtils.withAlpha(ColorUtils.lerp(ThemeColors.borderPrimary(), ThemeColors.accent(), this.activeAnimation), 255.0f * f2);
    }

    @Override
    protected void renderInputContent(GuiDrawApi drawApi, Matrix4f matrix4f, GuiBounds bounds2, int n, float f) {
        float f2;
        float f3 = bounds2.getX() + 4.0f;
        float f4 = bounds2.getY() + (11.5f - FontRegistry.font3.textHeight("Ф", 6.0f)) / 2.0f;
        FontRegistry.font3.drawGlyphText(matrix4f, drawApi, "Ф", f3, f4, 6.0f, n);
        float f5 = f3 + FontRegistry.font3.textWidth("Ф", 6.0f) + 3.0f;
        float f6 = bounds2.getX() + bounds2.getWidth() - 4.0f - f5;
        String string = this.textInputController.getText();
        boolean bl = this.textInputController.isFocused();
        if (this.hasClearButton()) {
            f2 = FontRegistry.font3.textWidth("d", 6.0f);
            f6 -= f2 + 3.0f;
            FontRegistry.font3.drawGlyphText(matrix4f, drawApi, "d", bounds2.getX() + bounds2.getWidth() - 4.0f - f2, bounds2.getY() + (11.5f - FontRegistry.font3.textHeight("d", 6.0f)) / 2.0f, 6.0f, n);
        }
        if (!bl) {
            String string2 = string.isEmpty() ? this.string4 : TextLayoutUtils.trimToWidth(string, FontRegistry.font2, 6.0f, f6);
            int n2 = string.isEmpty() ? n : ColorUtils.withAlpha(ThemeColors.textPrimary(), 255.0f * f);
            float f7 = bounds2.getY() + (11.5f - FontRegistry.font2.textHeight(string2, 6.0f)) / 2.0f;
            FontRegistry.font2.drawText(matrix4f, drawApi, string2, f5, f7, 6.0f, n2);
            this.horizontalScroll = 0.0f;
            return;
        }
        f2 = FontRegistry.font2.textWidth("|", 6.0f);
        float f8 = FontRegistry.font2.textWidth(string, 6.0f) + f2;
        this.horizontalScroll = FrameInterpolator.lerpTowards(this.horizontalScroll, Math.max(0.0f, f8 - f6), 30.0f);
        this.caretAnimation = FrameInterpolator.lerpTowards(this.caretAnimation, this.textInputController.isAllSelected() ? 1.0f : 0.0f, 30.0f);
        float f9 = f5 - this.horizontalScroll;
        float f10 = bounds2.getY() + (11.5f - FontRegistry.font2.textHeight(string, 6.0f)) / 2.0f;
        int n3 = ThemeColors.textPrimary();
        int n4 = ColorUtils.withAlpha(ColorUtils.lerp(n3, ThemeColors.adjustForTheme(n3), this.caretAnimation), 255.0f * f);
        drawApi.beginStencil(1);
        drawApi.fillRectangle(matrix4f, f5, bounds2.getY(), f6, 11.5f, ColorUtils.withAlpha(-1, 0.0f));
        drawApi.applyStencilMask(1);
        FontRegistry.font2.drawText(matrix4f, drawApi, string, f9, f10, 6.0f, n4);
        if (this.textInputController.isCaretVisible()) {
            FontRegistry.font2.drawText(matrix4f, drawApi, "|", f9 + FontRegistry.font2.textWidth(string, 6.0f), f10, 6.0f, ColorUtils.withAlpha(n3, 255.0f * f));
        }
        drawApi.endStencil();
    }

    @Override
    public float getWidth() {
        return 128.0f;
    }

    public boolean isFocused() {
        return this.textInputController.isFocused();
    }

    @Override
    public boolean isActive2() {
        return this.booleanSupplier.getAsBoolean();
    }
}

