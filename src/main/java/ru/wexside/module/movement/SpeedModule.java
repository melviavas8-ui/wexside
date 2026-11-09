package ru.wexside.module.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;

public final class SpeedModule
extends Module
implements ConfigSerializable {
    public static volatile float value;
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Увеличивает скорость перемещения").withKeybind().toggle()).build();
    private final ModeSetting mode;

    public SpeedModule(EventBus eventBus) {
        super(eventBus, "speed", "Speed", "Увеличивает скорость перемещения", ModuleCategory.valueOf("MOVEMENT"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.mode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Sticky", "Collision", "Sneak").defaultOption("Sticky").name("Mode").id("mode").description("Метод ускорения")).build();
        this.registerSetting(this.mode);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onClientTick);
    }

    private void onClientTick(ClientTickEvent event) {
        value = 0.0f;
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        switch (this.mode.getSelectedOption()) {
            case "Sneak": {
                this.applySneak(player);
                break;
            }
            case "Collision": {
                this.applyCollision(player, client.world);
                break;
            }
            case "Sticky": {
                this.applySticky(player, client.world);
            }
        }
    }

    private void applySticky(ClientPlayerEntity player, ClientWorld world) {
        Box box = player.getBoundingBox().expand(0.3);
        int nearby = 0;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof ClientPlayerEntity || !(entity instanceof LivingEntity) || entity instanceof ArmorStandEntity || !entity.getBoundingBox().intersects(box)) continue;
            ++nearby;
        }
        if (nearby > 0) {
            double yaw = Math.toRadians(player.getYaw());
            double boost = 0.08 * (double)nearby;
            Vec3d velocity = player.getVelocity();
            player.setVelocity(velocity.x + -Math.sin(yaw) * boost, velocity.y, velocity.z + Math.cos(yaw) * boost);
        }
    }

    private void applyCollision(ClientPlayerEntity player, ClientWorld world) {
        Box box = player.getBoundingBox().expand(0.1);
        int armorStands = 0;
        int living = 0;
        for (Entity entity : world.getEntities()) {
            if (entity.isRemoved() || !entity.getBoundingBox().intersects(box)) continue;
            if (entity instanceof ArmorStandEntity) {
                ++armorStands;
            }
            if (!(entity instanceof LivingEntity)) continue;
            ++living;
        }
        if (armorStands <= 1 && living <= 1) {
            return;
        }
        if (player.isOnGround()) {
            return;
        }
        value = armorStands > 1 ? 1.0f / (float)armorStands : 0.16f;
    }

    private void applySneak(ClientPlayerEntity player) {
        if (player.isUsingItem()) {
            Vec3d velocity = player.getVelocity();
            player.setVelocity(velocity.x * 1.5, velocity.y, velocity.z * 1.5);
        }
    }
}

