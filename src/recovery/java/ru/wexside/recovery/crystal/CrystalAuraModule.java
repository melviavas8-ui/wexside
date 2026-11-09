package ru.wexside.recovery.crystal;

import java.util.List;
import java.util.Optional;
import ru.wexside.util.Angle;
import ru.wexside.recovery.crystal.CrystalRotationLease;
import ru.wexside.event.EventBus;
import net.minecraft.item.Items;
import net.minecraft.world.RaycastContext;
import net.minecraft.entity.EntityType;
import ru.wexside.setting.BooleanSetting;
import net.minecraft.util.math.MathHelper;
import ru.wexside.event.IncomingPacketEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import ru.wexside.event.WorldSessionEvent;
import net.minecraft.entity.decoration.EndCrystalEntity;

import net.minecraft.network.packet.Packet;
import net.minecraft.util.hit.HitResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import ru.wexside.module.ModuleCategory;

public class CrystalAuraModule
extends ru.wexside.module.Module {
    static final int ITEM_USE_RETRY_LIMIT = 40;
    private int crystalSearchTicks;
    private final ru.wexside.setting.BooleanSetting enabled;
    private net.minecraft.util.math.Vec3d lastSpawnPosition;
    static final String ROTATION_STRATEGY = "Simple";
    private int placementHotbarSlot;
    private int itemUseRetryTicks;
    private net.minecraft.util.math.Box crystalSearchBounds;
    private boolean waitingForItemUseRetry;
    private int searchRetryTicks;
    private int spawnedCrystalEntityId;
    private int queuedCrystalSlot;
    private final ru.wexside.setting.BooleanSetting fast;
    private final ru.wexside.setting.BooleanSetting test;
    private int previousSelectedSlot;
    private final ru.wexside.setting.BooleanSetting nearbyItemGuard;
    private boolean selectedSlotRestorePending;
    private boolean blocked;
    private boolean offhandRestorePending;
    private int offhandSwapSourceSlot;
    private net.minecraft.util.math.BlockPos targetPos;
    private boolean previousEnabledState;
    private net.minecraft.util.math.BlockPos queuedPlacementPos;
    private final CrystalRotationLease rotation;
    private final CrystalRotationControllerContract rotationController;
    private boolean handledInputThisTick;

    @Override
    protected void initialize() {
        CrystalAuraModule.registerListeners(this);
    }

    private void attackCrystal(net.minecraft.entity.Entity lillIllllilll2) {
        CrystalAuraModule.attackCrystal(this, lillIllllilll2);
    }

    private boolean isOtherCrystalAttackBlocked() {
        return CrystalAuraModule.isOtherCrystalAttackBlocked(this);
    }

    private static /* synthetic */ net.minecraft.util.math.Vec3d clampPointToBox(CrystalAuraModule module, net.minecraft.util.math.Vec3d ilIiIiIlllIiIl, net.minecraft.util.math.Box illillIlIiIll) {
        return new net.minecraft.util.math.Vec3d(net.minecraft.util.math.MathHelper.clamp(ilIiIiIlllIiIl.x, illillIlIiIll.minX, illillIlIiIll.maxX), net.minecraft.util.math.MathHelper.clamp(ilIiIiIlllIiIl.y, illillIlIiIll.minY, illillIlIiIll.maxY), net.minecraft.util.math.MathHelper.clamp(ilIiIiIlllIiIl.z, illillIlIiIll.minZ, illillIlIiIll.maxZ));
    }

    private void onBlockInteraction(CrystalBlockInteractEvent ilIllillIiIiIil) {
        CrystalAuraModule.onBlockInteraction(this, ilIllillIiIiIil);
    }

    private void updatePlacementConfirmation() {
        CrystalAuraModule.updatePlacementConfirmation(this);
    }

    private static /* synthetic */ void clearPendingCrystalSearch(CrystalAuraModule module) {
        CrystalAuraModule.clearCrystalSearchBounds(module);
        module.lastSpawnPosition = null;
        module.crystalSearchTicks = 0;
    }

    private static /* synthetic */ boolean isOtherCrystalAttackBlocked(CrystalAuraModule module) {
        return CrystalAttackSharedState.blocked;
    }

    private static /* synthetic */ void attackCrystalAndClearSearch(net.minecraft.entity.decoration.EndCrystalEntity liIlliIlllIlIl2, CrystalAuraModule module) {
        module.attackCrystal(liIlliIlllIlIl2);
        module.clearPendingCrystalSearch();
    }

    private void updateBeforeInput() {
        CrystalAuraModule.updateBeforeInput(this);
    }

    private static void releaseRotationLease(CrystalAuraModule module) {
        CrystalRotationControllerContract controller = module.rotationController;
        if (controller == null) {
            module.rotation.clear();
        } else {
            module.rotation.release(controller.currentRequestOwner(), controller::release);
        }
    }

    private static void updatePlacementConfirmation(CrystalAuraModule module) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (module.targetPos == null) return;
        if (client.world.getBlockState(module.targetPos).isAir()) {
            module.targetPos = null;
        } else if (module.blocked) {
            module.blocked = false;
        } else if (module.placeCrystalFromHotbar(module.targetPos, module.placementHotbarSlot, net.minecraft.util.Hand.MAIN_HAND)) {
            module.selectedSlotRestorePending = true;
            module.targetPos = null;
        }
    }

    private void onIncomingPacket(ru.wexside.event.IncomingPacketEvent iillllIlIlllIil) {
        CrystalAuraModule.onIncomingPacket(this, iillllIlIlllIil);
    }

    private static boolean placeCrystalFromHotbar(CrystalAuraModule module, net.minecraft.util.math.BlockPos position,
                                                  int hotbarSlot, net.minecraft.util.Hand hand) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        net.minecraft.client.network.ClientPlayerEntity player = client.player;
        if (hotbarSlot < 0 || hotbarSlot > 8 || !player.getInventory().getStack(hotbarSlot).isOf(net.minecraft.item.Items.END_CRYSTAL)) {
            module.resetPlacementState();
            return false;
        }
        net.minecraft.util.hit.BlockHitResult hit = module.raycastPlacementTarget(position);
        if (hit == null) return false;
        module.previousSelectedSlot = player.getInventory().getSelectedSlot();
        module.placementHotbarSlot = hotbarSlot;
        player.getInventory().setSelectedSlot(hotbarSlot);
        client.interactionManager.interactBlock(player, hand, hit);
        player.swingHand(hand);
        module.handledInputThisTick = true;
        module.beginCrystalSpawnSearch(position);
        return true;
    }

    private net.minecraft.util.hit.BlockHitResult raycastPlacementTarget(net.minecraft.util.math.BlockPos ilIlliIiIllil) {
        return (net.minecraft.util.hit.BlockHitResult)CrystalAuraModule.raycastPlacementTarget(this, ilIlliIiIllil);
    }

    private static /* synthetic */ void updatePendingCrystalAttack(CrystalAuraModule module) {
        net.minecraft.client.MinecraftClient lillIiIlliliIll2 = net.minecraft.client.MinecraftClient.getInstance();
        if (module.crystalSearchTicks > 0 && lillIiIlliliIll2.world != null) {
            net.minecraft.entity.decoration.EndCrystalEntity liIlliIlllIlIl3;
            Object object;
            --module.crystalSearchTicks;
            Object object2 = object = module.spawnedCrystalEntityId == -1 ? null : lillIiIlliliIll2.world.getEntityById(module.spawnedCrystalEntityId);
            if (object instanceof net.minecraft.entity.decoration.EndCrystalEntity && !module.hasNearbyValuableDrops(liIlliIlllIlIl3 = (net.minecraft.entity.decoration.EndCrystalEntity)object)) {
                if (!module.prepareCrystalAttackRotation(liIlliIlllIlIl3)) {
                    return;
                }
                CrystalAuraModule.attackCrystalAndClearSearch(liIlliIlllIlIl3, module);
            } else if (module.lastSpawnPosition != null) {
                object = new net.minecraft.util.math.Box(module.lastSpawnPosition.x - 0.8, module.lastSpawnPosition.y - 0.8, module.lastSpawnPosition.z - 0.8, module.lastSpawnPosition.x + 0.8, module.lastSpawnPosition.y + 1.8, module.lastSpawnPosition.z + 0.8);
                for (net.minecraft.entity.decoration.EndCrystalEntity liIlliIlllIlIl4 : lillIiIlliliIll2.world.getEntitiesByClass(net.minecraft.entity.decoration.EndCrystalEntity.class, (net.minecraft.util.math.Box)object, liIlliIlllIlIl2 -> {

                    return (0x63C9D2DB ^ 0x63C9D2DA) != 0;
                })) {
                    if (module.hasNearbyValuableDrops(liIlliIlllIlIl4)) continue;
                    if (!module.prepareCrystalAttackRotation(liIlliIlllIlIl4)) {
                        return;
                    }
                    module.attackCrystal(liIlliIlllIlIl4);
                    module.clearPendingCrystalSearch();
                    return;
                }
            }
        }
    }

    private net.minecraft.util.math.Vec3d clampPointToBox(net.minecraft.util.math.Vec3d ilIiIiIlllIiIl, net.minecraft.util.math.Box illillIlIiIll) {
        return (net.minecraft.util.math.Vec3d)CrystalAuraModule.clampPointToBox(this, ilIiIiIlllIiIl, illillIlIiIll);
    }

    private static /* synthetic */ void clearTargetPositions(CrystalAuraModule module) {
        module.releaseRotationLease();
        module.clearPendingCrystalSearch();
        module.targetPos = null;
        module.queuedPlacementPos = null;
    }



    private void beginCrystalSpawnSearch(net.minecraft.util.math.BlockPos ilIlliIiIllil) {
        CrystalAuraModule.beginCrystalSpawnSearch(this, ilIlliIiIllil);
    }

    private static /* synthetic */ void deferItemUseForPlacement(BeforeItemUseEvent illiliIlllIlll, CrystalAuraModule module) {
        module.waitingForItemUseRetry = true;
        module.itemUseRetryTicks = 40;
        illiliIlllIlll.cancel();
    }

    private static /* synthetic */ void clearQueuedPlacementSlot(CrystalAuraModule module) {
        module.queuedCrystalSlot = -1;
        module.waitingForItemUseRetry = false;
    }

    private static /* synthetic */ void resetPlacementState(CrystalAuraModule module) {
        CrystalAuraModule.clearTargetPositions(module);
        CrystalAuraModule.clearQueuedPlacementSlot(module);
        module.itemUseRetryTicks = 0;
        module.offhandRestorePending = module.offhandSwapSourceSlot != -1;
    }

    private static net.minecraft.util.math.BlockPos getBlockPositionFromHit(net.minecraft.util.hit.HitResult hit) {
        return ((net.minecraft.util.hit.BlockHitResult) hit).getBlockPos();
    }

    private static /* synthetic */ void onIncomingPacket(CrystalAuraModule module, ru.wexside.event.IncomingPacketEvent iillllIlIlllIil) {
        net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket llIilillIiIiIll2;
        if (!module.enabled.isEnabled()) {
            return;
        }
        net.minecraft.client.MinecraftClient lillIiIlliliIll2 = net.minecraft.client.MinecraftClient.getInstance();
        net.minecraft.network.packet.Packet<?> lilillIlIiIiIll2 = iillllIlIlllIil.getPacket();
        if (lilillIlIiIiIll2 instanceof net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket && (llIilillIiIiIll2 = (net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket)lilillIlIiIiIll2).getEntityType() == net.minecraft.entity.EntityType.END_CRYSTAL && module.crystalSearchBounds != null) {
            double d = llIilillIiIiIll2.getX();
            double d2 = llIilillIiIiIll2.getY();
            double d3 = llIilillIiIiIll2.getZ();
            if (module.crystalSearchBounds.expand(0.5).contains(new net.minecraft.util.math.Vec3d(d, d2, d3))) {
                if (module.fast.isEnabled() && lillIiIlliliIll2.world != null) {
                    module.spawnedCrystalEntityId = llIilillIiIiIll2.getEntityId();
                    module.lastSpawnPosition = new net.minecraft.util.math.Vec3d(d, d2, d3);
                    module.crystalSearchTicks = 8;
                }
                module.searchRetryTicks = module.fast.isEnabled() ? 7 : 3;
            }
        }
    }

    private static /* synthetic */ void attackCrystal(CrystalAuraModule module, net.minecraft.entity.Entity lillIllllilll2) {
        net.minecraft.client.MinecraftClient lillIiIlliliIll2 = null;
        net.minecraft.client.network.ClientPlayerEntity illiliIiIilil = null;
        int n = -249393991;
        block6: while (true) {
            switch (n) {
                case -249393991: {
                    lillIiIlliliIll2 = net.minecraft.client.MinecraftClient.getInstance();
                    illiliIiIilil = lillIiIlliliIll2.player;
                    if (lillIllllilll2 != null) {
                        n = -734738464;
                        continue block6;
                    }
                    n = -1803799805;
                    continue block6;
                }
                case -734738464: {
                    if (illiliIiIilil != null) {
                        n = -158378385;
                        continue block6;
                    }
                    n = -1803799805;
                    continue block6;
                }
                case -158378385: {
                    lillIiIlliliIll2.interactionManager.attackEntity(illiliIiIilil, lillIllllilll2);
                    illiliIiIilil.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                    n = -1803799805;
                    continue block6;
                }
                case -1803799805: {
                    return;
                }
            }
            break;
        }
        throw null;
    }

    private static /* synthetic */ void queuePlacement(net.minecraft.util.math.BlockPos ilIlliIiIllil, int n, CrystalAuraModule module) {
        module.queuedPlacementPos = ilIlliIiIllil;
        module.queuedCrystalSlot = n;
    }

    private void onBeforeItemUse(BeforeItemUseEvent illiliIlllIlll) {
        CrystalAuraModule.onBeforeItemUse(this, illiliIlllIlll);
    }

    private static /* synthetic */ void onBlockInteraction(CrystalAuraModule module, CrystalBlockInteractEvent ilIllillIiIiIil) {
        net.minecraft.entity.player.PlayerEntity liIiIillIilllil2 = null;
        int n = 0;
        int n2 = -1755123303;
        block21: while (true) {
            switch (n2) {
                case -1755123303: {
                    if (!module.enabled.isEnabled()) {
                        n2 = -357076422;
                        continue block21;
                    }
                    n2 = -934948258;
                    continue block21;
                }
                case -357076422: {
                    return;
                }
                case -934948258: {
                    if (module.targetPos == null) {
                        n2 = -769782150;
                        continue block21;
                    }
                    n2 = 517897725;
                    continue block21;
                }
                case -769782150: {
                    if (module.queuedPlacementPos == null) {
                        n2 = 1648028902;
                        continue block21;
                    }
                    n2 = 517897725;
                    continue block21;
                }
                case 1648028902: {
                    if (module.crystalSearchBounds == null) {
                        n2 = -2035851073;
                        continue block21;
                    }
                    n2 = 517897725;
                    continue block21;
                }
                case -2035851073: {
                    if (!module.selectedSlotRestorePending) {
                        n2 = 957997502;
                        continue block21;
                    }
                    n2 = 517897725;
                    continue block21;
                }
                case 957997502: {
                    if (module.offhandSwapSourceSlot != -1) {
                        n2 = 517897725;
                        continue block21;
                    }
                    n2 = -95478202;
                    continue block21;
                }
                case 517897725: {
                    return;
                }
                case -95478202: {
                    liIiIillIilllil2 = net.minecraft.client.MinecraftClient.getInstance().player;
                    if (liIiIillIilllil2 == null) {
                        n2 = -2009017862;
                        continue block21;
                    }
                    n2 = 787050201;
                    continue block21;
                }
                case -2009017862: {
                    return;
                }
                case 787050201: {
                    if (!module.isOtherCrystalAttackBlocked()) {
                        n2 = 1908156996;
                        continue block21;
                    }
                    n2 = -1541685035;
                    continue block21;
                }
                case 1908156996: {
                    if (ilIllillIiIiIil.getBlock() == net.minecraft.block.Blocks.OBSIDIAN) {
                        n2 = 591217424;
                        continue block21;
                    }
                    n2 = -1541685035;
                    continue block21;
                }
                case 591217424: {
                    if (liIiIillIilllil2.getItemCooldownManager().isCoolingDown(new net.minecraft.item.ItemStack(net.minecraft.item.Items.END_CRYSTAL))) {
                        n2 = -1754923626;
                        continue block21;
                    }
                    n2 = 324834285;
                    continue block21;
                }
                case -1754923626: {
                    return;
                }
                case 324834285: {
                    n = CrystalInventorySearch.findFirstMainInventorySlot(net.minecraft.client.MinecraftClient.getInstance().player == null ? null : net.minecraft.client.MinecraftClient.getInstance().player.getInventory(), net.minecraft.item.Items.END_CRYSTAL);
                    if (n == -1) {
                        n2 = 1832350090;
                        continue block21;
                    }
                    n2 = -1882046999;
                    continue block21;
                }
                case 1832350090: {
                    return;
                }
                case -1882046999: {
                    if (n < 9) {
                        n2 = -1336163374;
                        continue block21;
                    }
                    n2 = -1541685035;
                    continue block21;
                }
                case -1336163374: {
                    module.targetPos = ilIllillIiIiIil.getPosition();
                    module.placementHotbarSlot = n;
                    module.itemUseRetryTicks = 40;
                    module.blocked = true;
                    n2 = -1541685035;
                    continue block21;
                }
                case -1541685035: {
                    return;
                }
            }
            break;
        }
        throw null;
    }

    private void updateQueuedPlacement() {
        CrystalAuraModule.updateQueuedPlacement(this);
    }

    private boolean hasNearbyValuableDrops(net.minecraft.entity.Entity lillIllllilll2) {
        return CrystalAuraModule.hasNearbyValuableDrops(this, lillIllllilll2);
    }

    private static void applyCrystalRotation(CrystalAuraModule module, net.minecraft.client.network.ClientPlayerEntity player,
                                              ru.wexside.util.Angle angle) {
        CrystalRotationControllerContract controller = module.rotationController;
        if (controller == null) return;
        CrystalRotationRequest request = new CrystalRotationRequest(player, null, angle,
                CrystalRotationStrength.HARD, CrystalMovementCorrection.NONE, false);
        controller.apply(request, "Simple");
        module.rotation.acquire(request);
    }



    private static /* synthetic */ void attackMatchingCrystalInSearchBox(CrystalAuraModule module) {
        net.minecraft.client.MinecraftClient lillIiIlliliIll2 = null;
        List<net.minecraft.entity.decoration.EndCrystalEntity> list = null;
        net.minecraft.entity.decoration.EndCrystalEntity liIlliIlllIlIl3 = null;
        int n = 1525967052;
        block12: while (true) {
            switch (n) {
                case 1525967052: {
                    lillIiIlliliIll2 = net.minecraft.client.MinecraftClient.getInstance();
                    if (module.crystalSearchBounds != null) {
                        n = -1636289763;
                        continue block12;
                    }
                    n = -1478787009;
                    continue block12;
                }
                case -1636289763: {
                    if (lillIiIlliliIll2.player != null) {
                        n = 358458427;
                        continue block12;
                    }
                    n = -1478787009;
                    continue block12;
                }
                case 358458427: {
                    if (lillIiIlliliIll2.world != null) {
                        n = 738161075;
                        continue block12;
                    }
                    n = -1478787009;
                    continue block12;
                }
                case 738161075: {
                    list = lillIiIlliliIll2.world.getEntitiesByClass(net.minecraft.entity.decoration.EndCrystalEntity.class, module.crystalSearchBounds.expand(0.25), liIlliIlllIlIl2 -> {

                        return (0x440FA987 ^ 0x440FA986) != 0;
                    });
                    if (!list.isEmpty()) {
                        n = -178093168;
                        continue block12;
                    }
                    n = -1478787009;
                    continue block12;
                }
                case -178093168: {
                    liIlliIlllIlIl3 = (net.minecraft.entity.decoration.EndCrystalEntity)list.get(0);
                    if (module.hasNearbyValuableDrops(liIlliIlllIlIl3)) {
                        n = 259302039;
                        continue block12;
                    }
                    n = 1379143186;
                    continue block12;
                }
                case 259302039: {
                    module.clearPendingCrystalSearch();
                    n = -1478787009;
                    continue block12;
                }
                case 1379143186: {
                    if (!module.prepareCrystalAttackRotation(liIlliIlllIlIl3)) {
                        n = -78798194;
                        continue block12;
                    }
                    n = 1997822539;
                    continue block12;
                }
                case -78798194: {
                    return;
                }
                case 1997822539: {
                    module.attackCrystal(liIlliIlllIlIl3);
                    module.clearPendingCrystalSearch();
                    n = -1478787009;
                    continue block12;
                }
                case -1478787009: {
                    return;
                }
            }
            break;
        }
        throw null;
    }



    private static boolean hasNearbyValuableDrops(CrystalAuraModule module, net.minecraft.entity.Entity target) {
        if (!module.nearbyItemGuard.isEnabled()) {
            return false;
        }
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        return client.world != null && client.world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class,
                target.getBoundingBox().expand(6.0), entity -> true).stream()
                .map(entity -> entity.getStack().getItem())
                .anyMatch(item -> item == net.minecraft.item.Items.TOTEM_OF_UNDYING
                        || item == net.minecraft.item.Items.END_CRYSTAL
                        || item == net.minecraft.item.Items.ENCHANTED_GOLDEN_APPLE
                        || item == net.minecraft.item.Items.NETHERITE_HELMET
                        || item == net.minecraft.item.Items.NETHERITE_CHESTPLATE
                        || item == net.minecraft.item.Items.NETHERITE_LEGGINGS
                        || item == net.minecraft.item.Items.NETHERITE_BOOTS
                        || item == net.minecraft.item.Items.NETHERITE_SWORD
                        || item == net.minecraft.item.Items.DIAMOND_SWORD
                        || item == net.minecraft.item.Items.ELYTRA
                        || item == net.minecraft.item.Items.TRIDENT);
    }

    private void applyCrystalRotation(net.minecraft.client.network.ClientPlayerEntity illiliIiIilil, ru.wexside.util.Angle iiIiliIlliliIil) {
        CrystalAuraModule.applyCrystalRotation(this, illiliIiIilil, iiIiliIlliliIil);
    }

    private static /* synthetic */ void registerListeners(CrystalAuraModule module) {
        module.listen(CrystalBlockInteractEvent.class, module::onBlockInteraction);
        module.listen(BeforeInputHandlingEvent.class, liIlllIiIlIilil2 -> {

            module.updateBeforeInput();
        });
        module.listen(CrystalUpdateEvent.class, illlllIlIillIil -> {

            net.minecraft.client.MinecraftClient lillIiIlliliIll2 = null;
            int n = -1615049610;
            block6: while (true) {
                switch (n) {
                    case -1615049610: {
                        lillIiIlliliIll2 = net.minecraft.client.MinecraftClient.getInstance();
                        if (module.enabled.isEnabled()) {
                            n = 0x89AB2111 ^ 0x5769C87F;
                            continue block6;
                        }
                        n = 0xC287E58 ^ 0x64B88964;
                        continue block6;
                    }
                    case -557651602: {
                        if (lillIiIlliliIll2.currentScreen != null) {
                            n = 1754330940;
                            continue block6;
                        }
                        n = -1455449646;
                        continue block6;
                    }
                    case 1754330940: {
                        module.resetPlacementState();
                        n = 0x4940575 ^ 0xADAB9CA7;
                        continue block6;
                    }
                    case -1455449646: {
                        return;
                    }
                }
                break;
            }
            throw null;
        });
        module.listen(ru.wexside.event.IncomingPacketEvent.class, module::onIncomingPacket);
        module.listen(BeforeItemUseEvent.class, module::onBeforeItemUse);
        module.listen(ru.wexside.event.WorldSessionEvent.class, liIlliIlIililil2 -> {

            module.selectedSlotRestorePending = false;
            module.offhandSwapSourceSlot = 0xA7D77A75 ^ 0x5828858A;
            module.offhandRestorePending = false;
            module.resetPlacementState();
        });
    }

    private static /* synthetic */ void updateBeforeInput(CrystalAuraModule module) {
        net.minecraft.client.MinecraftClient lillIiIlliliIll2 = null;
        boolean bl = false;
        int n = 1338640317;
        block38: while (true) {
            switch (n) {
                case 1338640317: {
                    module.releaseRotationLease();
                    module.handledInputThisTick = false;
                    lillIiIlliliIll2 = net.minecraft.client.MinecraftClient.getInstance();
                    if (lillIiIlliliIll2.player != null) {
                        n = -389554958;
                        continue block38;
                    }
                    n = 1572499284;
                    continue block38;
                }
                case -389554958: {
                    if (lillIiIlliliIll2.world != null) {
                        n = 619261899;
                        continue block38;
                    }
                    n = 1572499284;
                    continue block38;
                }
                case 619261899: {
                    if (lillIiIlliliIll2.interactionManager == null) {
                        n = 1572499284;
                        continue block38;
                    }
                    n = -446411126;
                    continue block38;
                }
                case 1572499284: {
                    return;
                }
                case -446411126: {
                    if (lillIiIlliliIll2.currentScreen == null) {
                        n = -1522375529;
                        continue block38;
                    }
                    n = -12638818;
                    continue block38;
                }
                case -1522375529: {
                    if (lillIiIlliliIll2.player.currentScreenHandler != lillIiIlliliIll2.player.playerScreenHandler) {
                        n = -12638818;
                        continue block38;
                    }
                    n = 1695677525;
                    continue block38;
                }
                case -12638818: {
                    module.resetPlacementState();
                    return;
                }
                case 1695677525: {
                    if (module.offhandRestorePending) {
                        n = -2081133843;
                        continue block38;
                    }
                    n = -1941661320;
                    continue block38;
                }
                case -2081133843: {
                    lillIiIlliliIll2.interactionManager.clickSlot(lillIiIlliliIll2.player.playerScreenHandler.syncId, module.offhandSwapSourceSlot, 40, net.minecraft.screen.slot.SlotActionType.SWAP, lillIiIlliliIll2.player);
                    module.offhandSwapSourceSlot = -1;
                    module.offhandRestorePending = false;
                    return;
                }
                case -1941661320: {
                    if (module.selectedSlotRestorePending) {
                        n = 1203850826;
                        continue block38;
                    }
                    n = -1317193187;
                    continue block38;
                }
                case 1203850826: {
                    module.selectedSlotRestorePending = false;
                    if (lillIiIlliliIll2.player.getInventory().getSelectedSlot() == module.placementHotbarSlot) {
                        n = 2121801302;
                        continue block38;
                    }
                    n = -459424790;
                    continue block38;
                }
                case 2121801302: {
                    lillIiIlliliIll2.player.getInventory().setSelectedSlot(module.previousSelectedSlot);
                    n = -459424790;
                    continue block38;
                }
                case -459424790: {
                    return;
                }
                case -1317193187: {
                    bl = module.enabled.isEnabled();
                    if (bl != module.previousEnabledState) {
                        n = 272688937;
                        continue block38;
                    }
                    n = 1183752318;
                    continue block38;
                }
                case 272688937: {
                    module.previousEnabledState = bl;
                    if (!bl) {
                        n = 1542622375;
                        continue block38;
                    }
                    n = 1183752318;
                    continue block38;
                }
                case 1542622375: {
                    module.resetPlacementState();
                    n = 1183752318;
                    continue block38;
                }
                case 1183752318: {
                    if (!bl) {
                        n = 483943102;
                        continue block38;
                    }
                    n = -111624709;
                    continue block38;
                }
                case 483943102: {
                    return;
                }
                case -111624709: {
                    if (module.targetPos == null) {
                        n = 2008683072;
                        continue block38;
                    }
                    n = 1330771556;
                    continue block38;
                }
                case 2008683072: {
                    if (module.queuedPlacementPos != null) {
                        n = 1330771556;
                        continue block38;
                    }
                    n = 249475601;
                    continue block38;
                }
                case 1330771556: {
                    if (--module.itemUseRetryTicks <= 0) {
                        n = 1215188104;
                        continue block38;
                    }
                    n = 249475601;
                    continue block38;
                }
                case 1215188104: {
                    module.resetPlacementState();
                    return;
                }
                case 249475601: {
                    if (!module.isOtherCrystalAttackBlocked()) {
                        n = -918049995;
                        continue block38;
                    }
                    n = 1117390036;
                    continue block38;
                }
                case -918049995: {
                    module.updateQueuedPlacement();
                    if (module.handledInputThisTick) {
                        n = 2115979555;
                        continue block38;
                    }
                    n = -1695064207;
                    continue block38;
                }
                case 2115979555: {
                    return;
                }
                case -1695064207: {
                    module.updatePlacementConfirmation();
                    if (module.handledInputThisTick) {
                        n = -1696248433;
                        continue block38;
                    }
                    n = 1117390036;
                    continue block38;
                }
                case -1696248433: {
                    return;
                }
                case 1117390036: {
                    if (module.searchRetryTicks > 0) {
                        n = 2042529245;
                        continue block38;
                    }
                    n = 1299918742;
                    continue block38;
                }
                case 2042529245: {
                    --module.searchRetryTicks;
                    n = 1299918742;
                    continue block38;
                }
                case 1299918742: {
                    module.updatePendingCrystalAttack();
                    if (module.crystalSearchBounds != null) {
                        n = -1203045303;
                        continue block38;
                    }
                    n = -1578214415;
                    continue block38;
                }
                case -1203045303: {
                    module.attackMatchingCrystalInSearchBox();
                    n = -1578214415;
                    continue block38;
                }
                case -1578214415: {
                    if (module.crystalSearchBounds != null) {
                        n = 441015046;
                        continue block38;
                    }
                    n = -1287387001;
                    continue block38;
                }
                case 441015046: {
                    if (module.crystalSearchTicks <= 0) {
                        n = 321636400;
                        continue block38;
                    }
                    n = -1287387001;
                    continue block38;
                }
                case 321636400: {
                    if (module.searchRetryTicks <= 0) {
                        n = -239496288;
                        continue block38;
                    }
                    n = -1287387001;
                    continue block38;
                }
                case -239496288: {
                    module.clearPendingCrystalSearch();
                    module.releaseRotationLease();
                    n = -1287387001;
                    continue block38;
                }
                case -1287387001: {
                    return;
                }
            }
            break;
        }
        throw null;
    }

    private boolean placeCrystalFromHotbar(net.minecraft.util.math.BlockPos ilIlliIiIllil, int n, net.minecraft.util.Hand illlllIlllIll) {
        return CrystalAuraModule.placeCrystalFromHotbar(this, ilIlliIiIllil, n, illlllIlllIll);
    }

    private static /* synthetic */ void updateQueuedPlacement(CrystalAuraModule module) {
        net.minecraft.client.MinecraftClient lillIiIlliliIll2 = net.minecraft.client.MinecraftClient.getInstance();
        net.minecraft.client.network.ClientPlayerEntity illiliIiIilil = lillIiIlliliIll2.player;
        if (module.test.isEnabled() && module.queuedPlacementPos != null) {
            if (lillIiIlliliIll2.world.getBlockState(module.queuedPlacementPos).getBlock() != net.minecraft.block.Blocks.OBSIDIAN || !lillIiIlliliIll2.world.getBlockState(module.queuedPlacementPos.up()).isAir()) {
                module.queuedPlacementPos = null;
                module.queuedCrystalSlot = -1;
                module.waitingForItemUseRetry = false;
                module.offhandRestorePending = module.offhandSwapSourceSlot != -1;
            } else if (module.waitingForItemUseRetry) {
                module.waitingForItemUseRetry = false;
            } else if (module.queuedCrystalSlot == -1) {
                module.queuedPlacementPos = null;
            } else {
                if (module.queuedCrystalSlot < 9) {
                    if (!module.placeCrystalFromHotbar(module.queuedPlacementPos, module.queuedCrystalSlot, net.minecraft.util.Hand.MAIN_HAND)) {
                        return;
                    }
                    module.selectedSlotRestorePending = true;
                    module.queuedPlacementPos = null;
                    module.queuedCrystalSlot = -1;
                    return;
                }
                if (module.offhandSwapSourceSlot == -1) {
                    if (!illiliIiIilil.getInventory().getStack(module.queuedCrystalSlot).isOf(net.minecraft.item.Items.END_CRYSTAL)) {
                        module.resetPlacementState();
                        return;
                    }
                    module.offhandSwapSourceSlot = module.queuedCrystalSlot;
                    lillIiIlliliIll2.interactionManager.clickSlot(illiliIiIilil.playerScreenHandler.syncId, module.offhandSwapSourceSlot, 40, net.minecraft.screen.slot.SlotActionType.SWAP, illiliIiIilil);
                    module.handledInputThisTick = true;
                    return;
                }
                net.minecraft.util.hit.BlockHitResult liIlIillllllll2 = module.raycastPlacementTarget(module.queuedPlacementPos);
                if (liIlIillllllll2 == null) {
                    return;
                }
                if (!illiliIiIilil.getOffHandStack().isOf(net.minecraft.item.Items.END_CRYSTAL)) {
                    module.resetPlacementState();
                    return;
                }
                lillIiIlliliIll2.interactionManager.interactBlock(illiliIiIilil, net.minecraft.util.Hand.OFF_HAND, liIlIillllllll2);
                module.handledInputThisTick = true;
                module.offhandRestorePending = true;
                module.beginCrystalSpawnSearch(module.queuedPlacementPos);
                module.queuedPlacementPos = null;
                module.queuedCrystalSlot = -1;
            }
        }
    }

    private void attackMatchingCrystalInSearchBox() {
        CrystalAuraModule.attackMatchingCrystalInSearchBox(this);
    }

    private static /* synthetic */ void beginCrystalSpawnSearch(CrystalAuraModule module, net.minecraft.util.math.BlockPos ilIlliIiIllil) {
        net.minecraft.util.math.BlockPos ilIlliIiIllil2 = ilIlliIiIllil.up();
        module.crystalSearchBounds = new net.minecraft.util.math.Box(ilIlliIiIllil2).expand(module.fast.isEnabled() ? 0.5 : 0.1);
        module.searchRetryTicks = module.fast.isEnabled() ? 7 : 5;
        module.spawnedCrystalEntityId = -1;
        module.lastSpawnPosition = new net.minecraft.util.math.Vec3d((double)ilIlliIiIllil2.getX() + 0.5, (double)ilIlliIiIllil2.getY() + 0.5, (double)ilIlliIiIllil2.getZ() + 0.5);
        module.crystalSearchTicks = module.fast.isEnabled() ? 8 : 6;
    }

    private static /* synthetic */ void onBeforeItemUse(CrystalAuraModule module, BeforeItemUseEvent illiliIlllIlll) {
        Object object;
        int n;
        if (!module.enabled.isEnabled()) {
            return;
        }
        net.minecraft.client.MinecraftClient lillIiIlliliIll2 = net.minecraft.client.MinecraftClient.getInstance();
        net.minecraft.client.network.ClientPlayerEntity illiliIiIilil = lillIiIlliliIll2.player;
        if (illiliIiIilil == null || lillIiIlliliIll2.world == null) {
            return;
        }
        if (!(module.isOtherCrystalAttackBlocked() || !module.test.isEnabled() || (n = CrystalInventorySearch.findFirstMainInventorySlot(net.minecraft.client.MinecraftClient.getInstance().player == null ? null : net.minecraft.client.MinecraftClient.getInstance().player.getInventory(), net.minecraft.item.Items.END_CRYSTAL)) == -1 || illiliIiIilil.getMainHandStack().getItem() instanceof net.minecraft.item.BlockItem && illiliIiIilil.getMainHandStack().getItem() != net.minecraft.item.Items.PLAYER_HEAD || illiliIiIilil.getOffHandStack().getItem() instanceof net.minecraft.item.BlockItem && illiliIiIilil.getOffHandStack().getItem() != net.minecraft.item.Items.PLAYER_HEAD || !((object = lillIiIlliliIll2.crosshairTarget) instanceof net.minecraft.util.hit.BlockHitResult) || lillIiIlliliIll2.world.getBlockState((net.minecraft.util.math.BlockPos)(object = CrystalAuraModule.getBlockPositionFromHit((net.minecraft.util.hit.HitResult)object))).getBlock() != net.minecraft.block.Blocks.OBSIDIAN || !lillIiIlliliIll2.world.getBlockState(((net.minecraft.util.math.BlockPos)object).up()).isAir())) {
            if (module.targetPos != null || module.queuedPlacementPos != null || module.crystalSearchBounds != null || module.selectedSlotRestorePending || module.offhandSwapSourceSlot != -1) {
                illiliIlllIlll.cancel();
                return;
            }
            CrystalAuraModule.queuePlacement((net.minecraft.util.math.BlockPos)object, n, module);
            CrystalAuraModule.deferItemUseForPlacement(illiliIlllIlll, module);
        }
    }

    private static void rotateTowardPoint(CrystalAuraModule module, net.minecraft.util.math.Vec3d point) {
        net.minecraft.client.network.ClientPlayerEntity player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player != null) {
            module.applyCrystalRotation(player, CrystalAimMath.angleToPoint(player.getEyePos(), point));
        }
    }

    private void updatePendingCrystalAttack() {
        CrystalAuraModule.updatePendingCrystalAttack(this);
    }

    private static /* synthetic */ void clearCrystalSearchBounds(CrystalAuraModule module) {
        module.crystalSearchBounds = null;
        module.spawnedCrystalEntityId = -1;
    }

    private void rotateTowardPoint(net.minecraft.util.math.Vec3d ilIiIiIlllIiIl) {
        CrystalAuraModule.rotateTowardPoint(this, ilIiIiIlllIiIl);
    }

    private void clearPendingCrystalSearch() {
        CrystalAuraModule.clearPendingCrystalSearch(this);
    }

    private static /* synthetic */ boolean prepareCrystalAttackRotation(CrystalAuraModule module, net.minecraft.entity.Entity lillIllllilll2) {
        net.minecraft.client.network.ClientPlayerEntity illiliIiIilil = net.minecraft.client.MinecraftClient.getInstance().player;
        if (lillIllllilll2 != null && illiliIiIilil != null) {
            net.minecraft.util.math.Vec3d ilIiIiIlllIiIl;
            ru.wexside.util.Angle iiIiliIlliliIil;
            CrystalRotationControllerContract ilIiIillIlIllil = module.rotationController;
            ru.wexside.util.Angle iiIiliIlliliIil2 = iiIiliIlliliIil = ilIiIillIlIllil == null ? null : ilIiIillIlIllil.currentAngle();
            if (iiIiliIlliliIil != null) {
                net.minecraft.util.hit.BlockHitResult liIlIillllllll2;
                ilIiIiIlllIiIl = illiliIiIilil.getEyePos();
                net.minecraft.util.math.Vec3d ilIiIiIlllIiIl2 = ilIiIiIlllIiIl.add(iiIiliIlliliIil.toVec3d().multiply(illiliIiIilil.getEntityInteractionRange()));
                Optional<net.minecraft.util.math.Vec3d> optional = lillIllllilll2.getBoundingBox().raycast(ilIiIiIlllIiIl, ilIiIiIlllIiIl2);
                if (optional.isPresent() && (liIlIillllllll2 = net.minecraft.client.MinecraftClient.getInstance().world.raycast(new net.minecraft.world.RaycastContext(ilIiIiIlllIiIl, optional.get(), net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE, illiliIiIilil))).getType() == net.minecraft.util.hit.HitResult.Type.MISS) {
                    module.applyCrystalRotation(illiliIiIilil, iiIiliIlliliIil);
                    return true;
                }
            }
            if (!module.fast.isEnabled() && !lillIllllilll2.getBoundingBox().contains(illiliIiIilil.getEyePos())) {
                ilIiIiIlllIiIl = lillIllllilll2.getBoundingBox().getCenter().subtract(illiliIiIilil.getEyePos());
                float f = (float)net.minecraft.util.math.MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(ilIiIiIlllIiIl.z, ilIiIiIlllIiIl.x)) - 90.0);
                float f2 = (float)(-Math.toDegrees(Math.atan2(ilIiIiIlllIiIl.y, Math.hypot(ilIiIiIlllIiIl.x, ilIiIiIlllIiIl.z))));
                module.applyCrystalRotation(illiliIiIilil, new ru.wexside.util.Angle(f, f2));
            }
        }
        return false;
    }

    private boolean prepareCrystalAttackRotation(net.minecraft.entity.Entity lillIllllilll2) {
        return CrystalAuraModule.prepareCrystalAttackRotation(this, lillIllllilll2);
    }

    private void resetPlacementState() {
        CrystalAuraModule.resetPlacementState(this);
    }

    private void releaseRotationLease() {
        CrystalAuraModule.releaseRotationLease(this);
    }

    private static /* synthetic */ net.minecraft.util.hit.BlockHitResult raycastPlacementTarget(CrystalAuraModule module, net.minecraft.util.math.BlockPos ilIlliIiIllil) {
        net.minecraft.util.math.Vec3d ilIiIiIlllIiIl;
        net.minecraft.util.math.Vec3d ilIiIiIlllIiIl2;
        net.minecraft.util.hit.BlockHitResult liIlIillllllll2;
        ru.wexside.util.Angle iiIiliIlliliIil;
        net.minecraft.client.MinecraftClient lillIiIlliliIll2 = net.minecraft.client.MinecraftClient.getInstance();
        net.minecraft.client.network.ClientPlayerEntity illiliIiIilil = lillIiIlliliIll2.player;
        CrystalRotationControllerContract ilIiIillIlIllil = module.rotationController;
        ru.wexside.util.Angle iiIiliIlliliIil2 = iiIiliIlliliIil = ilIiIillIlIllil == null ? null : ilIiIillIlIllil.currentAngle();
        if (iiIiliIlliliIil != null && (liIlIillllllll2 = lillIiIlliliIll2.world.raycast(new net.minecraft.world.RaycastContext(ilIiIiIlllIiIl2 = illiliIiIilil.getEyePos(), ilIiIiIlllIiIl = ilIiIiIlllIiIl2.add(iiIiliIlliliIil.toVec3d().multiply(illiliIiIilil.getBlockInteractionRange())), net.minecraft.world.RaycastContext.ShapeType.OUTLINE, net.minecraft.world.RaycastContext.FluidHandling.NONE, illiliIiIilil))).getType() == net.minecraft.util.hit.HitResult.Type.BLOCK && liIlIillllllll2.getBlockPos().equals(ilIlliIiIllil)) {
            module.applyCrystalRotation(illiliIiIilil, iiIiliIlliliIil);
            return liIlIillllllll2;
        }
        module.rotateTowardPoint(net.minecraft.util.math.Vec3d.ofCenter(ilIlliIiIllil));
        return null;
    }



    public CrystalAuraModule(ru.wexside.event.EventBus events, String id, String displayName,
                             String description, ru.wexside.module.ModuleCategory category,
                             String[] aliases, CrystalAuraSettings settings,
                             java.util.List<ru.wexside.setting.BooleanSetting> settingRegistrationOrder,
                             CrystalRotationControllerContract rotationController) {
        super(events, id, displayName, description, category, aliases);
        this.enabled = settings.enabled();
        this.fast = settings.fast();
        this.test = settings.test();
        this.nearbyItemGuard = settings.nearbyItemGuard();
        this.rotation = new CrystalRotationLease();
        this.rotationController = rotationController;
        if (settingRegistrationOrder.size() != 4
                || !new java.util.HashSet<>(settingRegistrationOrder).equals(java.util.Set.of(
                        this.enabled, this.fast, this.test, this.nearbyItemGuard))) {
            throw new IllegalArgumentException("Supply each of the four recovered settings exactly once");
        }
        for (ru.wexside.setting.BooleanSetting setting : settingRegistrationOrder) {
            this.registerSetting(setting);
        }
    }
}
