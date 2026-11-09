package ru.wexside.notification;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.notification.NotificationCategory;
import ru.wexside.notification.NotificationFactory;
import ru.wexside.notification.NotificationPreferences;
import ru.wexside.notification.NotificationToast;
import ru.wexside.util.GuiDrawApi;

public final class NotificationCenter {
    private static final int MAX_VISIBLE = 5;
    private static final int MAX_RETAINED = 8;
    private static final float ROW_HEIGHT = 17.5f;
    private static final long DEFAULT_DURATION_MILLIS = 3000L;
    private final NotificationPreferences preferences;
    private final LinkedList<NotificationToast> notifications = new LinkedList();

    public NotificationCenter(NotificationPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
    }

    public void push(NotificationFactory factory) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread()) {
            client.execute(() -> this.push(factory));
            return;
        }
        if (client.player == null || !this.preferences.isEnabled() || !this.preferences.isCategoryVisible(factory.category())) {
            return;
        }
        NotificationToast incoming = factory.create(3000L);
        if (incoming.key() != null) {
            for (NotificationToast existing : this.notifications) {
                if (!Objects.equals(existing.key(), incoming.key())) continue;
                existing.updateContent(incoming.icon(), incoming.accentColor(), incoming.parts());
                this.playSound(factory.category());
                return;
            }
        }
        this.notifications.addLast(incoming);
        int active = 0;
        for (NotificationToast notification : this.notifications) {
            if (notification.isDismissing() || ++active <= 5) continue;
            notification.beginDismiss();
        }
        while (this.notifications.size() > 8) {
            this.notifications.removeFirst().remove();
        }
        this.playSound(factory.category());
    }

    public boolean accepts(NotificationCategory category) {
        return this.preferences.isEnabled() && this.preferences.isCategoryVisible(category);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void render(float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || this.notifications.isEmpty()) {
            return;
        }
        GuiDrawApi renderer = WexSideClient.getHudRenderer();
        if (renderer == null) {
            return;
        }
        Iterator iterator = this.notifications.iterator();
        int row = 0;
        float startY = (float)client.getWindow().getScaledHeight() * 0.5f + 20.0f * tickDelta;
        float centerX = (float)client.getWindow().getScaledWidth() * 0.5f;
        Matrix4f matrix = new Matrix4f().scale((float)client.getWindow().getScaleFactor());
        renderer.begin();
        try {
            while (iterator.hasNext()) {
                NotificationToast notification = (NotificationToast)iterator.next();
                notification.moveTo(startY + (float)row * 17.5f * tickDelta, tickDelta);
                if (notification.isExpired()) {
                    iterator.remove();
                    continue;
                }
                notification.render(renderer, matrix, centerX, tickDelta);
                ++row;
            }
        }
        finally {
            renderer.end();
        }
    }

    private void playSound(NotificationCategory category) {
        if (!this.preferences.isSoundEnabled(category)) {
            return;
        }
        MinecraftClient.getInstance().getSoundManager().play((SoundInstance)PositionedSoundInstance.ui((SoundEvent)((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value()), (float)Math.max(0.0f, Math.min(1.0f, this.preferences.soundVolume()))));
    }
}

