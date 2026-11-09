package ru.wexside.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import ru.wexside.WexSideClient;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.AttackUrgency;
import ru.wexside.misc.Bundles;
import ru.wexside.misc.CorrectionMode;
import ru.wexside.misc.ItemAlerts;
import ru.wexside.misc.ServerHelper;
import ru.wexside.misc.ServerKind;
import ru.wexside.misc.SwapTiming;
import ru.wexside.server.ServerHelperAction;
import ru.wexside.server.ServerHelperActions;
import ru.wexside.server.ServerItemMatcher;
import ru.wexside.util.Angle;
import ru.wexside.util.InventoryController;
import ru.wexside.util.RotationController;
import ru.wexside.util.RotationIntent;

public class FTSnap {
    private final BooleanSupplier ftModeEnabled;
    private Angle heldSnapAngle;
    private final Supplier<ServerKind> serverKindSupplier;
    private int aimWaitTicks;
    private final BooleanSupplier windChargeSwapDelayEnabled;
    private int snapHoldTicks;
    private final float value;
    private final String strategyName;
    private final BooleanSupplier bundleUseEnabled;
    private final int slot3;
    private final int slot4;
    private final BooleanSupplier potionAimDownEnabled;
    private final float value2;
    private final List<ServerHelperAction> pendingActions;
    private final ServerHelper serverHelper = new ServerHelper();
    private final int slot5;
    private final BooleanSupplier windChargeAimDownEnabled;
    private final Supplier<SwapTiming> swapTimingSupplier;

    public FTSnap(EventBus eventBus, Supplier<ServerKind> supplier, Supplier<SwapTiming> supplier2, BooleanSupplier booleanSupplier, BooleanSupplier booleanSupplier2, BooleanSupplier booleanSupplier3, BooleanSupplier booleanSupplier4, BooleanSupplier booleanSupplier5) {
        this.value = 90.0f;
        this.value2 = 0.5f;
        this.slot3 = 10;
        this.slot5 = 3;
        this.slot4 = 2;
        this.strategyName = "FT Snap";
        this.pendingActions = new ArrayList<ServerHelperAction>();
        this.serverKindSupplier = supplier;
        this.swapTimingSupplier = supplier2;
        this.potionAimDownEnabled = booleanSupplier;
        this.windChargeAimDownEnabled = booleanSupplier2;
        this.windChargeSwapDelayEnabled = booleanSupplier3;
        this.bundleUseEnabled = booleanSupplier4;
        this.ftModeEnabled = booleanSupplier5;
        eventBus.subscribe(ClientTickEvent.class, gameEvent6 -> this.tickSnapActions());
    }

    public boolean hasQueuedActions() {
        return !this.pendingActions.isEmpty();
    }

    private boolean isLookingStraightDown() {
        RotationController rotations = WexSideClient.getRotationController();
        return rotations != null && rotations.isObservedPitchWithin(90.0f, 0.5f);
    }

