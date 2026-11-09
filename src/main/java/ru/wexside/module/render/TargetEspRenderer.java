package ru.wexside.module.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ru.wexside.module.render.TargetESPModule;

public abstract class TargetEspRenderer {
    protected final MinecraftClient client() {
        return MinecraftClient.getInstance();
    }

    protected final boolean canRender(MinecraftClient client) {
        return client != null && client.player != null && client.world != null && TargetESPModule.getInstance() != null && TargetESPModule.getInstance().getCurrentTarget() != null;
    }

    protected final <T extends Entity> T target(MinecraftClient client, Class<T> type) {
        TargetESPModule module = TargetESPModule.getInstance();
        LivingEntity target = module == null ? null : module.getCurrentTarget();
        return (T)(type.isInstance(target) ? (Entity)type.cast(target) : null);
    }

    protected final Vec3d interpolatedPosition(Entity entity, float tickDelta) {
        return entity.getLerpedPos(tickDelta);
    }

    protected final Vec3d cameraPosition() {
        MinecraftClient client = this.client();
        return client.gameRenderer.getCamera().getCameraPos();
    }

    protected final int primaryColor() {
        TargetESPModule module = TargetESPModule.getInstance();
        return module == null ? -1 : module.getPrimaryColor();
    }

    protected final int secondaryColor() {
        TargetESPModule module = TargetESPModule.getInstance();
        return module == null ? -1 : module.getSecondaryColor();
    }
}

