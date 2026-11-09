package ru.wexside.notification;

import java.util.List;
import java.util.function.IntSupplier;
import org.joml.Matrix4f;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.ThemeColors;
import ru.wexside.misc.ThemeManager;
import ru.wexside.notification.NotificationCategory;
import ru.wexside.notification.NotificationPart;
import ru.wexside.util.AnimationMath;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class NotificationToast {
    private static final float HEIGHT = 14.0f;
    private static final float HORIZONTAL_PADDING = 6.0f;
    private static final long EXIT_MILLIS = 250L;
    private final NotificationCategory category;
    private final Object key;
    private String icon;
    private IntSupplier accentColor;
    private List<NotificationPart> parts;
    private final long durationMillis;
    private final long createdAt = System.currentTimeMillis();
    private long dismissAt;
    private boolean removed;
    private float y;

    public NotificationToast(NotificationCategory category, Object key, String icon, IntSupplier accentColor, List<NotificationPart> parts, long durationMillis) {
        this.category = category;
        this.key = key;
        this.icon = icon == null ? "" : icon;
        this.accentColor = accentColor;
        this.parts = List.copyOf(parts);
        this.durationMillis = Math.max(250L, durationMillis);
    }

    public NotificationCategory category() {
        return this.category;
    }

    public Object key() {
        return this.key;
    }

    public String icon() {
        return this.icon;
    }

    public IntSupplier accentColor() {
        return this.accentColor;
    }

    public List<NotificationPart> parts() {
        return this.parts;
    }

    public boolean isDismissing() {
        return this.dismissAt != 0L;
    }

    public void updateContent(String icon, IntSupplier accentColor, List<NotificationPart> parts) {
        this.icon = icon == null ? "" : icon;
        this.accentColor = accentColor;
        this.parts = List.copyOf(parts);
        this.dismissAt = 0L;
    }

    public void moveTo(float targetY, float delta) {
        float speed = Math.max(8.0f, 18.0f * delta);
        this.y = AnimationMath.lerp(this.y, targetY, Math.min(1.0f, speed / 60.0f));
    }

    public void beginDismiss() {
        if (this.dismissAt == 0L) {
            this.dismissAt = System.currentTimeMillis();
        }
    }

    public void remove() {
        this.removed = true;
    }

    public boolean isExpired() {
        long now = System.currentTimeMillis();
        if (this.removed) {
            return true;
        }
        if (this.dismissAt != 0L) {
            return now - this.dismissAt >= 250L;
        }
        if (now - this.createdAt >= this.durationMillis) {
            this.beginDismiss();
        }
        return false;
    }

    public void render(GuiDrawApi renderer, Matrix4f matrix, float centerX, float delta) {
        float alpha = this.alpha();
        if (alpha <= 0.001f) {
            return;
        }
        String text = this.parts.stream().map(NotificationPart::text).reduce("", String::concat);
        float iconWidth = this.icon.isEmpty() ? 0.0f : 9.0f;
        float width = 12.0f + iconWidth + FontRegistry.font2.textWidth(text, 6.0f);
        float x = centerX - width / 2.0f;
        int background = ColorUtils.multiplyAlpha(ThemeColors.hudBackground(), alpha);
        if (ThemeManager.getThemeManager().isHudBlurEnabled()) {
            renderer.drawBlurredRoundedRectangle(matrix, x, this.y, width, 14.0f, 4.0f);
        }
        renderer.drawRoundedRectangle(matrix, x, this.y, width, 14.0f, 4.0f, background);
        renderer.drawRoundedOutline(matrix, x, this.y, width, 14.0f, 4.0f, 0.75f, this.withAlpha(ThemeColors.notificationOutline(), alpha));
        float textX = x + 6.0f;
        if (!this.icon.isEmpty()) {
            int accent = this.withAlpha(this.accentColor.getAsInt(), alpha);
            FontRegistry.font3.drawGlyphText(matrix, renderer, this.icon, textX, this.y + 3.5f, 7.0f, accent);
            textX += iconWidth;
        }
        for (NotificationPart part : this.parts) {
            int color = this.withAlpha(part.color().getAsInt(), alpha);
            FontRegistry.font2.drawText(matrix, renderer, part.text(), textX, this.y + 3.0f, 6.0f, color);
            textX += FontRegistry.font2.textWidth(part.text(), 6.0f);
        }
    }

    private float alpha() {
        long now = System.currentTimeMillis();
        float enter = Math.min(1.0f, (float)(now - this.createdAt) / 180.0f);
        if (this.dismissAt == 0L) {
            return enter;
        }
        return Math.max(0.0f, 1.0f - (float)(now - this.dismissAt) / 250.0f);
    }

    private int withAlpha(int color, float alpha) {
        return (int)((float)(color >>> 24 & 0xFF) * alpha) << 24 | color & 0xFFFFFF;
    }
}

