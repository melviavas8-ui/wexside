package ru.wexside.misc;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiInteractionState;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseHitTest;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.TextLayoutUtils;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public abstract class AbstractOptionRow
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
MouseHitTest {
    private float hoverProgress;
    private final String leadingIcon;
    private boolean enabled;
    private float selectionProgress;
    private final String label;
    public static final float DEFAULT_HEIGHT = 14.0f;
    public static final float DEFAULT_WIDTH = 109.0f;

    protected AbstractOptionRow(String string, String string2) {
        this(string, string2, 109.0f);
    }

    protected AbstractOptionRow(String string, String string2, float f) {
        super(new GuiBounds(0.0f, 0.0f, f, 14.0f));
        this.label = string;
        this.leadingIcon = string2;
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
        return n3 == 0 && this.getBounds().contains(n, n2);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        GuiInteractionState guiInteractionState = GuiInteractionState.getInstance();
        boolean bl = this.containsMouse(guiInteractionState.getScaledMouseX(), guiInteractionState.getScaledMouseY());
        this.hoverProgress = FrameInterpolator.lerpTowards(this.hoverProgress, bl ? 1.0f : 0.0f, 15.0f);
        this.selectionProgress = FrameInterpolator.lerpTowards(this.selectionProgress, this.enabled ? 1.0f : 0.0f, 30.0f);
        this.renderBackground(matrix4f, drawApi, bounds2);
        this.renderLabel(matrix4f, drawApi, bounds2);
        this.renderSelectionIndicator(matrix4f, drawApi, bounds2);
        return bounds2.getY() + bounds2.getHeight();
    }

    public String getString() {
        return this.leadingIcon;
    }

    public String getString2() {
        return this.label;
    }

    public boolean isActive() {
        return this.enabled;
    }

    protected int getBackgroundColor() {
        return ColorUtils.lerp(ColorUtils.withAlpha(ThemeColors.borderSubtle(), 0.0f), ThemeColors.borderSubtle(), this.hoverProgress);
    }

    protected float getSelectionIconFontSize() {
        return 7.0f;
    }

    protected float getSelectionProgress() {
        return this.selectionProgress;
    }

    protected float getLeadingIconFontSize() {
        return 7.0f;
    }

    protected boolean hasDescription() {
        return this.leadingIcon != null && !this.leadingIcon.isBlank();
    }

    protected int getLeadingIconColor() {
        return this.getHoverTextColor();
    }

    protected float getCornerRadius() {
        return 7.0f;
    }

    public float getHoverProgress() {
        return this.hoverProgress;
    }

    protected void renderBackground(Matrix4f matrix4f, GuiDrawApi drawApi, GuiBounds bounds2) {
        drawApi.drawRoundedRectangle(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), this.getCornerRadius(), this.getBackgroundColor());
    }

    protected abstract void renderSelectionIndicator(Matrix4f var1, GuiDrawApi var2, GuiBounds var3);

    protected int getHoverTextColor() {
        return ColorUtils.lerp(ThemeColors.textSecondary(), ThemeColors.textPrimary(), this.hoverProgress);
    }

    public float getSelectionProgressValue() {
        return this.selectionProgress;
    }

    protected int getLabelColor() {
        return this.getHoverTextColor();
    }

    @Override
    public boolean containsMouse(int n, int n2) {
        GuiBounds bounds2 = this.getBounds();
        float f = this.getAbsoluteX();
        float f2 = this.getAbsoluteY();
        return (float)n >= f && (float)n <= f + bounds2.getWidth() && (float)n2 >= f2 && (float)n2 <= f2 + bounds2.getHeight();
    }

    protected float getTrailingContentWidth() {
        return 14.0f;
    }

    protected void renderLabel(Matrix4f matrix4f, GuiDrawApi drawApi, GuiBounds bounds2) {
        float f = bounds2.getX() + 3.5f;
        if (this.hasDescription()) {
            FontRegistry.font3.drawGlyphText(matrix4f, drawApi, this.leadingIcon, f, bounds2.getY() + 3.25f, this.getLeadingIconFontSize(), this.getLeadingIconColor());
            f += 10.0f;
        }
        float f2 = bounds2.getX() + bounds2.getWidth() - this.getTrailingContentWidth() - f;
        String string = TextLayoutUtils.trimToWidth(this.label, FontRegistry.font2, this.getLabelFontSize(), f2);
        FontRegistry.font2.drawText(matrix4f, drawApi, string, f, bounds2.getY() + 3.0f, this.getLabelFontSize(), this.getLabelColor());
    }

    protected float getLabelFontSize() {
        return 6.0f;
    }
}

