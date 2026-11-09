package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import ru.wexside.util.ColorUtils;

public final class ClientChat {
    private static final int BRAND_COLOR = -7643914;

    private ClientChat() {
    }

    public static void send(String message) {
        ClientChat.send((Text)Text.literal((String)message));
    }

    public static void send(Text message) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.sendMessage((Text)ClientChat.createPrefix().append((Text)Text.literal((String)" >> ").formatted(Formatting.DARK_PURPLE)).append(message), false);
        }
    }

    private static MutableText createPrefix() {
        String brand = "WexSide";
        MutableText result = Text.empty();
        for (int index = 0; index < brand.length(); ++index) {
            int color = ColorUtils.animatedGradient(-7643914, -1, index * 12, 12);
            result.append((Text)Text.literal((String)String.valueOf(brand.charAt(index))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)color))));
        }
        return result;
    }
}

