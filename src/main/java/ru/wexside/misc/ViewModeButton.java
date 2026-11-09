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
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class ViewModeButton
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private boolean enabled;
    private float value;
    public static final float value2 = 18.5f;
    public static final float value3 = 50.0f;
    private final String string3;
    private final String string4;

    public ViewModeButton(String string, String string2) {
        super(new GuiBounds(0.0f, 0.0f, 50.0f, 18.5f));
        this.string3 = string;
        this.string4 = string2;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void setBooleanType(boolean bl) {
        this.enabled = bl;
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        return this.getBounds().contains(n, n2);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        float f2;
        float f3;
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.value = FrameInterpolator.lerpTowards(this.value, this.enabled ? 1.0f : 0.0f, 30.0f);
        int n = ColorUtils.lerp(ColorUtils.withAlpha(ThemeColors.accent(), 0.0f), ThemeColors.accent(), this.value);
        int n2 = ColorUtils.lerp(ThemeColors.borderStrong(), ColorUtils.withAlpha(ThemeColors.borderStrong(), 0.0f), this.value);
        int n3 = ColorUtils.lerp(ThemeColors.textSecondary(), ThemeColors.backgroundControl(), this.value);
        drawApi.drawRoundedRectangle(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, n);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, 0.75f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), n2);
        float f4 = 7.0f;
        float f5 = 5.5f;
        float f6 = this.string4 == null || this.string4.isEmpty() ? 0.0f : FontRegistry.font3.textWidth(this.string4, f4);
        float f7 = FontRegistry.font2.textWidth(this.string3, f5);
        float f8 = FontRegistry.font2.textHeight(this.string3, f5);
        if (f6 > 0.0f) {
            f3 = bounds2.getX() + (bounds2.getWidth() - f6) / 2.0f;
            f2 = bounds2.getY() + 2.0f;
            FontRegistry.font3.drawGlyphText(matrix4f, drawApi, this.string4, f3, f2, f4, n3);
        }
        f3 = bounds2.getX() + (bounds2.getWidth() - f7) / 2.0f;
        f2 = bounds2.getY() + bounds2.getHeight() - f8 - 2.0f;
        FontRegistry.font2.drawText(matrix4f, drawApi, this.string3, f3, f2, f5, n3);
        return bounds2.getY() + bounds2.getHeight();
    }

    public String getString() {
        return this.string4;
    }

    public String getString2() {
        return this.string3;
    }

    public boolean isActive() {
        return this.enabled;
    }

    public float getFloatType() {
        return this.value;
    }
}

