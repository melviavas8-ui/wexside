package ru.wexside.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.wexside.misc.HandledScreenAccessor;

@Mixin(value={HandledScreen.class})
public abstract class HandledScreenAccessorMixin
implements HandledScreenAccessor {
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    protected int backgroundWidth;
    @Shadow
    protected int backgroundHeight;
    @Shadow
    protected Slot focusedSlot;

    @Shadow
    protected abstract void drawBackground(DrawContext var1, float var2, int var3, int var4);

    @Override
    public int getContainerX() {
        return this.x;
    }

    @Override
    public int getContainerY() {
        return this.y;
    }

    @Override
    public int getContainerWidth() {
        return this.backgroundWidth;
    }

    @Override
    public int getContainerHeight() {
        return this.backgroundHeight;
    }

    @Override
    public Slot getFocusedSlot() {
        return this.focusedSlot;
    }

    @Override
    public void drawContainerBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        this.drawBackground(context, delta, mouseX, mouseY);
    }
}

