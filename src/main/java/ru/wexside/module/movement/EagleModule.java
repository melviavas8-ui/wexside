package ru.wexside.module.movement;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.input.InputBindings;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public class EagleModule
extends Module
implements ConfigSerializable {
    private boolean autoSneaking;
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();

    public EagleModule(EventBus eventBus) {
        super(eventBus, "eagle", "Eagle", "Автоматически включает приседание на краю блока", ModuleCategory.valueOf("MOVEMENT"), new String[0]);
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onClientTick);
    }

    private void onClientTick(ClientTickEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            if (this.autoSneaking) {
                this.restoreSneak();
                this.autoSneaking = false;
            }
            return;
        }
        this.autoSneaking = true;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        if (player.input.playerInput.jump() || !player.isOnGround()) {
            return;
        }
        BlockPos pos = BlockPos.ofFloored((double)player.getX(), (double)(player.getY() - 0.1), (double)player.getZ());
        Block block = client.world.getBlockState(pos).getBlock();
        client.options.sneakKey.setPressed(block == Blocks.AIR || block instanceof ScaffoldingBlock);
    }

    private void restoreSneak() {
        KeyBinding sneak = MinecraftClient.getInstance().options.sneakKey;
        InputUtil.Key key = InputUtil.fromTranslationKey((String)sneak.getBoundKeyTranslationKey());
        boolean pressed = key.getCategory() == InputUtil.Type.MOUSE ? InputBindings.isMouseButtonPressed(key.getCode()) : InputBindings.isKeyPressed(key.getCode());
        sneak.setPressed(pressed);
    }
}

