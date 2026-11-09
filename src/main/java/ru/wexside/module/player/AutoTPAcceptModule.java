package ru.wexside.module.player;

import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public final class AutoTPAcceptModule
extends Module
implements ConfigSerializable {
    private static final String[] REQUEST_HINTS = new String[]{"телепортироваться", "хочет телепортироваться", "просит телепортироваться", "requested to teleport", "has requested teleport", "tpa"};
    private final BooleanSetting enabledSetting;
    private final BooleanSetting onlyFriends;
    private final Queue<String> pending = new ConcurrentLinkedQueue<String>();

    public AutoTPAcceptModule(EventBus eventBus) {
        super(eventBus, "auto_tp_accept", "Auto TPAccept", "Автоматически принимает запросы на телепортацию", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.onlyFriends = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Only-Friends").id("only_friends").description("Принимать запросы только от друзей")).build();
        this.registerSetting(this.onlyFriends);
    }

    @Override
    protected void initialize() {
        this.listen(IncomingPacketEvent.class, this::onIncomingPacket);
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        String message;
        if (!this.enabledSetting.isEnabled()) {
            this.pending.clear();
            return;
        }
        boolean accept = false;
        while ((message = this.pending.poll()) != null) {
            if (this.onlyFriends.isEnabled() && !this.isFriendRequest(message)) continue;
            accept = true;
        }
        if (accept) {
            this.accept();
        }
    }

    private void onIncomingPacket(IncomingPacketEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        String message = this.textOf(event.getPacket());
        if (message != null && this.isTeleportRequest(message)) {
            this.pending.add(message);
        }
    }

    private String textOf(Packet<?> packet) {
        Text text = null;
        if (packet instanceof GameMessageS2CPacket) {
            GameMessageS2CPacket gameMessage = (GameMessageS2CPacket)packet;
            text = gameMessage.content();
        } else if (packet instanceof ChatMessageS2CPacket) {
            ChatMessageS2CPacket chatMessage = (ChatMessageS2CPacket)packet;
            text = chatMessage.unsignedContent();
        }
        return text == null ? null : text.getString();
    }

    private String stripColor(String message) {
        StringBuilder builder = new StringBuilder(message.length());
        char[] chars = message.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            char ch = chars[i];
            if (ch == '\u00a7') {
                ++i;
                continue;
            }
            builder.append(ch);
        }
        return builder.toString();
    }

    private boolean isFriendRequest(String message) {
        if (WexSideClient.getFriends() == null) {
            return false;
        }
        String stripped = this.stripColor(message).toLowerCase(Locale.ROOT);
        for (String friend : WexSideClient.getFriends().getNames()) {
            if (!stripped.contains(friend)) continue;
            return true;
        }
        return false;
    }

    private void accept() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.networkHandler.sendChatCommand("tpaccept");
        }
    }

    private boolean isTeleportRequest(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        for (String hint : REQUEST_HINTS) {
            if (!lower.contains(hint)) continue;
            return true;
        }
        return false;
    }
}

