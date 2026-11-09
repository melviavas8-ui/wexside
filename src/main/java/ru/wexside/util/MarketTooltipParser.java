package ru.wexside.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class MarketTooltipParser {
    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final String BUY_PROMPT = "нажмите, чтобы купить";

    public DonMarketOffer parseDonMarketOffer(ItemStack stack, MinecraftClient client) {
        if (stack == null || stack.isEmpty() || client == null) {
            return null;
        }
        Integer balance = null;
        Integer exchangeRate = null;
        Integer price = null;
        boolean purchasable = false;
        for (Text line : Screen.getTooltipFromItem((MinecraftClient)client, (ItemStack)stack)) {
            String text = line.getString().toLowerCase();
            if (text.contains("биржа баланс:")) {
                balance = MarketTooltipParser.firstNumber(text);
                continue;
            }
            if (text.contains("курс:")) {
                exchangeRate = MarketTooltipParser.firstNumber(text);
                continue;
            }
            if (text.contains("цена:")) {
                price = MarketTooltipParser.firstNumber(text);
                continue;
            }
            if (!text.contains(BUY_PROMPT)) continue;
            purchasable = true;
        }
        if (!purchasable || balance == null || exchangeRate == null || price == null || balance < price) {
            return null;
        }
        return new DonMarketOffer(balance, exchangeRate, price);
    }

    public Integer parseAuctionPrice(ItemStack stack, MinecraftClient client) {
        if (stack == null || stack.isEmpty() || client == null) {
            return null;
        }
        boolean purchasable = false;
        Integer highestDollarValue = null;
        List<Text> tooltip = Screen.getTooltipFromItem(client, stack);
        for (Text line : tooltip) {
            Integer value;
            int dollar;
            String text = line.getString();
            if (text.toLowerCase().contains(BUY_PROMPT)) {
                purchasable = true;
            }
            if ((dollar = text.indexOf(36)) < 0 || (value = MarketTooltipParser.parseDigits(text.substring(dollar + 1))) == null || highestDollarValue != null && value <= highestDollarValue) continue;
            highestDollarValue = value;
        }
        return purchasable ? highestDollarValue : null;
    }

    public Integer parseUnitAuctionPrice(ItemStack stack, MinecraftClient client) {
        Integer total = this.parseAuctionPrice(stack, client);
        return total == null ? null : Integer.valueOf(total / Math.max(1, stack.getCount()));
    }

    private static Integer firstNumber(String text) {
        Matcher matcher = NUMBER.matcher(text);
        return matcher.find() ? MarketTooltipParser.parseDigits(matcher.group()) : null;
    }

    private static Integer parseDigits(String text) {
        String digits = text.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        }
        catch (NumberFormatException ignored) {
            return null;
        }
    }

    public record DonMarketOffer(int balance, int exchangeRate, int price) {
    }
}

