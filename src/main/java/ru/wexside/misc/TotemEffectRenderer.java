package ru.wexside.misc;

import ru.wexside.event.TotemPopEvent;
import ru.wexside.event.WorldRenderEvent;

public interface TotemEffectRenderer {
    public void renderWorld(WorldRenderEvent var1);

    default public void render(WorldRenderEvent event) {
        this.renderWorld(event);
    }

    public void setTotemPopEvent(TotemPopEvent var1);

    public void clearHolograms();
}

