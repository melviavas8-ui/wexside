package ru.wexside.module.player;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.BlockBreakingAccessor;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;

public class FastBreakModule
extends Module
implements ConfigSerializable {
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private final NumberSetting speed;

    public FastBreakModule(EventBus eventBus) {
        super(eventBus, "fast_break", "Fast Break", "Увеличивает скорость ломания блоков", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.speed = ((NumberSettingBuilder)NumberSetting.builder().range(10.0, 100.0).defaultValue(50.0).multiplier(1.0).precision(0).animationSpeed(20.0f).showMarkers().name("Speed").id("speed").description("Ускорение (в % от изначальной скорости)")).build();
        this.registerSetting(this.speed);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.setFloatType());
    }

    private void setFloatType() {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager interactions = client.interactionManager;
        if (player == null || client.world == null || interactions == null) {
            return;
        }
        BlockBreakingAccessor breaking = (BlockBreakingAccessor)interactions;
        breaking.setBreakingCooldown(0);
        float speedBonus = (float)this.speed.getIntValue() / 100.0f;
        float progress = breaking.getBreakingProgress();
        if (this.blockAt(0.0, player.getVelocity().y, 0.0) != Blocks.COBWEB && !player.isOnGround()) {
            breaking.setBreakingProgress(progress *= 5.0f);
            speedBonus -= 0.8f;
        }
        if (progress > 1.0f - speedBonus && progress < 0.99f) {
            breaking.setBreakingProgress(0.99f);
        }
    }

    private Block blockAt(double offsetX, double offsetY, double offsetZ) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        BlockPos pos = BlockPos.ofFloored((double)(player.getX() + offsetX), (double)(player.getY() + offsetY), (double)(player.getZ() + offsetZ));
        return MinecraftClient.getInstance().world.getBlockState(pos).getBlock();
    }
}

