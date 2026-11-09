package ru.wexside.module.combat;

import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.AttackUrgency;
import ru.wexside.misc.Bundles;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.CorrectionMode;
import ru.wexside.misc.HotbarSelectAction;
import ru.wexside.misc.Inventories;
import ru.wexside.misc.InventoryAction;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.ItemAlerts;
import ru.wexside.misc.SwapSlotsAction;
import ru.wexside.misc.SwapTiming;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.misc.UseItemAction;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.module.combat.AttackAuraModule;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.BindSettingBuilder;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.Angle;
import ru.wexside.util.InventoryController;
import ru.wexside.util.RotationController;
import ru.wexside.util.RotationIntent;

public class ElytraHelperModule
extends Module
implements ConfigSerializable {
    private static final String OWNER = "elytra_helper";
    private static final int CHEST_SLOT = 6;
    private static volatile ElytraHelperModule instance;
    private final BooleanSetting enabledSetting;
    private final ModeSetting mode;
    private final BindSetting swapKey;
    private final BindSetting fireworkKey;
    private final BooleanSetting autoFly;
    private final BooleanSetting autoFirework;
    private final BooleanSetting followTarget;
    private final NumberSetting fireworkDelay;
    private final BooleanSetting armorOnLand;
    private final BooleanSetting fromBundle;
    private final BooleanSetting ftMode;
    private AttackAuraModule attackAura;
    private boolean fireworkUsedAfterTakeoff;
    private boolean requestStartGlide;
    private boolean wasOnGround;
    private int glideCooldown;
    private long lastFireworkTime;

    public ElytraHelperModule(EventBus eventBus) {
        super(eventBus, OWNER, "Elytra Helper", "Управление элитрой и фейерверками", ModuleCategory.valueOf("COMBAT"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.mode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Default", "Legit", "FS").defaultOption("Default").name("Mode").id("mode").description("Режим свапа элитры")).build();
        this.registerSetting(this.mode);
        this.swapKey = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(this::onSwapBind).name("Swap Key").id("swap_key").description("Кнопка надевания элитры")).build();
        this.registerSetting(this.swapKey);
        this.fireworkKey = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(this::onFireworkBind).name("Firework Key").id("firework_key").description("Кнопка фейерверка")).build();
        this.registerSetting(this.fireworkKey);
        this.autoFly = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Auto Fly").id("auto_fly").description("Автоматическое включение полёта")).build();
        this.registerSetting(this.autoFly);
        this.autoFirework = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Auto Firework").id("auto_firework").description("Запуск фейерверка после взлёта").visibleWhen(this.autoFly::isEnabled)).build();
        this.registerSetting(this.autoFirework);
        this.followTarget = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Follow Target").id("follow_target").description("Полёт за целью")).build();
        this.registerSetting(this.followTarget);
        this.fireworkDelay = ((NumberSettingBuilder)NumberSetting.builder().range(100.0, 5000.0).defaultValue(500.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Firework Delay").id("firework_delay").description("Задержка между фейерверками").visibleWhen(this.followTarget::isEnabled)).build();
        this.registerSetting(this.fireworkDelay);
        this.armorOnLand = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Armor On Land").id("armor_on_land").description("Надевает нагрудник при приземлении")).build();
        this.registerSetting(this.armorOnLand);
        this.fromBundle = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Из мешков").id("from_bundle").description("Доставать элитру и фейерверки из мешка если нет в инвентаре")).build();
        this.registerSetting(this.fromBundle);
        this.ftMode = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("FT-Mode").id("ft_mode").description("Поддержка мешков без лимита вместимости").visibleWhen(this.fromBundle::isEnabled)).build();
        this.registerSetting(this.ftMode);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        boolean wearingElytra = player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        boolean onGround = player.isOnGround();
        if (wearingElytra && this.armorOnLand.isEnabled() && onGround && !this.wasOnGround) {
            this.swapChest();
        }
        if (this.autoFly.isEnabled() && wearingElytra) {
            this.tickAutoFly(player, onGround);
        }
        if (this.followTarget.isEnabled() && wearingElytra) {
            this.tickFollowTarget(player);
        }
        this.wasOnGround = onGround;
    }

    private void onSwapBind(BindSetting ignored) {
        this.swapChest();
    }

    private void onFireworkBind(BindSetting ignored) {
        this.useFirework();
    }

    private void useFirework() {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null) {
            return;
        }
        if (player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
            inventory.submit(this.task(new UseItemAction(), ClickPolicy.SILENT));
            this.lastFireworkTime = System.currentTimeMillis();
            return;
        }
        int slot = Inventories.findSlot(Items.FIREWORK_ROCKET);
        if (slot == -1) {
            int[] found;
            if (this.fromBundle.isEnabled() && (found = Bundles.findInBundle(player.getInventory(), stack -> stack.isOf(Items.FIREWORK_ROCKET))) != null) {
                Bundles.useFromBundle(player, inventory, OWNER, found[0], found[1], this.ftMode.isEnabled(), inventory::pressUseForTick);
                this.lastFireworkTime = System.currentTimeMillis();
                return;
            }
            ItemStack firework = new ItemStack((ItemConvertible)Items.FIREWORK_ROCKET);
            ItemAlerts.warnMissing(firework, firework.getName().getString());
            return;
        }
        if (slot < 9) {
            inventory.submit(this.task(new HotbarSelectAction(slot, true), ClickPolicy.SILENT));
        } else {
            this.swapFireworkFromInventory(player, slot, inventory);
        }
        this.lastFireworkTime = System.currentTimeMillis();
    }

    private void swapFireworkFromInventory(ClientPlayerEntity player, int slot, InventoryController inventory) {
        int selected = player.getInventory().getSelectedSlot();
        int containerSlot = slot < 9 ? slot + 36 : slot;
        inventory.submit(this.task(inventory.createSwapUseRestore(containerSlot, selected, inventory::pressUseForTick, this.swapTiming()), ClickPolicy.VISIBLE));
    }

    private void swapChest() {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null) {
            return;
        }
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        boolean wearingElytra = chest.isOf(Items.ELYTRA);
        int slot = wearingElytra ? this.findChestplateSlot(player) : this.findElytraSlot(player);
        int n = slot;
        if (slot == -1 && this.fromBundle.isEnabled() && this.useChestFromBundle(player, inventory, chest)) {
            return;
        }
        if (slot == -1 && wearingElytra) {
            slot = this.findEmptySlot(player);
        }
        if (slot == -1) {
            if (wearingElytra) {
                ItemAlerts.warnMissing(new ItemStack((ItemConvertible)Items.DIAMOND_CHESTPLATE), "Нагрудник");
            } else {
                ItemStack elytra = new ItemStack((ItemConvertible)Items.ELYTRA);
                ItemAlerts.warnMissing(elytra, elytra.getName().getString());
            }
            return;
        }
        inventory.submit(this.task(new SwapSlotsAction(6, slot), this.legitMode() ? ClickPolicy.SWAP : ClickPolicy.VISIBLE));
    }

    private boolean useChestFromBundle(ClientPlayerEntity player, InventoryController inventory, ItemStack chest) {
        if (inventory.hasCurrentTask()) {
            return false;
        }
        Predicate<ItemStack> predicate = chest.isOf(Items.ELYTRA) ? ElytraHelperModule::isChestArmor : stack -> stack.isOf(Items.ELYTRA) && stack.getDamage() < stack.getMaxDamage() - 1;
        int[] found = Bundles.findInBundle(player.getInventory(), predicate);
        if (found == null) {
            return false;
        }
        return Bundles.useFromBundle(player, inventory, OWNER, found[0], found[1], 6, this.ftMode.isEnabled());
    }

    private void tickFollowTarget(ClientPlayerEntity player) {
        LivingEntity target = this.getAttackAuraTarget();
        if (target == null) {
            return;
        }
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations == null) {
            return;
        }
        Vec3d from = player.getEyePos();
        Vec3d to = target.getEyePos();
        Angle angle = Angle.fromVectors(from, to);
        rotations.applyIntent(new RotationIntent(target, to, angle, AttackUrgency.HIT, CorrectionMode.FOCUSED, false), "Simple");
        long now = System.currentTimeMillis();
        long delay = (long)this.fireworkDelay.getFloatValue();
        boolean fire = target.hurtTime == 0 && now - this.lastFireworkTime >= delay || player.distanceTo((Entity)target) < 10.0f && now - this.lastFireworkTime >= 500L;
        boolean bl = fire;
        if (fire) {
            this.useFirework();
        }
    }

    private void tickAutoFly(ClientPlayerEntity player, boolean onGround) {
        if (this.glideCooldown > 0) {
            --this.glideCooldown;
        }
        if (onGround) {
            this.fireworkUsedAfterTakeoff = false;
            this.requestStartGlide = true;
            return;
        }
        if (player.isGliding()) {
            if (this.autoFirework.isEnabled() && !this.fireworkUsedAfterTakeoff) {
                this.fireworkUsedAfterTakeoff = true;
                this.useFirework();
            }
            return;
        }
        if (this.glideCooldown > 0) {
            return;
        }
        if (player.getVelocity().y >= 0.0) {
            return;
        }
        if (player.isTouchingWater() || player.isSubmergedInWater() || player.getAbilities().flying) {
            return;
        }
        this.requestStartGlide = true;
        this.glideCooldown = 5;
    }

    private LivingEntity getAttackAuraTarget() {
        if (this.attackAura == null) {
            for (Module module : WexSideClient.getInstance().getModuleManager().getModules()) {
                AttackAuraModule aura;
                if (!(module instanceof AttackAuraModule)) continue;
                this.attackAura = aura = (AttackAuraModule)module;
                break;
            }
        }
        return this.attackAura == null ? null : this.attackAura.getLivingEntity();
    }

    private static boolean isChestArmor(ItemStack stack) {
        if (stack.isEmpty() || stack.isOf(Items.ELYTRA)) {
            return false;
        }
        EquippableComponent equippable = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }

    private int findElytraSlot(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 36; ++slot) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isOf(Items.ELYTRA) || stack.getDamage() >= stack.getMaxDamage() - 1) continue;
            return slot;
        }
        return -1;
    }

    private int findChestplateSlot(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 36; ++slot) {
            if (!ElytraHelperModule.isChestArmor(inventory.getStack(slot))) continue;
            return slot;
        }
        return -1;
    }

    private int findEmptySlot(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 36; ++slot) {
            if (!inventory.getStack(slot).isEmpty()) continue;
            return slot;
        }
        return -1;
    }

    private InventoryTask task(InventoryAction action, ClickPolicy policy) {
        return InventoryTask.builder().action(action).owner(OWNER).flag(TaskFlag.DEFAULT).policy(policy).priority(TaskPriority.NORMAL).build();
    }

    private boolean legitMode() {
        return "Legit".equals(this.mode.getSelectedOption());
    }

    private SwapTiming swapTiming() {
        String value = this.mode.getSelectedOption();
        if ("Legit".equals(value)) {
            return SwapTiming.LEGIT;
        }
        if ("FS".equals(value)) {
            return SwapTiming.FUNTIME;
        }
        return SwapTiming.DEFAULT;
    }

    public static boolean isEnabled2() {
        ElytraHelperModule module = instance;
        if (module == null || !module.requestStartGlide) {
            return false;
        }
        module.requestStartGlide = false;
        return true;
    }
}

