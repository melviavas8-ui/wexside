package ru.wexside.event;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import ru.wexside.event.CancellableEvent;
import ru.wexside.event.Event;

public final class FirstPersonItemTransformEvent
extends CancellableEvent
implements Event {
    private final MatrixStack matrices;
    private final ItemStack stack;
    private float scale = 1.0f;
    private final Hand hand;

    public FirstPersonItemTransformEvent(MatrixStack matrices2, ItemStack stack, Hand illlliiIiI2) {
        this.matrices = matrices2;
        this.stack = stack;
        this.hand = illlliiIiI2;
    }

    public Hand getHand() {
        return this.hand;
    }

    public float getScale() {
        return this.scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public MatrixStack getMatrices() {
        return this.matrices;
    }
}

