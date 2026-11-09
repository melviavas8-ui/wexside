package ru.wexside.misc;

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

public final class TextField
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final String caretText;
    private final float fontSize;
    private final TextInputController textInputController;
    private final String suffix;
    private float selectionProgress;
    private final float cornerRadius;
    private final float borderWidth;

    public TextField(GuiBounds bounds2, TextInputModel callback15) {
        this(bounds2, callback15, "");
    }

    public TextField(GuiBounds bounds2, TextInputModel callback15, String string) {
        super(bounds2);
        this.cornerRadius = 8.0f;
        this.borderWidth = 0.75f;
        this.fontSize = 5.5f;
        this.caretText = "|";
        this.suffix = string == null ? "" : string;
        this.textInputController = new TextInputController(callback15);
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
        String string2 = this.textInputController.isCaretVisible() ? "|" : "";
        String string3 = this.suffix;
        String string4 = string2;
        String string5 = string;
        String string6 = string5 + string4 + string3;
        Object string7 = string6.isEmpty() ? "0" : string6;
        float f2 = FontRegistry.font4.textWidth(string6, 5.5f);
        float f3 = FontRegistry.font4.textHeight((String)string7, 5.5f);
        float f4 = bounds2.getX() + (bounds2.getWidth() - f2) / 2.0f;
        float f5 = bounds2.getY() + (bounds2.getHeight() - f3) / 2.0f;
        drawApi.drawRoundedRectangle(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 8.0f, ThemeColors.formatFieldFill());
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 8.0f, 0.75f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), ThemeColors.borderPrimary());
        this.selectionProgress = FrameInterpolator.lerpTowards(this.selectionProgress, this.textInputController.isAllSelected() ? 1.0f : 0.0f, 30.0f);
        int n = ThemeColors.textSecondary();
        int n2 = ColorUtils.lerp(n, ThemeColors.adjustForTheme(n), this.selectionProgress);
        if (!string6.isEmpty()) {
            FontRegistry.font4.drawText(matrix4f, drawApi, string6, f4, f5, 5.5f, n2);
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
}

