package ru.wexside.module.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.BlockInteractEvent;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.misc.BundleUse;
import ru.wexside.misc.Bundles;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.ClickSlotAction;
import ru.wexside.misc.InventoryAction;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.ItemAlerts;
import ru.wexside.misc.ItemBindBox;
import ru.wexside.misc.ItemHelperCatalog;
import ru.wexside.misc.ItemHelperEntry;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.BindSettingBuilder;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.util.InventoryController;
import ru.wexside.util.ItemStatusHudElement;

public class ItemHelperModule
extends Module
implements ConfigSerializable {
    private static final String OWNER = "item_helper";
    private final BooleanSetting enabledSetting;
    private final ModeSetting swapMode;
    private final BooleanSetting fromBundle;
    private final BooleanSetting ftMode;
    private final BooleanSetting visualizer;
    private final BooleanSetting visualizerAll;
    private final MultiSelectSetting visualizerItems;
    private final BooleanSetting highlightEmpty;
    private ItemStatusHudElement visualizerHud;
    private ItemHelperEntry currentItem;
    private BundleUse bundleUse;
    private int itemSlot = -1;
    private int previousSlot = -1;
    private boolean swappedFromInventory;
    private boolean releasing;

    public ItemHelperModule(EventBus eventBus) {
        super(eventBus, OWNER, "Item Helper", "Быстрое использование предметов по биндам", ModuleCategory.valueOf("COMBAT"), "itemhelper", "items");
        List<ItemHelperEntry> presets = ItemHelperCatalog.list;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.swapMode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Default", "Legit").defaultOption("Default").name("Mode").id("swap_mode").description("Скорость свапа: Default - мгновенно, Legit - с задержкой")).build();
        this.registerSetting(this.swapMode);
        this.fromBundle = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Из мешков").id("from_bundle").description("Доставать предмет из мешка если нет в инвентаре")).build();
        this.registerSetting(this.fromBundle);
        this.ftMode = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("FT-Mode").id("ft_mode").description("Поддержка мешков без лимита вместимости").visibleWhen(this.fromBundle::isEnabled)).build();
        this.registerSetting(this.ftMode);
        ItemHelperEntry chorus = presets.get(0);
        BindSetting chorusBind = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(bind -> this.onPress(chorus, bind)).onPressed(bind -> this.onRelease(chorus, bind)).name("Плод хоруса").id("bind_chorus").description("Использовать пока зажато: Плод хоруса")).build();
        this.registerSetting(chorusBind);
        ItemHelperEntry gapple = presets.get(1);
        BindSetting gappleBind = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(bind -> this.onPress(gapple, bind)).onPressed(bind -> this.onRelease(gapple, bind)).name("Золотое яблоко").id("bind_gapple").description("Использовать пока зажато: Золотое яблоко")).build();
        this.registerSetting(gappleBind);
        ItemHelperEntry enchantedGapple = presets.get(2);
        BindSetting enchantedGappleBind = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(bind -> this.onPress(enchantedGapple, bind)).onPressed(bind -> this.onRelease(enchantedGapple, bind)).name("Зачарованное золотое яблоко").id("bind_enchanted_gapple").description("Использовать пока зажато: Зачарованное золотое яблоко")).build();
        this.registerSetting(enchantedGappleBind);
        ItemHelperEntry instantHealing = presets.get(3);
        BindSetting instantHealingBind = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(bind -> this.onPress(instantHealing, bind)).onPressed(bind -> this.onRelease(instantHealing, bind)).name("Зелье исцеления").id("bind_instant_healing").description("Использовать пока зажато: Зелье исцеления")).build();
        this.registerSetting(instantHealingBind);
        ItemHelperEntry shield = presets.get(4);
        BindSetting shieldBind = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(bind -> this.onPress(shield, bind)).onPressed(bind -> this.onRelease(shield, bind)).name("Щит").id("bind_shield").description("Использовать пока зажато: Щит")).build();
        this.registerSetting(shieldBind);
        ItemHelperEntry crossbow = presets.get(5);
        BindSetting crossbowBind = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(bind -> this.onPress(crossbow, bind)).onPressed(bind -> this.onRelease(crossbow, bind)).name("Арбалет").id("bind_crossbow").description("Использовать пока зажато: Арбалет")).build();
        this.registerSetting(crossbowBind);
        ItemHelperEntry milk = presets.get(6);
        BindSetting milkBind = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(bind -> this.onPress(milk, bind)).onPressed(bind -> this.onRelease(milk, bind)).name("Молоко").id("bind_milk").description("Использовать пока зажато: Молоко")).build();
        this.registerSetting(milkBind);
        this.visualizer = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Visualizer").id("visualizer").description("Сетка предметов с биндами")).build();
        this.registerSetting(this.visualizer);
        this.visualizerAll = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Все предметы").id("visualizer_all").description("Показывать все предметы в сетке").visibleWhen(this.visualizer::isEnabled)).build();
        this.registerSetting(this.visualizerAll);
        MultiSelectSetting visualizerItemsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Плод хоруса", "Золотое яблоко", "Зачарованное золотое яблоко", "Зелье исцеления", "Щит", "Арбалет", "Молоко").selectAll(true).optionListEnabled(false).name("Предметы").id("visualizer_items").description("Предметы в сетке").visibleWhen(() -> this.visualizer.isEnabled() && !this.visualizerAll.isEnabled())).build();
        visualizerItemsSetting.setOptions(new String[]{"Плод хоруса", "Золотое яблоко", "Зачарованное золотое яблоко", "Зелье исцеления", "Щит", "Арбалет", "Молоко"});
        this.visualizerItems = visualizerItemsSetting;
        this.registerSetting(visualizerItemsSetting);
        this.highlightEmpty = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Highlight Empty").id("highlight_empty").description("Подсветка отсутствующих предметов").visibleWhen(this.visualizer::isEnabled)).build();
        this.registerSetting(this.highlightEmpty);
        ArrayList<ItemBindBox> boxes = new ArrayList<ItemBindBox>();
        BindSetting[] binds = new BindSetting[]{chorusBind, gappleBind, enchantedGappleBind, instantHealingBind, shieldBind, crossbowBind, milkBind};
        for (int i = 0; i < presets.size(); ++i) {
            ItemHelperEntry preset = presets.get(i);
            boxes.add(new ItemBindBox(preset.getItem(), binds[i], stack -> ItemHelperModule.matchesItem(preset, stack), () -> this.isVisualizerItemEnabled(preset)));
        }
        this.visualizerHud = new ItemStatusHudElement("Item Helper", this::isVisualizerEnabled, boxes, this.highlightEmpty::isEnabled);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
        this.listen(HudRenderEvent.class, event -> {
            if (this.visualizerHud != null) {
                this.visualizerHud.renderFrame();
            }
        });
        this.listen(BlockInteractEvent.class, this::onBlockInteract);
    }

    private void onTick() {
        if (this.currentItem == null) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            this.reset();
            return;
        }
        if (this.releasing) {
            MinecraftClient.getInstance().options.useKey.setPressed(false);
            if (this.bundleUse == null && this.swappedFromInventory) {
                this.swapSlots(this.itemSlot, this.previousSlot);
            } else if (this.bundleUse == null && this.previousSlot != -1 && this.previousSlot != this.itemSlot) {
                player.getInventory().setSelectedSlot(this.previousSlot);
            }
            this.reset();
            return;
        }
        if (this.bundleUse != null && !ItemHelperModule.matches(player.getInventory().getStack(this.itemSlot), this.currentItem)) {
            return;
        }
        MinecraftClient.getInstance().options.useKey.setPressed(true);
    }

    private void reset() {
        this.currentItem = null;
        this.itemSlot = -1;
        this.previousSlot = -1;
        this.swappedFromInventory = false;
        this.releasing = false;
        this.bundleUse = null;
    }

    private void onPress(ItemHelperEntry itemBind, BindSetting ignored) {
        this.startUsing(itemBind);
    }

    private void onRelease(ItemHelperEntry itemBind, BindSetting ignored) {
        if (this.currentItem != itemBind) {
            return;
        }
        this.releasing = true;
    }

    private void startUsing(ItemHelperEntry itemBind) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        if (this.currentItem != null) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        int slot = this.findSlot(inventory, itemBind);
        boolean inBundle = this.fromBundle.isEnabled() && (itemBind.isInstantHealingPotion() ? this.findBestHealingInBundle(inventory) != null : Bundles.contains(inventory, stack -> stack.getItem() == itemBind.getItem()));
        ItemStack preview = new ItemStack((ItemConvertible)itemBind.getItem());
        if (slot == -1 && !inBundle) {
            ItemAlerts.warnMissing(preview, itemBind.getName());
            return;
        }
        if (ItemAlerts.isBusy(null, preview, itemBind.getName())) {
            return;
        }
        if (this.fromBundle.isEnabled() && this.bundleHasBetterHealing(inventory, itemBind, slot) && this.useFromBundle(player, itemBind)) {
            return;
        }
        if (slot == -1) {
            return;
        }
        this.equip(player, itemBind, slot);
    }

    private void equip(ClientPlayerEntity player, ItemHelperEntry itemBind, int slot) {
        this.currentItem = itemBind;
        this.itemSlot = slot;
        this.previousSlot = player.getInventory().getSelectedSlot();
        if (slot < 9) {
            this.swappedFromInventory = false;
            if (this.previousSlot != slot) {
                player.getInventory().setSelectedSlot(slot);
            }
        } else {
            this.swappedFromInventory = true;
            this.swapSlots(slot, this.previousSlot);
        }
    }

    private boolean useFromBundle(ClientPlayerEntity player, ItemHelperEntry itemBind) {
        int[] found;
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null || inventory.hasCurrentTask()) {
            return false;
        }
        int[] nArray = found = itemBind.isInstantHealingPotion() ? this.findBestHealingInBundle(player.getInventory()) : Bundles.findInBundle(player.getInventory(), stack -> stack.getItem() == itemBind.getItem());
        if (found == null) {
            return false;
        }
        BundleUse session = Bundles.useFromBundle(player, inventory, OWNER, found[0], found[1], this.ftMode.isEnabled());
        if (session == null) {
            return false;
        }
        this.bundleUse = session;
        this.currentItem = itemBind;
        this.itemSlot = session.slot();
        this.previousSlot = session.slot();
        this.swappedFromInventory = false;
        return true;
    }

    private boolean bundleHasBetterHealing(PlayerInventory inventory, ItemHelperEntry itemBind, int slot) {
        if (slot == -1) {
            return true;
        }
        if (!itemBind.isInstantHealingPotion()) {
            return false;
        }
        int[] found = this.findBestHealingInBundle(inventory);
        return found != null && found[2] > ItemHelperModule.healingAmplifier(inventory.getStack(slot));
    }

    private void swapSlots(int slot, int selected) {
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null) {
            return;
        }
        ClickPolicy policy = "Legit".equalsIgnoreCase(this.swapMode.getSelectedOption()) ? ClickPolicy.SWAP : ClickPolicy.VISIBLE;
        inventory.submit(this.task(new ClickSlotAction(slot, selected), policy));
    }

    private int findSlot(PlayerInventory inventory, ItemHelperEntry itemBind) {
        if (itemBind.isInstantHealingPotion()) {
            return this.findBestHealingSlot(inventory);
        }
        for (int slot = 0; slot < 36; ++slot) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty() || stack.getItem() != itemBind.getItem()) continue;
            return slot;
        }
        return -1;
    }

    private int findBestHealingSlot(PlayerInventory inventory) {
        int bestSlot = -1;
        int bestAmplifier = -1;
        for (int slot = 0; slot < 36; ++slot) {
            int amplifier = ItemHelperModule.healingAmplifier(inventory.getStack(slot));
            if (amplifier <= bestAmplifier) continue;
            bestAmplifier = amplifier;
            bestSlot = slot;
        }
        return bestSlot;
    }

    private int[] findBestHealingInBundle(PlayerInventory inventory) {
        int[] best = null;
        int bestAmplifier = -1;
        for (int slot = 0; slot < 36; ++slot) {
            BundleContentsComponent contents = (BundleContentsComponent)inventory.getStack(slot).get(DataComponentTypes.BUNDLE_CONTENTS);
            if (contents == null) continue;
            for (int nested = 0; nested < contents.size(); ++nested) {
                int amplifier = ItemHelperModule.healingAmplifier(contents.get(nested));
                if (amplifier <= bestAmplifier) continue;
                bestAmplifier = amplifier;
                best = new int[]{slot, nested, amplifier};
            }
        }
        return best;
    }

    private void onBlockInteract(BlockInteractEvent event) {
        if (this.currentItem != null) {
            event.cancel();
        }
    }

    private boolean isVisualizerEnabled() {
        return this.enabledSetting.isEnabled() && this.visualizer.isEnabled();
    }

    private boolean isVisualizerItemEnabled(ItemHelperEntry itemBind) {
        return this.visualizerAll.isEnabled() || this.visualizerItems.getSelectedOptions().contains(itemBind.getName());
    }

    private static boolean matches(ItemStack stack, ItemHelperEntry itemBind) {
        return itemBind.isInstantHealingPotion() ? ItemHelperModule.isHealingPotion(stack) : stack.getItem() == itemBind.getItem();
    }

    private static boolean matchesItem(ItemHelperEntry itemBind, ItemStack stack) {
        return stack.getItem() == itemBind.getItem();
    }

    private static boolean isHealingPotion(ItemStack stack) {
        return ItemHelperModule.healingAmplifier(stack) >= 0;
    }

    private static int healingAmplifier(ItemStack stack) {
        if (stack.getItem() != Items.POTION) {
            return -1;
        }
        PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) {
            return -1;
        }
        int amplifier = -1;
        for (StatusEffectInstance instance : contents.getEffects()) {
            if (!instance.getEffectType().equals((Object)StatusEffects.INSTANT_HEALTH)) continue;
            amplifier = Math.max(amplifier, instance.getAmplifier());
        }
        return amplifier;
    }

    private InventoryTask task(InventoryAction action, ClickPolicy policy) {
        return InventoryTask.builder().action(action).owner(OWNER).flag(TaskFlag.DEFAULT).policy(policy).priority(TaskPriority.HIGH).build();
    }
}

