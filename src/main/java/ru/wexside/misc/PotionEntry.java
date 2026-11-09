package ru.wexside.misc;

import java.util.Objects;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;

public final class PotionEntry {
    private final RegistryEntry<StatusEffect> effect;
    private final String name;

    public PotionEntry(String name, RegistryEntry<StatusEffect> effect) {
        this.name = name;
        this.effect = effect;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PotionEntry)) {
            return false;
        }
        PotionEntry other = (PotionEntry)object;
        return Objects.equals(this.name, other.name) && Objects.equals(this.effect, other.effect);
    }

    public String toString() {
        return "PotionEntry[label=" + this.name + ", effect=" + String.valueOf(this.effect) + "]";
    }

    public int hashCode() {
        return Objects.hash(this.name, this.effect);
    }

    public String getString() {
        return this.name;
    }

    public String getName() {
        return this.name;
    }

    public RegistryEntry<StatusEffect> getEffect() {
        return this.effect;
    }
}

