package ru.wexside.recovery.warden;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;

/** Complete IlIlIillIiIilil policy; original enum supplier is supplied, not guessed. */
public final class WardenLootFilter {
    private final Supplier<WardenLootLevel> level;

    public WardenLootFilter(Supplier<WardenLootLevel> level) {
        this.level = Objects.requireNonNull(level);
    }

    public int threshold(int base) {
        return level.get().scale(base);
    }

    public boolean shouldSkip(ItemStack stack) {
        WardenLootLevel current = level.get();
        if (current == WardenLootLevel.LOW) {
            return false;
        }
        return skippedAtMedium(stack) || current == WardenLootLevel.HIGH && skippedAtHigh(stack);
    }

    private static boolean skippedAtMedium(ItemStack stack) {
        return stack.isIn(ItemTags.SHOVELS) || stack.isIn(ItemTags.AXES) || stack.isIn(ItemTags.BANNERS)
                || Registries.ITEM.getId(stack.getItem()).getPath().endsWith("armor_trim_smithing_template")
                || stack.isOf(Items.BLAZE_ROD) || stack.isOf(Items.ENCHANTED_BOOK)
                || stack.isOf(Items.TRIDENT) || stack.isOf(Items.NAME_TAG)
                || stack.isOf(Items.SCULK) || stack.isOf(Items.SCULK_SENSOR)
                || stack.isOf(Items.ENDER_CHEST) || stack.isOf(Items.REINFORCED_DEEPSLATE)
                || stack.isOf(Items.PUFFERFISH) || stack.isOf(Items.HONEY_BOTTLE)
                || stack.isOf(Items.FERMENTED_SPIDER_EYE) || stack.isOf(Items.ANVIL)
                || stack.isOf(Items.COOKED_PORKCHOP);
    }

    private static boolean skippedAtHigh(ItemStack stack) {
        return stack.isIn(ItemTags.ARROWS) || stack.isIn(ItemTags.PICKAXES)
                || stack.isOf(Items.CHORUS_FRUIT) || stack.isOf(Items.DISC_FRAGMENT_5)
                || stack.isOf(Items.NAUTILUS_SHELL) || stack.isOf(Items.BOOKSHELF)
                || stack.isOf(Items.COOKED_MUTTON) || stack.isOf(Items.SKELETON_SPAWN_EGG)
                || stack.isOf(Items.CREEPER_SPAWN_EGG) || stack.isOf(Items.ZOMBIE_SPAWN_EGG)
                || stack.isOf(Items.VINDICATOR_SPAWN_EGG) || stack.isOf(Items.PIGLIN_SPAWN_EGG)
                || stack.isOf(Items.VEX_SPAWN_EGG) || stack.isOf(Items.ENDERMITE_SPAWN_EGG)
                || stack.isOf(Items.CAT_SPAWN_EGG) || stack.isOf(Items.FIRE_CHARGE)
                || stack.isOf(Items.LEATHER) || stack.isOf(Items.SHULKER_SHELL)
                || stack.isOf(Items.EXPERIENCE_BOTTLE) || stack.isOf(Items.WITHER_ROSE)
                || stack.isOf(Items.EMERALD) || stack.isOf(Items.SUGAR)
                || stack.isOf(Items.GHAST_TEAR) || stack.isOf(Items.DRAGON_BREATH)
                || stack.isOf(Items.ENCHANTING_TABLE) || stack.isOf(Items.DIAMOND_HELMET)
                || stack.isOf(Items.DIAMOND_CHESTPLATE) || stack.isOf(Items.DIAMOND_LEGGINGS)
                || stack.isOf(Items.DIAMOND_BOOTS) || stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE)
                || "potion-popper".equals(customItemId(stack));
    }

    private static String customItemId(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return "";
        }
        NbtCompound bukkit = customData.copyNbt().getCompound("PublicBukkitValues").orElse(null);
        return bukkit == null ? "" : bukkit.getString("minecraft:ftid").orElse("");
    }
}
