package ru.wexside.util;

import org.joml.Matrix4f;
import ru.wexside.misc.AbstractSettingDescription;
import ru.wexside.misc.ContainerDisplay;
import ru.wexside.misc.FontRegistry;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.GuiDrawApi;

final class SettingDescriptionRenderer
extends AbstractSettingDescription {
    private final float value;
    private final float value2;
    private final float value3;
    private final float value4;

    SettingDescriptionRenderer(GuiBounds bounds2, String string, String string2, ContainerDisplay containerDisplay) {
        super(bounds2, () -> string, string2, containerDisplay, 6.75f, 6.5f, 7.0f);
        this.value2 = 8.5f;
        this.value = 20.0f;
        this.value3 = 34.5f;
        this.value4 = 8.0f;
    }

    public float getDescriptionHeight() {
        return super.measureWrappedDescriptionHeight();
    }

    @Override
    public float getPreferredHeight() {
        if (!this.hasDescription()) {
            return 34.5f;
        }
        float descriptionHeight = this.getVisibleDescriptionHeight();
        float expandedHeight = 34.5f + descriptionHeight;
        return expandedHeight - (1.0f - this.getVisibilityProgress()) * (8.0f + descriptionHeight);
    }

    public void renderWithCollapse(Matrix4f matrix4f, GuiDrawApi drawApi, float f) {
        this.renderContent(matrix4f, drawApi, 1.0f - f);
    }

    @Override
    protected float getTitleBaseline(float f) {
        return this.bounds2.getY() + 8.5f;
    }

    private float getVisibleDescriptionHeight() {
        return Math.max(0.0f, super.measureWrappedDescriptionHeight() - this.getFloatType4());
    }

    @Override
    public float getFloatType4() {
        return this.hasDescription() ? FontRegistry.font2.textHeight("A", super.getFloatType4()) : 0.0f;
    }

    @Override
    protected boolean areDescriptionsVisible() {
        return this.containerDisplay.areModuleDescriptionsVisible();
    }

    @Override
    protected float getDescriptionBaseline() {
        return this.bounds2.getY() + 20.0f;
    }
}

