package ru.wexside.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.WexSideClient;
import ru.wexside.event.BlockInteractEvent;
import ru.wexside.event.EntityAttackEvent;
import ru.wexside.event.EventBus;

@Mixin(value={ClientPlayerInteractionManager.class})
public abstract class ClientInteractionEventMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method={"attackEntity"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$beforeAttack(PlayerEntity player, Entity target, CallbackInfo callback) {
        EventBus eventBus = WexSideClient.getEventBus();
        if (eventBus == null) {
            return;
        }
        EntityAttackEvent event = new EntityAttackEvent(target);
        eventBus.post(event);
        if (event.isCancelled()) {
            callback.cancel();
        }
    }

    @Inject(method={"interactBlock"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$beforeBlockInteract(ClientPlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> callback) {
        if (this.client.world == null) {
            return;
        }
        EventBus eventBus = WexSideClient.getEventBus();
        if (eventBus == null) {
            return;
        }
        BlockInteractEvent event = new BlockInteractEvent(this.client.world.getBlockState(hit.getBlockPos()).getBlock());
        eventBus.post(event);
        if (event.isCancelled()) {
            callback.setReturnValue(ActionResult.FAIL);
        }
    }
}

