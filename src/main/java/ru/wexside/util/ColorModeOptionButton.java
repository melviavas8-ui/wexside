package ru.wexside.util;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.ColorModeLabels;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

final class ColorModeOptionButton
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final float value;
    private final float value2;
    private final float value3;
    private final ColorModeLabels colorModeLabels;
    private final float value4;
    private boolean selected;
    private float selectionProgress;

    public ColorModeOptionButton(GuiBounds bounds2, ColorModeLabels colorModeLabels) {
        super(bounds2);
        this.value = 7.0f;
        this.value2 = 0.75f;
        this.value4 = 6.5f;
        this.value3 = 30.0f;
        this.colorModeLabels = colorModeLabels;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void setBooleanType(boolean bl) {
        this.selected = bl;
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        return n3 == 0 && this.getBounds().contains(n, n2);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.selectionProgress = FrameInterpolator.lerpTowards(this.selectionProgress, this.selected ? 1.0f : 0.0f, 30.0f);
        int n = ColorUtils.withAlpha(ThemeColors.backgroundHover(), (float)Math.round(255.0f * this.selectionProgress));
        int n2 = ColorUtils.lerp(ThemeColors.textMuted(), ThemeColors.textPrimary(), this.selectionProgress);
        drawApi.drawRoundedRectangle(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, n);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, 0.75f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), ThemeColors.borderPrimary());
        float f2 = FontRegistry.font3.textWidth(this.colorModeLabels.getString2(), 6.5f);
        float f3 = FontRegistry.font3.textHeight(this.colorModeLabels.getString2(), 6.5f);
        float f4 = bounds2.getX() + (bounds2.getWidth() - f2) / 2.0f;
        float f5 = bounds2.getY() + (bounds2.getHeight() - f3) / 2.0f;
        FontRegistry.font3.drawGlyphText(matrix4f, drawApi, this.colorModeLabels.getString2(), f4, f5, 6.5f, n2);
        return bounds2.getX() + bounds2.getWidth();
    }

    public ColorModeLabels getColorModeLabels() {
        return this.colorModeLabels;
    }
}

