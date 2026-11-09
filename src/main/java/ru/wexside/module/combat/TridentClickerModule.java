package ru.wexside.module.combat;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.AttackInvoker;
import ru.wexside.misc.ReachHelper;
import ru.wexside.misc.TargetFilter;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.Angle;
import ru.wexside.util.TargetSelector;

public class TridentClickerModule
extends Module
implements ConfigSerializable {
    private static final float RANGE = 3.0f;
    private static final long CLICK_INTERVAL_MS = 450L;
    private static final List<String> TARGET_TYPES = List.of("Players");
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private final NumberSetting cps;
    private final ModeSetting sorting;
    private TargetSelector targetSelector;
    private long lastClickTime;

    public TridentClickerModule(EventBus eventBus) {
        super(eventBus, "trident_clicker", "Trident Clicker", "Закликивание трезубцем", ModuleCategory.valueOf("COMBAT"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.cps = ((NumberSettingBuilder)NumberSetting.builder().range(5.0, 200.0).defaultValue(80.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("CPS").id("cps").description("Количество кликов в секунду")).build();
        this.registerSetting(this.cps);
        this.sorting = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Health", "Distance", "Crosshair").defaultOption("Distance").name("Сортировка").id("sorting").description("Сортировка целей для атаки")).build();
        this.registerSetting(this.sorting);
    }

    @Override
    protected void initialize() {
        this.targetSelector = new TargetSelector();
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (!this.enabledSetting.isEnabled() || player == null || client.world == null) {
            this.targetSelector.update();
            return;
        }
        if (!player.getMainHandStack().isOf(Items.TRIDENT)) {
            this.targetSelector.update();
            return;
        }
        TargetFilter filter = new TargetFilter(TARGET_TYPES, null, 0);
        LivingEntity target = this.targetSelector.selectTarget(filter, 3.0f, this.sorting.getSelectedOption(), true);
        if (target == null) {
            return;
        }
        Angle look = new Angle(player.getYaw(), player.getPitch());
        if (ReachHelper.raycastEntity((Entity)target, look, 3.0f, true) != target) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastClickTime < 450L) {
            return;
        }
        AttackInvoker clicks = (AttackInvoker)client;
        int count = this.cps.getIntValue();
        for (int i = 0; i < count; ++i) {
            clicks.invokeAttack();
        }
        this.lastClickTime = now;
    }
}

