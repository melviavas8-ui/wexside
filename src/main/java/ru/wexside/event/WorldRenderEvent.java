package ru.wexside.event;

import net.minecraft.client.util.math.MatrixStack;
import ru.wexside.event.Event;

public final class WorldRenderEvent
implements Event {
    private final float tickDelta;
    private final MatrixStack matrices;

    public WorldRenderEvent(MatrixStack matrices, float tickDelta) {
        this.matrices = matrices;
        this.tickDelta = tickDelta;
    }

    public MatrixStack getMatrices() {
        return this.matrices;
    }

    @Deprecated
    public float getFloatType() {
        return this.tickDelta;
    }

    public float getTickDelta() {
        return this.tickDelta;
    }
}

