package ru.wexside.mixin;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.listener.ClientPacketListener;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.WexSideClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.event.OutgoingPacketEvent;

@Mixin(value={ClientConnection.class})
public abstract class ClientConnectionEventMixin {
    @Inject(method={"send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$onPacketSend(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo callback) {
        ClientConnection connection = (ClientConnection)((Object)this);
        if (connection.getSide() != NetworkSide.CLIENTBOUND) {
            return;
        }
        EventBus eventBus = WexSideClient.getEventBus();
        if (eventBus == null) {
            return;
        }
        OutgoingPacketEvent event = new OutgoingPacketEvent(packet);
        eventBus.post(event);
        if (event.isCancelled()) {
            callback.cancel();
        }
    }

    @Inject(method={"handlePacket"}, at={@At(value="HEAD")}, cancellable=true)
    private static <T extends PacketListener> void wexside$onPacketReceive(Packet<T> packet, PacketListener listener, CallbackInfo callback) {
        if (!(listener instanceof ClientPacketListener)) {
            return;
        }
        EventBus eventBus = WexSideClient.getEventBus();
        if (eventBus == null) {
            return;
        }
        IncomingPacketEvent event = new IncomingPacketEvent(packet);
        eventBus.post(event);
        if (event.isCancelled()) {
            callback.cancel();
        }
    }
}

