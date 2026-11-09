package ru.wexside.util;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

public final class ContainerScreenHelper {
    private ContainerScreenHelper() {
    }

    public static boolean isAuctionContainer(ScreenHandler handler, Screen screen) {
        String lower;
        Text title;
        if (screen != null && (title = screen.getTitle()) != null && ((lower = title.getString().toLowerCase()).contains("auction") || lower.contains("аукцион") || lower.contains("donmarket") || lower.contains("донмаркет"))) {
            return true;
        }
        return handler != null && handler.syncId != 0 && handler.slots.size() >= 27;
    }

    public static boolean isPlayerInventoryContainer(ScreenHandler handler, Screen screen) {
        return !ContainerScreenHelper.isAuctionContainer(handler, screen);
    }
}

