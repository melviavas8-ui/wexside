package ru.wexside.module.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import ru.wexside.misc.SwapTiming;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.misc.UseItemAction;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.BindSettingBuilder;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.util.Angle;
import ru.wexside.util.InventoryController;
import ru.wexside.util.RotationController;
import ru.wexside.util.RotationIntent;

public class ClickPearlModule
extends Module
implements ConfigSerializable {
    private static final String OWNER = "click_pearl";
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private final ModeSetting mode;
    private final BooleanSetting fromBundle;
    private final BooleanSetting ftMode;
    private final BindSetting key;
    private Angle heldAngle;
    private int holdTicks;
    private boolean pendingThrow;

    public ClickPearlModule(EventBus eventBus) {
        super(eventBus, OWNER, "Click Pearl", "Бросок эндер-перла биндом", ModuleCategory.valueOf("COMBAT"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.mode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Default", "Legit", "FS").defaultOption("Default").name("Mode").id("mode").description("Режим бросания")).build();
        this.registerSetting(this.mode);
        this.fromBundle = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Из мешков").id("from_bundle").description("Доставать перл из мешка если нет в инвентаре")).build();
        this.registerSetting(this.fromBundle);
        this.ftMode = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("FT-Mode").id("ft_mode").description("Поддержка мешков без лимита вместимости").visibleWhen(this.fromBundle::isEnabled)).build();
        this.registerSetting(this.ftMode);
        this.key = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(this::onKeyPress).name("Key").id("key").description("Кнопка броска")).build();
        this.registerSetting(this.key);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        if (!this.enabledSetting.isEnabled()) {
            if (this.holdTicks > 0 || this.pendingThrow) {
                this.reset();
            }
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            this.reset();
            return;
        }
        if (this.holdTicks > 0) {
            --this.holdTicks;
            if (this.heldAngle != null) {
                this.applyLook(player, this.heldAngle);
            }
            if (this.holdTicks <= 0) {
                this.releaseLook();
            }
        }
        if (this.pendingThrow && this.lookReady()) {
            this.throwPearl(player);
            this.pendingThrow = false;
        }
    }

    private void onKeyPress(BindSetting ignored) {
        this.queueThrow();
    }

    private void queueThrow() {
        if (!this.enabledSetting.isEnabled() || this.pendingThrow) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack pearl = new ItemStack((ItemConvertible)Items.ENDER_PEARL);
        boolean offhand = player.getOffHandStack().isOf(Items.ENDER_PEARL);
        int slot = offhand ? -1 : Inventories.findSlot(Items.ENDER_PEARL);
        boolean inBundle = this.fromBundle.isEnabled() && Bundles.contains(player.getInventory(), stack -> stack.isOf(Items.ENDER_PEARL));
        boolean bl = inBundle;
        if (!offhand && slot == -1 && !inBundle) {
            ItemAlerts.warnMissing(pearl, pearl.getName().getString());
            return;
        }
        if (ItemAlerts.isBusy(null, pearl, pearl.getName().getString())) {
            return;
        }
        RotationController rotations = WexSideClient.getRotationController();
        Angle look = rotations != null ? rotations.getAngle() : null;
        Angle angle = look;
        if (look != null) {
            this.heldAngle = look;
            this.holdTicks = this.legitMode() ? 14 : 6;
            this.applyLook(player, look);
        }
        this.pendingThrow = true;
    }

    private void throwPearl(ClientPlayerEntity player) {
        if (player.getItemCooldownManager().isCoolingDown(new ItemStack((ItemConvertible)Items.ENDER_PEARL))) {
            return;
        }
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null) {
            return;
        }
        if (player.getOffHandStack().isOf(Items.ENDER_PEARL)) {
            inventory.submit(this.task(new UseItemAction(), ClickPolicy.SILENT));
            return;
        }
        int slot = Inventories.findSlot(Items.ENDER_PEARL);
        if (slot == -1) {
            int[] found;
            if (this.fromBundle.isEnabled() && (found = Bundles.findInBundle(player.getInventory(), stack -> stack.isOf(Items.ENDER_PEARL))) != null) {
                Bundles.useFromBundle(player, inventory, OWNER, found[0], found[1], this.ftMode.isEnabled(), inventory::pressUseForTick);
            }
            return;
        }
        if (slot < 9) {
            inventory.submit(this.task(new HotbarSelectAction(slot, true), ClickPolicy.SILENT));
            return;
        }
        this.swapFromInventory(player, slot, inventory);
    }

    private void swapFromInventory(ClientPlayerEntity player, int slot, InventoryController inventory) {
        int selected = player.getInventory().getSelectedSlot();
        int containerSlot = slot < 9 ? slot + 36 : slot;
        inventory.submit(this.task(inventory.createSwapUseRestore(containerSlot, selected, inventory::pressUseForTick, this.swapTiming()), ClickPolicy.VISIBLE));
    }

    private InventoryTask task(InventoryAction action, ClickPolicy policy) {
        return InventoryTask.builder().action(action).owner(OWNER).flag(TaskFlag.DEFAULT).policy(policy).priority(TaskPriority.NORMAL).build();
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
        this.heldAngle = null;
    }

    private boolean lookReady() {
        if (this.heldAngle == null) {
            return true;
        }
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations == null) {
            return true;
        }
        Angle current = rotations.getAngle();
        return current == null || current.angularDistanceTo(this.heldAngle) <= 10.0f;
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

    private void reset() {
        if (this.heldAngle != null) {
            this.releaseLook();
        }
        this.heldAngle = null;
        this.holdTicks = 0;
        this.pendingThrow = false;
    }
}

