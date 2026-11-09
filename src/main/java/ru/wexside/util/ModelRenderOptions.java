package ru.wexside.util;

import java.util.Objects;
import ru.wexside.misc.ModelSurfaceMode;

public final class ModelRenderOptions {
    private final int outlineColor;
    private static final float value = 2.0f;
    private final boolean outlineThroughFill;
    private final boolean depthTestEnabled;
    private final float outlineLineWidth;
    private static final ModelRenderOptions modelRenderOptions2 = new ModelRenderOptions(false, false, false, -1, 2.0f, true, ModelSurfaceMode.SOLID);
    private final ModelSurfaceMode modelSurfaceMode;
    private static final ModelRenderOptions modelRenderOptions3 = new ModelRenderOptions(true, true, false, -1, 2.0f, true, ModelSurfaceMode.SOLID);
    private final boolean outlineUsesModelColor;
    private final boolean outlineEnabled;

    public ModelRenderOptions(boolean bl, boolean bl2, boolean bl3, int n, float f, boolean bl4, ModelSurfaceMode modelSurfaceMode) {
        this.outlineEnabled = bl;
        this.outlineThroughFill = bl2;
        this.outlineUsesModelColor = bl3;
        this.outlineColor = n;
        this.outlineLineWidth = f;
        this.depthTestEnabled = bl4;
        this.modelSurfaceMode = modelSurfaceMode;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ModelRenderOptions)) {
            return false;
        }
        ModelRenderOptions modelRenderOptions2 = (ModelRenderOptions)object;
        return this.outlineEnabled == modelRenderOptions2.outlineEnabled && this.outlineThroughFill == modelRenderOptions2.outlineThroughFill && this.outlineUsesModelColor == modelRenderOptions2.outlineUsesModelColor && this.outlineColor == modelRenderOptions2.outlineColor && Float.compare(this.outlineLineWidth, modelRenderOptions2.outlineLineWidth) == 0 && this.depthTestEnabled == modelRenderOptions2.depthTestEnabled && Objects.equals((Object)this.modelSurfaceMode, (Object)modelRenderOptions2.modelSurfaceMode);
    }

    public String toString() {
        String string = String.valueOf((Object)this.modelSurfaceMode);
        boolean bl = this.depthTestEnabled;
        float f = this.outlineLineWidth;
        int n = this.outlineColor;
        boolean bl2 = this.outlineUsesModelColor;
        boolean bl3 = this.outlineThroughFill;
        boolean bl4 = this.outlineEnabled;
        return "ModelRenderOptions[outlineEnabled=" + bl4 + ", outlineThroughFill=" + bl3 + ", outlineUsesModelColor=" + bl2 + ", outlineColor=" + n + ", outlineLineWidth=" + f + ", depthTestEnabled=" + bl + ", surfaceEffect=" + string + "]";
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.outlineEnabled, this.outlineThroughFill, this.outlineUsesModelColor, this.outlineColor, Float.valueOf(this.outlineLineWidth), this.depthTestEnabled, this.modelSurfaceMode});
    }

    public float getClampedOutlineLineWidth() {
        return Math.max(1.0f, this.outlineLineWidth);
    }

    public boolean isOutlineThroughFill() {
        return this.outlineThroughFill;
    }

    public boolean isDepthTestEnabled() {
        return this.depthTestEnabled;
    }

    public boolean isOutlineEnabled() {
        return this.outlineEnabled;
    }

    public ModelRenderOptions withOutlineEnabled(boolean bl) {
        return new ModelRenderOptions(bl, this.outlineThroughFill, this.outlineUsesModelColor, this.outlineColor, this.outlineLineWidth, this.depthTestEnabled, this.modelSurfaceMode);
    }

    public boolean hasSurfaceEffect() {
        return this.modelSurfaceMode != ModelSurfaceMode.SOLID;
    }

    public boolean usesModelOutlineColor() {
        return this.outlineUsesModelColor;
    }

    public ModelRenderOptions withSurfaceMode(ModelSurfaceMode modelSurfaceMode) {
        return new ModelRenderOptions(this.outlineEnabled, this.outlineThroughFill, this.outlineUsesModelColor, this.outlineColor, this.outlineLineWidth, this.depthTestEnabled, modelSurfaceMode != null ? modelSurfaceMode : ModelSurfaceMode.SOLID);
    }

    public ModelRenderOptions withModelOutlineColor() {
        return new ModelRenderOptions(this.outlineEnabled, this.outlineThroughFill, true, this.outlineColor, this.outlineLineWidth, this.depthTestEnabled, this.modelSurfaceMode);
    }

    public float getOutlineLineWidth() {
        return this.outlineLineWidth;
    }

    public int getOutlineColor() {
        return this.outlineColor;
    }

    public static ModelRenderOptions forOutlineEnabled(boolean bl) {
        return bl ? ModelRenderOptions.getAlternateRenderOptions() : ModelRenderOptions.getDefaultRenderOptions();
    }

    public ModelRenderOptions withOutlineThroughFill(boolean bl) {
        return new ModelRenderOptions(this.outlineEnabled, bl, this.outlineUsesModelColor, this.outlineColor, this.outlineLineWidth, this.depthTestEnabled, this.modelSurfaceMode);
    }

    public static ModelRenderOptions outlinedColor(int n) {
        return new ModelRenderOptions(true, true, false, n, 2.0f, true, ModelSurfaceMode.SOLID);
    }

    public static ModelRenderOptions getDefaultRenderOptions() {
        return modelRenderOptions2;
    }

    public ModelRenderOptions withOutlineLineWidth(float f) {
        return new ModelRenderOptions(this.outlineEnabled, this.outlineThroughFill, this.outlineUsesModelColor, this.outlineColor, f, this.depthTestEnabled, this.modelSurfaceMode);
    }

    public int resolveOutlineColor(int n) {
        return this.outlineUsesModelColor ? n : this.outlineColor;
    }

    public ModelRenderOptions withOutlineColor(int n) {
        return new ModelRenderOptions(this.outlineEnabled, this.outlineThroughFill, false, n, this.outlineLineWidth, this.depthTestEnabled, this.modelSurfaceMode);
    }

    public static ModelRenderOptions getAlternateRenderOptions() {
        return modelRenderOptions3;
    }

    public ModelRenderOptions withDepthTestEnabled(boolean bl) {
        return new ModelRenderOptions(this.outlineEnabled, this.outlineThroughFill, this.outlineUsesModelColor, this.outlineColor, this.outlineLineWidth, bl, this.modelSurfaceMode);
    }

    public ModelSurfaceMode getModelSurfaceMode() {
        return this.modelSurfaceMode;
    }
}

