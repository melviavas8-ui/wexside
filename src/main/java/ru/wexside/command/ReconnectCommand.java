package ru.wexside.command;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import ru.wexside.WexSideClient;
import ru.wexside.command.Command;
import ru.wexside.command.CommandUsageException;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.ClientChat;
import ru.wexside.misc.ElapsedTimer;
import ru.wexside.server.FunTimeServerContext;

public final class ReconnectCommand
extends Command {
    private static final long HUB_DELAY_MS = 1000L;
    private static final long TIMEOUT_MS = 30000L;
    private final ElapsedTimer reconnectTimer = new ElapsedTimer();
    private volatile boolean reconnecting;
    private volatile int anarchyNumber = -1;

    public ReconnectCommand() {
        super("RCT", "Быстрый перезаход на ту же Анархию FT", "rct", "reconnect", "реконнект", "ркт");
        EventBus eventBus = WexSideClient.getEventBus();
        if (eventBus != null) {
            eventBus.subscribe(ClientTickEvent.class, this::onClientTick);
        }
    }

    @Override
    public String getUsage() {
        return ".rct";
    }

    @Override
    public void execute(String ... stringArray) throws CommandUsageException {
        if (!FunTimeServerContext.isConnected()) {
            ClientChat.send("К сожалению .rct не работает на этом сервере.");
            return;
        }
        int currentAnarchy = FunTimeServerContext.getAnarchyNumber();
        if (currentAnarchy == -1) {
            ClientChat.send("Вы должны находиться на сервере Анархии.");
            return;
        }
        if (FunTimeServerContext.isPvpLocked()) {
            ClientChat.send("Вы находитесь в режиме PVP.");
            return;
        }
        ClientChat.send("Перезахожу..");
        this.anarchyNumber = currentAnarchy;
        this.reconnectTimer.update();
        this.reconnecting = true;
        this.sendCommand("hub");
    }

    private void onClientTick(ClientTickEvent event) {
        if (!this.reconnecting) {
            return;
        }
        if (this.reconnectTimer.hasElapsed(30000L)) {
            this.reconnecting = false;
            ClientChat.send("Не удалось перезайти (таймаут).");
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (this.reconnectTimer.hasElapsed(1000L) && FunTimeServerContext.isOnHub()) {
            this.sendCommand("an" + this.anarchyNumber);
            this.reconnecting = false;
        }
    }

    private void sendCommand(String command) {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler != null) {
            networkHandler.sendChatCommand(command);
        }
    }
}

