package ru.wexside.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.ActionSequence;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.ClickSlotAction;
import ru.wexside.misc.DropSlotAction;
import ru.wexside.misc.HotbarSelectAction;
import ru.wexside.misc.InventoryAction;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.PickupSlotAction;
import ru.wexside.misc.QuickMoveAction;
import ru.wexside.misc.RunnableAction;
import ru.wexside.misc.SelectSlotAction;
import ru.wexside.misc.SwapSlotsAction;
import ru.wexside.misc.SwapTiming;
import ru.wexside.misc.TaskPhase;
import ru.wexside.misc.TimedAction;
import ru.wexside.misc.UseItemAction;

public class InventoryController {
    private final Map<Integer, List<InventoryTask>> tasksByTick = new HashMap<Integer, List<InventoryTask>>();
    private int useKeyHoldTicks;
    private int slotRestoreDelayTicks;
    private int restoreHotbarSlot;
    private boolean hotbarUseBusy;
    private int scheduledSwapDelayTicks;
    private int sequenceTick;
    private InventoryTask currentTask;
    private int postSwapCooldownTicks;
    private int phaseDelayTicks;
    private int currentTick;
    private TaskPhase taskPhase;
    static final int update = 3;
    private boolean enabled;
    private boolean sequenceFinished;
    private boolean useRequested;

    public InventoryController(EventBus eventBus) {
        eventBus.subscribe(ClientTickEvent.class, this::onClientTick);
    }

    private void onClientTick(ClientTickEvent event) {
        this.tickInventoryTasks();
    }

    public void update() {
        this.enabled = true;
    }

    public void update2() {
        this.enabled = false;
    }

    public void setIntType(int n) {
        if (n <= 0) {
            this.startPostSwapCooldown();
            return;
        }
        this.scheduledSwapDelayTicks = n;
    }

    public boolean t() {
        if (this.enabled) {
            return true;
        }
        if (this.currentTask != null && this.currentTask.clickPolicy().hasTiming() && !this.sequenceFinished) {
            return true;
        }
        return this.hasBlockingQueuedTask();
    }

    public boolean hasCurrentTask() {
        return this.currentTask != null;
    }

    public void submit(InventoryTask task) {
        if (task == null || task.action() == null) {
            return;
        }
        switch (task.conflictPolicy()) {
            case REPLACE: {
                this.setString(task.owner());
                break;
            }
            case SKIP_IF_PRESENT: {
                if (!this.hasOwnerTasks(task.owner())) break;
                return;
            }
        }
        int n2 = this.currentTick + task.delayTicks();
        this.tasksByTick.computeIfAbsent(n2, n -> new ArrayList()).add(task);
    }

    public void setString(String string) {
        if (string == null) {
            return;
        }
        for (List<InventoryTask> list : this.tasksByTick.values()) {
            list.removeIf(task -> string.equals(task.owner()));
        }
        this.tasksByTick.values().removeIf(List::isEmpty);
        if (this.currentTask != null && string.equals(this.currentTask.owner())) {
            if (this.currentTask.clickPolicy().closeScreenAfterwards) {
                this.sendCloseContainer();
            }
            this.clearCurrentTask();
        }
    }

    public boolean isAvailable() {
        return this.postSwapCooldownTicks > 0 || this.scheduledSwapDelayTicks > 0;
    }

    public boolean hasOwnerTasks(String string) {
        if (string == null) {
            return false;
        }
        if (this.currentTask != null && string.equals(this.currentTask.owner())) {
            return true;
        }
        for (List<InventoryTask> list : this.tasksByTick.values()) {
            for (InventoryTask task : list) {
                if (!string.equals(task.owner())) continue;
                return true;
            }
        }
        return false;
    }

