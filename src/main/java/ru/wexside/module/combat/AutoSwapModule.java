package ru.wexside.module.combat;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.KeyPressedEvent;
import ru.wexside.event.MousePressedEvent;
import ru.wexside.misc.Bundles;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.ClickSlotAction;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.ItemAlerts;
import ru.wexside.misc.SwapIcon;
import ru.wexside.misc.SwapWheelScreen;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.misc.VisibilityCondition;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.module.combat.AutoTotemModule;
import ru.wexside.notification.ItemNotification;
import ru.wexside.notification.NotificationCenter;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.BindSettingBuilder;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.InventoryController;

public class AutoSwapModule
extends Module
implements ConfigSerializable {
    private static final String OWNER = "auto_swap";
    private static final String BUKKIT_VALUES = "PublicBukkitValues";
    private static final String SESSION_KEY = "minecraft:s";
    private static final String DON_ITEM = "minecraft:don-item";
    private static final int OFFHAND_SLOT = 40;
    private static final int HOLD_TICKS_FOR_WHEEL = 4;
    private static final String SHORT_PRESS_WHEEL = "Круговой селектор";
    private final BooleanSetting enabledSetting;
    private final ModeSetting mode;
    private final ModeSetting serverMode;
    private final BooleanSetting separateBinds;
    private final BindSetting actionBind;
    private final BindSetting swapBind;
    private final BindSetting selectorBind;
    private final ModeSetting swapFrom;
    private final ModeSetting swapTo;
    private final ModeSetting shortPressMode;
    private final NumberSetting segments;
    private final BooleanSetting fromBundle;
    private final BooleanSetting ftMode;
    private String lastSwapFrom;
    private String lastSwapTo;
    private ItemStack lastWheelStack = ItemStack.EMPTY;
    private ItemStack previousWheelStack = ItemStack.EMPTY;
    private int holdTicks;
    private boolean actionHeld;
    private boolean wheelOpened;

    public AutoSwapModule(EventBus eventBus) {
        super(eventBus, OWNER, "Auto Swap", "Меняет предметы при прожатии", ModuleCategory.valueOf("COMBAT"), "autoswap", "swap");
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.mode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Default", "Legit").defaultOption("Default").name("Mode").id("mode").description("Default - мгновенный, Legit - с задержкой")).build();
        this.registerSetting(this.mode);
        this.serverMode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("FT", "Others").defaultOption("FT").name("Server Mode").id("server_mode").description("Режим сервера")).build();
        this.registerSetting(this.serverMode);
        this.separateBinds = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Раздельные бинды").id("separate_binds").description("Отдельные кнопки для свапа и селектора")).build();
        this.registerSetting(this.separateBinds);
        this.actionBind = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).name("Action Button").id("action_bind").description("Нажать - свап, удерживать - селектор").visibleWhen(() -> !this.separateBinds.isEnabled())).build();
        this.registerSetting(this.actionBind);
        Supplier[] supplierArray = new Supplier[1];
        supplierArray[0] = this.separateBinds::isEnabled;
        VisibilityCondition separateBindVisibility = VisibilityCondition.of("separate_binds", supplierArray);
        this.swapBind = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(this::onSwapBind).name("Свап").id("swap_bind").description("Замена предмета в оффхенде").visibility(separateBindVisibility)).build();
        this.registerSetting(this.swapBind);
        this.selectorBind = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(this::onSelectorBind).name("Селектор").id("selector_bind").description("Круговой селектор предметов").visibility(separateBindVisibility)).build();
        this.registerSetting(this.selectorBind);
        this.swapFrom = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Shield", "Sphere", "Totem", "GApple", "Firework").defaultOption("Shield").name("Swap From").id("swap_from").description("Предмет, который будет заменён")).build();
        this.registerSetting(this.swapFrom);
        this.swapTo = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Shield", "Sphere", "Totem", "GApple", "Firework").defaultOption("Totem").name("Swap To").id("swap_to").description("Предмет, на который будет замена")).build();
        this.registerSetting(this.swapTo);
        this.shortPressMode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Настроенные", SHORT_PRESS_WHEEL).defaultOption("Настроенные").dynamicName(() -> this.separateBinds.isEnabled() ? "Что менять свапом" : "Переключать коротким нажатием").id("short_press_mode").description("Настроенные - пара из селектбоксов выше\nКруговой селектор - предметы колеса")).build();
        this.registerSetting(this.shortPressMode);
        this.segments = ((NumberSettingBuilder)NumberSetting.builder().range(2.0, 9.0).defaultValue(3.0).multiplier(1.0).precision(0).animationSpeed(20.0f).snapTo(1.0).name("Segments").id("segments").description("Количество сегментов селектора")).build();
        this.registerSetting(this.segments);
        this.fromBundle = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Из мешков").id("from_bundle").description("Доставать предмет из мешка если нет в инвентаре")).build();
        this.registerSetting(this.fromBundle);
        this.ftMode = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("FT-Mode").id("ft_mode").description("Поддержка мешков без лимита вместимости").visibleWhen(this.fromBundle::isEnabled)).build();
        this.registerSetting(this.ftMode);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
        this.listen(KeyPressedEvent.class, event -> this.onKey(event.key()));
        this.listen(MousePressedEvent.class, event -> this.onMouse(event.button()));
    }

    @Override
    public void readConfig(DataInputStream dataInputStream) throws IOException {
        super.readConfig(dataInputStream);
        if (this.swapBind.getBindInput().isUnbound() && this.selectorBind.getBindInput().isUnbound()) {
            this.swapBind.setBindInput(this.actionBind.getBindInput());
        }
    }

    private void onTick() {
        this.resetHistoryIfPairChanged();
        if (!this.enabledSetting.isEnabled() || this.separateBinds.isEnabled()) {
            this.resetHoldState();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null && !(client.currentScreen instanceof SwapWheelScreen)) {
            this.resetHoldState();
            return;
        }
        boolean held = this.actionBind.isPressed();
        if (held) {
            if (!this.actionHeld) {
                this.holdTicks = 0;
                this.wheelOpened = false;
            }
            ++this.holdTicks;
            if (!this.wheelOpened && this.holdTicks >= 4 && client.currentScreen == null && client.player != null) {
                client.setScreen((Screen)this.openWheel(this.actionBind));
                this.wheelOpened = true;
            }
        } else {
            if (this.actionHeld && !this.wheelOpened && this.holdTicks < 4) {
                this.swapOffhand();
            }
            this.holdTicks = 0;
        }
        this.actionHeld = held;
    }

    private void onSwapBind(BindSetting ignored) {
        this.swapFromSeparateBind();
    }

    private void onSelectorBind(BindSetting ignored) {
        this.openSelector();
    }

    public void forgetStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (AutoSwapModule.sameItem(this.lastWheelStack, stack)) {
            this.lastWheelStack = ItemStack.EMPTY;
        }
        if (AutoSwapModule.sameItem(this.previousWheelStack, stack)) {
            this.previousWheelStack = ItemStack.EMPTY;
        }
    }

    public void selectStack(ItemStack stack) {
        if (!this.enabledSetting.isEnabled() || stack == null || stack.isEmpty()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack offhand = player.getOffHandStack();
        if (AutoSwapModule.sameItem(offhand, stack)) {
            return;
        }
        int slot = this.findInventorySlot(player.getInventory(), stack);
        if (slot == -1) {
            if (this.useFromBundle(player, stack)) {
                this.showHover(stack);
                this.rememberWheelStack(stack);
            } else {
                ItemAlerts.warnMissing(stack, stack.getName().getString());
            }
            return;
        }
        if (!this.swapToOffhand(slot)) {
            return;
        }
        this.showHover(stack);
        this.rememberWheelStack(stack);
    }

    public SwapIcon iconFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return SwapIcon.AVAILABLE;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return SwapIcon.AVAILABLE;
        }
        if (AutoSwapModule.sameItem(player.getOffHandStack(), stack)) {
            return SwapIcon.AVAILABLE;
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 36; ++slot) {
            if (!AutoSwapModule.sameItem(inventory.getStack(slot), stack)) continue;
            return SwapIcon.AVAILABLE;
        }
        if (this.fromBundle.isEnabled() && Bundles.contains(inventory, candidate -> AutoSwapModule.sameItem(candidate, stack))) {
            return SwapIcon.IN_BUNDLE;
        }
        return SwapIcon.MISSING;
    }

    private ItemStack counterpart(ClientPlayerEntity player, ItemStack offhand) {
        String from = this.swapFrom.getSelectedOption();
        String to = this.swapTo.getSelectedOption();
        String wanted = this.predicate(from).test(offhand) ? to : from;
        return this.findByName(player.getInventory(), wanted);
    }

    private SwapWheelScreen openWheel(BindSetting bind) {
        return new SwapWheelScreen(bind, this.activeBinds(), this.segments.getIntValue(), this::selectStack, this::forgetStack, this::iconFor);
    }

    private static NbtCompound stripSessionKeys(NbtCompound nbt) {
        NbtCompound copy = nbt.copy();
        NbtCompound bukkit = copy.getCompound(BUKKIT_VALUES).orElse(null);
        if (bukkit != null) {
            bukkit.remove(SESSION_KEY);
            copy.put(BUKKIT_VALUES, (NbtElement)bukkit);
        }
        return copy;
    }

    private static boolean sameCustomData(NbtCompound left, NbtCompound right) {
        if (Objects.equals(left, right)) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(AutoSwapModule.stripSessionKeys(left), AutoSwapModule.stripSessionKeys(right));
    }

    private static boolean sameItem(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }
        if (left.getItem() != right.getItem()) {
            return false;
        }
        if (!Objects.equals(left.get(DataComponentTypes.CUSTOM_NAME), right.get(DataComponentTypes.CUSTOM_NAME))) {
            return false;
        }
        if (!AutoSwapModule.sameCustomData(AutoSwapModule.customData(left), AutoSwapModule.customData(right))) {
            return false;
        }
        return Objects.equals(left.get(DataComponentTypes.LORE), right.get(DataComponentTypes.LORE));
    }

    private static NbtCompound customData(ItemStack stack) {
        NbtComponent component = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
        return component == null ? null : component.copyNbt();
    }

    private void swapFromSeparateBind() {
        if (!this.enabledSetting.isEnabled() || !this.separateBinds.isEnabled()) {
            return;
        }
        if (MinecraftClient.getInstance().currentScreen != null) {
            return;
        }
        this.swapOffhand();
    }

    private List<BindSetting> activeBinds() {
        return this.separateBinds.isEnabled() ? List.of(this.swapBind, this.selectorBind) : List.of(this.actionBind);
    }

    private boolean swapToOffhand(int slot) {
        if (AutoTotemModule.isActive()) {
            return false;
        }
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null) {
            return false;
        }
        ClickPolicy policy = "Legit".equalsIgnoreCase(this.mode.getSelectedOption()) ? ClickPolicy.SWAP : ClickPolicy.VISIBLE;
        inventory.submit(InventoryTask.builder().action(new ClickSlotAction(slot, 40)).owner(OWNER).flag(TaskFlag.DEFAULT).policy(policy).priority(TaskPriority.NORMAL).build());
        return true;
    }

    private ItemStack nextWheelStack(ItemStack offhand) {
        ItemStack previous;
        List<ItemStack> items = SwapWheelScreen.items();
        if (items.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (items.size() == 1) {
            return items.get(0).copy();
        }
        if (items.size() == 2) {
            ItemStack first = items.get(0);
            ItemStack second = items.get(1);
            return AutoSwapModule.sameItem(offhand, first) ? second.copy() : first.copy();
        }
        ItemStack last = this.listed(items, this.lastWheelStack) ? this.lastWheelStack : ItemStack.EMPTY;
        ItemStack class_17992 = previous = this.listed(items, this.previousWheelStack) ? this.previousWheelStack : ItemStack.EMPTY;
        if (last.isEmpty() && previous.isEmpty()) {
            return items.get(0).copy();
        }
        if (last.isEmpty()) {
            return previous.copy();
        }
        if (previous.isEmpty()) {
            return last.copy();
        }
        if (AutoSwapModule.sameItem(offhand, last)) {
            return previous.copy();
        }
        if (AutoSwapModule.sameItem(offhand, previous)) {
            return last.copy();
        }
        return last.copy();
    }

    private void onKey(int code) {
        if (!this.enabledSetting.isEnabled() || MinecraftClient.getInstance().currentScreen != null) {
            return;
        }
        for (BindSetting bind : this.activeBinds()) {
            if (!bind.getBindInput().matchesKeyboard(code)) continue;
            return;
        }
        ItemStack stack = SwapWheelScreen.stackAt(code);
        if (!stack.isEmpty()) {
            this.selectStack(stack);
        }
    }

    private void swapOffhand() {
        ItemStack wanted;
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack offhand = player.getOffHandStack();
        boolean fromWheel = SHORT_PRESS_WHEEL.equals(this.shortPressMode.getSelectedOption());
        ItemStack class_17992 = wanted = fromWheel ? this.nextWheelStack(offhand) : this.counterpart(player, offhand);
        if (wanted.isEmpty() || AutoSwapModule.sameItem(offhand, wanted)) {
            return;
        }
        int slot = this.findInventorySlot(player.getInventory(), wanted);
        if (slot == -1) {
            if (this.useFromBundle(player, wanted)) {
                this.showHover(wanted);
                if (fromWheel) {
                    this.rememberWheelStack(wanted);
                }
            }
            return;
        }
        if (!this.swapToOffhand(slot)) {
            return;
        }
        this.showHover(wanted);
        if (fromWheel) {
            this.rememberWheelStack(wanted);
        }
    }

    private int findInventorySlot(PlayerInventory inventory, ItemStack stack) {
        for (int slot = 0; slot < 36; ++slot) {
            if (!AutoSwapModule.sameItem(inventory.getStack(slot), stack)) continue;
            return slot < 9 ? slot + 36 : slot;
        }
        return -1;
    }

    private void resetHistoryIfPairChanged() {
        String from = this.swapFrom.getSelectedOption();
        String to = this.swapTo.getSelectedOption();
        if (Objects.equals(from, this.lastSwapFrom) && Objects.equals(to, this.lastSwapTo)) {
            return;
        }
        this.lastSwapFrom = from;
        this.lastSwapTo = to;
        this.lastWheelStack = ItemStack.EMPTY;
        this.previousWheelStack = ItemStack.EMPTY;
    }

    private ItemStack findMatching(PlayerInventory inventory, Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < 36; ++slot) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty() || !predicate.test(stack)) continue;
            return stack.copy();
        }
        if (!this.fromBundle.isEnabled()) {
            return ItemStack.EMPTY;
        }
        int[] found = Bundles.findInBundle(inventory, predicate);
        if (found == null) {
            return ItemStack.EMPTY;
        }
        BundleContentsComponent contents = (BundleContentsComponent)inventory.getStack(found[0]).get(DataComponentTypes.BUNDLE_CONTENTS);
        return contents == null ? ItemStack.EMPTY : contents.get(found[1]).copy();
    }

    private Predicate<ItemStack> predicate(String name) {
        if (name == null) {
            return stack -> false;
        }
        return switch (name) {
            case "Shield" -> stack -> stack.isOf(Items.SHIELD);
            case "Sphere" -> this::isSphere;
            case "Totem" -> stack -> stack.isOf(Items.TOTEM_OF_UNDYING);
            case "GApple" -> stack -> stack.isOf(Items.GOLDEN_APPLE);
            case "Firework" -> stack -> stack.isOf(Items.FIREWORK_ROCKET);
            default -> stack -> false;
        };
    }

    private boolean hasDonItem(ItemStack stack) {
        NbtCompound nbt = AutoSwapModule.customData(stack);
        if (nbt == null) {
            return false;
        }
        NbtCompound bukkit = nbt.copy().getCompound(BUKKIT_VALUES).orElse(null);
        return bukkit != null && bukkit.contains(DON_ITEM);
    }

    private boolean useFromBundle(ClientPlayerEntity player, ItemStack stack) {
        if (!this.fromBundle.isEnabled() || AutoTotemModule.isActive()) {
            return false;
        }
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null || inventory.hasCurrentTask()) {
            return false;
        }
        int[] found = Bundles.findInBundle(player.getInventory(), candidate -> AutoSwapModule.sameItem(candidate, stack));
        if (found == null) {
            return false;
        }
        return Bundles.useFromBundle(player, inventory, OWNER, found[0], found[1], 45, this.ftMode.isEnabled());
    }

    private void rememberWheelStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack copy = stack.copy();
        if (AutoSwapModule.sameItem(copy, this.lastWheelStack)) {
            return;
        }
        if (AutoSwapModule.sameItem(copy, this.previousWheelStack)) {
            ItemStack swap = this.lastWheelStack;
            this.lastWheelStack = copy;
            this.previousWheelStack = swap;
            return;
        }
        this.previousWheelStack = this.lastWheelStack;
        this.lastWheelStack = copy;
    }

    private boolean listed(List<ItemStack> items, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (ItemStack candidate : items) {
            if (!AutoSwapModule.sameItem(candidate, stack)) continue;
            return true;
        }
        return false;
    }

    private void showHover(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NotificationCenter overlays = WexSideClient.getNotificationCenter();
        if (overlays != null) {
            overlays.push(new ItemNotification(stack));
        }
    }

    private void onMouse(int button) {
        if (!this.enabledSetting.isEnabled() || MinecraftClient.getInstance().currentScreen != null) {
            return;
        }
        for (BindSetting bind : this.activeBinds()) {
            if (!bind.getBindInput().matchesMouse(button)) continue;
            return;
        }
        ItemStack stack = SwapWheelScreen.stackAt(SwapWheelScreen.indexAt(button));
        if (!stack.isEmpty()) {
            this.selectStack(stack);
        }
    }

    private void openSelector() {
        if (!this.enabledSetting.isEnabled() || !this.separateBinds.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) {
            return;
        }
        client.setScreen((Screen)this.openWheel(this.selectorBind));
    }

    private ItemStack findByName(PlayerInventory inventory, String name) {
        ItemStack enchanted;
        if ("Totem".equals(name) && !(enchanted = this.findMatching(inventory, stack -> stack.isOf(Items.TOTEM_OF_UNDYING) && stack.hasGlint())).isEmpty()) {
            return enchanted;
        }
        return this.findMatching(inventory, this.predicate(name));
    }

    private boolean isSphere(ItemStack stack) {
        if (!stack.isOf(Items.PLAYER_HEAD)) {
            return false;
        }
        if (!"FT".equals(this.serverMode.getSelectedOption())) {
            return true;
        }
        return this.hasDonItem(stack);
    }

    private void resetHoldState() {
        this.actionHeld = false;
        this.holdTicks = 0;
        this.wheelOpened = false;
    }
}

