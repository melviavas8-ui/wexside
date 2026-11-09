package ru.wexside.module.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.OutgoingPacketEvent;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public class AirStuckModule
extends Module
implements ConfigSerializable {
    private static volatile AirStuckModule instance;
    private final BooleanSetting enabledSetting;

    public AirStuckModule(EventBus eventBus) {
        super(eventBus, "air_stuck", "Air Stuck", "Остановка в воздухе", ModuleCategory.valueOf("MOVEMENT"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onClientTick);
        this.listen(OutgoingPacketEvent.class, this::onOutgoingPacket);
    }

    private void onClientTick(ClientTickEvent event) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (this.isStuck(player)) {
            player.setVelocity(Vec3d.ZERO);
        }
    }

    private void onOutgoingPacket(OutgoingPacketEvent event) {
        if (!this.isStuck(MinecraftClient.getInstance().player)) {
            return;
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket.LookAndOnGround) {
            event.update();
        }
    }

    private boolean isStuck(ClientPlayerEntity player) {
        return this.enabledSetting.isEnabled() && player != null && !player.isOnGround();
    }

    public static boolean compute2(ClientPlayerEntity player) {
        AirStuckModule module = instance;
        return module != null && module.isStuck(player);
    }
}

