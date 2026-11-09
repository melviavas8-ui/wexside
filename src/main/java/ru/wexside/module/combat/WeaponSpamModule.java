package ru.wexside.module.combat;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;

public final class WeaponSpamModule
extends Module
implements ConfigSerializable {
    private static final String BOW = "Bow";
    private static final String CROSSBOW = "Crossbow";
    private static final String TRIDENT = "Trident";
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Флудит выстрелами из оружия").withKeybind().toggle()).build();
    private final MultiSelectSetting weapons;
    private final NumberSetting delay;

    public WeaponSpamModule(EventBus eventBus) {
        super(eventBus, "weapon_spam", "Weapon Spam", "Флудит выстрелами из оружия", ModuleCategory.valueOf("COMBAT"), new String[0]);
        this.registerSetting(this.enabledSetting);
        MultiSelectSetting weaponSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options(BOW, CROSSBOW, TRIDENT).selectAll(false).optionListEnabled(false).name("Weapons").id("weapons").description("Типы оружия")).build();
        weaponSetting.setOptions(new String[0]);
        this.weapons = weaponSetting;
        this.registerSetting(weaponSetting);
        this.delay = ((NumberSettingBuilder)NumberSetting.builder().range(3.0, 20.0).defaultValue(5.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Delay").id("delay").description("Задержка на лук (тики)").visibleWhen(() -> this.weapons.getSelectedOptions().contains(BOW))).build();
        this.registerSetting(this.delay);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (!this.enabledSetting.isEnabled() || player == null || client.interactionManager == null) {
            return;
        }
        if (!player.isUsingItem()) {
            return;
        }
        ItemStack stack = player.getActiveItem();
        List<String> selected = this.weapons.getSelectedOptions();
        int useTime = player.getItemUseTime();
        if (selected.contains(BOW) && stack.getItem() instanceof BowItem && useTime >= this.delay.getIntValue()) {
            client.interactionManager.stopUsingItem((PlayerEntity)player);
            return;
        }
        if (selected.contains(CROSSBOW) && stack.getItem() instanceof CrossbowItem) {
            if (useTime >= CrossbowItem.getPullTime((ItemStack)stack, (LivingEntity)player) && !CrossbowItem.isCharged((ItemStack)stack)) {
                client.interactionManager.stopUsingItem((PlayerEntity)player);
            }
            return;
        }
        if (selected.contains(TRIDENT) && stack.getItem() instanceof TridentItem && useTime >= 10) {
            client.interactionManager.stopUsingItem((PlayerEntity)player);
        }
    }
}

