package ru.wexside.util.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;

public final class NpcDetector {
    private static final int REFRESH_INTERVAL_TICKS = 20;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Set<UUID> npcIds = new HashSet<UUID>();
    private Object currentWorld;
    private int ticksUntilRefresh;

    public NpcDetector(EventBus eventBus) {
        eventBus.subscribe(ClientTickEvent.class, ignored -> this.tick());
    }

    public boolean isNpc(LivingEntity entity) {
        return entity instanceof PlayerEntity && this.npcIds.contains(entity.getUuid());
    }

    private void tick() {
        if (this.client.player == null || this.client.world == null || this.client.getNetworkHandler() == null) {
            this.reset();
            return;
        }
        if (this.currentWorld != this.client.world) {
            this.currentWorld = this.client.world;
            this.npcIds.clear();
            this.ticksUntilRefresh = 0;
        }
        if (this.ticksUntilRefresh-- > 0) {
            return;
        }
        this.ticksUntilRefresh = 20;
        HashSet<UUID> listedPlayers = new HashSet<UUID>();
        for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
            listedPlayers.add(entry.getProfile().id());
        }
        this.npcIds.clear();
        for (PlayerEntity player : this.client.world.getPlayers()) {
            if (player == this.client.player || listedPlayers.contains(player.getUuid())) continue;
            this.npcIds.add(player.getUuid());
        }
    }

    private void reset() {
        this.currentWorld = null;
        this.npcIds.clear();
        this.ticksUntilRefresh = 0;
    }
}

