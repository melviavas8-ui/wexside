package ru.wexside.util;

import org.joml.Matrix4f;
import ru.wexside.input.InputBindings;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.ScrollController;

public final class Scrollbar {
    private ScrollController scrollController;
    private final float value;
    private final float value2;
    private boolean dragging;
    private final float value3;
    private final float value4;
    private float trackY;
    private final float value6;
    private final float BlockItem;
    private final float value7;
    private float hoverProgress;
    private float dragThumbOffsetY;
    private float trackRightX;
    private float visibilityProgress;
    private float viewportHeight;
    private final float value13;

    public Scrollbar() {
        this.value4 = 1.5f;
        this.value7 = 3.0f;
        this.value3 = 128.0f;
        this.value6 = 3.5f;
        this.value = 4.0f;
        this.BlockItem = 8.0f;
        this.value13 = 20.0f;
        this.value2 = 20.0f;
    }

    private void setFloatType(float f) {
        if (!this.dragging) {
            return;
        }
        if (!InputBindings.isMouseButtonPressed(0)) {
            this.dragging = false;
            return;
        }
        this.setFloatType2(f);
    }

    public boolean isDragging() {
        return this.dragging;
    }

    private boolean canScroll() {
        return this.scrollController != null && this.scrollController.getContentHeight() > this.viewportHeight + 0.5f && this.viewportHeight > 8.0f;
    }

    private GuiBounds getBounds() {
        float f = 11.0f;
        return new GuiBounds(this.trackRightX - 3.5f - 3.0f - 4.0f, this.trackY, f, this.viewportHeight);
    }

    private void setFloatType2(float f) {
        float f2 = this.viewportHeight - this.thumbHeight();
        if (f2 <= 0.0f) {
            return;
        }
        float f3 = Math.max(0.0f, Math.min(1.0f, (f - this.dragThumbOffsetY - this.trackY) / f2));
        this.scrollController.scrollTo(f3 * this.scrollController.getMinimumOffset(this.viewportHeight), this.viewportHeight);
    }

    public void render(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, ScrollController scrollController, float f4, float f5) {
        this.trackRightX = f;
        this.trackY = f2;
        this.viewportHeight = f3;
        this.scrollController = scrollController;
        boolean bl = this.canScroll();
        this.visibilityProgress = FrameInterpolator.lerpTowards(this.visibilityProgress, bl ? 1.0f : 0.0f, 20.0f);
        if (!bl) {
            this.dragging = false;
        }
        this.setFloatType(f5);
        boolean bl2 = this.dragging || this.getBounds().contains(f4, f5);
        this.hoverProgress = FrameInterpolator.lerpTowards(this.hoverProgress, bl2 && bl ? 1.0f : 0.0f, 20.0f);
        if (this.visibilityProgress <= 0.01f) {
            return;
        }
        float f6 = 1.5f + 1.5f * this.hoverProgress;
        drawApi.drawRoundedRectangle(matrix4f, f - 3.5f - f6, this.thumbY(), f6, this.thumbHeight(), 128.0f, ColorUtils.multiplyAlpha(ColorUtils.lerp(ThemeColors.borderPrimary(), ThemeColors.borderStrong(), this.hoverProgress), this.visibilityProgress));
    }

    private float thumbHeight() {
        return Math.max(8.0f, this.viewportHeight * this.viewportHeight / this.scrollController.getContentHeight());
    }

    private float thumbY() {
        float f = this.scrollController.getMinimumOffset(this.viewportHeight);
        float f2 = f >= -1.0E-4f ? 0.0f : Math.max(0.0f, Math.min(1.0f, this.scrollController.getOffset() / f));
        return this.trackY + (this.viewportHeight - this.thumbHeight()) * f2;
    }

    public boolean onMousePressed(int n, int n2, int n3) {
        if (n3 != 0 || !this.canScroll() || !this.getBounds().contains(n, n2)) {
            return false;
        }
        float f = this.thumbY();
        float f2 = this.thumbHeight();
        this.dragging = true;
        this.dragThumbOffsetY = (float)n2 >= f && (float)n2 <= f + f2 ? (float)n2 - f : f2 / 2.0f;
        this.setFloatType2(n2);
        return true;
    }
}

