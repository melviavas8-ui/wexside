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

public class FloatingPanel
extends GuiElement {
    private boolean open;
    private float openingProgress;

    public FloatingPanel(GuiBounds bounds) {
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
            ClippedLayerRenderer.renderLayer(renderer, matrix, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 0.0f, animated, opacity, localMatrix -> {
                this.renderSurface((Matrix4f)localMatrix, renderer);
                this.renderPanel(delta, (Matrix4f)localMatrix, renderer);
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

    @Override
    public void onMouseReleased(int mouseX, int mouseY, int button) {
        GuiBounds bounds = this.getBounds();
        super.onMouseReleased(Math.round((float)mouseX - bounds.getX()), Math.round((float)mouseY - bounds.getY()), button);
    }

    protected void updateLayout() {
    }

    protected void renderPanel(float delta, Matrix4f matrix, GuiDrawApi renderer) {
        for (GuiElement child : this.children) {
            child.render(delta, matrix);
        }
    }

    private void renderSurface(Matrix4f matrix, GuiDrawApi renderer) {
        GuiBounds bounds = this.getBounds();
        float width = bounds.getWidth();
        float height = bounds.getHeight();
        if (ThemeManager.getThemeManager().isBlurEnabled()) {
            renderer.drawBlurredRoundedRectangle(matrix, 0.0f, 0.0f, width, height, 6.0f);
        }
        renderer.drawRoundedRectangle(matrix, 0.0f, 0.0f, width, height, 6.0f, ThemeColors.panelBackground());
        renderer.drawRoundedRectangleOutlined(matrix, 0.0f, 0.0f, width, height, 6.0f, 0.75f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), ThemeColors.borderPrimary());
    }
}

