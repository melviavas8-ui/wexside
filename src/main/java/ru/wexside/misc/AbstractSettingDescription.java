package ru.wexside.misc;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.joml.Matrix4f;
import ru.wexside.misc.ContainerDisplay;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.TextLayoutUtils;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.VisibilityAnimationCache;

public abstract class AbstractSettingDescription {
    protected final Supplier<String> titleSupplier;
    protected final float descriptionLineSpacing;
    protected List<String> wrappedLines;
    protected float wrapWidth = -1.0f;
    protected final String description;
    protected float cachedWrapWidth;
    protected final float titleFontSize;
    protected final ContainerDisplay containerDisplay;
    protected final GuiBounds bounds2;
    protected final float horizontalPadding;
    protected final float visibilityAnimationSpeed;
    protected final VisibilityAnimationCache visibilityAnimationCache = new VisibilityAnimationCache();
    protected final float descriptionFontSize;

    protected AbstractSettingDescription(GuiBounds bounds2, Supplier<String> supplier, String string, ContainerDisplay containerDisplay, float f, float f2, float f3) {
        this.visibilityAnimationSpeed = 20.0f;
        this.descriptionLineSpacing = 1.0f;
        this.cachedWrapWidth = Float.NaN;
        this.bounds2 = bounds2;
        this.titleSupplier = supplier;
        this.description = string;
        this.containerDisplay = containerDisplay;
        this.titleFontSize = f;
        this.descriptionFontSize = f2;
        this.horizontalPadding = f3;
    }

    protected final List<String> getList() {
        if (!this.hasDescription()) {
            return List.of("");
        }
        if (this.wrapWidth <= 0.0f) {
            if (this.cachedWrapWidth != -1.0f || this.wrappedLines == null) {
                this.cachedWrapWidth = -1.0f;
                this.wrappedLines = List.of(this.description);
            }
            return this.wrappedLines;
        }
        if (this.wrappedLines == null || this.cachedWrapWidth != this.wrapWidth) {
            this.cachedWrapWidth = this.wrapWidth;
            this.wrappedLines = TextLayoutUtils.wrapText(this.description, FontRegistry.font2, this.descriptionFontSize, this.wrapWidth);
        }
        return this.wrappedLines;
    }

    protected final boolean hasDescription() {
        return this.description != null && !this.description.isBlank();
    }

    protected final float getDescriptionLineHeight() {
        return FontRegistry.font2.textHeight("A", this.descriptionFontSize);
    }

    public String getString() {
        return this.description;
    }

    public float getPreferredHeight() {
        return this.titleFontSize;
    }

    public ContainerDisplay getContainerDisplay() {
        return this.containerDisplay;
    }

    protected abstract float getTitleBaseline(float var1);

    public void setWrapWidth(float f) {
        this.wrapWidth = f;
    }

    public final float getVisibilityProgress() {
        return this.visibilityAnimationCache.visibilityProgress(this.areDescriptionsVisible(), this.hasDescription(), 20.0f);
    }

    protected final void renderContent(Matrix4f matrix4f, GuiDrawApi drawApi, float f) {
        float f2 = this.getVisibilityProgress();
        float f3 = this.hasDescription() ? 1.0f - f2 : 0.0f;
        float f4 = this.bounds2.getX() + this.horizontalPadding;
        float f5 = this.getTitleBaseline(f3);
        FontRegistry.font2.drawText(matrix4f, drawApi, this.getString2(), f4, f5, this.titleFontSize, ThemeColors.textPrimary());
        if (!this.hasDescription()) {
            return;
        }
        float f6 = f2 * f;
        if (f6 <= 0.001f) {
            return;
        }
        int n = ColorUtils.withAlpha(ThemeColors.textMuted(), 255.0f * f6);
        List<String> list = this.getList();
        float f7 = this.getDescriptionLineHeight();
        float f8 = this.getDescriptionBaseline();
        for (int i = 0; i < list.size(); ++i) {
            float f9 = f8 + (float)i * (f7 + 1.0f);
            FontRegistry.font2.drawText(matrix4f, drawApi, list.get(i), f4, f9, this.descriptionFontSize, n);
        }
    }

    protected final float measureWrappedDescriptionHeight() {
        if (!this.hasDescription()) {
            return 0.0f;
        }
        int n = this.getList().size();
        if (n <= 0) {
            return 0.0f;
        }
        float f = this.getDescriptionLineHeight();
        return (float)n * f + (float)(n - 1) * 1.0f;
    }

    public final String getString2() {
        return this.titleSupplier.get();
    }

    protected abstract boolean areDescriptionsVisible();

    public float getFloatType4() {
        return this.descriptionFontSize;
    }

    public float getContentOffset() {
        return this.horizontalPadding;
    }

    protected abstract float getDescriptionBaseline();

    public float getVisibilityAnimationSpeed() {
        Objects.requireNonNull(this);
        return 20.0f;
    }

    public float getDescriptionLineSpacing() {
        Objects.requireNonNull(this);
        return 1.0f;
    }

    public final void render(Matrix4f matrix4f, GuiDrawApi drawApi) {
        this.renderContent(matrix4f, drawApi, 1.0f);
    }

    public GuiBounds getBounds() {
        return this.bounds2;
    }
}

