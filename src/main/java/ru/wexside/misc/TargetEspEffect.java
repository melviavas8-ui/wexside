package ru.wexside.misc;

import ru.wexside.event.EntityAttackEvent;
import ru.wexside.event.WorldRenderEvent;

public interface TargetEspEffect {
    public void setEntityAttackEvent(EntityAttackEvent var1);

    public void setWorldRenderEvent(WorldRenderEvent var1);

    public void update();

    default public boolean isActive() {
        return true;
    }
}

