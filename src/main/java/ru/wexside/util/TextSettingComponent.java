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
import ru.wexside.misc.TextFieldStyle;
import ru.wexside.misc.TextInputController;
import ru.wexside.misc.TextSettingTextAdapter;
import ru.wexside.misc.ThemeColors;
import ru.wexside.setting.TextSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.setting.SettingComponent;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class TextSettingComponent
extends SettingComponent
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final String string3;
    private final String string4;
    private final TextInputController textInputController;
    private float horizontalScroll;
    private float selectionProgress;
    private final TextFieldStyle callback60;

    public TextSettingComponent(TextSetting textSetting, TextFieldStyle callback60) {
        super(new GuiBounds(0.0f, 0.0f, callback60.getHorizontalPadding(), callback60.getPreferredHeight()), textSetting);
        this.string3 = "Введите текст...";
        this.string4 = "|";
        this.callback60 = callback60;
        this.textInputController = new TextInputController(new TextSettingTextAdapter(textSetting));
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
        String string = this.textInputController.getText();
        boolean bl = string.isBlank();
        boolean bl2 = bl && !this.textInputController.isFocused();
        String string2 = bl2 ? "Введите текст..." : string;
        float f2 = this.callback60.getFontSize();
        float f3 = this.callback60.getInputHeight();
        float f4 = bounds2.getY() + (f3 - FontRegistry.font2.textHeight(string2, f2)) / 2.0f;
        int n = bl2 ? ThemeColors.textPlaceholder() : ThemeColors.textPrimary();
        this.selectionProgress = FrameInterpolator.lerpTowards(this.selectionProgress, this.textInputController.isAllSelected() ? 1.0f : 0.0f, 30.0f);
        int n2 = bl2 ? n : ColorUtils.lerp(n, ThemeColors.adjustForTheme(n), this.selectionProgress);
        float f5 = bounds2.getX() + this.callback60.getHorizontalPadding();
        float f6 = this.textInputController.isFocused() ? FontRegistry.font2.textWidth("|", f2) : 0.0f;
        float f7 = FontRegistry.font2.textWidth(string2, f2) + f6;
        float f8 = Math.max(0.0f, bounds2.getWidth() - this.callback60.getHorizontalPadding() * 2.0f);
        float f9 = this.textInputController.isFocused() ? Math.max(0.0f, f7 - f8) : 0.0f;
        this.horizontalScroll = FrameInterpolator.lerpTowards(this.horizontalScroll, f9, 30.0f);
        float f10 = f5 - this.horizontalScroll;
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), f3, this.callback60.getOutlineSize(), this.callback60.getOutlineSize(), this.callback60.getFillColor(), this.callback60.getBorderColor());
        drawApi.beginStencil(1);
        drawApi.drawRoundedRectangle(matrix4f, bounds2.getX() + 1.0f, bounds2.getY() + 1.0f, bounds2.getWidth() - 2.0f, f3 - 2.0f, this.callback60.getOutlineSize(), ColorUtils.rgba(0, 0, 0, 0));
        drawApi.applyStencilMask(1);
        FontRegistry.font2.drawText(matrix4f, drawApi, string2, f10, f4, f2, n2);
        if (this.textInputController.isCaretVisible()) {
            float f11 = f10 + FontRegistry.font2.textWidth(string2, f2);
            FontRegistry.font2.drawText(matrix4f, drawApi, "|", f11, f4, f2, ThemeColors.textPrimary());
        }
        drawApi.endStencil();
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
        super.update2();
    }

    @Override
    public boolean onKeyPressed(int n) {
        return this.textInputController.onKeyPressed(n);
    }

    @Override
    public float getPreferredWidth() {
        return this.callback60.getHorizontalPadding();
    }

    @Override
    public float getPreferredHeight() {
        return this.callback60.getPreferredHeight();
    }
}

