package ru.wexside.misc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.wexside.misc.PotionCatalogEntry;
import ru.wexside.misc.ServerKind;
import ru.wexside.server.ServerHelperActions;

public final class PotionCatalog {
    private static final List<PotionCatalogEntry> ALL_ENTRIES = PotionCatalog.buildEntries();
    private static final Map<String, PotionCatalogEntry> ENTRIES_BY_ID = PotionCatalog.indexEntries(ALL_ENTRIES);

    private PotionCatalog() {
    }

    public static List<PotionCatalogEntry> inventoryEntries() {
        return ALL_ENTRIES;
    }

    public static List<PotionCatalogEntry> allEntries() {
        return ALL_ENTRIES;
    }

    public static PotionCatalogEntry findById(String id) {
        return id == null ? null : ENTRIES_BY_ID.get(id);
    }

    public static int findSlot(PlayerInventory inventory, PotionCatalogEntry entry, ServerKind server) {
        if (inventory == null || entry == null) {
            return -1;
        }
        for (int slot = 0; slot < inventory.size(); ++slot) {
            if (!entry.matches(inventory.getStack(slot), server)) continue;
            return slot;
        }
        return -1;
    }

    public static ItemStack findStack(PlayerInventory inventory, PotionCatalogEntry entry, ServerKind server) {
        int slot = PotionCatalog.findSlot(inventory, entry, server);
        return slot < 0 ? ItemStack.EMPTY : inventory.getStack(slot);
    }

    private static List<PotionCatalogEntry> buildEntries() {
        ArrayList<PotionCatalogEntry> entries = new ArrayList<PotionCatalogEntry>();
        for (RegistryEntry.Reference potionEntry : Registries.POTION.streamEntries().toList()) {
            Identifier id = potionEntry.registryKey().getValue();
            Potion potion = (Potion)potionEntry.value();
            String name = Text.translatable((String)potion.getBaseName()).getString();
            int color = new PotionContentsComponent((RegistryEntry)potionEntry).getColor();
            entries.add(PotionCatalogEntry.forPotion(id.toString(), name, color, (RegistryEntry<Potion>)potionEntry));
        }
        ServerHelperActions.ALL.stream().map(PotionCatalogEntry::forServerAction).forEach(entries::add);
        return List.copyOf(entries);
    }

    private static Map<String, PotionCatalogEntry> indexEntries(List<PotionCatalogEntry> entries) {
        LinkedHashMap<String, PotionCatalogEntry> index = new LinkedHashMap<String, PotionCatalogEntry>();
        for (PotionCatalogEntry entry : entries) {
            index.put(entry.getId(), entry);
        }
        return Map.copyOf(index);
    }
}

