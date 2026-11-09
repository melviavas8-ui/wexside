package ru.wexside.misc;

import java.util.List;

public record PotionPreset(String name, int keyCode, boolean favorite, List<String> potionIds) {
    public PotionPreset {
        name = name == null ? "" : name;
        potionIds = potionIds == null ? List.of() : List.copyOf(potionIds);
    }
}

