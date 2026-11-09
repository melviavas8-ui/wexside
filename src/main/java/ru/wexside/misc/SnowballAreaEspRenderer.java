package ru.wexside.misc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.ConsumableEspEffect;
import ru.wexside.misc.ConsumablesEspSettings;
import ru.wexside.misc.PlacementPreview;
import ru.wexside.render.world.ConsumableAreaRenderer;
import ru.wexside.render.world.SnowballImpactPredictor;
import ru.wexside.setting.ColorSetting;
import ru.wexside.util.ColorUtils;

public final class SnowballAreaEspRenderer
implements ConsumableEspEffect {
    private final Map<UUID, PlacementPreview> map2 = new HashMap<UUID, PlacementPreview>();
    static final int slot = 7;
    private final ConsumablesEspSettings consumablesEspSettings;

    public SnowballAreaEspRenderer(ConsumablesEspSettings consumablesEspSettings) {
        this.consumablesEspSettings = consumablesEspSettings;
    }

    @Override
    public void renderOtherPlayerPreview(WorldRenderEvent floatTypeEvent2, PlayerEntity player, ColorSetting colorSetting) {
        this.renderPlayerImpactPreview(player, colorSetting);
    }

    @Override
    public void renderProjectileImpactArea(WorldRenderEvent floatTypeEvent2, SnowballEntity snowballEntity, ColorSetting colorSetting) {
        if (snowballEntity.isRemoved()) {
            return;
        }
        Vec3d vec = SnowballImpactPredictor.predict(snowballEntity);
        if (vec == null) {
            return;
        }
        BlockPos pos = BlockPos.ofFloored((Position)vec);
        int n = 7;
        Box box = new Box((double)(pos.getX() - n), (double)pos.getY(), (double)(pos.getZ() - n), (double)(pos.getX() + n + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + n + 1));
        this.renderImpactBox(box, colorSetting, this.getLongType());
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
        return iiIilIIilI2 == Items.SNOWBALL;
    }

    private long getLongType() {
        return System.currentTimeMillis();
    }

    private void renderImpactBox(Box box, ColorSetting colorSetting, long l) {
        ConsumableAreaRenderer.renderBox(box, ColorUtils.multiplyAlpha(ConsumablesEspSettings.sampleEffectColor(colorSetting, 0.0f), 0.2f), ColorUtils.multiplyAlpha(ConsumablesEspSettings.sampleEffectColor(colorSetting, 1.0f), 0.2f), ColorUtils.multiplyAlpha(ConsumablesEspSettings.sampleEffectColor(colorSetting, 0.0f), 0.65f), ColorUtils.multiplyAlpha(ConsumablesEspSettings.sampleEffectColor(colorSetting, 1.0f), 0.65f), l);
    }

    @Override
    public void renderLocalPlayerPreview(WorldRenderEvent floatTypeEvent2, PlayerEntity player, ColorSetting colorSetting) {
        this.renderPlayerImpactPreview(player, colorSetting);
    }

    private void renderPlayerImpactPreview(PlayerEntity player, ColorSetting colorSetting) {
        if (!this.supportsItem(player.getMainHandStack().getItem())) {
            this.setUUID(player.getUuid());
            return;
        }
        Vec3d vec = SnowballImpactPredictor.predict(player);
        if (vec == null) {
            this.update();
            return;
        }
        BlockPos pos = BlockPos.ofFloored((Position)vec);
        int n = 3;
        Box box = new Box((double)(pos.getX() - n), (double)pos.getY(), (double)(pos.getZ() - n), (double)(pos.getX() + n + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + n + 1));
        this.map2.computeIfAbsent(player.getUuid(), uUID -> new PlacementPreview(pos, box)).update(pos, box);
        Box box2 = this.map2.get(player.getUuid()).box();
        if (box2 != null) {
            this.renderImpactBox(box2, colorSetting, this.getLongType());
        }
    }
}

