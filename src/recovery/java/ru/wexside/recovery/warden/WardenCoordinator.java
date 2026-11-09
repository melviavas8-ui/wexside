package ru.wexside.recovery.warden;

import java.util.Objects;
import java.util.function.IntUnaryOperator;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Recovered state transitions, target watchdog and chest approach; not a registered Module. */
public final class WardenCoordinator {
    /** Required operations from surviving helpers. No default or no-op implementation is supplied. */
    public interface Actions {
        void resetDepositHelper();
        void resetRestockHelper();
        void resetContainerLoot();
        void stopNavigation();
        boolean navigationActive();
        void moveTo(BlockPos position);
        void releaseInteraction();
        void aimAt(BlockPos chest);
        void resetDropsOnEscapeBoundary();
        void resetEscapePath();
        void log(String line);
        void command(String command);
    }

    public interface Targets {
        BlockPos target();
        long remaining(BlockPos target);
        boolean shouldWait(long remainingMillis);
        void ban(BlockPos target, long durationMillis);
        BlockPos standPosition(ClientPlayerEntity player, BlockPos chest);
    }

    private final WardenSettings settings;
    private final Actions actions;
    private final Targets targets;
    private WardenState state;
    private boolean clearingHand;
    private boolean threatEscapeRequested;
    private boolean previouslyPvpTagged;
    private int angryTicks;
    private BlockPos pendingChest;
    private int stalledSinceAge;
    private int interactionResumeAge;

    /** Initial state and all configuration/helper associations must be supplied explicitly. */
    public WardenCoordinator(WardenState initialState, WardenSettings settings,
            Actions actions, Targets targets) {
        this.state = Objects.requireNonNull(initialState);
        this.settings = Objects.requireNonNull(settings);
        this.actions = Objects.requireNonNull(actions);
        this.targets = Objects.requireNonNull(targets);
    }

    public WardenState state() {
        return state;
    }

    public WardenSettings settings() {
        return settings;
    }

    public void transition(WardenState next, String reason) {
        Objects.requireNonNull(next);
        if (state != next) {
            if (state == WardenState.SAVE) {
                actions.resetDepositHelper();
                actions.resetRestockHelper();
            }
            actions.resetContainerLoot();
            actions.log(state + " -> " + next + ": " + reason);
            if (next == WardenState.ESCAPE || state == WardenState.ESCAPE) {
                actions.stopNavigation();
                actions.releaseInteraction();
                actions.resetDropsOnEscapeBoundary();
                pendingChest = null;
                clearingHand = false;
                threatEscapeRequested = false;
                actions.resetEscapePath();
            }
        }
        state = next;
    }

    public void setHandClearing(boolean value) {
        clearingHand = value;
    }

    public void setPendingChest(BlockPos value) {
        pendingChest = value;
    }

    public void setThreatEscapeRequested(boolean value) {
        threatEscapeRequested = value;
    }

    public boolean threatEscapeRequested() {
        return threatEscapeRequested;
    }

    public int interactionResumeAge() {
        return interactionResumeAge;
    }

    public void pauseAfterInteraction(ClientPlayerEntity player) {
        actions.stopNavigation();
        interactionResumeAge = player.age + 10;
    }

    public void pulseHomeCommand(ClientPlayerEntity player) {
        if (player.age % 50 == 0) {
            actions.command("home");
        }
    }

    public static void closeContainerPeriodically(ClientPlayerEntity player) {
        if (player.age % 2 == 0) {
            player.closeHandledScreen();
        }
    }

    public void approachChest(ClientPlayerEntity player, BlockPos chest) {
        BlockPos stand = targets.standPosition(player, chest);
        if (stand == null) {
            actions.releaseInteraction();
            return;
        }
        if (player.squaredDistanceTo(Vec3d.ofCenter(stand)) > 2.0) {
            actions.releaseInteraction();
            actions.moveTo(stand);
        } else {
            actions.stopNavigation();
            actions.aimAt(chest);
        }
    }

    public void tickStallWatchdog(ClientPlayerEntity player, boolean controllerActive) {
        BlockPos target = targets.target();
        boolean stalled = state == WardenState.COLLECT && target != null
                && !WardenQueries.moving(player) && !actions.navigationActive()
                && !(player.currentScreenHandler instanceof GenericContainerScreenHandler)
                && !controllerActive && !clearingHand && pendingChest == null
                && !targets.shouldWait(targets.remaining(target));
        if (!stalled) {
            stalledSinceAge = 0;
            return;
        }
        if (stalledSinceAge == 0) {
            stalledSinceAge = player.age;
            return;
        }
        if (player.age - stalledSinceAge > 120) {
            actions.log("простой " + (player.age - stalledSinceAge) / 20 + " с у "
                    + target.getX() + "," + target.getY() + "," + target.getZ() + ", сброс цели");
            targets.ban(target, 90_000L);
            stalledSinceAge = 0;
        }
    }

    /** Snapshots of the actual iiIlliiilII consumers, not replacement feature defaults. */
    public record CollectionSnapshot(String immediateEscapeReason, boolean glowingNearPlayer,
            boolean pvpTagged, boolean hasLootToDeposit, boolean angryWardenApproaching,
            int playerAge, int occupiedInventorySlots, long emptyChestCount, int foodLevel,
            int pvpStartsInSeconds, boolean otherPlayerWithin14) {}

    public boolean handleCollectionEscape(CollectionSnapshot snapshot, IntUnaryOperator scaleThreshold) {
        if (snapshot.immediateEscapeReason() != null) {
            transition(WardenState.ESCAPE, snapshot.immediateEscapeReason());
            return true;
        }
        boolean glowing = snapshot.glowingNearPlayer();
        boolean pvpEnded = previouslyPvpTagged && !snapshot.pvpTagged();
        previouslyPvpTagged = snapshot.pvpTagged();
        if (pvpEnded && !glowing && snapshot.hasLootToDeposit()) {
            transition(WardenState.ESCAPE, "пвп-тег снят, лут на сдачу");
            return true;
        }
        angryTicks = snapshot.angryWardenApproaching() ? angryTicks + 1 : 0;
        if (snapshot.playerAge() > 100 && angryTicks >= 3) {
            transition(WardenState.ESCAPE, "варден агрится");
            threatEscapeRequested = true;
            return true;
        }
        int occupied = snapshot.occupiedInventorySlots();
        boolean emptyChests = !glowing && snapshot.emptyChestCount() >= 3L
                && snapshot.playerAge() % 30 == 0;
        if (snapshot.playerAge() > 100
                && (!glowing && occupied > scaleThreshold.applyAsInt(20)
                    || snapshot.foodLevel() < 8 || emptyChests)) {
            transition(WardenState.ESCAPE, emptyChests ? "сундуки пустые" : "инвентарь " + occupied + " или голод");
            return true;
        }
        if (glowing) {
            return false;
        }
        if (occupied > scaleThreshold.applyAsInt(8)) {
            transition(WardenState.ESCAPE, "инвентарь " + occupied + (snapshot.pvpTagged() ? ", пвп-тег" : ""));
            return true;
        }
        int startsIn = snapshot.pvpStartsInSeconds();
        if (startsIn >= 0 && startsIn < 7 && !snapshot.otherPlayerWithin14()
                && occupied > scaleThreshold.applyAsInt(7)) {
            transition(WardenState.ESCAPE, "пвп через " + startsIn + " с");
            return true;
        }
        return false;
    }
}
