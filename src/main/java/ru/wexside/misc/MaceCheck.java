package ru.wexside.misc;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import ru.wexside.misc.HitCooldown;
import ru.wexside.misc.ShieldBreaker;

public final class MaceCheck {
    public boolean isHoldingMace(ClientPlayerEntity player) {
        return this.hasMaceInMainHand(player);
    }

    public boolean hasMaceInMainHand(ClientPlayerEntity player2) {
        return player2.getMainHandStack().isOf(Items.MACE);
    }

    public boolean canPerformMaceAttack(ClientPlayerEntity player2, HitCooldown hitCooldown, float f, boolean bl) {
        boolean bl2 = player2.isOnGround();
        boolean bl3 = player2.isClimbing();
        double d = player2.fallDistance;
        if (d > (double)f && !bl3 && !bl2) {
            return hitCooldown.isAttackReady(bl, 0.0f);
        }
        if (bl2 || bl3) {
            return false;
        }
        if (!hitCooldown.isActive()) {
            return false;
        }
        if (this.isShieldUseProgressAbove(f)) {
            return false;
        }
        return hitCooldown.isAttackReady(bl);
    }

    private boolean isShieldUseProgressAbove(float f) {
        return ShieldBreaker.hasSample() && !ShieldBreaker.isBlocking() && ShieldBreaker.getUseProgress() > (double)f;
    }
}

