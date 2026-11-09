package ru.wexside.misc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.ConsumableEspEffect;
import ru.wexside.misc.ConsumablesEspSettings;
import ru.wexside.misc.PlacementPreview;
import ru.wexside.render.world.ConsumableAreaRenderer;
import ru.wexside.setting.ColorSetting;
import ru.wexside.util.ColorUtils;

public final class RadialConsumableEspRenderer
implements ConsumableEspEffect {
    private final Map<UUID, PlacementPreview> map2 = new HashMap<UUID, PlacementPreview>();
    private final ConsumablesEspSettings consumablesEspSettings;
    static final int slot = 57;
    static final double value = 1.0;
    static final double value2 = 9.0;

    public RadialConsumableEspRenderer(ConsumablesEspSettings consumablesEspSettings) {
        this.consumablesEspSettings = consumablesEspSettings;
    }

    @Override
    public void renderOtherPlayerPreview(WorldRenderEvent floatTypeEvent2, PlayerEntity player, ColorSetting colorSetting) {
        this.renderPlayerRadius(player, colorSetting);
    }

    @Override
    public void update() {
        this.map2.clear();
    }

    @Override
    public void setSet(Set<UUID> set) {
        this.map2.keySet().removeIf(uUID -> !set.contains(uUID));
    }

    @Override
    public void setUUID(UUID uUID) {
        this.map2.remove(uUID);
    }

    @Override
    public boolean supportsItem(Item iiIilIIilI2) {
        return iiIilIIilI2 == Items.FIREWORK_STAR || iiIilIIilI2 == Items.SUGAR;
    }

    @Override
    public void renderLocalPlayerPreview(WorldRenderEvent floatTypeEvent2, PlayerEntity player, ColorSetting colorSetting) {
        this.renderPlayerRadius(player, colorSetting);
    }

    private void renderPlayerRadius(PlayerEntity player, ColorSetting colorSetting) {
        boolean bl;
        Item iiIilIIilI2 = player.getMainHandStack().getItem();
        boolean bl2 = iiIilIIilI2 == Items.FIREWORK_STAR;
        boolean bl3 = bl = iiIilIIilI2 == Items.SUGAR;
        if (!bl2 && !bl) {
            this.setUUID(player.getUuid());
            return;
        }
        BlockPos pos = player.getBlockPos();
        this.map2.computeIfAbsent(player.getUuid(), uUID -> new PlacementPreview(pos, null)).update(pos, null);
        Vec3d vec = this.map2.get(player.getUuid()).center();
        if (vec == null) {
            return;
        }
        ConsumableAreaRenderer.renderCylinder(vec.x, vec.y, vec.z, 9.0, 1.0, ColorUtils.multiplyAlpha(ConsumablesEspSettings.sampleEffectColor(colorSetting, 0.0f), 0.2f), ColorUtils.multiplyAlpha(ConsumablesEspSettings.sampleEffectColor(colorSetting, 1.0f), 0.2f), ColorUtils.multiplyAlpha(ConsumablesEspSettings.sampleEffectColor(colorSetting, 0.0f), 0.65f), ColorUtils.multiplyAlpha(ConsumablesEspSettings.sampleEffectColor(colorSetting, 1.0f), 0.65f), System.currentTimeMillis());
    }
}

