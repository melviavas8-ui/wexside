package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

public final class HotbarTracker {
    public ItemStack simulate(int ticksAhead) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || ticksAhead <= 0 || !player.isUsingItem()) {
            return null;
        }
        ItemStack activeItem = player.getActiveItem();
        return activeItem.isEmpty() ? null : activeItem.copy();
    }

    public ItemStack copyActiveItemWhenUsing(int ticksAhead) {
        return this.simulate(ticksAhead);
    }
}

