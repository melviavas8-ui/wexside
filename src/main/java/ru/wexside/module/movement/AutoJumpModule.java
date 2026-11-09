package ru.wexside.module.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public class AutoJumpModule
extends Module
implements ConfigSerializable {
    private static volatile AutoJumpModule instance;
    private final BooleanSetting enabledSetting;

    public AutoJumpModule(EventBus eventBus) {
        super(eventBus, "auto_jump", "Auto Jump", "Автоматически выполняет прыжок при касании земли", ModuleCategory.valueOf("MOVEMENT"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
    }

    public static boolean isEnabled() {
        AutoJumpModule module = instance;
        if (module == null || !module.enabledSetting.isEnabled()) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        return player != null && client.world != null && !player.getAbilities().flying && !player.isTouchingWater();
    }
}

