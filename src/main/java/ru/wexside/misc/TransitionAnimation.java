package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import ru.wexside.util.Easing;

public class TransitionAnimation {
    private double progress;
    private final Easing secondaryEasing;
    private double previousProgress;
    private final Easing primaryEasing;

    public TransitionAnimation(Easing easing) {
        this(easing, easing);
    }

    public TransitionAnimation(Easing primaryEasing, Easing secondaryEasing) {
        this.primaryEasing = primaryEasing;
        this.secondaryEasing = secondaryEasing;
    }

    public void setActive(boolean active) {
        this.advance(active, 0.2);
    }

    public float getPrimaryProgress() {
        return (float)this.getPrimaryProgressDouble();
    }

    public void advance(boolean active, double step) {
        this.previousProgress = this.progress;
        this.progress = MathHelper.clamp((double)(this.progress + (active ? step : -step)), (double)0.0, (double)1.0);
    }

    public float getSecondaryProgress(float tickProgress) {
        return (float)this.interpolateSecondary(tickProgress);
    }

    public double interpolatePrimary(float tickProgress) {
        double interpolated = this.previousProgress + (this.progress - this.previousProgress) * (double)tickProgress;
        return this.primaryEasing.apply(interpolated);
    }

    public void finish() {
        this.previousProgress = 1.0;
        this.progress = 1.0;
    }

    public double getSecondaryProgressDouble() {
        return this.interpolateSecondary(MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true));
    }

    public double interpolateSecondary(float tickProgress) {
        double interpolated = this.previousProgress + (this.progress - this.previousProgress) * (double)tickProgress;
        return this.secondaryEasing.apply(interpolated);
    }

    public boolean isAtStart() {
        return this.previousProgress + this.progress <= 1.0E-4;
    }

    public float getSecondaryProgress() {
        return (float)this.getSecondaryProgressDouble();
    }

    public float getPrimaryProgress(float tickProgress) {
        return (float)this.interpolatePrimary(tickProgress);
    }

    public void reset() {
        this.progress = 0.0;
        this.previousProgress = 0.0;
    }

    public void setProgress(double progress) {
        this.progress = progress;
        this.previousProgress = progress;
    }

    public double getPrimaryProgressDouble() {
        return this.interpolatePrimary(MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true));
    }
}

