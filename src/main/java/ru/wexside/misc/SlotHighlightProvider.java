package ru.wexside.misc;

import java.util.List;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import ru.wexside.misc.SlotHighlight;

public interface SlotHighlightProvider {
    public List<SlotHighlight> collectHighlights(HandledScreen<?> var1);
}

