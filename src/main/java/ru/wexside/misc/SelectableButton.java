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

public final class SelectableButton
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private float value;
    private final Runnable runnable;
    private final String string3;
    private final float value2;
    private final float value3;
    private final float value4;
    private boolean enabled;
    private final float value5;
    private final String string4;
    private final float value6;
    private final float value7;

    public SelectableButton(String string, String string2, float f, float f2, Runnable runnable) {
        super(new GuiBounds(0.0f, 0.0f, f, f2));
        this.value7 = 7.0f;
        this.value6 = 0.75f;
        this.value3 = 6.0f;
        this.value2 = 2.5f;
        this.value4 = 6.0f;
        this.value5 = 15.0f;
        this.string3 = string == null ? "" : string;
        this.string4 = string2 == null ? "" : string2;
        this.runnable = runnable;
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
        if (n3 != 0 || !this.getBounds().contains(n, n2)) {
            return false;
        }
        if (this.runnable != null) {
            this.runnable.run();
        }
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.value = FrameInterpolator.lerpTowards(this.value, this.enabled ? 1.0f : 0.0f, 15.0f);
        int n = ColorUtils.lerp(ThemeColors.controlFill(), ThemeColors.accent(), this.value);
        int n2 = ColorUtils.lerp(ThemeColors.borderPrimary(), ColorUtils.withAlpha(ThemeColors.accent(), 0.0f), this.value);
        int n3 = ColorUtils.lerp(ThemeColors.textSecondary(), ThemeColors.backgroundControl(), this.value);
        drawApi.drawRoundedRectangle(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, n);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, 0.75f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), n2);
        float f2 = FontRegistry.font3.textWidth(this.string3, 6.0f);
        float f3 = FontRegistry.font3.textHeight(this.string3, 6.0f);
        float f4 = FontRegistry.font2.textWidth(this.string4, 6.0f);
        float f5 = FontRegistry.font2.textHeight(this.string4, 6.0f);
        float f6 = f2 + 2.5f + f4;
        float f7 = bounds2.getX() + (bounds2.getWidth() - f6) / 2.0f;
        FontRegistry.font3.drawGlyphText(matrix4f, drawApi, this.string3, f7, bounds2.getY() + (bounds2.getHeight() - f3) / 2.0f, 6.0f, n3);
        FontRegistry.font2.drawText(matrix4f, drawApi, this.string4, f7 + f2 + 2.5f, bounds2.getY() + (bounds2.getHeight() - f5) / 2.0f, 6.0f, n3);
        return bounds2.getY() + bounds2.getHeight();
    }
}

