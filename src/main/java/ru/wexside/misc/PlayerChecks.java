package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

public final class PlayerChecks {
    private PlayerChecks() {
    }

    public static boolean isHoldingWeapon() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }
        ItemStack stack = player.getMainHandStack();
        return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES) || stack.isOf(Items.MACE) || stack.isOf(Items.TRIDENT);
    }

    public static boolean isUsingItem() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && player.isUsingItem();
    }
}

