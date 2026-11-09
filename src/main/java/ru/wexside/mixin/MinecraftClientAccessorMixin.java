package ru.wexside.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.WexSideClient;
import ru.wexside.misc.AttackInvoker;
import ru.wexside.misc.ItemUseCooldownAccessor;
import ru.wexside.module.player.FastPlaceModule;

@Mixin(value={MinecraftClient.class})
public abstract class MinecraftClientAccessorMixin
implements ItemUseCooldownAccessor,
AttackInvoker {
    @Shadow
    private int itemUseCooldown;

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void applyClientTitle(CallbackInfoReturnable<String> callback) {
        callback.setReturnValue(WexSideClient.VERSION_LABEL);
    }

    @ModifyConstant(method = "doItemUse", constant = @Constant(intValue = 4))
    private int wexside$itemUseDelay(int vanillaDelay) {
        return FastPlaceModule.getPlacementDelay();
    }

    @Override
    @Invoker(value="doAttack")
    public abstract boolean invokeAttack();

    @Override
    public void setItemUseCooldown(int cooldown) {
        this.itemUseCooldown = cooldown;
    }
}

