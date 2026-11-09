package ru.wexside.server;

import java.util.Locale;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import ru.wexside.misc.ServerKind;
import ru.wexside.server.ServerHelperAction;

public final class ServerItemMatcher {
    private ServerItemMatcher() {
    }

    public static boolean matches(ItemStack stack, ServerHelperAction action, ServerKind serverKind) {
        if (stack == null || stack.isEmpty() || action == null) {
            return false;
        }
        if (action.matchByItem()) {
            return stack.isOf(action.icon());
        }
        String name = ServerItemMatcher.normalize(stack.getName());
        if (serverKind == ServerKind.OTHERS) {
            return action.alternateServerTags().stream().map(ServerItemMatcher::normalize).anyMatch(tag -> !tag.isEmpty() && name.contains((CharSequence)tag));
        }
        String generalTag = ServerItemMatcher.normalize(action.generalServerTag());
        return !generalTag.isEmpty() && name.contains(generalTag);
    }

    public static ItemStack findStack(PlayerInventory inventory, ServerHelperAction action, ServerKind serverKind) {
        int slot = ServerItemMatcher.findSlot(inventory, action, serverKind, false);
        return slot < 0 ? ItemStack.EMPTY : inventory.getStack(slot);
    }

    public static int findSlot(PlayerInventory inventory, ServerHelperAction action, ServerKind serverKind, boolean hotbarOnly) {
        if (inventory == null || action == null) {
            return -1;
        }
        int limit = hotbarOnly ? Math.min(9, inventory.size()) : inventory.size();
        for (int slot = 0; slot < limit; ++slot) {
            if (!ServerItemMatcher.matches(inventory.getStack(slot), action, serverKind)) continue;
            return slot;
        }
        return -1;
    }

    private static String normalize(Text text) {
        return text == null ? "" : ServerItemMatcher.normalize(text.getString());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}

