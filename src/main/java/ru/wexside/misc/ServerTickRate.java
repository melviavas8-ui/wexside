package ru.wexside.misc;

import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;

public class ServerTickRate {
    private float ticksPerSecond;
    private long lastTimeUpdateMillis;
    static final int slot = 20;
    static final float value2 = 20.0f;
    static final float value3 = 0.85f;

    public ServerTickRate(EventBus eventBus) {
        eventBus.subscribe(IncomingPacketEvent.class, this::onIncomingPacket);
    }

    public float scaleForTickRate(float f) {
        if (this.ticksPerSecond <= 0.0f) {
            return f;
        }
        return f * (20.0f / this.ticksPerSecond);
    }

    private void onIncomingPacket(IncomingPacketEvent gameEvent5) {
        long l;
        if (!(gameEvent5.getPacket() instanceof WorldTimeUpdateS2CPacket)) {
            return;
        }
        long l2 = System.currentTimeMillis();
        if (this.lastTimeUpdateMillis > 0L && (l = l2 - this.lastTimeUpdateMillis) > 0L && l < 5000L) {
            float f = 20000.0f / (float)l;
            if (f > 22.0f) {
                f = 22.0f;
            }
            if (f < 1.0f) {
                f = 1.0f;
            }
            this.ticksPerSecond = this.ticksPerSecond * 0.85f + f * 0.14999998f;
        }
        this.lastTimeUpdateMillis = l2;
    }

    public float getTicksPerSecond() {
        return this.ticksPerSecond;
    }
}

