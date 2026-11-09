package ru.wexside.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public final class VelocityTracker {
    private double velocityY;
    private double lastY = Double.NaN;
    private boolean lastWasGrounded = true;
    static final double JUMP_MOTION = 0.42;
    private boolean hittingGround;
    static final double DRAG = 0.98;
    static final double MOTION_EPSILON = 0.003;

    public void reset() {
        this.update();
    }

    public void onMovementPacket(PlayerMoveC2SPacket.LookAndOnGround packet) {
        this.atomicLong(packet);
    }

    public void setYVelocity(double velocityY) {
        this.setDoubleType(velocityY);
    }

    public void update() {
        this.velocityY = 0.0;
        this.lastY = Double.NaN;
        this.lastWasGrounded = true;
        this.hittingGround = false;
    }

    public void atomicLong(PlayerMoveC2SPacket.LookAndOnGround packet) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (player == null || world == null) {
            return;
        }
        double y = packet.getX(Double.isNaN(this.lastY) ? player.getY() : this.lastY);
        if (packet.isOnGround()) {
            this.velocityY = 0.0;
            this.hittingGround = false;
        } else {
            if (this.lastWasGrounded && packet.changesPosition() && !Double.isNaN(this.lastY) && y > this.lastY) {
                this.velocityY = VelocityTracker.jumpMotion(player);
            }
            this.tickCollision(player, (World)world);
        }
        this.lastWasGrounded = packet.isOnGround();
        if (packet.changesPosition()) {
            this.lastY = y;
        }
    }

    public void setDoubleType(double velocityY) {
        this.velocityY = velocityY;
    }

    public boolean isHittingGround() {
        return this.hittingGround;
    }

    private void tickCollision(ClientPlayerEntity player, World world) {
        Box movedBox;
        this.hittingGround = false;
        if (this.velocityY != 0.0 && world.getBlockCollisions((Entity)player, movedBox = player.getBoundingBox().offset(0.0, this.velocityY, 0.0)).iterator().hasNext()) {
            if (this.velocityY < 0.0) {
                this.hittingGround = true;
            }
            this.velocityY = 0.0;
        }
        this.velocityY = (this.velocityY - 0.08) * 0.98;
        if (Math.abs(this.velocityY) < 0.003) {
            this.velocityY = 0.0;
        }
    }

    private static double jumpMotion(ClientPlayerEntity player) {
        StatusEffectInstance jumpBoost = player.getStatusEffect(StatusEffects.JUMP_BOOST);
        return 0.42 + (jumpBoost != null ? 0.1 * (double)(jumpBoost.getAmplifier() + 1) : 0.0);
    }

    private static double jumpMotionForPlayer(ClientPlayerEntity player) {
        return VelocityTracker.jumpMotion(player);
    }
}

