package ru.wexside.util;

import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import org.joml.Matrix4f;
import ru.wexside.misc.TextureResource;
import ru.wexside.util.Easing;
import ru.wexside.util.GuiDrawApi;

public final class GuiPhotoBanner {
    private final TextureResource photo;
    private final LongSupplier clock;
    private final Easing enterEasing;
    private final Easing exitEasing;
    private final IntSupplier backgroundColor;
    private final IntSupplier borderColor;
    private final long cycleDurationNanos = 10000000000L;
    private final long enterDurationNanos = 650000000L;
    private final long holdDurationNanos = 2700000000L;
    private final long exitDurationNanos = 650000000L;
    private final float cardWidth = 70.0f;
    private final float cardHeight = 93.0f;
    private long openedAtNanos;
    private boolean open;

    public GuiPhotoBanner(TextureResource photo, LongSupplier clock, Easing enterEasing, Easing exitEasing, IntSupplier backgroundColor, IntSupplier borderColor) {
        this.photo = photo;
        this.clock = clock;
        this.enterEasing = enterEasing;
        this.exitEasing = exitEasing;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
    }

    public void onGuiOpened() {
        this.openedAtNanos = this.clock.getAsLong();
        this.open = true;
    }

    public void onGuiClosed() {
        this.open = false;
    }

    public void render(GuiDrawApi renderer, Matrix4f matrix, float panelWidth, float panelHeight) {
        float visibility = this.visibilityAt(this.clock.getAsLong());
        if (visibility <= 0.001f) {
            return;
        }
        float visibleX = panelWidth - this.cardWidth - 7.0f;
        float hiddenX = panelWidth + 3.0f;
        float x = hiddenX + (visibleX - hiddenX) * visibility;
        float y = panelHeight - this.cardHeight - 7.0f;
        renderer.drawRoundedRectangle(matrix, x - 2.0f, y - 2.0f, this.cardWidth + 4.0f, this.cardHeight + 4.0f, 11.0f, this.backgroundColor.getAsInt());
        renderer.drawRoundedOutline(matrix, x - 2.0f, y - 2.0f, this.cardWidth + 4.0f, this.cardHeight + 4.0f, 11.0f, 0.75f, this.borderColor.getAsInt());
        int texture = renderer.bindTexture(this.photo.getTextureId(), this.photo.getWidth(), this.photo.getHeight());
        renderer.drawRoundedTextureTinted(matrix, x, y, this.cardWidth, this.cardHeight, 9.0f, texture, -1);
    }

    private float visibilityAt(long nowNanos) {
        if (!this.open) {
            return 0.0f;
        }
        long elapsed = Math.max(0L, nowNanos - this.openedAtNanos) % this.cycleDurationNanos;
        Objects.requireNonNull(this);
        if (elapsed < 650000000L) {
            float f = elapsed;
            Objects.requireNonNull(this);
            return this.enterEasing.apply(f / 6.5E8f);
        }
        Objects.requireNonNull(this);
        long exitStartsAt = 650000000L + this.holdDurationNanos;
        if (elapsed < exitStartsAt) {
            return 1.0f;
        }
        Objects.requireNonNull(this);
        if (elapsed < exitStartsAt + 650000000L) {
            float f = elapsed - exitStartsAt;
            Objects.requireNonNull(this);
            float progress = f / 6.5E8f;
            return 1.0f - this.exitEasing.apply(progress);
        }
        return 0.0f;
    }
}

