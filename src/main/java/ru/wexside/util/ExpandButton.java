package ru.wexside.util;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
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

public final class ExpandButton
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private boolean expanded = false;
    private float expansionProgress;
    private boolean interactive = true;

    public ExpandButton(GuiBounds bounds2) {
        super(bounds2);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (!this.interactive) {
            return false;
        }
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        if (n3 == 0) {
            this.expanded = !this.expanded;
        }
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.expansionProgress = FrameInterpolator.lerpTowards(this.expansionProgress, this.expanded ? 1.0f : 0.0f, 15.0f);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, 1.0f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), ThemeColors.borderPrimary());
        float f2 = 6.0f;
        float f3 = FontRegistry.font3.textWidth("F", f2);
        float f4 = FontRegistry.font3.textHeight("F", f2);
        float f5 = bounds2.getX() + (bounds2.getWidth() - f3) / 2.0f;
        float f6 = bounds2.getY() + (bounds2.getHeight() - f4) / 2.0f;
        float f7 = bounds2.getX() + bounds2.getWidth() / 2.0f;
        float f8 = bounds2.getY() + bounds2.getHeight() / 2.0f;
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(f7, f8, 0.0f).rotateZ((float)Math.toRadians(90.0f * this.expansionProgress)).translate(-f7, -f8, 0.0f);
        FontRegistry.font3.drawGlyphText(matrix4f2, drawApi, "F", f5, f6, f2, ThemeColors.textMuted());
        return bounds2.getY() + bounds2.getHeight();
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public float getExpansionProgress() {
        return this.expansionProgress;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    @Override
    public boolean isActive2() {
        return this.interactive;
    }
}

