package ru.wexside.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.module.misc.CustomSoundsModule;
import ru.wexside.module.misc.SoundRemoverModule;

@Mixin(value={SoundManager.class})
public abstract class SoundManagerEventMixin {
    @Inject(method={"play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$filterImmediateSound(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> callback) {
        if (sound == null) {
            return;
        }
        Identifier soundId = sound.getId();
        boolean muted = SoundRemoverModule.compute4(soundId);
        SoundRemoverModule.handle(soundId, muted);
        if (muted) {
            callback.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
            return;
        }
        if (CustomSoundsModule.compute8(sound, 0L)) {
            callback.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
        }
    }

    @Inject(method={"play(Lnet/minecraft/client/sound/SoundInstance;I)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$filterDelayedSound(SoundInstance sound, int delay, CallbackInfo callback) {
        if (sound == null) {
            return;
        }
        Identifier soundId = sound.getId();
        boolean muted = SoundRemoverModule.compute4(soundId);
        SoundRemoverModule.handle(soundId, muted);
        if (muted) {
            callback.cancel();
            return;
        }
        if (CustomSoundsModule.compute8(sound, (long)delay * 50L)) {
            callback.cancel();
        }
    }
}

