package ru.wexside.util;

import java.util.function.Supplier;
import ru.wexside.misc.AbstractSettingDescription;
import ru.wexside.misc.ContainerDisplay;
import ru.wexside.misc.FontRegistry;
import ru.wexside.ui.GuiBounds;

public final class CompactSettingRow
extends AbstractSettingDescription {
    private static final float COLLAPSED_HEIGHT = 15.0f;
    private static final float EXPANDED_BASE_HEIGHT = 26.5f;
    private static final float TITLE_FONT_SIZE = 7.0f;
    private static final float TITLE_DESCRIPTION_GAP = 2.0f;

    public CompactSettingRow(GuiBounds bounds, Supplier<String> titleSupplier, String description, ContainerDisplay display) {
        super(bounds, titleSupplier, description, display, 6.75f, 6.25f, 4.5f);
    }

    @Override
    protected float getTitleBaseline(float collapseOffset) {
        return this.titleTop() + collapseOffset;
    }

    @Override
    protected boolean areDescriptionsVisible() {
        return this.containerDisplay.areSettingDescriptionsVisible();
    }

    @Override
    protected float getDescriptionBaseline() {
        return this.titleTop() + this.titleHeight() + 2.0f;
    }

    @Override
    public float getPreferredHeight() {
        if (!this.hasDescription()) {
            return 15.0f;
        }
        float descriptionHeight = super.measureWrappedDescriptionHeight();
        float baseLineHeight = this.lineHeight(this.getFloatType4());
        float contentHeight = this.lineHeight(7.0f) + 2.0f + descriptionHeight;
        float verticalPadding = Math.max(0.0f, (26.5f - this.lineHeight(7.0f) - 2.0f - baseLineHeight) / 2.0f);
        float expandedHeight = verticalPadding + contentHeight + verticalPadding;
        float additionalDescriptionHeight = Math.max(0.0f, descriptionHeight - baseLineHeight);
        return expandedHeight - (1.0f - this.getVisibilityProgress()) * (11.5f + additionalDescriptionHeight);
    }

    private float titleTop() {
        float contentTop;
        if (!this.hasDescription()) {
            contentTop = (15.0f - this.titleHeight()) / 2.0f;
        } else {
            float oneLineContent = this.lineHeight(7.0f) + 2.0f + this.lineHeight(this.getFloatType4());
            contentTop = (26.5f - oneLineContent) / 2.0f;
        }
        return this.bounds2.getY() + contentTop;
    }

    private float titleHeight() {
        return FontRegistry.font2.textHeight(this.getString2(), 7.0f);
    }

    private float lineHeight(float fontSize) {
        return FontRegistry.font2.textHeight("A", fontSize);
    }

    public float getFloatType5() {
        return this.horizontalPadding;
    }
}

