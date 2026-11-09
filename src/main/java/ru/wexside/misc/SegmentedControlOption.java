package ru.wexside.misc;

import org.joml.Matrix4f;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.MsdfFontRenderer;
import ru.wexside.util.SegmentedControlStyle;

public final class SegmentedControlOption
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final SegmentedControlStyle segmentedControlStyle;
    private final String string3;
    private float value;
    private boolean enabled;
    private final String string4;

    public SegmentedControlOption(GuiBounds bounds2, String string, SegmentedControlStyle segmentedControlStyle) {
        this(bounds2, string, null, segmentedControlStyle);
    }

    public SegmentedControlOption(GuiBounds bounds2, String string, String string2, SegmentedControlStyle segmentedControlStyle) {
        super(bounds2);
        this.string4 = string == null ? "" : string;
        this.string3 = string2 == null || string2.isBlank() ? null : string2;
        this.segmentedControlStyle = segmentedControlStyle == null ? new SegmentedControlStyle() : segmentedControlStyle;
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
        float f2;
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = this.segmentedControlStyle.getGuiDrawApi();
        MsdfFontRenderer font5 = this.segmentedControlStyle.getMsdfFontRenderer2();
        if (drawApi == null || font5 == null) {
            return bounds2.getY() + bounds2.getHeight();
        }
        this.value = FrameInterpolator.lerpTowards(this.value, this.enabled ? 1.0f : 0.0f, this.segmentedControlStyle.getTextAnimationSpeed());
        int n = ColorUtils.lerp(this.segmentedControlStyle.getIntType4(), this.segmentedControlStyle.getIntType(), this.value);
        float f3 = this.segmentedControlStyle.getLabelSize();
        float f4 = font5.textWidth(this.string4, f3);
        float f5 = font5.textHeight(this.string4, f3);
        MsdfFontRenderer font8 = this.segmentedControlStyle.getMsdfFontRenderer();
        boolean bl = this.string3 != null && font8 != null;
        float f6 = bl ? this.segmentedControlStyle.getIconSize() : 0.0f;
        float f7 = bl ? font8.textWidth(this.string3, f6) : 0.0f;
        float f8 = bl ? font8.textHeight(this.string3, f6) : 0.0f;
        float f9 = bl ? this.segmentedControlStyle.getIconTextGap() : 0.0f;
        float f10 = f7 + f9 + f4;
        float f11 = bounds2.getX() + (bounds2.getWidth() - f10) / 2.0f;
        if (bl) {
            f2 = bounds2.getY() + (bounds2.getHeight() - f8) / 2.0f;
            font8.drawGlyphText(matrix4f, drawApi, this.string3, f11, f2, f6, n);
            f11 += f7 + f9;
        }
        f2 = bounds2.getY() + (bounds2.getHeight() - f5) / 2.0f;
        font5.drawText(matrix4f, drawApi, this.string4, f11, f2, f3, n);
        return bounds2.getY() + bounds2.getHeight();
    }

    public String getString() {
        return this.string3;
    }

    public String getString2() {
        return this.string4;
    }

    public boolean isActive() {
        return this.enabled;
    }
}