    private ItemStack findActionStack(ClientPlayerEntity player, ServerHelperAction action, ServerKind serverKind) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        if (ServerItemMatcher.matches(player.getOffHandStack(), action, serverKind)) {
            return player.getOffHandStack();
        }
        return ServerItemMatcher.findStack(player.getInventory(), action, serverKind);
    }

    private boolean shouldAimDown(ServerHelperAction action) {
        if (action == ServerHelperActions.WIND_CHARGE) {
            return this.windChargeAimDownEnabled.getAsBoolean();
        }
        if (!action.splashPotion() || action.debuff()) {
            return false;
        }
        return this.potionAimDownEnabled.getAsBoolean();
    }

    private void applySnapAngle(RotationController rotations, Angle angle) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null) {
            return;
        }
        RotationIntent intent = new RotationIntent((LivingEntity)player2, null, angle, AttackUrgency.HIT, CorrectionMode.NONE, false);
        rotations.applyIntent(intent, "FT Snap");
    }

    public void queueAction(ServerHelperAction action) {
        if (action == null) {
            return;
        }
        if (!this.pendingActions.contains(action)) {
            this.pendingActions.add(action);
        }
    }

    public void clearQueuedActions() {
        this.pendingActions.clear();
        this.aimWaitTicks = 0;
    }

    private Runnable createWindChargeSwapDelay(ServerHelperAction action) {
        if (action != ServerHelperActions.WIND_CHARGE) {
            return null;
        }
        if (!this.windChargeAimDownEnabled.getAsBoolean() || !this.windChargeSwapDelayEnabled.getAsBoolean()) {
            return null;
        }
        return () -> {
            InventoryController inventory = WexSideClient.getInventoryController();
            if (inventory == null) {
                return;
            }
            int n = inventory.hasCurrentTask() ? 1 : 2;
            inventory.setIntType(Math.max(0, n));
        };
    }

    private Runnable createAimDownCallback(ServerHelperAction action) {
        if (action == ServerHelperActions.WIND_CHARGE) {
            return this.windChargeAimDownEnabled.getAsBoolean() ? this::aimStraightDown : null;
        }
        if (!action.splashPotion() || action.debuff()) {
            return null;
        }
        return this.potionAimDownEnabled.getAsBoolean() ? this::aimStraightDown : null;
    }

    private void executeQueuedActions() {
        if (this.pendingActions.isEmpty()) {
            return;
        }
        ServerKind serverKind2 = this.serverKindSupplier.get();
        SwapTiming timing = this.swapTimingSupplier.get();
        boolean bl = this.bundleUseEnabled.getAsBoolean();
        boolean bl2 = this.ftModeEnabled.getAsBoolean();
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        Iterator<ServerHelperAction> iterator = this.pendingActions.iterator();
        while (iterator.hasNext()) {
            ItemStack stack;
            boolean bl3;
            ServerHelperAction action = iterator.next();
            String string = action.selectorLabel();
            ItemStack stack2 = this.findActionStack(player2, action, serverKind2);
            boolean bl4 = bl3 = bl && player2 != null && Bundles.contains(player2.getInventory(), stackInBundle -> ServerItemMatcher.matches(stackInBundle, action, serverKind2));
            if (stack2.isEmpty() && !bl3) {
                ItemAlerts.warnMissing(new ItemStack((ItemConvertible)action.icon()), string);
                iterator.remove();
                this.aimWaitTicks = 0;
                continue;
            }
            ItemStack stack3 = stack = stack2.isEmpty() ? new ItemStack((ItemConvertible)action.icon()) : stack2;
            if (ItemAlerts.isBusy(player2.currentScreenHandler, stack, string)) {
                iterator.remove();
                this.aimWaitTicks = 0;
                continue;
            }
            if (this.shouldAimDown(action) && !this.isLookingStraightDown()) {
                this.aimStraightDown();
                if (this.aimWaitTicks++ < 10) continue;
            }
            this.serverHelper.useActionItemWithBundleFallback(action, serverKind2, timing, this.createAimDownCallback(action), this.createWindChargeSwapDelay(action), bl, bl2);
            iterator.remove();
            this.aimWaitTicks = 0;
        }
    }

    private void advanceSnapHold() {
        if (this.snapHoldTicks <= 0) {
            return;
        }
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations == null) {
            this.snapHoldTicks = 0;
            this.heldSnapAngle = null;
            return;
        }
        --this.snapHoldTicks;
        if (this.snapHoldTicks > 0 && this.heldSnapAngle != null) {
            this.applySnapAngle(rotations, this.heldSnapAngle);
        } else {
            rotations.clearIntent();
            this.heldSnapAngle = null;
        }
    }

    private void tickSnapActions() {
        this.advanceSnapHold();
        this.executeQueuedActions();
    }

    private void aimStraightDown() {
        Angle angle;
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        RotationController rotations = WexSideClient.getRotationController();
        if (player2 == null || rotations == null) {
            return;
        }
        this.heldSnapAngle = angle = new Angle(player2.getYaw(), 90.0f);
        this.snapHoldTicks = 4;
        this.applySnapAngle(rotations, angle);
    }
}

