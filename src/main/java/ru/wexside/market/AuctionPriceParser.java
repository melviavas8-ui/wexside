package ru.wexside.market;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import ru.wexside.market.AuctionPrice;

public final class AuctionPriceParser {
    private static final String PURCHASE_MARKER = "чтобы купить";

    public AuctionPrice parse(ItemStack stack, MinecraftClient client) {
        if (stack == null || stack.isEmpty() || client == null) {
            return null;
        }
        List<Text> tooltip = Screen.getTooltipFromItem(client, stack);
        boolean auctionItem = false;
        Integer totalPrice = null;
        Integer unitPrice = null;
        Integer dollarPrice = null;
        for (Text line : tooltip) {
            String text = line.getString();
            String lowerCase = text.toLowerCase(Locale.ROOT);
            auctionItem |= lowerCase.contains(PURCHASE_MARKER);
            if (lowerCase.contains("за 1 ед")) {
                unitPrice = AuctionPriceParser.parseAfterColon(text);
                continue;
            }
            if (lowerCase.contains("цена")) {
                totalPrice = AuctionPriceParser.parseAfterColon(text);
                continue;
            }
            int dollarIndex = text.indexOf(36);
            if (dollarIndex < 0) continue;
            dollarPrice = AuctionPriceParser.parseDigits(text.substring(dollarIndex + 1));
        }
        if (!auctionItem) {
            return null;
        }
        if (totalPrice == null) {
            totalPrice = dollarPrice;
        }
        int count = Math.max(1, stack.getCount());
        if (unitPrice == null && totalPrice != null) {
            unitPrice = totalPrice / count;
        }
        if (unitPrice == null) {
            return null;
        }
        if (totalPrice == null) {
            totalPrice = (int)Math.min((long)unitPrice.intValue() * (long)count, Integer.MAX_VALUE);
        }
        return new AuctionPrice(totalPrice, unitPrice);
    }

    public boolean isAuctionItem(ItemStack stack, MinecraftClient client) {
        if (stack == null || stack.isEmpty() || client == null) {
            return false;
        }
        return Screen.getTooltipFromItem((MinecraftClient)client, (ItemStack)stack).stream().map(Text::getString).map(text -> text.toLowerCase(Locale.ROOT)).anyMatch(text -> text.contains(PURCHASE_MARKER));
    }

    private static Integer parseAfterColon(String text) {
        int colonIndex = text.lastIndexOf(58);
        return AuctionPriceParser.parseDigits(colonIndex >= 0 ? text.substring(colonIndex + 1) : text);
    }

    private static Integer parseDigits(String text) {
        String digits = text.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return (int)Math.min(Long.parseLong(digits), Integer.MAX_VALUE);
        }
        catch (NumberFormatException ignored) {
            return null;
        }
    }
}

