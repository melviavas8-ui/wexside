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

public final class LabeledSegmentOption
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private Runnable runnable = () -> {};
    private final float titleHeight;
    private boolean enabled;
    private final float titleWidth;
    private final float optionHeight;
    private float selectionAnimation;
    private final String string3;
    private final String string4;

    public LabeledSegmentOption(String string, String string2, float f) {
        super(new GuiBounds(0.0f, 0.0f, 0.0f, f));
        this.string3 = string;
        this.string4 = string2;
        this.titleWidth = FontRegistry.font3.textWidth(string, 6.0f);
        this.titleHeight = FontRegistry.font3.textHeight(string, 6.0f);
        this.optionHeight = FontRegistry.font4.textHeight(string2, 6.0f);
        float f2 = FontRegistry.font4.textWidth(string2, 6.0f);
        float f3 = 4.0f + this.titleWidth + 2.0f + f2 + 4.0f;
        this.getBounds().setSize(f3, f);
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
        this.runnable.run();
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.selectionAnimation = FrameInterpolator.lerpTowards(this.selectionAnimation, this.enabled ? 1.0f : 0.0f, 25.0f);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, 0.75f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), ThemeColors.borderPrimary());
        if (this.selectionAnimation > 0.001f) {
            drawApi.drawRoundedRectangle(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, ColorUtils.withAlpha(ThemeColors.accent(), 255.0f * this.selectionAnimation));
        }
        int n = ColorUtils.lerp(ThemeColors.textSecondary(), ThemeColors.backgroundControl(), this.selectionAnimation);
        float f2 = bounds2.getX() + 4.0f;
        float f3 = bounds2.getY() + (bounds2.getHeight() - this.titleHeight) / 2.0f;
        FontRegistry.font3.drawGlyphText(matrix4f, drawApi, this.string3, f2, f3, 6.0f, n);
        float f4 = f2 + this.titleWidth + 2.0f;
        float f5 = bounds2.getY() + (bounds2.getHeight() - this.optionHeight) / 2.0f;
        FontRegistry.font4.drawText(matrix4f, drawApi, this.string4, f4, f5, 6.0f, n);
        return bounds2.getY() + bounds2.getHeight();
    }

    public boolean isActive() {
        return this.enabled;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable == null ? () -> {} : runnable;
    }
}

