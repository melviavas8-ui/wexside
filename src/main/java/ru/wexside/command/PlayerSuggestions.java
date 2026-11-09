package ru.wexside.command;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

final class PlayerSuggestions {
    private PlayerSuggestions() {
    }

    static List<String> onlinePlayerNames() {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) {
            return List.of();
        }
        return networkHandler.getPlayerList().stream().map(entry -> entry.getProfile().name()).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}

