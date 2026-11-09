package ru.wexside.module.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.FriendList;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.BindSettingBuilder;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;

public final class AutoSpawnModule
extends Module
implements ConfigSerializable {
    private static final long COOLDOWN_MS = 9000L;
    private static final String MODE_AUTO = "Auto";
    private static final String MODE_KEY = "Key";
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private final ModeSetting mode;
    private final BindSetting key;
    private final NumberSetting range;
    private long lastCommandAt;

    public AutoSpawnModule(EventBus eventBus) {
        super(eventBus, "auto_spawn", "Auto Spawn", "Автоматическая телепортация на спавн при обнаружении игрока поблизости", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.mode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options(MODE_KEY, MODE_AUTO).defaultOption(MODE_KEY).name("Mode").id("mode").description("Режим телепортации на спавн")).build();
        this.registerSetting(this.mode);
        this.key = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(this::onKeyPress).name(MODE_KEY).id("key").description("Клавиша телепортации на спавн").visibleWhen(() -> MODE_KEY.equals(this.mode.getSelectedOption()))).build();
        this.registerSetting(this.key);
        this.range = ((NumberSettingBuilder)NumberSetting.builder().range(16.0, 150.0).defaultValue(50.0).multiplier(1.0).precision(0).animationSpeed(20.0f).showMarkers().name("Range").id("range").description("Дистанция обнаружения игрока").visibleWhen(() -> MODE_AUTO.equals(this.mode.getSelectedOption()))).build();
        this.registerSetting(this.range);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onTick);
    }

    private void onTick(ClientTickEvent event) {
        if (!this.enabledSetting.isEnabled() || !MODE_AUTO.equals(this.mode.getSelectedOption())) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        FriendList friends = WexSideClient.getFriends();
        for (PlayerEntity other : client.world.getPlayers()) {
            if (other == player || friends != null && friends.contains(other.getName().getString()) || !((double)player.distanceTo((Entity)other) < this.range.getValue())) continue;
            this.sendSpawn();
            return;
        }
    }

    private void onKeyPress(BindSetting ignored) {
        if (this.enabledSetting.isEnabled() && MODE_KEY.equals(this.mode.getSelectedOption())) {
            this.sendSpawn();
        }
    }

    private void sendSpawn() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastCommandAt < 9000L) {
            return;
        }
        player.networkHandler.sendChatCommand("spawn");
        this.lastCommandAt = now;
    }
}

