package ru.wexside.misc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.ConsumableEspEffect;
import ru.wexside.misc.ConsumablesEspSettings;
import ru.wexside.misc.RadialConsumableEspRenderer;
import ru.wexside.misc.SnowballAreaEspRenderer;
import ru.wexside.server.FunTimeServerContext;
import ru.wexside.util.PlacementConsumableEspRenderer;

public final class ConsumablesEspRenderer {
    private final PlacementConsumableEspRenderer placementConsumableEspRenderer;
    private final SnowballAreaEspRenderer snowballAreaEspRenderer;
    static final double value = 1024.0;
    static final double process9 = 32.0;
    private final ConsumablesEspSettings consumablesEspSettings;
    private final RadialConsumableEspRenderer radialConsumableEspRenderer;
    private final List<ConsumableEspEffect> effects;

    public ConsumablesEspRenderer(ConsumablesEspSettings consumablesEspSettings) {
        this.consumablesEspSettings = consumablesEspSettings;
        this.placementConsumableEspRenderer = new PlacementConsumableEspRenderer(consumablesEspSettings);
        this.radialConsumableEspRenderer = new RadialConsumableEspRenderer(consumablesEspSettings);
        this.snowballAreaEspRenderer = new SnowballAreaEspRenderer(consumablesEspSettings);
        this.effects = List.of(this.placementConsumableEspRenderer, this.radialConsumableEspRenderer, this.snowballAreaEspRenderer);
    }

    private void renderOtherPlayerPreviews(WorldRenderEvent floatTypeEvent2, MinecraftClient mc, Set<UUID> set) {
        if (!this.consumablesEspSettings.isActive3()) {
            return;
        }
        for (PlayerEntity player : mc.world.getPlayers()) {
            ConsumableEspEffect callback26;
            Item iiIilIIilI2;
            if (player == mc.player || mc.player.squaredDistanceTo((Entity)player) > 1024.0 || !this.consumablesEspSettings.isOtherPlayerItemEnabled(iiIilIIilI2 = player.getMainHandStack().getItem()) || (callback26 = this.findEffectForItem(iiIilIIilI2)) == null) continue;
            this.clearOtherEffectsForPlayer(player.getUuid(), callback26);
            set.add(player.getUuid());
            callback26.renderOtherPlayerPreview(floatTypeEvent2, player, this.consumablesEspSettings.getOtherPlayerColor(iiIilIIilI2));
        }
    }

    public void setWorldRenderEvent(WorldRenderEvent floatTypeEvent2) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!this.consumablesEspSettings.isActive() || mc.player == null || mc.world == null) {
            this.update();
            return;
        }
        if (mc.isPaused() || !FunTimeServerContext.isConnected()) {
            this.update();
            return;
        }
        HashSet<UUID> hashSet = new HashSet<UUID>();
        this.renderLocalPlayerPreview(floatTypeEvent2, mc, hashSet);
        this.renderOtherPlayerPreviews(floatTypeEvent2, mc, hashSet);
        this.effects.forEach(callback26 -> callback26.setSet(hashSet));
        this.renderThrownSnowballPreviews(floatTypeEvent2, mc);
    }

    private void renderThrownSnowballPreviews(WorldRenderEvent floatTypeEvent2, MinecraftClient mc) {
        boolean bl;
        boolean bl2 = this.consumablesEspSettings.isActive2() && this.consumablesEspSettings.isLocalPlayerItemEnabled(Items.SNOWBALL);
        boolean bl3 = bl = this.consumablesEspSettings.isActive3() && this.consumablesEspSettings.isOtherPlayerItemEnabled(Items.SNOWBALL);
        if (!bl2 && !bl) {
            return;
        }
        for (Entity entity2 : mc.world.getEntities()) {
            PlayerEntity ownerPlayer;
            boolean bl4;
            SnowballEntity snowballEntity;
            if (!(entity2 instanceof SnowballEntity) || mc.player.squaredDistanceTo((Entity)(snowballEntity = (SnowballEntity)entity2)) > 1024.0) continue;
            Entity iIiiiilIiI2 = snowballEntity.getOwner();
            boolean bl5 = iIiiiilIiI2 == mc.player && bl2;
            boolean bl6 = bl4 = iIiiiilIiI2 instanceof PlayerEntity && (ownerPlayer = (PlayerEntity)iIiiiilIiI2) != mc.player && bl;
            if (!bl5 && !bl4) continue;
            this.snowballAreaEspRenderer.renderProjectileImpactArea(floatTypeEvent2, snowballEntity, bl5 ? this.consumablesEspSettings.getLocalPlayerColor(Items.SNOWBALL) : this.consumablesEspSettings.getOtherPlayerColor(Items.SNOWBALL));
        }
    }

    private void clearOtherEffectsForPlayer(UUID uUID, ConsumableEspEffect callback262) {
        this.effects.stream().filter(member3652 -> member3652 != callback262).forEach(callback26 -> callback26.setUUID(uUID));
    }

    public void update() {
        this.effects.forEach(ConsumableEspEffect::update);
    }

    private void renderLocalPlayerPreview(WorldRenderEvent floatTypeEvent2, MinecraftClient mc, Set<UUID> set) {
        if (!this.consumablesEspSettings.isActive2()) {
            return;
        }
        ItemStack stack = mc.player.getMainHandStack();
        if (mc.player.getItemCooldownManager().isCoolingDown(stack)) {
            return;
        }
        Item iiIilIIilI2 = stack.getItem();
        if (!this.consumablesEspSettings.isLocalPlayerItemEnabled(iiIilIIilI2)) {
            return;
        }
        ConsumableEspEffect callback26 = this.findEffectForItem(iiIilIIilI2);
        if (callback26 == null) {
            return;
        }
        this.clearOtherEffectsForPlayer(mc.player.getUuid(), callback26);
        set.add(mc.player.getUuid());
        callback26.renderLocalPlayerPreview(floatTypeEvent2, (PlayerEntity)mc.player, this.consumablesEspSettings.getLocalPlayerColor(iiIilIIilI2));
    }

    private ConsumableEspEffect findEffectForItem(Item iiIilIIilI2) {
        return this.effects.stream().filter(callback26 -> callback26.supportsItem(iiIilIIilI2)).findFirst().orElse(null);
    }
}

