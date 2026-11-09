package ru.wexside.event;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import ru.wexside.event.CancellableEvent;
import ru.wexside.event.Event;

public final class FirstPersonSwingTransformEvent
extends CancellableEvent
implements Event {
    private final MatrixStack matrices;
    private final float swingProgress;
    private final float equipProgress;
    private final Hand hand;

    public FirstPersonSwingTransformEvent(MatrixStack matrices, Hand hand, float swingProgress, float equipProgress) {
        this.matrices = matrices;
        this.hand = hand;
        this.swingProgress = swingProgress;
        this.equipProgress = equipProgress;
    }

    public MatrixStack getMatrices() {
        return this.matrices;
    }

    public Hand getHand() {
        return this.hand;
    }

    public float getEquipProgress() {
        return this.equipProgress;
    }

    public float getSwingProgress() {
        return this.swingProgress;
    }
}

