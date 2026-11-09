package ru.wexside.mixin;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import ru.wexside.misc.BossBarMapAccessor;

@Mixin(value={BossBarHud.class})
public abstract class BossBarHudAccessorMixin
implements BossBarMapAccessor {
    @Override
    @Accessor(value="bossBars")
    public abstract Map<UUID, ClientBossBar> getMap();
}

