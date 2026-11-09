package ru.wexside.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;

public class HotbarSlotLock {
    private long expiresAtMillis;
    private int lockedSlot = -1;

    public HotbarSlotLock(EventBus eventBus) {
        eventBus.subscribe(ClientTickEvent.class, ignored -> this.expireLock());
        eventBus.subscribe(IncomingPacketEvent.class, this::onIncomingPacket);
    }

    public boolean isLocked() {
        return this.lockedSlot != -1;
    }

    public void lockSlot(int n, long l) {
        if (n < 0 || n > 8) {
            return;
        }
        this.lockedSlot = n;
        this.expiresAtMillis = System.currentTimeMillis() + Math.max(50L, l);
    }

    private void onIncomingPacket(IncomingPacketEvent gameEvent5) {
        if (this.lockedSlot == -1) {
            return;
        }
        if (!(gameEvent5.getPacket() instanceof UpdateSelectedSlotS2CPacket)) {
            return;
        }
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null) {
            return;
        }
        PlayerInventory inv = player2.getInventory();
        if (inv.getSelectedSlot() != this.lockedSlot) {
            inv.setSelectedSlot(this.lockedSlot);
        }
    }

    public void clearLock() {
        this.lockedSlot = -1;
        this.expiresAtMillis = 0L;
    }

    private void expireLock() {
        if (this.lockedSlot == -1) {
            return;
        }
        if (System.currentTimeMillis() >= this.expiresAtMillis) {
            this.clearLock();
        }
    }
}

