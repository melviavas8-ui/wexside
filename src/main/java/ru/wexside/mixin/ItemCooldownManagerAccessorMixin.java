package ru.wexside.mixin;

import java.util.Map;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={ItemCooldownManager.class})
public interface ItemCooldownManagerAccessorMixin {
    @Accessor(value="entries")
    public Map<Identifier, Object> wexside$getEntries();

    @Accessor(value="tick")
    public int wexside$getTick();
}

