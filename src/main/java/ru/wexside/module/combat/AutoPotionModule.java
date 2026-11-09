package ru.wexside.module.combat;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.AttackUrgency;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.CorrectionMode;
import ru.wexside.misc.InventoryAction;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.PotionEntry;
import ru.wexside.misc.SwapTiming;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.util.Angle;
import ru.wexside.util.HotbarSlotLock;
import ru.wexside.util.InventoryController;
import ru.wexside.util.RotationController;
import ru.wexside.util.RotationIntent;

public class AutoPotionModule
extends Module
implements ConfigSerializable {
    private static final String OWNER = "auto_potion";
    private final BooleanSetting enabledSetting;
    private final MultiSelectSetting potions;
    private final List<PotionEntry> potionPresets = List.of(new PotionEntry("Fire Resistance", (RegistryEntry<StatusEffect>)StatusEffects.FIRE_RESISTANCE), new PotionEntry("Strength", (RegistryEntry<StatusEffect>)StatusEffects.STRENGTH), new PotionEntry("Speed", (RegistryEntry<StatusEffect>)StatusEffects.SPEED));
    private int lookTicks;
    private long lastUseTime;
    private Angle throwAngle;

    public AutoPotionModule(EventBus eventBus) {
        super(eventBus, OWNER, "Auto Potion", "Бросает зелья под себя при отсутствии эффекта", ModuleCategory.valueOf("COMBAT"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        MultiSelectSetting potionsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Fire Resistance", "Strength", "Speed").selectAll(false).optionListEnabled(false).name("Potions").id("potions").description("Список зелий для использования")).build();
        potionsSetting.setOptions(new String[0]);
        this.potions = potionsSetting;
        this.registerSetting(potionsSetting);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        if (this.lookTicks > 0) {
            this.tickLook(player);
            return;
        }
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        if (System.currentTimeMillis() - this.lastUseTime < 250L) {
            return;
        }
        if (!player.isOnGround()) {
            return;
        }
        int slot = this.findPotionSlot(player);
        if (slot == -1) {
            return;
        }
        this.throwAngle = new Angle(player.getYaw(), 90.0f);
        this.lookTicks = 5;
        this.applyLook(player, this.throwAngle);
        if (slot < 9) {
            this.throwFromHotbar(player, slot);
        } else {
            this.throwFromInventory(player, slot);
        }
        this.lastUseTime = System.currentTimeMillis();
    }

    private void tickLook(ClientPlayerEntity player) {
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations == null) {
            this.lookTicks = 0;
            this.throwAngle = null;
            return;
        }
        --this.lookTicks;
        if (this.lookTicks > 0 && this.throwAngle != null) {
            this.applyLook(player, this.throwAngle);
        } else {
            this.releaseLook();
        }
    }

    private void applyLook(ClientPlayerEntity player, Angle angle) {
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations == null) {
            return;
        }
        rotations.applyIntent(new RotationIntent((LivingEntity)player, null, angle, AttackUrgency.HIT, CorrectionMode.FOCUSED, false), "FT Snap");
    }

    private void releaseLook() {
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations != null) {
            rotations.clearIntent();
        }
        this.throwAngle = null;
    }

    private void throwFromHotbar(ClientPlayerEntity player, int slot) {
        PlayerInventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        if (selected != slot) {
            inventory.setSelectedSlot(slot);
        }
        this.useMainHand();
        HotbarSlotLock hotbar = WexSideClient.getHotbarSlotLock();
        if (hotbar != null && selected != slot) {
            hotbar.lockSlot(selected, 400L);
        }
    }

    private void throwFromInventory(ClientPlayerEntity player, int slot) {
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null) {
            return;
        }
        int selected = player.getInventory().getSelectedSlot();
        int containerSlot = slot < 9 ? slot + 36 : slot;
        inventory.submit(this.task(inventory.createSwapUseRestore(containerSlot, selected, this::useMainHand, SwapTiming.DEFAULT), ClickPolicy.VISIBLE));
    }

    private void useMainHand() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        if (player == null || interactionManager == null) {
            return;
        }
        if (player.getMainHandStack().isEmpty()) {
            return;
        }
        interactionManager.interactItem((PlayerEntity)player, Hand.MAIN_HAND);
        player.swingHand(Hand.MAIN_HAND);
    }

    private int findPotionSlot(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        List<String> selected = this.potions.getSelectedOptions();
        for (PotionEntry preset : this.potionPresets) {
            int slot;
            if (!selected.contains(preset.getName()) || player.hasStatusEffect(preset.getEffect()) || (slot = this.findMatchingPotion(inventory, preset.getEffect())) == -1) continue;
            return slot;
        }
        return -1;
    }

    private int findMatchingPotion(PlayerInventory inventory, RegistryEntry<StatusEffect> effect) {
        for (int slot = 0; slot < 36; ++slot) {
            PotionContentsComponent contents;
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof PotionItem) || (contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS)) == null) continue;
            for (StatusEffectInstance instance : contents.getEffects()) {
                if (!instance.getEffectType().equals(effect)) continue;
                return slot;
            }
        }
        return -1;
    }

    private InventoryTask task(InventoryAction action, ClickPolicy policy) {
        return InventoryTask.builder().action(action).owner(OWNER).flag(TaskFlag.DEFAULT).policy(policy).priority(TaskPriority.NORMAL).build();
    }
}

