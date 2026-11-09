package ru.wexside.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.WexSideClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.KeyPressedEvent;
import ru.wexside.event.KeyReleasedEvent;
import ru.wexside.event.MousePressedEvent;
import ru.wexside.event.MouseReleasedEvent;

public final class InputEventMixin {
    private InputEventMixin() {
    }

    @Mixin(value={Mouse.class})
    public static class MouseHook {
        @Inject(method={"onMouseButton"}, at={@At(value="TAIL")})
        private void wexside$onMouseButton(long window, MouseInput input, int action, CallbackInfo callback) {
            EventBus eventBus = WexSideClient.getEventBus();
            if (eventBus == null) {
                return;
            }
            if (action == 1) {
                eventBus.post(new MousePressedEvent(input.button()));
            } else if (action == 0) {
                eventBus.post(new MouseReleasedEvent(input.button()));
            }
        }
    }

    @Mixin(value={Keyboard.class})
    public static class KeyboardHook {
        @Inject(method={"onKey"}, at={@At(value="TAIL")})
        private void wexside$onKey(long window, int action, KeyInput input, CallbackInfo callback) {
            EventBus eventBus = WexSideClient.getEventBus();
            if (eventBus == null) {
                return;
            }
            if (action == 1) {
                eventBus.post(new KeyPressedEvent(input.key(), input.scancode()));
            } else if (action == 0) {
                eventBus.post(new KeyReleasedEvent(input.key(), input.scancode()));
            }
        }
    }
}

