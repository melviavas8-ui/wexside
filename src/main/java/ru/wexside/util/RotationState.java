package ru.wexside.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ru.wexside.util.Angle;

public record RotationState(Angle appliedAngle, Angle previousAngle, Angle serverAngle, Angle playerAngle, LivingEntity target, Vec3d targetPosition, int ticksSinceAttack, int ticksSinceHit, float attackCooldown, boolean attackReady) {
    public Angle getAppliedAngle() {
        return this.appliedAngle;
    }

    public Angle getPlayerAngle() {
        return this.playerAngle;
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public int getTicksSinceAttack() {
        return this.ticksSinceAttack;
    }

    public int getTicksSinceHit() {
        return this.ticksSinceHit;
    }

    public boolean isAttackReady() {
        return this.attackReady;
    }
}

