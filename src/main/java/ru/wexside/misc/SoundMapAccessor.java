package ru.wexside.misc;

import java.util.Map;
import net.minecraft.client.sound.SoundInstance;

public interface SoundMapAccessor {
    public Map<SoundInstance, ?> getMap();
}

