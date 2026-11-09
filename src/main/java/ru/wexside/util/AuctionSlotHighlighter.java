package ru.wexside.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import ru.wexside.market.AuctionPrice;
import ru.wexside.market.AuctionPriceParser;
import ru.wexside.misc.AuctionHighlightSettings;
import ru.wexside.misc.SlotHighlight;
import ru.wexside.misc.SlotHighlightProvider;
import ru.wexside.util.ContainerScreenHelper;

public final class AuctionSlotHighlighter
implements SlotHighlightProvider {
    private final Supplier<AuctionHighlightSettings> configSupplier;
    private final AuctionPriceParser priceParser = new AuctionPriceParser();
    private final BooleanSupplier enabledSupplier;

    public AuctionSlotHighlighter(BooleanSupplier enabledSupplier, Supplier<AuctionHighlightSettings> configSupplier) {
        this.enabledSupplier = enabledSupplier;
        this.configSupplier = configSupplier;
    }

    private static int withAlpha(int rgb, int alpha) {
        return alpha << 24 | rgb & 0xFFFFFF;
    }

    private static int pulseAlpha(int speed) {
        double period = Math.max(1, speed);
        float wave = 0.5f + 0.5f * (float)Math.sin((double)System.currentTimeMillis() / 1000.0 * period * Math.PI);
        return Math.round(50.0f + 180.0f * wave);
    }

    @Override
    public List<SlotHighlight> collectHighlights(HandledScreen<?> screen) {
        if (!this.enabledSupplier.getAsBoolean()) {
            return List.of();
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ScreenHandler handler = screen.getScreenHandler();
        if (handler == null || !ContainerScreenHelper.isAuctionContainer(handler, screen)) {
            return List.of();
        }
        AuctionHighlightSettings config = this.configSupplier.get();
        ArrayList<PricedSlot> pricedSlots = new ArrayList<PricedSlot>();
        for (int i = 0; i < handler.slots.size(); ++i) {
            AuctionPrice priceInfo;
            Slot slot = (Slot)handler.slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || config.enabled3 && stack.getDamage() > 0 || (priceInfo = this.priceParser.parse(stack, client)) == null || config.enabled && config.slot2 >= 0 && priceInfo.unitPrice() > config.slot2) continue;
            pricedSlots.add(new PricedSlot(i, priceInfo.totalPrice()));
        }
        if (pricedSlots.isEmpty()) {
            return List.of();
        }
        pricedSlots.sort(Comparator.comparingInt(PricedSlot::price));
        int count = Math.min(Math.max(1, config.slot4), pricedSlots.size());
        int color = config.enabled2 ? AuctionSlotHighlighter.withAlpha(config.slot, AuctionSlotHighlighter.pulseAlpha(config.slot3)) : config.slot;
        ArrayList<SlotHighlight> highlights = new ArrayList<SlotHighlight>(count);
        for (int i = 0; i < count; ++i) {
            PricedSlot pricedSlot = (PricedSlot)pricedSlots.get(i);
            highlights.add(new SlotHighlight(pricedSlot.slotIndex(), color));
        }
        return highlights;
    }

    private record PricedSlot(int slotIndex, int price) {
    }
}

