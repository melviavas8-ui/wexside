package ru.wexside.event;

import net.minecraft.network.packet.Packet;
import ru.wexside.event.CancellableEvent;
import ru.wexside.event.Event;

public class OutgoingPacketEvent
extends CancellableEvent
implements Event {
    private final Packet<?> packet2;

    public OutgoingPacketEvent(Packet<?> packet3) {
        this.packet2 = packet3;
    }

    public Packet<?> getPacket() {
        return this.packet2;
    }
}

