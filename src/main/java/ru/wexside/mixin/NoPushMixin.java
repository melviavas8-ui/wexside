package ru.wexside.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.module.player.NoPushModule;

public final class NoPushMixin {
    private NoPushMixin() {
    }

    @Mixin(value={ClientPlayerEntity.class})
    public static abstract class ClientPlayerHook {
        @Inject(method={"pushOutOfBlocks(DD)V"}, at={@At(value="HEAD")}, cancellable=true)
        private void wexside$ignoreBlockPush(double x, double z, CallbackInfo callback) {
            if (NoPushModule.compute("Blocks")) {
                callback.cancel();
            }
        }
    }

    @Mixin(value={PlayerEntity.class})
    public static abstract class PlayerHook {
        @Inject(method={"isPushedByFluids"}, at={@At(value="HEAD")}, cancellable=true)
        private void wexside$ignoreFluidPush(CallbackInfoReturnable<Boolean> callback) {
            if ((Object)this == MinecraftClient.getInstance().player && NoPushModule.compute("Water")) {
                callback.setReturnValue(false);
            }
        }
    }

    @Mixin(value={Entity.class})
    public static abstract class EntityHook {
        @Inject(method={"pushAwayFrom"}, at={@At(value="HEAD")}, cancellable=true)
        private void wexside$ignoreEntityPush(Entity other, CallbackInfo callback) {
            if ((Object)this == MinecraftClient.getInstance().player && NoPushModule.compute("Players")) {
                callback.cancel();
            }
        }
    }
}

