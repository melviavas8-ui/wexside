package ru.wexside.event;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import ru.wexside.event.Event;

public record HudRenderEvent(DrawContext context, RenderTickCounter tickCounter) implements Event
{
}

