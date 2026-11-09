package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import ru.wexside.WexSideClient;
import ru.wexside.misc.ColorPlaneInputHandler;
import ru.wexside.misc.GuiInteractionState;
import ru.wexside.misc.HudDragRegistry;
import ru.wexside.util.AbstractHudElement;

public final class HudDragInputHandler
implements ColorPlaneInputHandler {
    private final HudDragRegistry hudDragRegistry;

    public HudDragInputHandler(HudDragRegistry hudDragRegistry) {
        this.hudDragRegistry = hudDragRegistry;
    }

    public void updateScreenState() {
        MinecraftClient client = MinecraftClient.getInstance();
        double scale = client.getWindow().getScaleFactor();
        GuiInteractionState mouse = GuiInteractionState.getInstance();
        mouse.setRawMousePosition((int)client.mouse.getX(), (int)client.mouse.getY());
        mouse.setScaledMousePosition((int)(client.mouse.getX() / scale), (int)(client.mouse.getY() / scale));
        if (!this.isEditorScreen()) {
            this.hudDragRegistry.clearSelection();
        }
    }

    @Override
    public boolean onMousePressed(float x, float y, int button) {
        if (button == 1) {
            AbstractHudElement element = WexSideClient.getInventoryHudRegistry().findAt(x, y);
            if (element == null) {
                this.hudDragRegistry.clearSelection();
                return false;
            }
            this.hudDragRegistry.select(element);
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (this.hudDragRegistry.onMousePressed(x, y)) {
            return true;
        }
        AbstractHudElement element = WexSideClient.getInventoryHudRegistry().findAt(x, y);
        if (element != null) {
            this.hudDragRegistry.select(element);
            return this.hudDragRegistry.onMousePressed(x, y);
        }
        this.hudDragRegistry.clearSelection();
        return false;
    }

    public void onMouseReleased(int button) {
        if (button == 0) {
            this.hudDragRegistry.clearSelection();
        }
    }

    public boolean isEditorScreen() {
        return MinecraftClient.getInstance().currentScreen instanceof ChatScreen || MinecraftClient.getInstance().currentScreen instanceof InventoryScreen;
    }
}

