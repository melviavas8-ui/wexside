package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import ru.wexside.WexSideClient;
import ru.wexside.misc.Bundles;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.HotbarSelectAction;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.ItemUseResult;
import ru.wexside.misc.ServerKind;
import ru.wexside.misc.SwapTiming;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.misc.UseItemAction;
import ru.wexside.module.combat.MaceHelperModule;
import ru.wexside.server.ServerHelperAction;
import ru.wexside.server.ServerHelperActions;
import ru.wexside.server.ServerItemMatcher;
import ru.wexside.util.InventoryController;

public class ServerHelper {
    private final String inventoryTaskOwner;

    public ServerHelper() {
        this.inventoryTaskOwner = "server_helper";
    }

    public ItemUseResult useActionItemWithBundleFallback(ServerHelperAction action, ServerKind serverKind2, SwapTiming timing, Runnable runnable, Runnable runnable2, boolean bl, boolean bl2) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null) {
            return ItemUseResult.UNAVAILABLE;
        }
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null) {
            return ItemUseResult.UNAVAILABLE;
        }
        if (ServerItemMatcher.matches(player2.getOffHandStack(), action, serverKind2)) {
            if (runnable != null) {
                runnable.run();
            }
            inventory.submit(InventoryTask.builder().action(new UseItemAction()).owner("server_helper").flag(TaskFlag.DEFAULT).policy(ClickPolicy.SILENT).priority(TaskPriority.NORMAL).build());
            if (runnable2 != null) {
                runnable2.run();
            }
            this.afterUse(action);
            return ItemUseResult.USED_OFFHAND;
        }
        PlayerInventory inv = player2.getInventory();
        int n = ServerItemMatcher.findSlot(inv, action, serverKind2, false);
        if (n == -1) {
            int[] nArray;
            if (bl && (nArray = Bundles.findInBundle(inv, stack -> ServerItemMatcher.matches(stack, action, serverKind2))) != null) {
                Runnable runnable3 = () -> {
                    if (runnable != null) {
                        runnable.run();
                    }
                    inventory.pressUseForTick();
                    if (runnable2 != null) {
                        runnable2.run();
                    }
                };
                Bundles.useFromBundle(player2, inventory, "server_helper", nArray[0], nArray[1], bl2, runnable3);
                this.afterUse(action);
                return ItemUseResult.QUEUED_INVENTORY_SWAP;
            }
            return ItemUseResult.UNAVAILABLE;
        }
        if (n < 9) {
            if (runnable != null) {
                runnable.run();
            }
            inventory.submit(InventoryTask.builder().action(new HotbarSelectAction(n, true)).owner("server_helper").flag(TaskFlag.DEFAULT).policy(ClickPolicy.SILENT).priority(TaskPriority.NORMAL).build());
            if (runnable2 != null) {
                runnable2.run();
            }
            this.afterUse(action);
            return ItemUseResult.SELECTED_HOTBAR;
        }
        int n2 = inv.getSelectedSlot();
        Runnable runnable4 = runnable;
        Runnable runnable5 = runnable2;
        Runnable runnable6 = () -> {
            if (runnable4 != null) {
                runnable4.run();
            }
            inventory.pressUseForTick();
            if (runnable5 != null) {
                runnable5.run();
            }
        };
        int n3 = n;
        Runnable runnable7 = null;
        if (action == ServerHelperActions.WIND_CHARGE && MaceHelperModule.isEnabled2()) {
            if (MaceHelperModule.isEnabled5()) {
                MaceHelperModule.refreshSession();
            } else if (!MaceHelperModule.isEnabled6()) {
                if (MaceHelperModule.isEnabled3() || MaceHelperModule.isEnabled()) {
                    MaceHelperModule.startSession();
                } else {
                    n3 = MaceHelperModule.findMaceContainerSlot();
                    MaceHelperModule.startSwapSession(n3, n2);
                }
            }
            if (MaceHelperModule.isEnabled4()) {
                runnable7 = MaceHelperModule::selectMaceHotbar;
            }
        }
        inventory.submit(InventoryTask.builder().action(inventory.createSwapSequence(n, n2, runnable6, timing, n3, runnable7)).owner("server_helper").flag(TaskFlag.DEFAULT).policy(ClickPolicy.VISIBLE).priority(TaskPriority.NORMAL).blocking(true).build());
        return ItemUseResult.QUEUED_INVENTORY_SWAP;
    }

    public ItemUseResult useActionItem(ServerHelperAction action, ServerKind serverKind2, SwapTiming timing, Runnable runnable, Runnable runnable2) {
        return this.useActionItemWithBundleFallback(action, serverKind2, timing, runnable, runnable2, false, false);
    }

    private void afterUse(ServerHelperAction action) {
        if (action != ServerHelperActions.WIND_CHARGE) {
            return;
        }
        if (MaceHelperModule.isEnabled5()) {
            MaceHelperModule.refreshSession();
        } else if (!MaceHelperModule.isEnabled6()) {
            MaceHelperModule.startSession();
        }
    }
}

