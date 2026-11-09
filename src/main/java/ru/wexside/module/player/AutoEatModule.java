package ru.wexside.module.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.Inventories;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;

public class AutoEatModule
extends Module
implements ConfigSerializable {
    public static volatile boolean eating;
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private final NumberSetting satiety;
    private int previousHotbarSlot = -1;
    private int swappedInventorySlot = -1;
    private int swappedHotbarSlot = -1;
    private boolean useKeyHeld;

    public AutoEatModule(EventBus eventBus) {
        super(eventBus, "auto_eat", "Auto Eat", "Автоматически ест при низкой сытости", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.satiety = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 20.0).defaultValue(18.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Satiety").id("satiety").description("Начинать есть при сытости <=")).build();
        this.registerSetting(this.satiety);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
        this.listen(WorldSessionEvent.class, event -> this.reset());
    }

    private void onTick() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (!this.enabledSetting.isEnabled() || player == null || MinecraftClient.getInstance().world == null) {
            this.reset();
            return;
        }
        int foodLevel = player.getHungerManager().getFoodLevel();
        int threshold = MathHelper.clamp((int)this.satiety.getIntValue(), (int)0, (int)20);
        if (!eating) {
            if (foodLevel < 20 && foodLevel <= threshold) {
                eating = true;
            } else {
                this.releaseUseKey();
                return;
            }
        }
        if (foodLevel >= 20) {
            this.stopEating(player);
            return;
        }
        switch (this.prepareFood(player).ordinal()) {
            case 0: {
                MinecraftClient.getInstance().options.useKey.setPressed(true);
                this.useKeyHeld = true;
                break;
            }
            case 1: {
                this.releaseUseKey();
                break;
            }
            case 2: {
                this.reset();
            }
        }
    }

    private EatState prepareFood(ClientPlayerEntity player) {
        if (this.swappedInventorySlot != -1 && !Inventories.isFood(player.getMainHandStack())) {
            this.restoreInventorySwap(player);
        }
        if (Inventories.isFood(player.getMainHandStack())) {
            return this.useFood(player, Hand.MAIN_HAND);
        }
        if (Inventories.isFood(player.getOffHandStack())) {
            return this.useFood(player, Hand.OFF_HAND);
        }
        int hotbarSlot = Inventories.findFoodHotbar();
        if (hotbarSlot != -1) {
            this.selectHotbar(player, hotbarSlot);
            return this.useFood(player, Hand.MAIN_HAND);
        }
        int inventorySlot = Inventories.findFoodInventory();
        if (inventorySlot != -1) {
            this.swapFromInventory(player, inventorySlot);
            return this.useFood(player, Hand.MAIN_HAND);
        }
        return EatState.NONE;
    }

    private EatState useFood(ClientPlayerEntity player, Hand hand) {
        if (player.isUsingItem()) {
            if (player.getActiveHand() != hand) {
                player.stopUsingItem();
                return EatState.WAIT;
            }
            return EatState.READY;
        }
        ClientPlayerInteractionManager interactions = MinecraftClient.getInstance().interactionManager;
        if (interactions != null) {
            interactions.interactItem((PlayerEntity)player, hand);
        }
        return EatState.READY;
    }

    private void selectHotbar(ClientPlayerEntity player, int slot) {
        PlayerInventory inventory = player.getInventory();
        if (this.previousHotbarSlot == -1) {
            this.previousHotbarSlot = inventory.getSelectedSlot();
        }
        inventory.setSelectedSlot(slot);
    }

    private void swapFromInventory(ClientPlayerEntity player, int slot) {
        ClientPlayerInteractionManager interactions = MinecraftClient.getInstance().interactionManager;
        if (this.swappedInventorySlot != -1 || interactions == null) {
            return;
        }
        int selected = player.getInventory().getSelectedSlot();
        interactions.clickSlot(player.currentScreenHandler.syncId, slot, selected, SlotActionType.SWAP, (PlayerEntity)player);
        this.swappedInventorySlot = slot;
        this.swappedHotbarSlot = selected;
    }

    private void restoreInventorySwap(ClientPlayerEntity player) {
        if (this.swappedInventorySlot == -1) {
            return;
        }
        ClientPlayerInteractionManager interactions = MinecraftClient.getInstance().interactionManager;
        if (interactions != null) {
            interactions.clickSlot(player.currentScreenHandler.syncId, this.swappedInventorySlot, this.swappedHotbarSlot, SlotActionType.SWAP, (PlayerEntity)player);
        }
        this.swappedInventorySlot = -1;
        this.swappedHotbarSlot = -1;
    }

    private void restoreHotbar(ClientPlayerEntity player) {
        if (this.previousHotbarSlot == -1) {
            return;
        }
        player.getInventory().setSelectedSlot(this.previousHotbarSlot);
        this.previousHotbarSlot = -1;
    }

    private void stopEating(ClientPlayerEntity player) {
        this.releaseUseKey();
        eating = false;
        this.restoreInventorySwap(player);
        this.restoreHotbar(player);
    }

    private void reset() {
        this.releaseUseKey();
        eating = false;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            this.restoreInventorySwap(player);
            this.restoreHotbar(player);
        } else {
            this.swappedInventorySlot = -1;
            this.swappedHotbarSlot = -1;
            this.previousHotbarSlot = -1;
        }
    }

    private void releaseUseKey() {
        if (!this.useKeyHeld) {
            return;
        }
        MinecraftClient.getInstance().options.useKey.setPressed(false);
        this.useKeyHeld = false;
    }

    private static enum EatState {
        READY,
        WAIT,
        NONE;

    }
}

