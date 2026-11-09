package ru.wexside.module.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;

public class VelocityModule
extends Module
implements ConfigSerializable {
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private final ModeSetting mode;

    public VelocityModule(EventBus eventBus) {
        super(eventBus, "velocity", "Velocity", "Уменьшает отталкивание от ударов", ModuleCategory.valueOf("COMBAT"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.mode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Packet", "Legit").defaultOption("Packet").name("Mode").id("mode").description("Метод уменьшения отталкивания")).build();
        this.registerSetting(this.mode);
    }

    @Override
    protected void initialize() {
        this.listen(IncomingPacketEvent.class, this::onIncomingPacket);
    }

    private void onIncomingPacket(IncomingPacketEvent event) {
        EntityVelocityUpdateS2CPacket velocity;
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if ("Packet".equals(this.mode.getSelectedOption())) {
            EntityVelocityUpdateS2CPacket velocity2;
            if (packet instanceof EntityVelocityUpdateS2CPacket && (velocity2 = (EntityVelocityUpdateS2CPacket)packet).getEntityId() == player.getId()) {
                event.update();
            } else if (packet instanceof ExplosionS2CPacket) {
                event.update();
            }
            return;
        }
        if ("Legit".equals(this.mode.getSelectedOption()) && packet instanceof EntityVelocityUpdateS2CPacket && (velocity = (EntityVelocityUpdateS2CPacket)packet).getEntityId() == player.getId()) {
            player.setVelocity(player.getVelocity().multiply(0.6, 1.0, 0.6));
        }
    }
}

