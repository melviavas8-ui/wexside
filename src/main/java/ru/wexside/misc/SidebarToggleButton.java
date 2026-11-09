package ru.wexside.misc;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.input.InputBindings;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.ModuleBrowser;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class SidebarToggleButton
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private float value;
    private final ModuleBrowser moduleBrowser;
    private final String string;
    private boolean enabled;
    private float value2;
    private final String string2;

    public SidebarToggleButton(GuiBounds bounds2, ModuleBrowser moduleBrowser) {
        super(bounds2);
        this.string2 = "w";
        this.string = "Свернуть всё";
        this.moduleBrowser = moduleBrowser;
        this.value = moduleBrowser.isActive2() ? 1.0f : 0.0f;
        this.getBounds().setSize(this.getPreferredWidth(), 11.5f);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (!this.moduleBrowser.isActive2()) {
            return false;
        }
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        if (n3 == 0) {
            this.enabled = true;
            this.moduleBrowser.update3();
        }
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        if (this.enabled && !InputBindings.isMouseButtonPressed(0)) {
            this.enabled = false;
        }
        this.value = FrameInterpolator.lerpTowards(this.value, this.moduleBrowser.isActive2() ? 1.0f : 0.0f, 45.0f);
        this.value2 = FrameInterpolator.lerpTowards(this.value2, this.enabled ? 1.0f : 0.0f, 30.0f);
        if (this.value <= 0.01f) {
            return bounds2.getY() + bounds2.getHeight();
        }
        int n = (int)(255.0f * this.value);
        int n2 = (int)(255.0f * this.value2 * this.value);
        int n3 = ColorUtils.withAlpha(ThemeColors.backgroundHover(), (float)n2);
        drawApi.drawRoundedRectangle(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, n3);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, 1.0f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), ColorUtils.withAlpha(ThemeColors.borderPrimary(), (float)n));
        float f2 = 5.75f;
        float f3 = 5.75f;
        float f4 = FontRegistry.font3.textWidth("w", f2);
        float f5 = FontRegistry.font2.textWidth("Свернуть всё", f3);
        float f6 = f4 + 3.0f + f5;
        float f7 = bounds2.getX() + (bounds2.getWidth() - f6) / 2.0f;
        float f8 = bounds2.getY() + (bounds2.getHeight() - FontRegistry.font3.textHeight("w", f2)) / 2.0f;
        float f9 = f7 + f4 + 3.0f;
        float f10 = bounds2.getY() + (bounds2.getHeight() - FontRegistry.font2.textHeight("Свернуть всё", f3)) / 2.0f;
        FontRegistry.font3.drawGlyphText(matrix4f, drawApi, "w", f7, f8, f2, ColorUtils.withAlpha(ThemeColors.textSecondary(), (float)n));
        FontRegistry.font2.drawText(matrix4f, drawApi, "Свернуть всё", f9, f10, f3, ColorUtils.withAlpha(ThemeColors.textSecondary(), (float)n));
        return bounds2.getY() + bounds2.getHeight();
    }

    public float getPreferredWidth() {
        float f = 5.75f;
        float f2 = 5.75f;
        float f3 = FontRegistry.font3.textWidth("w", f) + 3.0f + FontRegistry.font2.textWidth("Свернуть всё", f2);
        return f3 + 6.0f;
    }
}

