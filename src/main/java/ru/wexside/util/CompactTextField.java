package ru.wexside.util;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.TextInputController;
import ru.wexside.misc.TextInputModel;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class CompactTextField
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final float value2;
    private final TextInputController textInputController;
    private final float value4;
    private float horizontalScroll = 4.0f;
    private final String string3;
    private final TextInputModel textModel;
    private final float value5;
    private final float value6;
    private final float value7;
    private final float value8;
    private float selectionProgress;

    public CompactTextField(TextInputModel textModel) {
        super(new GuiBounds(0.0f, 0.0f, 50.0f, 14.0f));
        this.value8 = 50.0f;
        this.value5 = 14.0f;
        this.value2 = 8.0f;
        this.value4 = 1.0f;
        this.value7 = 6.0f;
        this.value6 = 4.0f;
        this.string3 = "|";
        this.textModel = textModel;
        this.textInputController = new TextInputController(textModel);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
        this.textInputController.tick();
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        return this.textInputController.onMousePressed(this.getBounds(), n, n2, n3);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        String string = this.textModel.getText();
        String string2 = this.textInputController.getText();
        float f2 = string.isBlank() ? 0.0f : FontRegistry.font2.textWidth(string, 6.0f);
        float f3 = FontRegistry.font2.textHeight(string.isBlank() ? " " : string, 6.0f);
        float f4 = FontRegistry.font2.textHeight(string2, 6.0f);
        float f5 = bounds2.getY() + (bounds2.getHeight() - f4) / 2.0f;
        float f6 = bounds2.getY() + (bounds2.getHeight() - f3) / 2.0f;
        float f7 = bounds2.getX() + bounds2.getWidth() - 4.0f - f2;
        float f8 = string.isBlank() ? bounds2.getX() + bounds2.getWidth() - 4.0f : f7 - 4.0f;
        float f9 = Math.max(0.0f, f8 - (bounds2.getX() + 4.0f));
        float f10 = this.textInputController.isFocused() ? FontRegistry.font2.textWidth("|", 6.0f) : 0.0f;
        float f11 = FontRegistry.font2.textWidth(string2, 6.0f) + f10;
        float f12 = this.textInputController.isFocused() ? Math.max(0.0f, f11 - f9) : 0.0f;
        this.horizontalScroll = FrameInterpolator.lerpTowards(this.horizontalScroll, f12, 30.0f);
        this.selectionProgress = FrameInterpolator.lerpTowards(this.selectionProgress, this.textInputController.isAllSelected() ? 1.0f : 0.0f, 30.0f);
        int n = ThemeColors.textPrimary();
        int n2 = ColorUtils.lerp(n, ThemeColors.adjustForTheme(n), this.selectionProgress);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 8.0f, 1.0f, ThemeColors.backgroundPrimary(), ThemeColors.borderPrimary());
        drawApi.beginStencil(1);
        drawApi.drawRoundedRectangle(matrix4f, bounds2.getX() + 1.0f, bounds2.getY() + 1.0f, bounds2.getWidth() - 2.0f, bounds2.getHeight() - 2.0f, 8.0f, ColorUtils.rgba(0, 0, 0, 0));
        drawApi.applyStencilMask(1);
        float f13 = bounds2.getX() + 4.0f - this.horizontalScroll;
        FontRegistry.font2.drawText(matrix4f, drawApi, string2, f13, f5, 6.0f, n2);
        if (this.textInputController.isCaretVisible()) {
            float f14 = f13 + FontRegistry.font2.textWidth(string2, 6.0f);
            FontRegistry.font2.drawText(matrix4f, drawApi, "|", f14, f5, 6.0f, ThemeColors.textSecondary());
        }
        drawApi.endStencil();
        if (!string.isBlank()) {
            FontRegistry.font2.drawText(matrix4f, drawApi, string, f7, f6, 6.0f, ThemeColors.textPlaceholder());
        }
        return bounds2.getY() + bounds2.getHeight();
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
    }

    @Override
    public boolean onKeyPressed(int n) {
        return this.textInputController.onKeyPressed(n);
    }

    public float getFloatType() {
        return 50.0f;
    }

    public float getFloatType2() {
        return 14.0f;
    }
}

