package ru.wexside.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.module.movement.AutoJumpModule;
import ru.wexside.module.movement.AutoSprintModule;
import ru.wexside.module.movement.FreeCameraModule;

@Mixin(value={KeyboardInput.class})
public abstract class KeyboardMovementMixin
extends Input {
    @Inject(method={"tick"}, at={@At(value="TAIL")})
    private void wexside$applyMovementModules(CallbackInfo callback) {
        boolean sprint;
        if (FreeCameraModule.isEnabled()) {
            this.playerInput = PlayerInput.DEFAULT;
            this.movementVector = Vec2f.ZERO;
            return;
        }
        PlayerInput input = this.playerInput;
        boolean jump = input.jump() || AutoJumpModule.isEnabled();
        boolean bl = sprint = input.sprint() || AutoSprintModule.isEnabled();
        if (jump == input.jump() && sprint == input.sprint()) {
            return;
        }
        this.playerInput = new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), jump, input.sneak(), sprint);
    }
}

