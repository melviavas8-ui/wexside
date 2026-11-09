package ru.wexside.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.wexside.misc.AttackOptions;
import ru.wexside.misc.HitCooldown;
import ru.wexside.misc.Inventories;
import ru.wexside.misc.MaceCheck;
import ru.wexside.misc.ReachHelper;
import ru.wexside.misc.ShieldDesync;
import ru.wexside.util.Angle;
import ru.wexside.util.CriticalsHandler;

public class AuraProcessor {
    private final HitCooldown hitCooldown = new HitCooldown();
    private final CriticalsHandler criticals;
    private boolean sprintDelayPending;
    private final MaceCheck maceCheck;
    private final ShieldDesync shieldDesync = new ShieldDesync();

    public AuraProcessor(CriticalsHandler criticals) {
        this.maceCheck = new MaceCheck();
        this.criticals = criticals;
    }

    public boolean attack(LivingEntity target, AttackOptions options) {
        return this.executeAttack(target, options);
    }

    public void tick() {
        this.clearSprintDelay();
    }

    public boolean executeAttack(LivingEntity entity2, AttackOptions iliiiillII2) {
        int n;
        if (entity2 == null || this.sprintDelayPending) {
            return false;
        }
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null) {
            return false;
        }
        if (!this.canAttackNow(entity2, iliiiillII2)) {
            return false;
        }
        this.shieldDesync.setMode(iliiiillII2.sprintResetMode());
        boolean bl = player2.isSprinting() && !player2.isUsingItem();
        boolean bl2 = bl;
        if (iliiiillII2.sprintResetEnabled()) {
            this.shieldDesync.prepareForAttack();
            if (this.shieldDesync.delaysAttackUntilSprintStops() && bl) {
                this.sprintDelayPending = true;
                return false;
            }
        }
        if (iliiiillII2.desyncShield()) {
            this.releaseShieldUse(player2);
        }
        int n2 = -1;
        if (iliiiillII2.breakShield() && entity2.isBlocking()) {
            n = Inventories.findAxeSlot();
            int n3 = player2.getInventory().getSelectedSlot();
            if (n != -1 && n != n3) {
                n2 = n3;
                player2.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(n));
            }
        }
        n = iliiiillII2.accuracyPercent() < 100.0 && Math.random() * 100.0 > iliiiillII2.accuracyPercent() ? 1 : 0;
        int n4 = n;
        if (n != 0) {
            player2.swingHand(Hand.MAIN_HAND);
        } else {
            MinecraftClient.getInstance().interactionManager.attackEntity((PlayerEntity)player2, (Entity)entity2);
            player2.swingHand(Hand.MAIN_HAND);
        }
        if (n2 != -1) {
            player2.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(n2));
        }
        if (iliiiillII2.sprintResetEnabled() && this.shieldDesync.sendsSprintPackets()) {
            this.shieldDesync.restoreSprint();
        }
        long l = iliiiillII2.legacyCombat() ? Math.max(50L, 1000L / (long)Math.max(1, iliiiillII2.clicksPerSecond())) : 500L;
        this.hitCooldown.setLongType(l);
        return n == 0;
    }

    public HitCooldown getHitCooldown() {
        return this.hitCooldown;
    }

    public void clearSprintDelay() {
        this.sprintDelayPending = false;
    }

    public MaceCheck getMaceCheck() {
        return this.maceCheck;
    }

    private void releaseShieldUse(ClientPlayerEntity player2) {
        if (!player2.getOffHandStack().isOf(Items.SHIELD)) {
            return;
        }
        if (!player2.isUsingItem()) {
            return;
        }
        player2.networkHandler.sendPacket((Packet)new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
    }

    public ShieldDesync getShieldDesync() {
        return this.shieldDesync;
    }

    private boolean canAttackNow(LivingEntity entity2, AttackOptions iliiiillII2) {
        if (iliiiillII2.raycastEnabled() && !this.rayHitsTarget(entity2, iliiiillII2.lookAngle(), iliiiillII2.range(), iliiiillII2.allowsThroughWalls())) {
            return false;
        }
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 != null && this.maceCheck.hasMaceInMainHand(player2)) {
            return this.maceCheck.canPerformMaceAttack(player2, this.hitCooldown, iliiiillII2.maceFallDistance(), iliiiillII2.legacyCombat());
        }
        if (!this.hitCooldown.isAttackReady(iliiiillII2.legacyCombat())) {
            return false;
        }
        return this.criticals.passesCriticalRequirement(iliiiillII2.criticalsOnly(), iliiiillII2.raycastMode());
    }

    private boolean rayHitsTarget(LivingEntity entity2, Angle angle, float f, boolean bl) {
        if (angle == null) {
            return false;
        }
        return ReachHelper.raycastEntity((Entity)entity2, angle, f, bl) == entity2;
    }
}

