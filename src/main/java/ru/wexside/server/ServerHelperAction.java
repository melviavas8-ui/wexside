package ru.wexside.server;

import java.util.List;
import java.util.Objects;
import net.minecraft.item.Item;

public record ServerHelperAction(String id, String displayName, String bindLabel, Item icon, int color, String generalServerTag, List<String> alternateServerTags, boolean donationItem, boolean splashPotion, float activationDistance, boolean debuff, boolean matchByItem) {
    public ServerHelperAction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(bindLabel, "bindLabel");
        Objects.requireNonNull(icon, "icon");
        generalServerTag = generalServerTag == null ? "" : generalServerTag;
        alternateServerTags = List.copyOf(alternateServerTags == null ? List.of() : alternateServerTags);
    }

    public String selectorLabel() {
        return this.alternateServerTags.isEmpty() ? this.displayName : this.alternateServerTags.getFirst();
    }
}

