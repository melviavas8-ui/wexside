package ru.wexside.misc;

import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.setting.ColorSetting;

public interface ConsumableEspEffect {
    default public void renderOtherPlayerPreview(WorldRenderEvent floatTypeEvent2, PlayerEntity player, ColorSetting colorSetting) {
        this.renderLocalPlayerPreview(floatTypeEvent2, player, colorSetting);
    }

    default public void renderProjectileImpactArea(WorldRenderEvent floatTypeEvent2, SnowballEntity snowballEntity, ColorSetting colorSetting) {
        this.onProjectileRender(floatTypeEvent2, snowballEntity);
    }

    public void update();

    default public void setSet(Set<UUID> set) {
    }

    default public void setUUID(UUID uUID) {
    }

    public boolean supportsItem(Item var1);

    default public void onProjectileRender(WorldRenderEvent floatTypeEvent2, SnowballEntity snowballEntity) {
    }

    public void renderLocalPlayerPreview(WorldRenderEvent var1, PlayerEntity var2, ColorSetting var3);
}

