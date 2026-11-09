package ru.wexside.misc;

import java.util.function.BooleanSupplier;
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
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public class IconButton
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final String primaryLabel;
    private float value;
    private float value2;
    private boolean enabled;
    private final Runnable runnable;
    private final BooleanSupplier booleanSupplier;
    private final String secondaryLabel;

    public IconButton(GuiBounds bounds2, String string, String string2, Runnable runnable) {
        this(bounds2, string, string2, runnable, null);
    }

    public IconButton(GuiBounds bounds2, String string, Runnable runnable) {
        this(bounds2, string, null, runnable, null);
    }

    public IconButton(GuiBounds bounds2, String string) {
        this(bounds2, string, null, null, null);
    }

    public IconButton(GuiBounds bounds2, String string, String string2, Runnable runnable, BooleanSupplier booleanSupplier) {
        super(bounds2);
        this.primaryLabel = string;
        this.secondaryLabel = string2;
        this.runnable = runnable;
        this.booleanSupplier = booleanSupplier;
    }

    public IconButton(GuiBounds bounds2, String string, Runnable runnable, BooleanSupplier booleanSupplier) {
        this(bounds2, string, null, runnable, booleanSupplier);
    }

    public void setFloatType(float f) {
        this.value2 = f;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (this.getBounds().contains(n, n2)) {
            if (this.booleanSupplier == null) {
                this.enabled = true;
            }
            if (this.runnable != null) {
                this.runnable.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        if (this.booleanSupplier != null) {
            this.enabled = this.booleanSupplier.getAsBoolean();
        } else if (this.enabled && !InputBindings.isMouseButtonPressed(0)) {
            this.enabled = false;
        }
        this.value = FrameInterpolator.lerpTowards(this.value, this.enabled ? 1.0f : 0.0f, 30.0f);
        int n = ColorUtils.lerp(ColorUtils.withAlpha(ThemeColors.borderSoft(), 0.0f), ThemeColors.borderSoft(), this.value);
        float f2 = this.getBounds().getX();
        drawApi.drawRoundedRectangleBordered(matrix4f, f2, this.getBounds().getY() + 0.25f, this.getBounds().getWidth(), this.getBounds().getHeight() - 0.25f, 8.0f, 0.0f, n);
        drawApi.drawRoundedOutline(matrix4f, f2, this.getBounds().getY(), this.getBounds().getWidth(), this.getBounds().getHeight(), 8.0f, 1.25f, ThemeColors.borderPrimary());
        String string = this.value2 > 0.5f && this.secondaryLabel != null ? this.secondaryLabel : this.primaryLabel;
        float f3 = FontRegistry.font3.textWidth(string, 7.0f);
        float f4 = FontRegistry.font3.textHeight(string, 7.0f);
        int n2 = ColorUtils.lerp(ThemeColors.textMuted(), ThemeColors.textPrimary(), this.value);
        FontRegistry.font3.drawGlyphText(matrix4f, drawApi, string, f2 + this.getBounds().getWidth() / 2.0f - f3 / 2.0f, this.getBounds().getY() + this.getBounds().getHeight() / 2.0f - f4 / 2.0f, 7.0f, n2);
        return bounds2.getY() + bounds2.getHeight();
    }
}

