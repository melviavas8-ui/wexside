package ru.wexside.event;

import net.minecraft.client.util.math.MatrixStack;
import ru.wexside.event.Event;
import ru.wexside.event.HandRenderPhase;

public final class HandRenderEvent
implements Event {
    private final HandRenderPhase phase;
    private final MatrixStack matrices;
    private final float tickDelta;

    public HandRenderEvent(HandRenderPhase phase, MatrixStack matrices, float tickDelta) {
        this.phase = phase;
        this.matrices = matrices;
        this.tickDelta = tickDelta;
    }

    public HandRenderPhase phase() {
        return this.phase;
    }

    public MatrixStack matrices() {
        return this.matrices;
    }

    public float tickDelta() {
        return this.tickDelta;
    }
}

