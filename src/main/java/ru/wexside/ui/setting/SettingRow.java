package ru.wexside.ui.setting;

import org.joml.Matrix4f;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.render.RenderFrameClock;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.Setting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.setting.SettingComponent;

public abstract class SettingRow<T extends Setting>
extends SettingComponent<T> {
    private final SettingComponent<?> control;
    private float animatedVisibility;
    private boolean visibilityInitialized;
    private int visibilityFrame = Integer.MIN_VALUE;

    public SettingRow(GuiBounds bounds, Setting setting, SettingComponent<?> control) {
        super(bounds, setting);
        this.control = control;
        if (control != null) {
            this.addChild(control);
        }
        this.syncVisibility();
    }

    public SettingComponent<?> getSettingComponent() {
        return this.control;
    }

    public final boolean isHeaderControl() {
        return this.getSetting() instanceof BindSetting;
    }

    protected final void syncVisibility() {
        this.updateVisibilityAnimation();
    }

    protected final boolean handleRowClick(int mouseX, int mouseY, int button) {
        this.syncVisibility();
        return !this.isTargetVisible() || this.animatedVisibility < 0.99f || super.onMousePressed(mouseX, mouseY, button);
    }

    protected final void renderRowDecorations(float delta, Matrix4f matrix) {
        this.syncVisibility();
    }

    protected final void updateComponentVisibility() {
        this.syncVisibility();
    }

    public final void resetVisibilityAnimation() {
        this.animatedVisibility = this.isTargetVisible() ? 1.0f : 0.0f;
        this.visibilityInitialized = true;
        this.visibilityFrame = RenderFrameClock.currentFrame();
        this.applyAnimatedVisibility();
    }

    public final void updateLayoutState() {
        this.syncVisibility();
    }

    public abstract void refreshLayout();

    public final boolean isTargetVisible() {
        return ((Setting)this.getSetting()).isVisible();
    }

    public final boolean shouldRemainInLayout() {
        this.updateVisibilityAnimation();
        return this.isTargetVisible() || this.animatedVisibility > 0.001f;
    }

    public final float visibilityProgress() {
        this.updateVisibilityAnimation();
        return this.animatedVisibility;
    }

    private void updateVisibilityAnimation() {
        float target;
        int frame = RenderFrameClock.currentFrame();
        if (this.visibilityFrame == frame) {
            return;
        }
        float f = target = this.isTargetVisible() ? 1.0f : 0.0f;
        if (!this.visibilityInitialized) {
            this.animatedVisibility = target;
            this.visibilityInitialized = true;
        } else {
            this.animatedVisibility = FrameInterpolator.lerpTowards(this.animatedVisibility, target, 20.0f);
            if (Math.abs(target - this.animatedVisibility) <= 0.001f) {
                this.animatedVisibility = target;
            }
        }
        this.visibilityFrame = frame;
        this.applyAnimatedVisibility();
    }

    private void applyAnimatedVisibility() {
        boolean participatesInLayout = this.isTargetVisible() || this.animatedVisibility > 0.001f;
        super.setBooleanType(participatesInLayout);
        if (this.control != null) {
            this.control.setBooleanType(participatesInLayout);
        }
    }
}

