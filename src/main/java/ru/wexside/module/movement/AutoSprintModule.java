package ru.wexside.module.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public class AutoSprintModule
extends Module
implements ConfigSerializable {
    private static volatile AutoSprintModule instance;
    private final BooleanSetting enabledSetting;
    private final BooleanSetting keepSprint;
    private boolean suppressUntilNextTick;

    public AutoSprintModule(EventBus eventBus) {
        super(eventBus, "auto_sprint", "Auto Sprint", "Автоматически включает спринт", ModuleCategory.valueOf("MOVEMENT"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.keepSprint = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Keep Sprint").id("keep_sprint").description("Сохранение спринта при атаке")).build();
        this.registerSetting(this.keepSprint);
    }

    @Override
    protected void initialize() {
    }

    public static boolean isEnabled() {
        AutoSprintModule module = instance;
        if (module == null || !module.enabledSetting.isEnabled()) {
            return false;
        }
        if (module.suppressUntilNextTick) {
            module.suppressUntilNextTick = false;
            return false;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }
        if (!player.input.hasForwardMovement() || player.isUsingItem() || player.isSneaking()) {
            return false;
        }
        return module.canSprint(player);
    }

    public static boolean isEnabled2() {
        AutoSprintModule module = instance;
        return module != null && module.enabledSetting.isEnabled() && module.keepSprint.isEnabled();
    }

    public static void tick() {
        AutoSprintModule module = instance;
        if (module != null) {
            module.suppressUntilNextTick = true;
        }
    }

    private boolean canSprint(ClientPlayerEntity player) {
        if (player.isSubmergedInWater()) {
            return true;
        }
        if (player.getAbilities().flying) {
            return true;
        }
        return player.getHungerManager().getFoodLevel() > 6;
    }
}

