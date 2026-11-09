package ru.wexside.module.player;

import java.util.List;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.ActionSequence;
import ru.wexside.misc.BundleUse;
import ru.wexside.misc.Bundles;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.ClickSlotAction;
import ru.wexside.misc.InventoryAction;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.RunnableAction;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.misc.TimedAction;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.util.InventoryController;

public class AutoToolModule
extends Module
implements ConfigSerializable {
    private static final String OWNER = "auto_tool";
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private final BooleanSetting fromInventory;
    private final BooleanSetting swapBack;
    private final BooleanSetting fromBundle;
    private final BooleanSetting ftMode;
    private int rememberedHotbarSlot = -1;
    private int inventorySourceSlot = -1;
    private int inventoryHotbarSlot = -1;
    private int hotbarSwitchSlot = -1;
    private boolean inventorySwapActive;
    private boolean hotbarSwitchActive;
    private boolean toolActive;
    private BundleUse bundleUse;

    public AutoToolModule(EventBus eventBus) {
        super(eventBus, OWNER, "Auto Tool", "Автосмена на лучший инструмент", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.fromInventory = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("From Inventory").id("from_inventory").description("Брать из инвентаря")).build();
        this.registerSetting(this.fromInventory);
        this.swapBack = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Swap Back").id("swap_back").description("Возвращать предыдущий предмет")).build();
        this.registerSetting(this.swapBack);
        this.fromBundle = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Из мешков").id("from_bundle").description("Доставать инструмент из мешка если нет в инвентаре")).build();
        this.registerSetting(this.fromBundle);
        this.ftMode = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("FT-Mode").id("ft_mode").description("Поддержка мешков без лимита вместимости").visibleWhen(this.fromBundle::isEnabled)).build();
        this.registerSetting(this.ftMode);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        boolean mining;
        if (!this.enabledSetting.isEnabled()) {
            this.reset();
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (player == null || world == null || player.isSpectator()) {
            this.reset();
            return;
        }
        BlockPos pos = this.targetedBlock(world);
        boolean bl = mining = pos != null && MinecraftClient.getInstance().options.attackKey.isPressed();
        if (!mining) {
            this.onStopMining();
            return;
        }
        BlockState state = world.getBlockState(pos);
        int bestSlot = this.findBestSlot(state, player, this.fromInventory.isEnabled() ? 36 : 9);
        if (this.fromBundle.isEnabled() && this.tryUseBundle(player, state, bestSlot)) {
            this.toolActive = true;
            return;
        }
        if (this.bundleUse != null) {
            if (this.inventoryBetterThanBundle(player, state, bestSlot)) {
                this.restoreBundle();
            }
            this.toolActive = true;
            return;
        }
        if (bestSlot == -1) {
            this.toolActive = true;
            return;
        }
        if (bestSlot <= 8) {
            this.switchHotbar(player, bestSlot);
            this.toolActive = true;
            return;
        }
        this.switchFromInventory(player, bestSlot);
        this.toolActive = true;
    }

    private boolean inventoryBetterThanBundle(ClientPlayerEntity player, BlockState state, int slot) {
        if (slot == -1 || this.bundleUse == null) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        return this.miningSpeed(inventory.getStack(slot), state) > this.miningSpeed(inventory.getStack(this.bundleUse.slot()), state);
    }

    private boolean restoreBundle() {
        if (this.bundleUse == null) {
            return true;
        }
        this.bundleUse = null;
        return true;
    }

    private void restorePrevious() {
        if (this.inventorySwapActive) {
            this.restoreInventorySwap();
        } else if (this.hotbarSwitchActive) {
            this.restoreHotbarSwitch();
        } else {
            this.clearSwapState();
        }
    }

    private void restoreInventorySwap() {
        if (this.inventorySourceSlot < 9 || this.inventoryHotbarSlot < 0 || this.inventoryHotbarSlot > 8 || this.rememberedHotbarSlot < 0 || this.rememberedHotbarSlot > 8) {
            this.clearSwapState();
            return;
        }
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null) {
            this.clearSwapState();
            return;
        }
        int sourceSlot = this.inventorySourceSlot;
        int hotbarSlot = this.inventoryHotbarSlot;
        int remembered = this.rememberedHotbarSlot;
        inventory.submit(InventoryTask.builder().action(this.swapSequence(this.toContainerSlot(sourceSlot), hotbarSlot, () -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                this.selectHotbar(player, remembered);
            }
        })).owner(OWNER).flag(TaskFlag.REPLACE).policy(ClickPolicy.VISIBLE).priority(TaskPriority.HIGH).build());
        this.clearSwapState();
    }

    private void restoreHotbarSwitch() {
        if (this.rememberedHotbarSlot < 0 || this.rememberedHotbarSlot > 8) {
            this.clearSwapState();
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            this.selectHotbar(player, this.rememberedHotbarSlot);
        }
        this.clearSwapState();
    }

    private void selectHotbar(ClientPlayerEntity player, int slot) {
        if (slot < 0 || slot > 8) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (inventory.getSelectedSlot() == slot) {
            return;
        }
        inventory.setSelectedSlot(slot);
    }

    private float miningSpeed(ItemStack stack, BlockState state) {
        if (stack.isEmpty()) {
            return 1.0f;
        }
        float speed = stack.getMiningSpeedMultiplier(state);
        if (speed <= 1.0f) {
            return 1.0f;
        }
        int efficiency = this.efficiencyLevel(stack);
        if (efficiency > 0) {
            speed += (float)(efficiency * efficiency + 1);
        }
        return this.isCorrectTool(stack, state) ? speed : speed * 0.3f;
    }

    private BlockPos targetedBlock(ClientWorld world) {
        HitResult hit = MinecraftClient.getInstance().crosshairTarget;
        if (!(hit instanceof BlockHitResult)) {
            return null;
        }
        BlockHitResult blockHit = (BlockHitResult)hit;
        BlockPos pos = blockHit.getBlockPos();
        return world.getBlockState(pos).isAir() ? null : pos;
    }

    private int findBestSlot(BlockState state, ClientPlayerEntity player, int limit) {
        if (state.isAir()) {
            return -1;
        }
        int bestSlot = -1;
        float bestSpeed = 1.0f;
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < limit; ++slot) {
            float speed;
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty() || !((speed = this.miningSpeed(stack, state)) > bestSpeed)) continue;
            bestSpeed = speed;
            bestSlot = slot;
        }
        return bestSlot;
    }

    private int efficiencyLevel(ItemStack stack) {
        ItemEnchantmentsComponent enchants = (ItemEnchantmentsComponent)stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, (Object)ItemEnchantmentsComponent.DEFAULT);
        for (RegistryEntry enchantment : enchants.getEnchantments()) {
            Optional key = enchantment.getKey();
            if (!key.isPresent() || !((RegistryKey)key.get()).equals(Enchantments.EFFICIENCY)) continue;
            return enchants.getLevel(enchantment);
        }
        return 0;
    }

    private int toContainerSlot(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }

    private boolean tryUseBundle(ClientPlayerEntity player, BlockState state, int bestSlot) {
        float current;
        if (state.isAir() || this.bundleUse != null) {
            return false;
        }
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null || inventory.hasCurrentTask()) {
            return false;
        }
        PlayerInventory playerInventory = player.getInventory();
        int[] found = this.findInBundles(playerInventory, state, current = bestSlot == -1 ? 1.0f : this.miningSpeed(playerInventory.getStack(bestSlot), state));
        if (found == null) {
            return false;
        }
        this.bundleUse = Bundles.useFromBundle(player, inventory, OWNER, found[0], found[1], this.ftMode.isEnabled());
        return this.bundleUse != null;
    }

    private void onStopMining() {
        if (!this.toolActive) {
            return;
        }
        if (this.swapBack.isEnabled()) {
            this.restorePrevious();
        } else {
            this.clearSwapState();
        }
        if (!this.restoreBundle()) {
            return;
        }
        this.toolActive = false;
    }

    private void switchHotbar(ClientPlayerEntity player, int slot) {
        if (this.inventorySwapActive) {
            if (slot == this.inventoryHotbarSlot) {
                this.selectHotbar(player, slot);
                return;
            }
            if (this.swapBack.isEnabled()) {
                this.restorePrevious();
            } else {
                this.clearInventorySwap();
            }
        }
        if (this.hotbarSwitchActive && this.hotbarSwitchSlot == slot) {
            this.selectHotbar(player, slot);
            return;
        }
        if (this.shouldRememberSlot()) {
            this.rememberedHotbarSlot = player.getInventory().getSelectedSlot();
        }
        this.hotbarSwitchActive = true;
        this.hotbarSwitchSlot = slot;
        this.selectHotbar(player, slot);
    }

    private void switchFromInventory(ClientPlayerEntity player, int slot) {
        InventoryController inventory;
        if (slot < 9 || slot >= 36) {
            return;
        }
        if (this.inventorySwapActive && this.inventorySourceSlot == slot) {
            return;
        }
        if (this.hotbarSwitchActive) {
            if (this.swapBack.isEnabled()) {
                this.restorePrevious();
            } else {
                this.clearHotbarSwitch();
            }
        }
        if (this.inventorySwapActive) {
            if (this.swapBack.isEnabled()) {
                this.restorePrevious();
            } else {
                this.clearInventorySwap();
            }
        }
        if ((inventory = WexSideClient.getInventoryController()) == null || inventory.hasOwnerTasks(OWNER)) {
            return;
        }
        if (this.shouldRememberSlot()) {
            this.rememberedHotbarSlot = player.getInventory().getSelectedSlot();
        }
        this.inventoryHotbarSlot = this.pickHotbarDestination(player.getInventory());
        this.inventorySourceSlot = slot;
        this.inventorySwapActive = true;
        int hotbarSlot = this.inventoryHotbarSlot;
        inventory.submit(InventoryTask.builder().action(this.swapSequence(this.toContainerSlot(slot), hotbarSlot, () -> {
            ClientPlayerEntity current = MinecraftClient.getInstance().player;
            if (current != null) {
                this.selectHotbar(current, hotbarSlot);
            }
        })).owner(OWNER).flag(TaskFlag.REPLACE).policy(ClickPolicy.VISIBLE).priority(TaskPriority.NORMAL).build());
    }

    private InventoryAction swapSequence(int containerSlot, int hotbarSlot, Runnable after) {
        return new ActionSequence(List.of(new TimedAction(0, new ClickSlotAction(containerSlot, hotbarSlot)), new TimedAction(0, new RunnableAction(after))));
    }

    private int[] findInBundles(PlayerInventory inventory, BlockState state, float minSpeed) {
        int[] best = null;
        float bestSpeed = minSpeed;
        for (int slot = 0; slot < 36; ++slot) {
            BundleContentsComponent contents = (BundleContentsComponent)inventory.getStack(slot).get(DataComponentTypes.BUNDLE_CONTENTS);
            if (contents == null) continue;
            for (int nested = 0; nested < contents.size(); ++nested) {
                float speed;
                ItemStack stack = contents.get(nested);
                if (stack.isEmpty() || !((speed = this.miningSpeed(stack, state)) > bestSpeed)) continue;
                bestSpeed = speed;
                best = new int[]{slot, nested};
            }
        }
        return best;
    }

    private void clearHotbarSwitch() {
        this.hotbarSwitchActive = false;
        this.hotbarSwitchSlot = -1;
    }

    private void clearInventorySwap() {
        this.inventorySwapActive = false;
        this.inventorySourceSlot = -1;
        this.inventoryHotbarSlot = -1;
    }

    private void clearSwapState() {
        this.clearHotbarSwitch();
        this.clearInventorySwap();
        this.rememberedHotbarSlot = -1;
    }

    private void reset() {
        this.restoreBundle();
        this.bundleUse = null;
        this.hotbarSwitchActive = false;
        this.inventorySwapActive = false;
        this.hotbarSwitchSlot = -1;
        this.inventorySourceSlot = -1;
        this.rememberedHotbarSlot = -1;
        this.inventoryHotbarSlot = -1;
        this.toolActive = false;
    }

    private boolean shouldRememberSlot() {
        return !this.hotbarSwitchActive && !this.inventorySwapActive && this.rememberedHotbarSlot == -1;
    }

    private boolean isCorrectTool(ItemStack stack, BlockState state) {
        return !state.isToolRequired() || stack.isSuitableFor(state);
    }

    private int pickHotbarDestination(PlayerInventory inventory) {
        int selected = inventory.getSelectedSlot();
        int fallback = -1;
        for (int i = 0; i < 9; ++i) {
            int slot = (selected + i) % 9;
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                return slot;
            }
            if (this.isProtected(stack) || fallback != -1) continue;
            fallback = slot;
        }
        return fallback == -1 ? selected : fallback;
    }

    private boolean isProtected(ItemStack stack) {
        return stack.hasGlint() || stack.isOf(Items.TOTEM_OF_UNDYING) || stack.contains(DataComponentTypes.FOOD);
    }
}

