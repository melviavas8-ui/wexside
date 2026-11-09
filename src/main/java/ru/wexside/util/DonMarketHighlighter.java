package ru.wexside.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import ru.wexside.misc.SlotHighlight;
import ru.wexside.misc.SlotHighlightProvider;
import ru.wexside.util.MarketTooltipParser;

public final class DonMarketHighlighter
implements SlotHighlightProvider {
    private final BooleanSupplier enabled;
    private final IntSupplier highlightCount;
    private final IntSupplier highlightColor;
    private final MarketTooltipParser tooltipParser = new MarketTooltipParser();

    public DonMarketHighlighter(BooleanSupplier enabled, IntSupplier highlightCount, IntSupplier highlightColor) {
        this.enabled = enabled;
        this.highlightCount = highlightCount;
        this.highlightColor = highlightColor;
    }

    @Override
    public List<SlotHighlight> collectHighlights(HandledScreen<?> screen) {
        if (!this.enabled.getAsBoolean() || !screen.getTitle().getString().contains("ДонМаркет")) {
            return List.of();
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ScreenHandler handler = screen.getScreenHandler();
        ArrayList<ScoredSlot> offers = new ArrayList<ScoredSlot>();
        for (int index = 0; index < handler.slots.size(); ++index) {
            Slot slot = (Slot)handler.slots.get(index);
            ItemStack stack = slot.getStack();
            MarketTooltipParser.DonMarketOffer offer = this.tooltipParser.parseDonMarketOffer(stack, client);
            if (offer == null) continue;
            offers.add(new ScoredSlot(index, offer.exchangeRate()));
        }
        offers.sort(Comparator.comparingInt(ScoredSlot::score).reversed());
        int count = Math.min(Math.max(0, this.highlightCount.getAsInt()), offers.size());
        ArrayList<SlotHighlight> highlights = new ArrayList<SlotHighlight>(count);
        for (int index = 0; index < count; ++index) {
            highlights.add(new SlotHighlight(((ScoredSlot)offers.get(index)).slotIndex(), this.highlightColor.getAsInt()));
        }
        return highlights;
    }

    private record ScoredSlot(int slotIndex, int score) {
    }
}

