package ru.wexside.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.module.movement.AirStuckModule;
import ru.wexside.module.movement.AutoSprintModule;
import ru.wexside.module.movement.SpeedModule;

@Mixin(value={PlayerEntity.class})
public abstract class PlayerEntityFeatureMixin {
    private Vec3d wexside$velocityBeforeAttack;
    private boolean wexside$sprintingBeforeAttack;
    private boolean wexside$restoreSprintAfterAttack;

    @Inject(method={"travel"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$freezeAirStuckMovement(Vec3d movement, CallbackInfo callback) {
        ClientPlayerEntity player;
        Object playerEntityFeatureMixin = this;
        if (playerEntityFeatureMixin instanceof ClientPlayerEntity && AirStuckModule.compute2(player = (ClientPlayerEntity)playerEntityFeatureMixin)) {
            player.setVelocity(Vec3d.ZERO);
            callback.cancel();
        }
    }

    @Inject(method={"getMovementSpeed"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$applyCollisionSpeed(CallbackInfoReturnable<Float> callback) {
        if ((Object)this == MinecraftClient.getInstance().player && SpeedModule.value > 0.0f) {
            callback.setReturnValue(Float.valueOf(SpeedModule.value));
        }
    }

    @Inject(method={"attack"}, at={@At(value="HEAD")})
    private void wexside$captureSprintState(Entity target, CallbackInfo callback) {
        PlayerEntity player = (PlayerEntity)((Object)this);
        boolean bl = this.wexside$restoreSprintAfterAttack = player == MinecraftClient.getInstance().player && AutoSprintModule.isEnabled2() && !player.isSpectator();
        if (this.wexside$restoreSprintAfterAttack) {
            this.wexside$velocityBeforeAttack = player.getVelocity();
            this.wexside$sprintingBeforeAttack = player.isSprinting();
        }
    }

    @Inject(method={"attack"}, at={@At(value="TAIL")})
    private void wexside$restoreSprintState(Entity target, CallbackInfo callback) {
        if (!this.wexside$restoreSprintAfterAttack) {
            return;
        }
        PlayerEntity player = (PlayerEntity)((Object)this);
        player.setVelocity(this.wexside$velocityBeforeAttack);
        player.setSprinting(this.wexside$sprintingBeforeAttack);
        this.wexside$restoreSprintAfterAttack = false;
    }
}

