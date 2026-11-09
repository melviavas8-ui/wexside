package ru.wexside.event;

import net.minecraft.client.gui.DrawContext;
import ru.wexside.event.Event;

public class TooltipRenderEvent
implements Event {
    private final DrawContext context;
    private final int mouseY;
    private final int mouseX;

    public int getMouseY() {
        return this.mouseY;
    }

    public int getMouseX() {
        return this.mouseX;
    }

    public DrawContext getDrawContext() {
        return this.context;
    }

    public TooltipRenderEvent(DrawContext context, int mouseX, int mouseY) {
        this.context = context;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}

