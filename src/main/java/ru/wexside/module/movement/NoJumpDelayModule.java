package ru.wexside.module.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.LivingEntityStateAccessor;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.module.combat.AttackAuraModule;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.Setting;

public class NoJumpDelayModule
extends Module
implements ConfigSerializable {
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private AttackAuraModule attackAura;

    public NoJumpDelayModule(EventBus eventBus) {
        super(eventBus, "no_jump_delay", "No Jump Delay", "Убирает задержку между прыжками", ModuleCategory.valueOf("MOVEMENT"), new String[0]);
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        if (!this.enabledSetting.isEnabled() || this.auraEnabled()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        ((LivingEntityStateAccessor)player).setJumpingCooldown(0);
    }

    private boolean auraEnabled() {
        BooleanSetting enabled;
        if (this.attackAura == null && WexSideClient.getInstance() != null && WexSideClient.getInstance().getModuleManager() != null) {
            this.attackAura = WexSideClient.getInstance().getModuleManager().getModules().stream().filter(AttackAuraModule.class::isInstance).map(AttackAuraModule.class::cast).findFirst().orElse(null);
        }
        if (this.attackAura == null) {
            return false;
        }
        Setting toggle = this.attackAura.getToggleSetting();
        return toggle instanceof BooleanSetting && (enabled = (BooleanSetting)toggle).isEnabled();
    }
}

