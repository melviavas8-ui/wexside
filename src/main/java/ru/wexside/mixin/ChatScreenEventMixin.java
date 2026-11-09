package ru.wexside.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.WexSideClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.OutgoingChatEvent;
import ru.wexside.misc.NativeHandle;
import ru.wexside.module.hud.AnimateModule;

@Mixin(value={ChatScreen.class})
public abstract class ChatScreenEventMixin
implements NativeHandle {
    @Shadow
    protected TextFieldWidget chatField;
    @Unique
    private long wexside$openedAt;

    @Inject(method={"init()V"}, at={@At(value="TAIL")})
    private void wexside$startInputAnimation(CallbackInfo callback) {
        this.wexside$openedAt = System.currentTimeMillis();
    }

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void wexside$animateInputWidth(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo callback) {
        if (this.chatField == null) {
            return;
        }
        float fullWidth = Math.max(1.0f, (float)context.getScaledWindowWidth() - 4.0f);
        this.chatField.setWidth(Math.max(1, Math.round(AnimateModule.compute4(fullWidth, this))));
    }

    @Override
    public long getLongType() {
        return this.wexside$openedAt;
    }

    @Inject(method={"sendMessage"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$beforeSendMessage(String original, boolean addToHistory, CallbackInfo callback) {
        EventBus eventBus = WexSideClient.getEventBus();
        if (eventBus == null) {
            return;
        }
        OutgoingChatEvent event = new OutgoingChatEvent(original);
        eventBus.post(event);
        if (event.isCancelled()) {
            callback.cancel();
            return;
        }
        String message = event.getMessage();
        if (message == null || message.equals(original)) {
            return;
        }
        callback.cancel();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.networkHandler == null) {
            return;
        }
        if (addToHistory) {
            client.inGameHud.getChatHud().addToMessageHistory(message);
        }
        if (message.startsWith("/")) {
            client.player.networkHandler.sendChatCommand(message.substring(1));
        } else {
            client.player.networkHandler.sendChatMessage(message);
        }
    }
}

