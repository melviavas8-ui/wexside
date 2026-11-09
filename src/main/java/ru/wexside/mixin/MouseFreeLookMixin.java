package ru.wexside.mixin;

import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.wexside.module.movement.FreeCameraModule;
import ru.wexside.module.render.FreeLookModule;

@Mixin(value={Mouse.class})
public abstract class MouseFreeLookMixin {
    @Redirect(method={"updateMouse"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void wexside$redirectLookDirection(ClientPlayerEntity player, double cursorDeltaX, double cursorDeltaY) {
        if (FreeCameraModule.isEnabled()) {
            FreeCameraModule.handle(cursorDeltaX, cursorDeltaY);
            return;
        }
        if (FreeLookModule.isEnabled()) {
            FreeLookModule.handle(cursorDeltaX, cursorDeltaY);
            return;
        }
        player.changeLookDirection(cursorDeltaX, cursorDeltaY);
    }
}

