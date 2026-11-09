package ru.wexside.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets={"net/minecraft/entity/player/ItemCooldownManager$Entry"})
public interface ItemCooldownEntryAccessorMixin {
    @Accessor(value="endTick")
    public int wexside$getEndTick();
}

