package ru.wexside.module.player;

import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.text.Text;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.ClientChat;
import ru.wexside.misc.ElapsedTimer;
import ru.wexside.misc.PasswordStore;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public final class AutoAuthModule
extends Module
implements ConfigSerializable {
    private static final String PASSWORD_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final long PROMPT_DELAY_MS = 1500L;
    private static final long COMMAND_COOLDOWN_MS = 3000L;
    private static final int PASSWORD_LENGTH = 10;
    private final BooleanSetting enabledSetting;
    private final BooleanSetting showPassword;
    private final ElapsedTimer commandCooldown = new ElapsedTimer();
    private final ElapsedTimer promptDelay = new ElapsedTimer();
    private final Queue<AuthAction> pending = new ConcurrentLinkedQueue<AuthAction>();
    private boolean waiting;
    private boolean pendingLogin;
    private boolean pendingRegister;

    public AutoAuthModule(EventBus eventBus) {
        super(eventBus, "auto_auth", "Auto Auth", "Автоматически авторизуется на сервере", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Автоматически авторизуется на сервере").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.showPassword = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Show Password").id("show_password").description("Показывать сообщение с паролем в чате")).build();
        this.registerSetting(this.showPassword);
    }

    @Override
    protected void initialize() {
        this.listen(IncomingPacketEvent.class, this::onIncomingPacket);
        this.listen(ClientTickEvent.class, event -> this.onTick());
        this.listen(WorldSessionEvent.class, event -> {
            if (this.waiting) {
                this.promptDelay.update();
            }
        });
    }

    private void onTick() {
        AuthAction action;
        if (!this.enabledSetting.isEnabled()) {
            this.reset();
            return;
        }
        while ((action = this.pending.poll()) != null) {
            if (action == AuthAction.REGISTER) {
                this.pendingRegister = true;
            } else {
                this.pendingLogin = true;
            }
            if (this.waiting) continue;
            this.waiting = true;
            this.promptDelay.update();
        }
        if (!this.waiting) {
            return;
        }
        if (!this.promptDelay.hasElapsed(1500L) || !this.commandCooldown.hasElapsed(3000L)) {
            return;
        }
        boolean register = this.pendingRegister;
        boolean login = this.pendingLogin;
        this.pendingRegister = false;
        this.pendingLogin = false;
        this.waiting = false;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        PasswordStore passwords = WexSideClient.getPasswordStore();
        if (passwords == null) {
            return;
        }
        String username = client.getSession().getUsername();
        String server = this.serverAddress();
        if (register) {
            String password = passwords.getPassword(server, username);
            if (password == null || password.isEmpty()) {
                password = this.generatePassword();
                passwords.savePassword(server, username, password);
                if (this.showPassword.isEnabled()) {
                    ClientChat.send("Ваш пароль: \u00a7c" + password + "\u00a7r. Успешно сохранён.");
                } else {
                    ClientChat.send("Пароль сгенерирован и сохранён.");
                }
            }
            player.networkHandler.sendChatCommand("register " + password + " " + password);
            this.commandCooldown.update();
            return;
        }
        if (!login) {
            return;
        }
        String saved = passwords.getPassword(server, username);
        if (saved == null || saved.isEmpty()) {
            return;
        }
        player.networkHandler.sendChatCommand("login " + saved);
        this.commandCooldown.update();
    }

    private void onIncomingPacket(IncomingPacketEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        String message = this.extractMessage(event.getPacket());
        if (message == null) {
            return;
        }
        String lower = message.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("/reg") || lower.contains("/register")) {
            this.pending.add(AuthAction.REGISTER);
        } else if (lower.contains("/login") || lower.contains("/l ")) {
            this.pending.add(AuthAction.LOGIN);
        }
    }

    private void reset() {
        this.pending.clear();
        this.pendingRegister = false;
        this.pendingLogin = false;
        this.waiting = false;
    }

    private String extractMessage(Packet<?> packet) {
        Text text = null;
        if (packet instanceof GameMessageS2CPacket) {
            GameMessageS2CPacket chat = (GameMessageS2CPacket)packet;
            text = chat.content();
        } else if (packet instanceof OverlayMessageS2CPacket) {
            OverlayMessageS2CPacket overlay = (OverlayMessageS2CPacket)packet;
            text = overlay.text();
        }
        return text == null ? null : text.getString();
    }

    private String serverAddress() {
        ServerInfo server = MinecraftClient.getInstance().getCurrentServerEntry();
        return server == null || server.address == null ? "" : server.address;
    }

    private String generatePassword() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder builder = new StringBuilder(10);
        for (int i = 0; i < 10; ++i) {
            builder.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return builder.toString();
    }

    private static enum AuthAction {
        REGISTER,
        LOGIN;

    }
}