    public void pressUseForTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.options.useKey.setPressed(true);
        this.useKeyHoldTicks = 1;
        this.hotbarUseBusy = true;
    }

    public InventoryAction createSwapUseRestore(int n, int n2, Runnable runnable, SwapTiming timing) {
        return this.createSwapUseReturn(n, n2, runnable, timing, n);
    }

    public InventoryAction createSwapSequence(int n, int n2, Runnable runnable, SwapTiming timing, int n3, Runnable runnable2) {
        ArrayList<TimedAction> arrayList = new ArrayList<TimedAction>();
        arrayList.add(new TimedAction(timing.beforeClickDelay, new ClickSlotAction(n, n2)));
        arrayList.add(new TimedAction(timing.afterFirstClickDelay, new RunnableAction(runnable)));
        arrayList.add(new TimedAction(timing.secondClickDelay, new ClickSlotAction(n3, n2)));
        if (runnable2 != null) {
            arrayList.add(new TimedAction(timing.completionDelay, new RunnableAction(runnable2)));
        }
        arrayList.add(new TimedAction(timing.completionDelay, new RunnableAction(this::markSequenceFinished)));
        return new ActionSequence(arrayList);
    }

    private boolean hasBlockingQueuedTask() {
        for (List<InventoryTask> list : this.tasksByTick.values()) {
            for (InventoryTask task : list) {
                if (!task.isBlocking()) continue;
                return true;
            }
        }
        return false;
    }

    private void tickInventoryTasks() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            this.tasksByTick.clear();
            this.clearCurrentTask();
            this.resetUseState();
            ++this.currentTick;
            return;
        }
        this.advanceUseAndSlotRestore();
        if (this.scheduledSwapDelayTicks > 0 && --this.scheduledSwapDelayTicks == 0) {
            this.scheduledSwapDelayTicks = -1;
            this.postSwapCooldownTicks = 3;
        }
        if (this.currentTask != null) {
            this.advanceTaskPhase();
            this.deferCurrentTasks();
            ++this.currentTick;
            return;
        }
        List<InventoryTask> list = this.getList();
        if (!list.isEmpty()) {
            list.sort(Comparator.comparingInt((InventoryTask task) -> task.priority().ordinal()).reversed());
            Iterator<InventoryTask> iterator = list.iterator();
            while (iterator.hasNext()) {
                InventoryTask task2 = iterator.next();
                iterator.remove();
                if (!task2.condition().getAsBoolean()) continue;
                ClickPolicy clickPolicy = task2.clickPolicy();
                InventoryAction action = task2.action();
                if (action instanceof ActionSequence) {
                    ActionSequence sequence = (ActionSequence)action;
                    this.currentTask = task2;
                    this.taskPhase = TaskPhase.SEQUENCE;
                    this.sequenceTick = 0;
                    this.advanceActionSequence(sequence);
                    if (this.currentTask == null) {
                        continue;
                    }
                } else if (!clickPolicy.hasTiming()) {
                    if (InventoryController.isHotbarOrUseAction(task2.action()) && this.hotbarUseBusy) {
                        this.tasksByTick.computeIfAbsent(this.currentTick + 1, n -> new ArrayList()).add(task2);
                        continue;
                    }
                    this.executeAction(task2.action());
                    if (!InventoryController.isHotbarOrUseAction(task2.action())) {
                        continue;
                    }
                } else {
                    this.currentTask = task2;
                    if (clickPolicy.beforeDelay > 0) {
                        this.taskPhase = TaskPhase.PRE_DELAY;
                        this.phaseDelayTicks = clickPolicy.beforeDelay;
                    } else {
                        this.taskPhase = TaskPhase.EXECUTE;
                        this.phaseDelayTicks = 0;
                    }
                }
                if (!iterator.hasNext()) break;
                ArrayList deferred = new ArrayList();
                iterator.forEachRemaining(deferred::add);
                this.tasksByTick.computeIfAbsent(this.currentTick + 1, n -> new ArrayList()).addAll(deferred);
                break;
            }
        }
        ++this.currentTick;
    }

    public InventoryAction createSwapUseReturn(int n, int n2, Runnable runnable, SwapTiming timing, int n3) {
        return this.createSwapSequence(n, n2, runnable, timing, n3, null);
    }

    private void executeAction(InventoryAction action) {
        Objects.requireNonNull(action, "action");
        if (action instanceof ClickSlotAction) {
            ClickSlotAction click = (ClickSlotAction)action;
            this.clickHotbarSwap(click);
        } else if (action instanceof PickupSlotAction) {
            PickupSlotAction pickup = (PickupSlotAction)action;
            this.clickSlot(pickup.slot(), pickup.button(), SlotActionType.PICKUP);
        } else if (action instanceof SwapSlotsAction) {
            SwapSlotsAction swap = (SwapSlotsAction)action;
            this.swapSlots(swap);
        } else if (action instanceof HotbarSelectAction) {
            HotbarSelectAction select = (HotbarSelectAction)action;
            this.selectAndUse(select);
        } else if (action instanceof UseItemAction) {
            this.markUseRequested();
        } else if (action instanceof DropSlotAction) {
            DropSlotAction drop = (DropSlotAction)action;
            this.dropSlot(drop);
        } else if (action instanceof QuickMoveAction) {
            QuickMoveAction quickMove = (QuickMoveAction)action;
            this.clickSlot(quickMove.slot(), 0, SlotActionType.QUICK_MOVE);
        } else if (action instanceof SelectSlotAction) {
            SelectSlotAction select = (SelectSlotAction)action;
            this.selectSlot(select);
        } else if (action instanceof ActionSequence) {
            ActionSequence sequence = (ActionSequence)action;
            for (int delay = 0; delay <= sequence.maxDelay(); ++delay) {
                for (TimedAction step : sequence.steps()) {
                    if (step.delay() != delay) continue;
                    this.executeAction(step.action());
                }
            }
        } else if (action instanceof RunnableAction) {
            RunnableAction runnable = (RunnableAction)action;
            runnable.runnable().run();
        } else {
            throw new IllegalArgumentException("Unsupported inventory action: " + action.getClass().getName());
        }
    }

    private void clearCurrentTask() {
        this.currentTask = null;
        this.taskPhase = null;
        this.phaseDelayTicks = 0;
        this.sequenceTick = 0;
        this.sequenceFinished = false;
    }

    private void clickSlot(int slot, int button, SlotActionType actionType) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        ClientPlayerInteractionManager clientPlayerInteractionManager = MinecraftClient.getInstance().interactionManager;
        if (player2 == null || clientPlayerInteractionManager == null) {
            return;
        }
        int syncId = player2.currentScreenHandler.syncId;
        clientPlayerInteractionManager.clickSlot(syncId, slot, button, actionType, (PlayerEntity)player2);
    }

    private void sendCloseContainer() {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null) {
            return;
        }
        int n = player2.currentScreenHandler.syncId;
        if (n <= 0) {
            return;
        }
        player2.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(n));
    }

    private void selectSlot(SelectSlotAction action) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null) {
            return;
        }
        if (action.slot() < 0 || action.slot() > 8) {
            return;
        }
        PlayerInventory inv = player2.getInventory();
        if (inv.getSelectedSlot() == action.slot()) {
            return;
        }
        inv.setSelectedSlot(action.slot());
    }

    public boolean isIdle() {
        return this.currentTask == null && this.tasksByTick.isEmpty() && this.useKeyHoldTicks <= 0 && this.restoreHotbarSlot == -1;
    }

    private void deferCurrentTasks() {
        List<InventoryTask> list = this.getList();
        if (!list.isEmpty()) {
            this.tasksByTick.computeIfAbsent(this.currentTick + 1, n -> new ArrayList()).addAll(list);
        }
    }

    public boolean consumeSwapCooldownTick() {
        if (this.postSwapCooldownTicks <= 0) {
            return false;
        }
        --this.postSwapCooldownTicks;
        return true;
    }

    private void advanceUseAndSlotRestore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player2 = mc.player;
        if (player2 == null) {
            return;
        }
        if (this.useKeyHoldTicks > 0) {
            if (--this.useKeyHoldTicks == 0) {
                mc.options.useKey.setPressed(false);
            }
        } else if (this.restoreHotbarSlot != -1 && --this.slotRestoreDelayTicks <= 0) {
            player2.getInventory().setSelectedSlot(this.restoreHotbarSlot);
            this.restoreHotbarSlot = -1;
        }
        this.hotbarUseBusy = this.useKeyHoldTicks > 0 || this.restoreHotbarSlot != -1;
    }

    private void advanceTaskPhase() {
        ClickPolicy clickPolicy = this.currentTask.clickPolicy();
        switch (this.taskPhase) {
            case SEQUENCE: {
                this.advanceActionSequence((ActionSequence)this.currentTask.action());
                break;
            }
            case PRE_DELAY: {
                --this.phaseDelayTicks;
                if (this.phaseDelayTicks > 0) break;
                this.taskPhase = TaskPhase.EXECUTE;
                break;
            }
            case EXECUTE: {
                this.executeAction(this.currentTask.action());
                if (clickPolicy.afterDelay > 0) {
                    this.taskPhase = TaskPhase.POST_DELAY;
                    this.phaseDelayTicks = clickPolicy.afterDelay;
                    break;
                }
                if (clickPolicy.closeScreenAfterwards) {
                    this.sendCloseContainer();
                }
                this.clearCurrentTask();
                break;
            }
            case POST_DELAY: {
                if (--this.phaseDelayTicks > 0) break;
                if (clickPolicy.closeScreenAfterwards) {
                    this.sendCloseContainer();
                }
                this.clearCurrentTask();
            }
        }
    }

    public void markSequenceFinished() {
        this.sequenceFinished = true;
    }

    public boolean isSwapTaskRunning() {
        return this.currentTask != null && this.currentTask.clickPolicy() == ClickPolicy.SWAP;
    }

    private void finishTaskExecution() {
        ClickPolicy clickPolicy = this.currentTask.clickPolicy();
        if (clickPolicy.afterDelay > 0) {
            this.taskPhase = TaskPhase.POST_DELAY;
            this.phaseDelayTicks = clickPolicy.afterDelay;
        } else {
            if (clickPolicy.closeScreenAfterwards) {
                this.sendCloseContainer();
            }
            this.clearCurrentTask();
        }
    }

    private void resetUseState() {
        if (this.useKeyHoldTicks > 0) {
            MinecraftClient.getInstance().options.useKey.setPressed(false);
        }
        this.useKeyHoldTicks = 0;
        this.restoreHotbarSlot = -1;
        this.slotRestoreDelayTicks = 0;
        this.hotbarUseBusy = false;
        this.useRequested = false;
    }

    private List<InventoryTask> getList() {
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (Integer clipToSpace : this.tasksByTick.keySet()) {
            if (clipToSpace > this.currentTick) continue;
            arrayList.add(clipToSpace);
        }
        arrayList.sort(Comparator.naturalOrder());
        ArrayList<InventoryTask> arrayList2 = new ArrayList<InventoryTask>();
        for (Integer n : arrayList) {
            arrayList2.addAll((Collection<InventoryTask>)this.tasksByTick.remove(n));
        }
        return arrayList2;
    }

    private void selectAndUse(HotbarSelectAction action) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null) {
            return;
        }
        if (action.slot() < 0 || action.slot() > 8) {
            return;
        }
        PlayerInventory inv = player2.getInventory();
        int n = inv.getSelectedSlot();
        if (n != action.slot()) {
            inv.setSelectedSlot(action.slot());
            if (action.useAfterSelect()) {
                this.restoreHotbarSlot = n;
                this.slotRestoreDelayTicks = 2;
            }
        }
        this.pressUseForTick();
    }

    private static boolean isHotbarOrUseAction(InventoryAction action) {
        return action instanceof HotbarSelectAction || action instanceof UseItemAction;
    }

    private void dropSlot(DropSlotAction action) {
        this.clickSlot(action.slot(), action.entireStack() ? 1 : 0, SlotActionType.THROW);
    }

    private void clickHotbarSwap(ClickSlotAction action) {
        this.clickSlot(action.slot(), action.hotbarButton(), SlotActionType.SWAP);
    }

    private void swapSlots(SwapSlotsAction action) {
        int sourceContainerSlot;
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null) {
            return;
        }
        PlayerInventory inv = player2.getInventory();
        int sourceSlot = action.getFromSlot();
        int n = sourceContainerSlot = sourceSlot < 9 ? sourceSlot + 36 : sourceSlot;
        if (action.getToSlot() == sourceContainerSlot) {
            return;
        }
        boolean sourceOccupied = sourceSlot >= 0 && sourceSlot < inv.size() && !inv.getStack(sourceSlot).isEmpty();
        this.clickSlot(action.getToSlot(), 0, SlotActionType.PICKUP);
        this.clickSlot(sourceContainerSlot, 0, SlotActionType.PICKUP);
        if (sourceOccupied) {
            this.clickSlot(action.getToSlot(), 0, SlotActionType.PICKUP);
        }
    }

    public boolean consumeUseRequested() {
        if (!this.useRequested) {
            return false;
        }
        this.useRequested = false;
        return true;
    }

    private void advanceActionSequence(ActionSequence actionSequence) {
        for (TimedAction timedAction : actionSequence.steps()) {
            if (timedAction.delay() != this.sequenceTick) continue;
            this.executeAction(timedAction.action());
        }
        ++this.sequenceTick;
        if (this.sequenceTick > actionSequence.maxDelay()) {
            this.finishTaskExecution();
        }
    }

    private void markUseRequested() {
        this.useRequested = true;
    }

    public void startPostSwapCooldown() {
        this.postSwapCooldownTicks = 3;
    }
}

