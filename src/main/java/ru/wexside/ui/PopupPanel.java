package ru.wexside.ui;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.ThemeColors;
import ru.wexside.misc.ThemeManager;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ClippedLayerRenderer;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public class PopupPanel
extends GuiElement {
    private boolean open;
    private float openingProgress;

    public PopupPanel(GuiBounds bounds) {
        super(bounds);
        super.setBooleanType(false);
    }

    public boolean isActive() {
        return this.open || this.openingProgress > 0.01f;
    }

    @Override
    public boolean isActive2() {
        return this.open;
    }

    @Override
    public void setBooleanType(boolean open) {
        this.open = open;
        super.setBooleanType(open);
        if (open) {
            this.updateLayout();
        }
    }

    @Override
    public float render(float delta, Matrix4f matrix) {
        this.openingProgress = FrameInterpolator.lerpTowards(this.openingProgress, this.open ? 1.0f : 0.0f, 30.0f);
        if (this.openingProgress <= 0.01f) {
            return this.getBounds().getY();
        }
        this.updateLayout();
        GuiDrawApi renderer = WexSideClient.getGuiRenderer();
        if (renderer != null) {
            GuiBounds bounds = this.getBounds();
            boolean animated = this.openingProgress < 0.99f;
            int opacity = ColorUtils.withAlpha(-1, 255.0f * this.openingProgress);
            ClippedLayerRenderer.renderLayer(renderer, matrix, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 16.0f, animated, opacity, localMatrix -> {
                this.renderSurface((Matrix4f)localMatrix, renderer);
                this.renderPopup(delta, (Matrix4f)localMatrix, renderer);
            });
        }
        return this.getBounds().getY() + this.getBounds().getHeight();
    }

    @Override
    public boolean onMousePressed(int mouseX, int mouseY, int button) {
        int localY;
        GuiBounds bounds = this.getBounds();
        if (!this.open || !bounds.contains(mouseX, mouseY)) {
            return false;
        }
        int localX = Math.round((float)mouseX - bounds.getX());
        return super.onMousePressed(localX, localY = Math.round((float)mouseY - bounds.getY()), button) || bounds.contains(mouseX, mouseY);
    }

    protected void updateLayout() {
    }

    protected void renderPopup(float delta, Matrix4f matrix, GuiDrawApi renderer) {
        this.renderChildren(delta, matrix);
    }

    private void renderSurface(Matrix4f matrix, GuiDrawApi renderer) {
        GuiBounds bounds = this.getBounds();
        float width = bounds.getWidth();
        float height = bounds.getHeight();
        renderer.drawRoundedShadow(matrix, 0.0f, 0.0f, width, height, 8.0f, 12.0f, ColorUtils.rgba(0, 0, 0, 28));
        if (ThemeManager.getThemeManager().isBlurEnabled()) {
            renderer.drawBlurredRoundedRectangle(matrix, 0.0f, 0.0f, width, height, 8.0f);
        }
        renderer.drawRoundedRectangle(matrix, 0.0f, 0.0f, width, height, 8.0f, ThemeColors.panelBackground());
        renderer.drawRoundedRectangleOutlined(matrix, 0.0f, 0.0f, width, height, 8.0f, 0.75f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), ThemeColors.borderPrimary());
    }

    protected final void renderChildren(float delta, Matrix4f matrix) {
        for (GuiElement child : this.children) {
            child.render(delta, matrix);
        }
    }
}

