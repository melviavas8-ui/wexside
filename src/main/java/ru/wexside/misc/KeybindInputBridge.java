package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.KeyPressedEvent;
import ru.wexside.event.KeyReleasedEvent;
import ru.wexside.event.MousePressedEvent;
import ru.wexside.event.MouseReleasedEvent;
import ru.wexside.misc.KeybindDispatcher;

public final class KeybindInputBridge {
    private final KeybindDispatcher dispatcher;

    public KeybindInputBridge(EventBus eventBus, KeybindDispatcher keybindDispatcher) {
        this.dispatcher = keybindDispatcher;
        eventBus.subscribe(KeyPressedEvent.class, intTypeEvent -> {
            if (this.acceptsGameplayInput()) {
                this.dispatcher.onKeyPressed(intTypeEvent.key());
            }
        });
        eventBus.subscribe(KeyReleasedEvent.class, intTypeEvent3 -> {
            if (this.acceptsGameplayInput()) {
                this.dispatcher.onKeyReleased(intTypeEvent3.key());
            }
        });
        eventBus.subscribe(MousePressedEvent.class, intTypeEvent2 -> {
            if (this.acceptsGameplayInput()) {
                this.dispatcher.onMousePressed(intTypeEvent2.button());
            }
        });
        eventBus.subscribe(MouseReleasedEvent.class, intTypeEvent4 -> {
            if (this.acceptsGameplayInput()) {
                this.dispatcher.onMouseReleased(intTypeEvent4.button());
            }
        });
    }

    private boolean acceptsGameplayInput() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.currentScreen == null;
    }
}

