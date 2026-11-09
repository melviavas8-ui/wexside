package ru.wexside.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.misc.PlayerShieldStateAccessor;
import ru.wexside.module.render.HitParticlesModule;

@Mixin(value={ClientPlayerEntity.class})
public abstract class ClientPlayerStateMixin
implements PlayerShieldStateAccessor {
    private boolean wexside$shieldDesynchronized;

    @Override
    public boolean isShieldUseForced() {
        return this.wexside$shieldDesynchronized;
    }

    @Override
    public void setShieldUseForced(boolean state) {
        this.wexside$shieldDesynchronized = state;
    }

    @Inject(method={"addCritParticles"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$replaceCriticalParticles(Entity target, CallbackInfo callback) {
        if (HitParticlesModule.isEnabled()) {
            callback.cancel();
        }
    }

    @Inject(method={"addEnchantedHitParticles"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$replaceEnchantedParticles(Entity target, CallbackInfo callback) {
        if (HitParticlesModule.isEnabled()) {
            callback.cancel();
        }
    }
}

