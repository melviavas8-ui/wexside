package ru.wexside.misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import ru.wexside.event.EventBus;
import ru.wexside.event.KeyPressedEvent;
import ru.wexside.misc.MacroConfigStore;
import ru.wexside.misc.MacroDefinition;
import ru.wexside.misc.MacroType;

public class MacroManager {
    private final MacroConfigStore store;

    public MacroManager(MacroConfigStore store, EventBus eventBus) {
        this.store = store;
        eventBus.subscribe(KeyPressedEvent.class, this::onKeyPressed);
    }

    public boolean containsMacro(String string) {
        if (string == null) {
            return false;
        }
        for (MacroDefinition macroDefinition : this.store.getMacros()) {
            if (!macroDefinition.getName().equalsIgnoreCase(string)) continue;
            return true;
        }
        return false;
    }

    public List<MacroDefinition> getList() {
        return Collections.unmodifiableList(this.store.getMacros());
    }

    public boolean removeMacro(String string) {
        if (string == null) {
            return false;
        }
        boolean bl = this.store.getMacros().removeIf(macroDefinition -> macroDefinition.getName().equalsIgnoreCase(string));
        if (bl) {
            this.saveMacros();
        }
        return bl;
    }

    public void update() {
        if (this.store.getMacros().isEmpty()) {
            return;
        }
        this.store.getMacros().clear();
        this.saveMacros();
    }

    public void addMacro(String string, String string2, int n, MacroType macroType) {
        this.store.getMacros().add(new MacroDefinition(string, string2, n, macroType));
        this.saveMacros();
    }

    private void executeMacro(ClientPlayNetworkHandler networkHandler, MacroDefinition macro) {
        String string = macro.getMessage();
        if (string == null) {
            return;
        }
        String string2 = string.trim();
        if (string2.isEmpty()) {
            return;
        }
        if (macro.getType() == MacroType.COMMAND) {
            if (string2.startsWith("/")) {
                string2 = string2.substring(1).trim();
            }
            if (string2.isEmpty()) {
                return;
            }
            networkHandler.sendChatCommand(string2);
        } else {
            networkHandler.sendChatMessage(string2);
        }
    }

    private void onKeyPressed(KeyPressedEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player2 = mc.player;
        if (player2 == null || mc.currentScreen != null) {
            return;
        }
        ClientPlayNetworkHandler networkHandler = player2.networkHandler;
        if (networkHandler == null) {
            return;
        }
        int n = event.key();
        for (MacroDefinition macroDefinition : new ArrayList<MacroDefinition>(this.store.getMacros())) {
            if (macroDefinition.getKeyCode() != n) continue;
            this.executeMacro(networkHandler, macroDefinition);
        }
    }

    public void setConsumer(Consumer<MacroDefinition> consumer) {
        for (MacroDefinition macroDefinition : this.store.getMacros()) {
            consumer.accept(macroDefinition);
        }
    }

    private void saveMacros() {
        try {
            this.store.save();
        }
        catch (IOException exception) {
            throw new IllegalStateException("Failed to save macros", exception);
        }
    }
}

