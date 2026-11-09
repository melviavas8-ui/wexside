package ru.wexside.module.combat;

import java.util.HashSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.Bundles;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.Inventories;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.SwapSlotsAction;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.InventoryController;

public class AutoArmorModule
extends Module
implements ConfigSerializable {
    private static final String OWNER = "auto_armor";
    private static final long BUNDLE_RETRY_DELAY_MS = 3000L;
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final EquipmentSlot[] ARMOR_SLOTS_KEEP_ELYTRA = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private final BooleanSetting fromBundle;
    private final BooleanSetting ftMode;
    private final BooleanSetting byDurability;
    private final NumberSetting durabilityThreshold;
    private long nextBundleAttemptAt;

    public AutoArmorModule(EventBus eventBus) {
        super(eventBus, OWNER, "Auto Armor", "Автоматически надевает лучшую броню", ModuleCategory.valueOf("COMBAT"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.fromBundle = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Из мешков").id("from_bundle").description("Учитывать броню в мешках")).build();
        this.registerSetting(this.fromBundle);
        this.ftMode = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("FT-Mode").id("ft_mode").description("Поддержка мешков без лимита вместимости").visibleWhen(this.fromBundle::isEnabled)).build();
        this.registerSetting(this.ftMode);
        this.byDurability = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("По прочности").id("by_durability").description("Менять броню, когда её прочность упала до предела")).build();
        this.registerSetting(this.byDurability);
        this.durabilityThreshold = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 99.0).defaultValue(10.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Порог прочности").id("durability_threshold").description("Прочность, при которой и ниже броня меняется на целую").visibleWhen(this.byDurability::isEnabled)).build();
        this.registerSetting(this.durabilityThreshold);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        EquipmentSlot[] slots;
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null) {
            return;
        }
        if (!this.enabledSetting.isEnabled()) {
            inventory.setString(OWNER);
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen != null && !(screen instanceof InventoryScreen)) {
            return;
        }
        boolean wearingElytra = player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        for (EquipmentSlot slot : slots = wearingElytra ? ARMOR_SLOTS_KEEP_ELYTRA : ARMOR_SLOTS) {
            int armorSlot = Inventories.findArmorSlot(slot);
            if (armorSlot == -1) continue;
            int bestSlot = this.findBestArmorSlot(player, slot);
            if (this.tryEquipFromBundle(player, inventory, slot, armorSlot, bestSlot)) {
                return;
            }
            if (bestSlot == -1 || !this.isBetterArmor(slot, bestSlot)) continue;
            inventory.submit(InventoryTask.builder().action(new SwapSlotsAction(armorSlot, bestSlot)).owner(OWNER).flag(TaskFlag.REPLACE).policy(ClickPolicy.VISIBLE).priority(TaskPriority.NORMAL).build());
            return;
        }
    }

    private boolean isBetterArmor(EquipmentSlot slot, int inventorySlot) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || inventorySlot < 0 || inventorySlot >= 36) {
            return false;
        }
        ItemStack candidate = player.getInventory().getStack(inventorySlot);
        if (candidate.isEmpty()) {
            return false;
        }
        EquippableComponent equippable = (EquippableComponent)candidate.get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null || equippable.slot() != slot) {
            return false;
        }
        ItemStack equipped = player.getEquippedStack(slot);
        if (this.isBelowDurabilityThreshold(equipped)) {
            return !this.isBelowDurabilityThreshold(candidate);
        }
        return AutoArmorModule.getProtection(candidate, slot) > AutoArmorModule.getProtection(equipped, slot);
    }

    private boolean isBelowDurabilityThreshold(ItemStack stack) {
        if (!this.byDurability.isEnabled() || stack.isEmpty() || !stack.isDamageable()) {
            return false;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return false;
        }
        double remainingPercent = (double)(maxDamage - stack.getDamage()) * 100.0 / (double)maxDamage;
        return remainingPercent <= this.durabilityThreshold.getValue();
    }

    private int findBestArmorSlot(ClientPlayerEntity player, EquipmentSlot slot) {
        ItemStack equipped = player.getEquippedStack(slot);
        boolean replaceDamaged = this.isBelowDurabilityThreshold(equipped);
        int bestProtection = replaceDamaged ? Integer.MIN_VALUE : AutoArmorModule.getProtection(equipped, slot);
        int bestSlot = -1;
        HashSet<Integer> seenProtection = new HashSet<Integer>();
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 36; ++i) {
            int protection;
            EquippableComponent equippable;
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty() || (equippable = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE)) == null || equippable.slot() != slot || replaceDamaged && this.isBelowDurabilityThreshold(stack) || seenProtection.contains(protection = AutoArmorModule.getProtection(stack, slot))) continue;
            seenProtection.add(protection);
            if (protection <= bestProtection) continue;
            bestProtection = protection;
            bestSlot = i;
        }
        return bestSlot;
    }

    private boolean tryEquipFromBundle(ClientPlayerEntity player, InventoryController inventory, EquipmentSlot slot, int armorSlot, int bestInventorySlot) {
        int[] found;
        int bestProtection;
        if (!this.fromBundle.isEnabled() || inventory.hasCurrentTask()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < this.nextBundleAttemptAt) {
            return false;
        }
        PlayerInventory playerInventory = player.getInventory();
        ItemStack equipped = player.getEquippedStack(slot);
        boolean replaceDamaged = this.isBelowDurabilityThreshold(equipped);
        int n = bestProtection = replaceDamaged ? Integer.MIN_VALUE : AutoArmorModule.getProtection(equipped, slot);
        if (bestInventorySlot != -1) {
            ItemStack best = playerInventory.getStack(bestInventorySlot);
            if (!replaceDamaged || !this.isBelowDurabilityThreshold(best)) {
                bestProtection = Math.max(bestProtection, AutoArmorModule.getProtection(best, slot));
            }
        }
        if ((found = this.findBestInBundle(playerInventory, slot, bestProtection, replaceDamaged)) == null) {
            return false;
        }
        if (Bundles.useFromBundle(player, inventory, OWNER, found[0], found[1], armorSlot, this.ftMode.isEnabled())) {
            return true;
        }
        this.nextBundleAttemptAt = now + 3000L;
        return false;
    }

    private int[] findBestInBundle(PlayerInventory inventory, EquipmentSlot slot, int minProtection, boolean replaceDamaged) {
        int[] best = null;
        int bestProtection = minProtection;
        for (int i = 0; i < 36; ++i) {
            BundleContentsComponent contents = (BundleContentsComponent)inventory.getStack(i).get(DataComponentTypes.BUNDLE_CONTENTS);
            if (contents == null) continue;
            for (int nested = 0; nested < contents.size(); ++nested) {
                int protection;
                EquippableComponent equippable;
                ItemStack stack = contents.get(nested);
                if (stack.isEmpty() || (equippable = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE)) == null || equippable.slot() != slot || replaceDamaged && this.isBelowDurabilityThreshold(stack) || (protection = AutoArmorModule.getProtection(stack, slot)) <= bestProtection) continue;
                bestProtection = protection;
                best = new int[]{i, nested};
            }
        }
        return best;
    }

    private static int getProtection(ItemStack stack, EquipmentSlot slot) {
        AttributeModifiersComponent modifiers = (AttributeModifiersComponent)stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0;
        }
        double[] total = new double[]{0.0};
        modifiers.applyModifiers(slot, (attribute, modifier) -> {
            if (attribute.matches(EntityAttributes.ARMOR)) {
                total[0] = total[0] + modifier.value();
            }
        });
        return (int)total[0];
    }
}

