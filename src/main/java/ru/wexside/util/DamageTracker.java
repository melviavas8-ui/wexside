package ru.wexside.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.world.World;
import ru.wexside.event.DamageEvent;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.misc.ElapsedTimer;

public final class DamageTracker {
    private static final byte HURT_STATUS = 2;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final ElapsedTimer confirmationTimer = new ElapsedTimer();
    private boolean damageConfirmed;
    private boolean explosionPending;
    private boolean directDamagePending;
    private boolean projectileDamagePending;
    private boolean environmentalDamagePending;

    public boolean consumeAfter(long delayMillis) {
        if (this.damageConfirmed && this.confirmationTimer.hasElapsed(delayMillis)) {
            this.damageConfirmed = false;
            this.confirmationTimer.update();
            return true;
        }
        if (!this.damageConfirmed) {
            this.confirmationTimer.update();
        }
        return false;
    }

    public boolean isDamageConfirmed() {
        return this.damageConfirmed;
    }

    public void onIncomingPacket(IncomingPacketEvent event) {
        EntityStatusS2CPacket statusPacket;
        ClientPlayerEntity player = this.client.player;
        if (player == null || this.client.world == null || this.hasPeriodicHealthEffect(player)) {
            return;
        }
        boolean damageWasPending = this.hasPendingDamage();
        Packet<?> packet = event.getPacket();
        if (packet instanceof ExplosionS2CPacket) {
            this.explosionPending = true;
        }
        if (!damageWasPending && packet instanceof EntityStatusS2CPacket && (statusPacket = (EntityStatusS2CPacket)packet).getStatus() == 2 && statusPacket.getEntity((World)this.client.world) == player) {
            this.damageConfirmed = true;
        } else if (damageWasPending && player.hurtTime > 0) {
            this.damageConfirmed = false;
            this.clearPendingDamage();
        }
    }

    public void onDamage(DamageEvent event) {
        switch (event.type()) {
            case DIRECT: {
                this.directDamagePending = true;
                break;
            }
            case PROJECTILE: {
                this.projectileDamagePending = true;
                break;
            }
            case ENVIRONMENTAL: {
                this.environmentalDamagePending = true;
            }
        }
        this.damageConfirmed = false;
    }

    public void reset() {
        this.damageConfirmed = false;
        this.clearPendingDamage();
        this.confirmationTimer.update();
    }

    private boolean hasPeriodicHealthEffect(ClientPlayerEntity player) {
        return player.hasStatusEffect(StatusEffects.POISON) || player.hasStatusEffect(StatusEffects.WITHER) || player.hasStatusEffect(StatusEffects.REGENERATION);
    }

    private boolean hasPendingDamage() {
        return this.explosionPending || this.directDamagePending || this.projectileDamagePending || this.environmentalDamagePending;
    }

    private void clearPendingDamage() {
        this.explosionPending = false;
        this.directDamagePending = false;
        this.projectileDamagePending = false;
        this.environmentalDamagePending = false;
    }
}

