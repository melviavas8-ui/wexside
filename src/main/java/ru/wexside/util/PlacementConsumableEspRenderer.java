package ru.wexside.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.ConsumableEspEffect;
import ru.wexside.misc.ConsumablesEspSettings;
import ru.wexside.misc.PlacementPreview;
import ru.wexside.render.world.ConsumableAreaRenderer;
import ru.wexside.setting.ColorSetting;
import ru.wexside.util.ColorUtils;

public final class PlacementConsumableEspRenderer
implements ConsumableEspEffect {
    private final Map<UUID, PlacementPreview> map2 = new HashMap<UUID, PlacementPreview>();
    static final int slot = 5;
    private final ConsumablesEspSettings consumablesEspSettings;
    static final float value = 22.5f;
    static final int slot2 = 3;
    static final int slot3 = 3;
    static final int slot4 = 5;

    public PlacementConsumableEspRenderer(ConsumablesEspSettings consumablesEspSettings) {
        this.consumablesEspSettings = consumablesEspSettings;
    }

    @Override
    public void renderOtherPlayerPreview(WorldRenderEvent floatTypeEvent2, PlayerEntity player, ColorSetting colorSetting) {
        this.renderHeldConsumable(player, colorSetting);
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

    private void renderScrapPlacement(UUID uUID, PlayerEntity player, BlockPos pos, ColorSetting colorSetting, long l) {
        int n;
        int n2;
        boolean bl;
        int n3 = 2;
        float f = player.getPitch();
        float f2 = this.positiveYaw(player.getYaw());
        boolean bl2 = this.isDiagonalYaw(f2);
        if (f < -45.0f) {
            bl = true;
            n2 = pos.getY() + 4;
            n = n2 - 1;
        } else if (f > 45.0f) {
            bl = true;
            n2 = pos.getY() - 1;
            n = n2 - 1;
        } else {
            bl = false;
            n2 = -1;
            n = 4;
        }
        if (bl) {
            Box box = this.orderedBox(pos.getX() - n3, n2, pos.getZ() - n3, pos.getX() + n3 + 1, n, pos.getZ() + n3 + 1);
            this.updatePlayerPreview(uUID, pos, box);
            Box box2 = this.map2.get(uUID).box();
            if (box2 != null) {
                this.renderPreviewBox(box2, colorSetting, l);
            }
            return;
        }
        if (bl2) {
            this.renderDiagonalScrapPlacement(uUID, player, pos, f2, n2, n, n3, colorSetting, l);
            return;
        }
        Direction process17 = player.getHorizontalFacing();
        BlockPos pos2 = pos.offset(process17, 3);
        int n4 = pos2.getY() + n2;
        int n5 = pos2.getY() + n;
        Box box3 = process17.getAxis() == Direction.Axis.X ? this.orderedBox(pos2.getX(), n4, pos2.getZ() - n3, pos2.getX() + 1, n5, pos2.getZ() + n3 + 1) : this.orderedBox(pos2.getX() - n3, n4, pos2.getZ(), pos2.getX() + n3 + 1, n5, pos2.getZ() + 1);
        this.updatePlayerPreview(uUID, pos2, box3);
        Box box4 = this.map2.get(uUID).box();
        if (box4 != null) {
            this.renderPreviewBox(box4, colorSetting, l);
        }
    }

    @Override
    public boolean supportsItem(Item iiIilIIilI2) {
        return iiIilIIilI2 == Items.NETHERITE_SCRAP || iiIilIIilI2 == Items.PHANTOM_MEMBRANE;
    }

    private float positiveYaw(float f) {
        float f2 = f % 360.0f;
        if (f2 < 0.0f) {
            f2 += 360.0f;
        }
        return f2;
    }

    private void renderPreviewBox(Box box, ColorSetting colorSetting, long l) {
        ConsumableAreaRenderer.renderBox(box, ColorUtils.multiplyAlpha(ConsumablesEspSettings.sampleEffectColor(colorSetting, 0.0f), 0.2f), ColorUtils.multiplyAlpha(ConsumablesEspSettings.sampleEffectColor(colorSetting, 1.0f), 0.2f), ColorUtils.multiplyAlpha(ConsumablesEspSettings.sampleEffectColor(colorSetting, 0.0f), 0.65f), ColorUtils.multiplyAlpha(ConsumablesEspSettings.sampleEffectColor(colorSetting, 1.0f), 0.65f), l);
    }

    private void renderDiagonalScrapPlacement(UUID uUID, PlayerEntity player, BlockPos pos, float f, int n, int n2, int n3, ColorSetting colorSetting, long l) {
        BlockPos pos2 = pos.offset(player.getHorizontalFacing(), 0);
        int n4 = pos2.getY() + n;
        int n5 = pos2.getY() + n2;
        int n6 = 0;
        int n7 = 0;
        if (f < 67.5f) {
            n6 = 1;
            n7 = 1;
            pos2 = pos2.add(-2, 0, 2);
        } else if (f >= 112.5f && f < 157.5f) {
            n6 = -1;
            n7 = 1;
            pos2 = pos2.add(-2, 0, -2);
        } else if (f >= 202.5f && f < 247.5f) {
            n6 = -1;
            n7 = -1;
            pos2 = pos2.add(2, 0, -2);
        } else if (f >= 292.5f || f < 337.5f) {
            n6 = 1;
            n7 = -1;
            pos2 = pos2.add(2, 0, 2);
        }
        this.updatePlayerPreview(uUID, pos2, null);
        Vec3d vec = this.map2.get(uUID).center();
        if (vec == null) {
            return;
        }
        for (int i = -n3; i <= n3; ++i) {
            double d = vec.x + (double)(i * n6);
            double d2 = vec.z + (double)(i * n7);
            Box box = this.orderedBox(d, n4, d2, d + 1.0, n5, d2 + 1.0);
            this.renderPreviewBox(box, colorSetting, l);
        }
    }

    private void updatePlayerPreview(UUID uUID2, BlockPos pos, Box box) {
        this.map2.computeIfAbsent(uUID2, uUID -> new PlacementPreview(pos, box)).update(pos, box);
    }

    @Override
    public void renderLocalPlayerPreview(WorldRenderEvent floatTypeEvent2, PlayerEntity player, ColorSetting colorSetting) {
        this.renderHeldConsumable(player, colorSetting);
    }

    private void renderHeldConsumable(PlayerEntity player, ColorSetting colorSetting) {
        boolean bl;
        Item iiIilIIilI2 = player.getMainHandStack().getItem();
        boolean bl2 = iiIilIIilI2 == Items.NETHERITE_SCRAP;
        boolean bl3 = bl = iiIilIIilI2 == Items.PHANTOM_MEMBRANE;
        if (!bl2 && !bl) {
            this.setUUID(player.getUuid());
            return;
        }
        long l = System.currentTimeMillis();
        BlockPos pos = player.getBlockPos();
        if (bl) {
            this.renderMembranePlacement(player.getUuid(), pos, colorSetting, l);
            return;
        }
        this.renderScrapPlacement(player.getUuid(), player, pos, colorSetting, l);
    }

    private boolean isDiagonalYaw(float f) {
        for (float f2 : new float[]{45.0f, 135.0f, 225.0f, 315.0f}) {
            if (!(Math.abs(f - f2) < 22.5f)) continue;
            return true;
        }
        return false;
    }

    private Box orderedBox(double d, double d2, double d3, double d4, double d5, double d6) {
        return new Box(Math.min(d, d4), Math.min(d2, d5), Math.min(d3, d6), Math.max(d, d4), Math.max(d2, d5), Math.max(d3, d6));
    }

    private void renderMembranePlacement(UUID uUID, BlockPos pos, ColorSetting colorSetting, long l) {
        int n = 1;
        Box box = this.orderedBox(pos.getX() - n, pos.getY() - n + 1, pos.getZ() - n, pos.getX() + n + 1, pos.getY() + n + 2, pos.getZ() + n + 1);
        this.updatePlayerPreview(uUID, pos, box);
        Box box2 = this.map2.get(uUID).box();
        if (box2 != null) {
            this.renderPreviewBox(box2, colorSetting, l);
        }
    }
}

