package ru.wexside.module.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.BindSettingBuilder;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public class EdgeJumpModule
extends Module
implements ConfigSerializable {
    private final BindSetting key;
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();

    public EdgeJumpModule(EventBus eventBus) {
        super(eventBus, "edge_jump", "Edge Jump", "Прыжок с края блока", ModuleCategory.valueOf("MOVEMENT"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.key = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).name("Key").id("key").description("Клавиша для активации прыжка")).build();
        this.registerSetting(this.key);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onClientTick);
    }

    private void onClientTick(ClientTickEvent event) {
        if (!this.enabledSetting.isEnabled() || !this.key.isPressed()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        if (this.atEdge(player)) {
            player.jump();
        }
    }

    private boolean atEdge(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        return player.isOnGround() && !client.options.jumpKey.isPressed() && client.world.isSpaceEmpty((Entity)player, player.getBoundingBox().offset(0.0, -0.99, 0.0));
    }
}

