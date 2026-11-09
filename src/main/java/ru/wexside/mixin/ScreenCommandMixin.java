package ru.wexside.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.WexSideClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.OutgoingChatEvent;

@Mixin(value={Screen.class})
public abstract class ScreenCommandMixin {
    @Inject(method={"handleRunCommand"}, at={@At(value="HEAD")}, cancellable=true)
    private static void wexside$dispatchClientCommand(ClientPlayerEntity player, String command, Screen screen, CallbackInfo callback) {
        if (command == null || !command.startsWith(".")) {
            return;
        }
        EventBus eventBus = WexSideClient.getEventBus();
        if (eventBus == null) {
            return;
        }
        eventBus.post(new OutgoingChatEvent(command));
        callback.cancel();
    }
}

