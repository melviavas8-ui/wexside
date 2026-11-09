package ru.wexside.misc;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.hud.ClientBossBar;

public interface BossBarMapAccessor {
    public Map<UUID, ClientBossBar> getMap();
}

