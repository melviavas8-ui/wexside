package ru.wexside.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.module.render.HitParticlesModule;

@Mixin(value={ParticleManager.class})
public abstract class HitParticleFilterMixin {
    @Inject(method={"addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$removeVanillaHitParticles(ParticleEffect effect, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> callback) {
        if (effect != null && HitParticlesModule.isEnabled() && HitParticleFilterMixin.isVanillaHitParticle(effect.getType())) {
            callback.setReturnValue(null);
        }
    }

    private static boolean isVanillaHitParticle(ParticleType<?> type) {
        return type == ParticleTypes.CRIT || type == ParticleTypes.ENCHANTED_HIT || type == ParticleTypes.DAMAGE_INDICATOR;
    }
}

