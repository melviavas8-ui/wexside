package ru.wexside.misc;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import ru.wexside.WexSideClient;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.ClientChat;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.PickupSlotAction;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.util.InventoryController;

public class ServerHelperShulker {
    private final String inventoryTaskOwner;

    public ServerHelperShulker() {
        this.inventoryTaskOwner = "server_helper_shulker";
    }

    public void update() {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null) {
            return;
        }
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null) {
            return;
        }
        if (this.isActive()) {
            ClientChat.send("Во время открытия шалкера нельзя двигаться.");
            return;
        }
        PlayerInventory inv = player2.getInventory();
        int n = this.findShulkerInventorySlot(inv);
        if (n == -1) {
            ClientChat.send("В Вашем инвентаре нет шалкера.");
            return;
        }
        int n2 = n < 9 ? n + 36 : n;
        inventory.submit(InventoryTask.builder().action(new PickupSlotAction(n2, 1)).owner("server_helper_shulker").flag(TaskFlag.REPLACE).policy(ClickPolicy.SILENT).priority(TaskPriority.NORMAL).build());
    }

    private boolean isActive() {
        GameOptions options = MinecraftClient.getInstance().options;
        if (options == null) {
            return false;
        }
        return options.forwardKey.isPressed() || options.backKey.isPressed() || options.leftKey.isPressed() || options.rightKey.isPressed() || options.jumpKey.isPressed();
    }

    private int findShulkerInventorySlot(PlayerInventory inv) {
        for (int i = 0; i < 36; ++i) {
            BlockItem blockItem2;
            Item iiIilIIilI2;
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty() || !((iiIilIIilI2 = stack.getItem()) instanceof BlockItem) || !((blockItem2 = (BlockItem)iiIilIIilI2).getBlock() instanceof ShulkerBoxBlock)) continue;
            return i;
        }
        return -1;
    }
}

