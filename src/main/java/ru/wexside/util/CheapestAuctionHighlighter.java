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

public final class CheapestAuctionHighlighter
implements SlotHighlightProvider {
    private final BooleanSupplier enabled;
    private final IntSupplier highlightCount;
    private final IntSupplier highlightColor;
    private final MarketTooltipParser tooltipParser = new MarketTooltipParser();

    public CheapestAuctionHighlighter(BooleanSupplier enabled, IntSupplier highlightCount, IntSupplier highlightColor) {
        this.enabled = enabled;
        this.highlightCount = highlightCount;
        this.highlightColor = highlightColor;
    }

    @Override
    public List<SlotHighlight> collectHighlights(HandledScreen<?> screen) {
        String title = screen.getTitle().getString();
        if (!this.enabled.getAsBoolean() || !title.contains("/") || title.contains("ДонМаркет")) {
            return List.of();
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ScreenHandler handler = screen.getScreenHandler();
        ArrayList<PricedSlot> offers = new ArrayList<PricedSlot>();
        for (int index = 0; index < handler.slots.size(); ++index) {
            Slot slot = (Slot)handler.slots.get(index);
            ItemStack stack = slot.getStack();
            Integer unitPrice = this.tooltipParser.parseUnitAuctionPrice(stack, client);
            if (unitPrice == null) continue;
            offers.add(new PricedSlot(index, unitPrice));
        }
        offers.sort(Comparator.comparingInt(PricedSlot::unitPrice));
        int count = Math.min(Math.max(0, this.highlightCount.getAsInt()), offers.size());
        ArrayList<SlotHighlight> highlights = new ArrayList<SlotHighlight>(count);
        for (int index = 0; index < count; ++index) {
            highlights.add(new SlotHighlight(((PricedSlot)offers.get(index)).slotIndex(), this.highlightColor.getAsInt()));
        }
        return highlights;
    }

    private record PricedSlot(int slotIndex, int unitPrice) {
    }
}

