package ru.wexside.misc;

import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundEvent;

public final class StartupMusicPlayer {
    private final SoundEvent music;
    private final Consumer<String> statusSink;
    private int readyTicks;
    private boolean started;

    public StartupMusicPlayer(SoundEvent music, Consumer<String> statusSink) {
        this.music = music;
        this.statusSink = statusSink;
    }

    public void tick(MinecraftClient client) {
        if (this.started || client.getSoundManager().get(this.music.id()) == null) {
            return;
        }
        if (++this.readyTicks < 10) {
            return;
        }
        SoundSystem.PlayResult result = client.getSoundManager().play((SoundInstance)PositionedSoundInstance.ui((SoundEvent)this.music, (float)1.0f, (float)0.8f));
        if (result == SoundSystem.PlayResult.NOT_STARTED) {
            this.readyTicks = 0;
            return;
        }
        this.started = true;
        this.statusSink.accept("Startup music playback: " + String.valueOf(result));
    }
}

