package ru.wexside.mixin;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.WexSideClient;
import ru.wexside.util.RotationController;

@Mixin(value={PlayerMoveC2SPacket.class})
public abstract class PlayerMovePacketRotationMixin {
    @Shadow
    @Final
    protected boolean changeLook;
    @Shadow
    @Final
    @Mutable
    protected float yaw;
    @Shadow
    @Final
    @Mutable
    protected float pitch;

    @Inject(method={"<init>"}, at={@At(value="RETURN")})
    private void wexside$applySilentRotation(double x, double y, double z, float yaw, float pitch, boolean onGround, boolean horizontalCollision, boolean changePosition, boolean changeLook, CallbackInfo callback) {
        if (!this.changeLook) {
            return;
        }
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations == null) {
            return;
        }
        Float requestedYaw = rotations.getViewYaw();
        if (requestedYaw == null) {
            return;
        }
        this.yaw = requestedYaw.floatValue();
        this.pitch = rotations.getViewPitch();
        rotations.onTick(this.pitch);
    }
}

