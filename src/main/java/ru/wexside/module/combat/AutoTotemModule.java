package ru.wexside.module.combat;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.TotemPopEvent;
import ru.wexside.misc.Bundles;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.ClickSlotAction;
import ru.wexside.misc.FriendList;
import ru.wexside.misc.Inventories;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.misc.VisibilityCondition;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.InventoryController;
import ru.wexside.util.entity.NpcDetector;

public class AutoTotemModule
extends Module
implements ConfigSerializable {
    private static final String OWNER = "auto_totem";
    private static final int OFFHAND_CONTAINER_SLOT = 45;
    private static final int TOTEM_POP_LOCK_TICKS = 20;
    private static final int SWAP_CONFIRM_TICKS = 10;
    private static volatile AutoTotemModule instance;
    private final BooleanSetting enabledSetting;
    private final MultiSelectSetting consider;
    private final NumberSetting health;
    private final NumberSetting maceHealth;
    private final BooleanSetting ignoreAutoswap;
    private final BooleanSetting swapBack;
    private final NumberSetting swapBackDelay;
    private final BooleanSetting ignoreWhenUsing;
    private final MultiSelectSetting ignoreOnlyItems;
    private final MultiSelectSetting ignoreExceptThreats;
    private final BooleanSetting fromBundle;
    private final BooleanSetting ftMode;
    private int swapBackTicks;
    private int pendingOffhandTicks;
    private int totemPopTicks;
    private ItemStack expectedOffhand = ItemStack.EMPTY;
    private ItemStack previousOffhand = ItemStack.EMPTY;

    public AutoTotemModule(EventBus eventBus) {
        super(eventBus, OWNER, "Auto Totem", "Берёт тотем в офхенд при опасности", ModuleCategory.valueOf("COMBAT"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        MultiSelectSetting considerSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Absorption", "Falling", "Crystals", "Anchor", "Mace").selectAll(false).optionListEnabled(false).name("Consider").id("consider").description("Факторы для активации")).build();
        considerSetting.setOptions(new String[0]);
        this.consider = considerSetting;
        this.registerSetting(considerSetting);
        this.health = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 20.0).defaultValue(4.0).multiplier(1.0).precision(0).animationSpeed(20.0f).snapTo(1.0).name("Health").id("health").description("Минимальный HP для активации")).build();
        this.registerSetting(this.health);
        this.maceHealth = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 20.0).defaultValue(10.0).multiplier(1.0).precision(0).animationSpeed(20.0f).snapTo(1.0).name("Mace HP").id("mace_health").description("Порог HP, ниже которого угроза Mace функционирует").visibleWhen(() -> this.consider.getSelectedOptions().contains("Mace"))).build();
        this.registerSetting(this.maceHealth);
        this.ignoreAutoswap = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Ignore AutoSwap").id("ignore_autoswap").description("Блокировка AutoSwap когда нужен тотем (HP-порог или угроза)")).build();
        this.registerSetting(this.ignoreAutoswap);
        this.swapBack = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Swap-Back").id("swap_back").description("Возврат предыдущего предмета из офхенда когда тотем не нужен")).build();
        this.registerSetting(this.swapBack);
        this.swapBackDelay = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 6.0).defaultValue(3.0).multiplier(1.0).precision(1).animationSpeed(20.0f).snapTo(0.5).name("Задержка возврата").id("swap_back_delay").description("Задержка перед возвратом предмета после того как тотем стал не нужен\nПомогает при активном PvP когда HP дёргается в районе порога").visibleWhen(this.swapBack::isEnabled)).build();
        this.registerSetting(this.swapBackDelay);
        this.ignoreWhenUsing = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Игнор при использовании").id("ignore_when_using").description("Не свапать если используется предмет")).build();
        this.registerSetting(this.ignoreWhenUsing);
        Supplier[] supplierArray = new Supplier[1];
        supplierArray[0] = this.ignoreWhenUsing::isEnabled;
        VisibilityCondition ignoreWhenUsingVisibility = VisibilityCondition.of("ignore_when_using", supplierArray);
        MultiSelectSetting ignoreOnlyItemsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("GApple", "Чарка", "Зелье Исцеления").selectAll(false).optionListEnabled(false).name("Только если:").id("ignore_only_items").description("Игнор только при исп. выбранных предметов, которые сейвят по HP-порогу. Если 0/3 - абсолютно любой предмет").visibility(ignoreWhenUsingVisibility)).build();
        ignoreOnlyItemsSetting.setOptions(new String[0]);
        this.ignoreOnlyItems = ignoreOnlyItemsSetting;
        this.registerSetting(ignoreOnlyItemsSetting);
        MultiSelectSetting ignoreExceptThreatsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Falling", "Crystals", "Anchor", "Mace").selectAll(false).optionListEnabled(false).name("За исключением").id("ignore_except_threats").description("Свап на тотем даже при игноре, если есть выбранная угроза").visibility(ignoreWhenUsingVisibility)).build();
        ignoreExceptThreatsSetting.setOptions(new String[0]);
        this.ignoreExceptThreats = ignoreExceptThreatsSetting;
        this.registerSetting(ignoreExceptThreatsSetting);
        this.fromBundle = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Из мешков").id("from_bundle").description("Доставать тотем из мешка если нет в инвентаре")).build();
        this.registerSetting(this.fromBundle);
        this.ftMode = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("FT-Mode").id("ft_mode").description("Поддержка мешков без лимита вместимости").visibleWhen(this.fromBundle::isEnabled)).build();
        this.registerSetting(this.ftMode);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
        this.listen(TotemPopEvent.class, this::onTotemPop);
    }

    public static boolean isActive() {
        AutoTotemModule module = instance;
        if (module == null || !module.enabledSetting.isEnabled() || !module.ignoreAutoswap.isEnabled()) {
            return false;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && module.needsTotem(player);
    }

    private void onTick() {
        boolean holdingTotem;
        boolean totemPopLock;
        if (!this.enabledSetting.isEnabled()) {
            this.reset();
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            this.reset();
            return;
        }
        this.tickPendingOffhand(player);
        if (this.totemPopTicks > 0) {
            --this.totemPopTicks;
        }
        if (this.shouldIgnoreWhileUsing(player)) {
            return;
        }
        InventoryController inventory = WexSideClient.getInventoryController();
        if (inventory == null) {
            return;
        }
        boolean needTotem = this.needsTotem(player);
        if (needTotem) {
            this.swapBackTicks = this.swapBackDelayTicks();
        } else if (this.swapBackTicks > 0) {
            --this.swapBackTicks;
        }
        boolean bl = totemPopLock = this.totemPopTicks > 0;
        if (totemPopLock) {
            this.swapBackTicks = 0;
        }
        if (!totemPopLock && this.isSwapPending()) {
            return;
        }
        PlayerInventory playerInventory = player.getInventory();
        ItemStack offhand = player.getOffHandStack();
        boolean bl2 = holdingTotem = offhand.getItem() == Items.TOTEM_OF_UNDYING;
        if (needTotem) {
            this.equipTotem(player, inventory, playerInventory, offhand, holdingTotem);
            return;
        }
        this.trySwapBack(player, inventory, playerInventory);
    }

    private void equipTotem(ClientPlayerEntity player, InventoryController inventory, PlayerInventory playerInventory, ItemStack offhand, boolean holdingTotem) {
        boolean offhandGlint;
        if (player.getItemCooldownManager().isCoolingDown(new ItemStack((ItemConvertible)Items.TOTEM_OF_UNDYING))) {
            return;
        }
        int totemSlot = this.findTotemSlot(playerInventory);
        if (totemSlot == -1) {
            int[] found;
            if (!holdingTotem && this.fromBundle.isEnabled() && !inventory.hasCurrentTask() && (found = Bundles.findInBundle(playerInventory, stack -> stack.isOf(Items.TOTEM_OF_UNDYING))) != null) {
                ItemStack previous = offhand.copy();
                ItemStack fromBundle = this.stackFromBundle(playerInventory, found);
                if (Bundles.useFromBundle(player, inventory, OWNER, found[0], found[1], 45, this.ftMode.isEnabled())) {
                    if (!previous.isEmpty()) {
                        this.previousOffhand = previous;
                    }
                    this.rememberSwap(fromBundle);
                }
            }
            return;
        }
        boolean foundGlint = playerInventory.getStack(totemSlot).hasGlint();
        boolean bl = offhandGlint = holdingTotem && offhand.hasGlint();
        if (!holdingTotem || offhandGlint && !foundGlint) {
            if (!offhand.isEmpty()) {
                this.previousOffhand = offhand.copy();
            }
            ItemStack totem = playerInventory.getStack(totemSlot).copy();
            this.swapToOffhand(inventory, Inventories.toContainerSlot(totemSlot));
            this.rememberSwap(totem);
        }
    }

    private void trySwapBack(ClientPlayerEntity player, InventoryController inventory, PlayerInventory playerInventory) {
        if (!this.swapBack.isEnabled() || this.previousOffhand.isEmpty() || this.swapBackTicks > 0) {
            return;
        }
        int slot = this.findMatchingSlot(playerInventory, this.previousOffhand);
        if (slot != -1) {
            ItemStack restored = playerInventory.getStack(slot).copy();
            this.swapToOffhand(inventory, Inventories.toContainerSlot(slot));
            this.rememberSwap(restored);
            this.previousOffhand = ItemStack.EMPTY;
            return;
        }
        if (!this.fromBundle.isEnabled()) {
            this.previousOffhand = ItemStack.EMPTY;
            return;
        }
        ItemStack previous = this.previousOffhand;
        int[] found = Bundles.findInBundle(playerInventory, stack -> ItemStack.areEqual((ItemStack)stack, (ItemStack)previous));
        if (found == null) {
            this.previousOffhand = ItemStack.EMPTY;
            return;
        }
        if (inventory.hasCurrentTask()) {
            return;
        }
        if (Bundles.useFromBundle(player, inventory, OWNER, found[0], found[1], 45, this.ftMode.isEnabled())) {
            this.rememberSwap(previous);
        }
        this.previousOffhand = ItemStack.EMPTY;
    }

    private void onTotemPop(TotemPopEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || event.getEntity() != player) {
            return;
        }
        this.totemPopTicks = 20;
        this.swapBackTicks = 0;
    }

    private void tickPendingOffhand(ClientPlayerEntity player) {
        if (this.pendingOffhandTicks <= 0) {
            return;
        }
        if (ItemStack.areEqual((ItemStack)player.getOffHandStack(), (ItemStack)this.expectedOffhand)) {
            this.pendingOffhandTicks = 0;
            this.expectedOffhand = ItemStack.EMPTY;
            return;
        }
        if (--this.pendingOffhandTicks <= 0) {
            this.expectedOffhand = ItemStack.EMPTY;
        }
    }

    private boolean shouldIgnoreWhileUsing(ClientPlayerEntity player) {
        if (!this.ignoreWhenUsing.isEnabled()) {
            return false;
        }
        if (!this.isUsingIgnoredItem(player)) {
            return false;
        }
        return !this.hasThreat(player, this.ignoreExceptThreats.getSelectedOptions());
    }

    private boolean isUsingIgnoredItem(ClientPlayerEntity player) {
        List<String> items = this.ignoreOnlyItems.getSelectedOptions();
        if (items.isEmpty()) {
            return MinecraftClient.getInstance().options.useKey.isPressed();
        }
        if (!player.isUsingItem()) {
            return false;
        }
        ItemStack active = player.getActiveItem();
        if (items.contains("GApple") && active.isOf(Items.GOLDEN_APPLE)) {
            return true;
        }
        if (items.contains("Чарка") && active.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            return true;
        }
        return items.contains("Зелье Исцеления") && this.isHealingPotion(active);
    }

    private boolean needsTotem(ClientPlayerEntity player) {
        if (this.hasConsideredThreat(player)) {
            return true;
        }
        float hp = player.getHealth();
        if (this.consider.getSelectedOptions().contains("Absorption")) {
            hp += player.hasStatusEffect(StatusEffects.ABSORPTION) ? player.getAbsorptionAmount() : 0.0f;
        }
        return hp <= this.health.getFloatValue();
    }

    private boolean hasConsideredThreat(ClientPlayerEntity player) {
        return this.hasThreat(player, this.consider.getSelectedOptions());
    }

    private boolean hasThreat(ClientPlayerEntity player, List<String> threats) {
        if (threats.contains("Crystals") && this.isNearExplosive(player)) {
            return true;
        }
        if (threats.contains("Anchor") && this.isNearCobweb(player)) {
            return true;
        }
        if (threats.contains("Falling") && this.isFallingFar(player)) {
            return true;
        }
        return threats.contains("Mace") && this.isMaceThreat(player);
    }

    private boolean isNearExplosive(ClientPlayerEntity player) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return false;
        }
        for (Entity entity : world.getEntities()) {
            if (!this.isExplosive(entity) || !((double)player.distanceTo(entity) <= 6.0)) continue;
            return true;
        }
        return false;
    }

    private boolean isExplosive(Entity entity) {
        return entity instanceof EndCrystalEntity || entity instanceof CreeperEntity || entity instanceof TntEntity;
    }

    private boolean isNearCobweb(ClientPlayerEntity player) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return false;
        }
        BlockPos origin = player.getBlockPos();
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int x = -6; x <= 6; ++x) {
            for (int y = -6; y <= 6; ++y) {
                for (int z = -6; z <= 6; ++z) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!world.getBlockState((BlockPos)cursor).isOf(Blocks.COBWEB)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isFallingFar(ClientPlayerEntity player) {
        if (player.isTouchingWater()) {
            return false;
        }
        if (player.isClimbing()) {
            return false;
        }
        return player.fallDistance > 10.0;
    }

    private boolean isMaceThreat(ClientPlayerEntity player) {
        if (player.getHealth() > this.maceHealth.getFloatValue()) {
            return false;
        }
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return false;
        }
        for (PlayerEntity other : world.getPlayers()) {
            if (!this.isMaceAttacker(player, other)) continue;
            return true;
        }
        return false;
    }

    private boolean isMaceAttacker(ClientPlayerEntity player, PlayerEntity other) {
        double dz;
        if (other == player || !other.isAlive()) {
            return false;
        }
        if (!other.getMainHandStack().isOf(Items.MACE)) {
            return false;
        }
        FriendList friends = WexSideClient.getFriends();
        if (friends != null && friends.contains(other.getName().getString())) {
            return false;
        }
        NpcDetector npcDetector = WexSideClient.getNpcDetector();
        if (npcDetector != null && npcDetector.isNpc((LivingEntity)other)) {
            return false;
        }
        if (other.isOnGround()) {
            return false;
        }
        if (other.getY() - other.lastY > -0.15) {
            return false;
        }
        if (other.getY() - player.getY() < 2.0) {
            return false;
        }
        double dx = other.getX() - player.getX();
        return dx * dx + (dz = other.getZ() - player.getZ()) * dz <= 25.0;
    }

    private int findTotemSlot(PlayerInventory inventory) {
        int enchanted = -1;
        for (int slot = 0; slot < 36; ++slot) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.getItem() != Items.TOTEM_OF_UNDYING) continue;
            if (!stack.hasGlint()) {
                return slot;
            }
            enchanted = slot;
        }
        return enchanted;
    }

    private ItemStack stackFromBundle(PlayerInventory inventory, int[] found) {
        BundleContentsComponent contents = (BundleContentsComponent)inventory.getStack(found[0]).get(DataComponentTypes.BUNDLE_CONTENTS);
        return contents == null ? ItemStack.EMPTY : contents.get(found[1]).copy();
    }

    private int findMatchingSlot(PlayerInventory inventory, ItemStack target) {
        for (int slot = 0; slot < 36; ++slot) {
            if (!ItemStack.areEqual((ItemStack)inventory.getStack(slot), (ItemStack)target)) continue;
            return slot;
        }
        return -1;
    }

    private boolean isHealingPotion(ItemStack stack) {
        if (!stack.isOf(Items.POTION)) {
            return false;
        }
        PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (StatusEffectInstance effect : contents.getEffects()) {
            if (!effect.getEffectType().equals((Object)StatusEffects.INSTANT_HEALTH)) continue;
            return true;
        }
        return false;
    }

    private void swapToOffhand(InventoryController inventory, int containerSlot) {
        inventory.submit(InventoryTask.builder().action(new ClickSlotAction(containerSlot, 40)).owner(OWNER).flag(TaskFlag.DEFAULT).policy(ClickPolicy.VISIBLE).priority(TaskPriority.NORMAL).build());
    }

    private void rememberSwap(ItemStack stack) {
        this.expectedOffhand = stack == null ? ItemStack.EMPTY : stack.copy();
        this.pendingOffhandTicks = 10;
        this.totemPopTicks = 0;
    }

    private int swapBackDelayTicks() {
        return Math.round(this.swapBackDelay.getFloatValue() * 20.0f);
    }

    private boolean isSwapPending() {
        return this.pendingOffhandTicks > 0;
    }

    private void reset() {
        this.swapBackTicks = 0;
        this.pendingOffhandTicks = 0;
        this.totemPopTicks = 0;
        this.expectedOffhand = ItemStack.EMPTY;
    }
}

