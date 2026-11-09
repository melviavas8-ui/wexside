package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

public final class ItemAlerts {
    private ItemAlerts() {
    }

    public static void warnMissing(ItemStack stack, String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        client.player.sendMessage((Text)Text.literal((String)("Нет предмета: " + name)), true);
    }

    public static boolean isBusy(ScreenHandler ignored, ItemStack stack, String ignoredName) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && client.player.getItemCooldownManager().isCoolingDown(stack);
    }
}

