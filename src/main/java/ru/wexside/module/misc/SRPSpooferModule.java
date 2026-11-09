package ru.wexside.module.misc;

import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public final class SRPSpooferModule
extends Module
implements ConfigSerializable {
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();

    public SRPSpooferModule(EventBus eventBus) {
        super(eventBus, "srp_spoofer", "SRP Spoofer", "Подтверждает ресурс-паки сервера без их загрузки", ModuleCategory.valueOf("MISC"), new String[0]);
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
        this.listen(IncomingPacketEvent.class, this::onIncomingPacket);
    }

    private void onIncomingPacket(IncomingPacketEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (!(packet instanceof ResourcePackSendS2CPacket)) {
            return;
        }
        ResourcePackSendS2CPacket resourcePackPacket = (ResourcePackSendS2CPacket)packet;
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) {
            return;
        }
        UUID packId = resourcePackPacket.id();
        networkHandler.sendPacket((Packet)new ResourcePackStatusC2SPacket(packId, ResourcePackStatusC2SPacket.Status.ACCEPTED));
        networkHandler.sendPacket((Packet)new ResourcePackStatusC2SPacket(packId, ResourcePackStatusC2SPacket.Status.DOWNLOADED));
        networkHandler.sendPacket((Packet)new ResourcePackStatusC2SPacket(packId, ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
        event.update();
    }
}

