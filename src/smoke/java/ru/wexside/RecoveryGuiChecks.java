package ru.wexside;

import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import ru.wexside.event.KeyPressedEvent;
import ru.wexside.input.BindInput;
import ru.wexside.misc.PotionEditorState;
import ru.wexside.misc.PotionSelectorPopup;
import ru.wexside.module.misc.AlternativeMenuKeyModule;
import ru.wexside.render.ItemIconRenderer;
import ru.wexside.setting.BindSetting;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.PotionOptionRow;
import ru.wexside.ui.WexsideScreen;

/** In-process checks for recovered UI lifecycle and menu-key integration. */
public final class RecoveryGuiChecks {
    public static void run(MinecraftClient client) {
        WexSideClient root = WexSideClient.getInstance();
        AlternativeMenuKeyModule menu = root.getModuleManager().getModule(AlternativeMenuKeyModule.class);
        BindSetting key = (BindSetting) menu.getSettings().stream().filter(s -> "key".equals(s.getId())).findFirst().orElseThrow();
        BindInput previousBinding = key.getBindInput();
        Screen previousScreen = client.currentScreen;
        try {
            key.setBindInput(BindInput.keyboard(345));
            // Model the in-world no-screen precondition without creating a test world.
            // setScreen(null) redirects to TitleScreen when no world is loaded.
            client.currentScreen = null;
            WexSideClient.getEventBus().post(new KeyPressedEvent(345, 0));
            require(client.currentScreen instanceof WexsideScreen, "Alternative menu key opens client screen");
            WexSideClient.getEventBus().post(new KeyPressedEvent(345, 0));
            require(!(client.currentScreen instanceof WexsideScreen), "Alternative menu key closes client screen");
        } finally {
            key.setBindInput(previousBinding);
            client.setScreen(previousScreen);
        }
        PotionEditorState state = new PotionEditorState();
        var controller = root.getPotionPresetController();
        PotionSelectorPopup popup = new PotionSelectorPopup(controller, state, new ItemIconRenderer(), entry -> {});
        List<GuiElement> previousRows = List.of();
        for (String query : List.of("", "no_matching_potion_probe", "strength", "", "a", "")) {
            state.setSearchQuery(query);
            popup.rebuildFilteredOptions();
            long expectedRows = controller.getCatalogSortedByAvailability().stream()
                    .filter(entry -> query.isEmpty() || entry.getDisplayName().toLowerCase(Locale.ROOT).contains(query)).count();
            require(popup.getChildren().size() == expectedRows + 1, "Potion popup retains only matching rows and search field");
            for (GuiElement old : previousRows) require(old.getParent() == null, "Removed potion row is detached");
            List<GuiElement> currentRows = new ArrayList<>();
            for (GuiElement child : popup.getChildren()) {
                require(child.getParent() == popup, "Current potion child owns correct parent");
                if (child instanceof PotionOptionRow) currentRows.add(child);
            }
            if (query.isEmpty()) require(!currentRows.isEmpty(), "Popup lifecycle test exercises real catalog entries");
            previousRows = currentRows;
        }
        System.out.println("RECOVERED GUI CHECKS PASS: alternative menu open/close, six potion query rebuilds, child ownership");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
