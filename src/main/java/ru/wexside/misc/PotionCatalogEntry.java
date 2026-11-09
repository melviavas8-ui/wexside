package ru.wexside.misc;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;
import ru.wexside.misc.ServerKind;
import ru.wexside.server.ServerHelperAction;
import ru.wexside.server.ServerItemMatcher;

public final class PotionCatalogEntry {
    private final ServerHelperAction action;
    private final String id;
    private final String displayName;
    private final RegistryEntry<Potion> potion;
    public static final String SERVER_ACTION_PREFIX = "server:";
    private final int color;

    public boolean matches(ItemStack stack, ServerKind serverKind) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (this.action != null) {
            return ServerItemMatcher.matches(stack, this.action, serverKind);
        }
        PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
        return contents != null && contents.matches(this.potion);
    }

    private PotionCatalogEntry(String id, String displayName, int color, ServerHelperAction action, RegistryEntry<Potion> potion) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.action = action;
        this.potion = potion;
    }

    public RegistryEntry<Potion> getPotion() {
        return this.potion;
    }

    public String getId() {
        return this.id;
    }

    public static PotionCatalogEntry forServerAction(ServerHelperAction action) {
        return new PotionCatalogEntry(SERVER_ACTION_PREFIX + action.id(), action.selectorLabel(), action.color(), action, null);
    }

    public static PotionCatalogEntry forPotion(String id, String displayName, int color, RegistryEntry<Potion> potion) {
        return new PotionCatalogEntry(id, displayName, color, null, potion);
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean isServerAction() {
        return this.action != null;
    }

    public ItemStack createDefaultStack() {
        return this.action != null ? new ItemStack((ItemConvertible)this.action.icon()) : PotionContentsComponent.createStack((Item)Items.SPLASH_POTION, this.potion);
    }

    public int getColor() {
        return this.color;
    }

    public ServerHelperAction getAction() {
        return this.action;
    }
}

