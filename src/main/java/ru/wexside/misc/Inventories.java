package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

public final class Inventories {
    private Inventories() {
    }

    public static int findSlot(Item item) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return -1;
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 36; ++slot) {
            if (!inventory.getStack(slot).isOf(item)) continue;
            return slot;
        }
        return -1;
    }

    public static int findAxeSlot() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return -1;
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isIn(ItemTags.AXES) && !stack.isOf(Items.MACE)) continue;
            return slot;
        }
        return -1;
    }

    public static int findArmorSlot(EquipmentSlot slot) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || slot == null) {
            return -1;
        }
        PlayerInventory inventory = player.getInventory();
        for (int index = 0; index < inventory.size(); ++index) {
            EquippableComponent equippable;
            ItemStack stack = inventory.getStack(index);
            if (stack.isEmpty() || player.getEquippedStack(slot) == stack || (equippable = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE)) == null || equippable.slot() != slot) continue;
            return index;
        }
        return -1;
    }

    public static int findHotbarSlot(Item item) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return -1;
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 9; ++slot) {
            if (!inventory.getStack(slot).isOf(item)) continue;
            return slot;
        }
        return -1;
    }

    public static int findInventorySlot(Item item) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return -1;
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 9; slot < 36; ++slot) {
            if (!inventory.getStack(slot).isOf(item)) continue;
            return slot;
        }
        return -1;
    }

    public static int toContainerSlot(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }

    public static boolean isFood(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.get(DataComponentTypes.FOOD) != null;
    }

    public static int findFoodHotbar() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return -1;
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 9; ++slot) {
            if (!Inventories.isFood(inventory.getStack(slot))) continue;
            return slot;
        }
        return -1;
    }

    public static int findFoodInventory() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return -1;
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 9; slot < 36; ++slot) {
            if (!Inventories.isFood(inventory.getStack(slot))) continue;
            return slot;
        }
        return -1;
    }
}

