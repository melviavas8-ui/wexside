package ru.wexside.recovery.warden;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;

/** Complete IiIlIiIilllilil: message detection and one-shot death/killer consumption. */
public final class WardenDeathTracker {
    private static final String DEATH = "Помянем. Вы погибли";
    private static final String KILLER = "Вас убил ";
    private final BooleanSupplier captureKiller;
    private volatile boolean death;
    private volatile String killer;

    /** The supplier's original module association is unestablished and must be supplied. */
    public WardenDeathTracker(EventBus bus, BooleanSupplier captureKiller) {
        this.captureKiller = Objects.requireNonNull(captureKiller);
        Objects.requireNonNull(bus).subscribe(IncomingPacketEvent.class, event -> {
            if (event.getPacket() instanceof GameMessageS2CPacket message) {
                acceptMessage(message.content().getString());
            }
        });
    }

    public void acceptMessage(String message) {
        if (!message.contains(DEATH)) {
            return;
        }
        death = true;
        int offset = message.indexOf(KILLER);
        if (offset < 0 || !captureKiller.getAsBoolean()) {
            return;
        }
        String suffix = message.substring(offset + KILLER.length());
        int comma = suffix.indexOf(',');
        String name = (comma < 0 ? suffix : suffix.substring(0, comma)).trim();
        if (!name.isEmpty()) {
            killer = name;
        }
    }

    public boolean consumeDeath() {
        if (!death) {
            return false;
        }
        death = false;
        return true;
    }

    public String consumeKiller() {
        String value = killer;
        killer = null;
        return value;
    }
}
