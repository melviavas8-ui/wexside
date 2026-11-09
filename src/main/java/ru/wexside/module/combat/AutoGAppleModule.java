package ru.wexside.module.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;

public class AutoGAppleModule
extends Module
implements ConfigSerializable {
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private final NumberSetting health;
    private boolean eating;

    public AutoGAppleModule(EventBus eventBus) {
        super(eventBus, "auto_gapple", "Auto GApple", "Ест яблоко из офхенда при низком HP", ModuleCategory.valueOf("COMBAT"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.health = ((NumberSettingBuilder)NumberSetting.builder().range(4.0, 20.0).defaultValue(15.0).multiplier(1.0).precision(1).animationSpeed(20.0f).name("Health").id("health").description("Минимальный HP для активации")).build();
        this.registerSetting(this.health);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        if (this.shouldEat(player)) {
            MinecraftClient.getInstance().options.useKey.setPressed(true);
            this.eating = true;
        } else if (this.eating) {
            MinecraftClient.getInstance().options.useKey.setPressed(false);
            this.eating = false;
        }
    }

    private boolean shouldEat(ClientPlayerEntity player) {
        if (!this.enabledSetting.isEnabled()) {
            return false;
        }
        if (!player.getOffHandStack().isOf(Items.GOLDEN_APPLE)) {
            return false;
        }
        if (player.getItemCooldownManager().isCoolingDown(new ItemStack((ItemConvertible)Items.GOLDEN_APPLE))) {
            return false;
        }
        return player.getHealth() + player.getAbsorptionAmount() <= this.health.getFloatValue();
    }
}

