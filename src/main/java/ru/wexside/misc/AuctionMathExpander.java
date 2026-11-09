package ru.wexside.misc;

import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.wexside.event.OutgoingChatEvent;

public class AuctionMathExpander {
    private final BooleanSupplier enabledSupplier;
    private final String[] auctionCommandPrefixes;
    private final Pattern multiplicationPattern;

    public AuctionMathExpander() {
        this(() -> true);
    }

    public AuctionMathExpander(BooleanSupplier enabled) {
        this.enabledSupplier = enabled == null ? () -> false : enabled;
        this.auctionCommandPrefixes = new String[]{"/ah", "/ah ", "ah "};
        this.multiplicationPattern = Pattern.compile("(\\d+)\\s*[*x\u00d7]\\s*(\\d+)");
    }

    private String expandMultiplications(String string) {
        Matcher matcher = this.multiplicationPattern.matcher(string);
        StringBuilder stringBuilder = new StringBuilder();
        while (matcher.find()) {
            try {
                long l = Long.parseLong(matcher.group(1));
                long l2 = Long.parseLong(matcher.group(2));
                long l3 = l * l2;
                matcher.appendReplacement(stringBuilder, Long.toString(l3));
            }
            catch (NumberFormatException numberFormatException) {
                matcher.appendReplacement(stringBuilder, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(stringBuilder);
        return stringBuilder.toString();
    }

    private boolean hasSupportedCommandPrefix(String string) {
        for (String string2 : this.auctionCommandPrefixes) {
            if (!string.startsWith(string2)) continue;
            return true;
        }
        return false;
    }

    public void onOutgoingChat(OutgoingChatEvent gameEvent15) {
        if (!this.enabledSupplier.getAsBoolean()) {
            return;
        }
        String string = gameEvent15.getString();
        if (string == null || !this.hasSupportedCommandPrefix(string)) {
            return;
        }
        String string2 = this.expandMultiplications(string);
        if (string2.equals(string)) {
            return;
        }
        gameEvent15.setString(string2);
    }
}

