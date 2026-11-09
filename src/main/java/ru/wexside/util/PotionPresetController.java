package ru.wexside.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import ru.wexside.WexSideClient;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.input.BindInput;
import ru.wexside.misc.AttackUrgency;
import ru.wexside.misc.Bundles;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.CorrectionMode;
import ru.wexside.misc.HotbarSelectAction;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.ItemAlerts;
import ru.wexside.misc.ItemUseCooldownAccessor;
import ru.wexside.misc.KeybindRegistry;
import ru.wexside.misc.PotionCatalog;
import ru.wexside.misc.PotionCatalogEntry;
import ru.wexside.misc.PotionPreset;
import ru.wexside.misc.PotionPresetDraft;
import ru.wexside.misc.PotionPresetKeybind;
import ru.wexside.misc.PotionPresetStore;
import ru.wexside.misc.ServerKind;
import ru.wexside.misc.SwapTiming;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.misc.UseItemAction;
import ru.wexside.module.misc.PotionCombinerModule;
import ru.wexside.util.Angle;
import ru.wexside.util.InventoryController;
import ru.wexside.util.RotationController;
import ru.wexside.util.RotationIntent;

public final class PotionPresetController {
    private static final int MAX_FAVORITES = 5;
    private static final int ROTATION_HOLD_TICKS = 2;
    private static final float ROTATION_RANDOMIZATION_RANGE = 4.0f;
    private static final int USE_COOLDOWN_TICKS = 1;
    private static final float ROTATION_TOLERANCE = 3.0f;
    private static final String TASK_OWNER = "potion-combiner";
    private static final float DOWNWARD_PITCH = 90.0f;
    private static final int INVENTORY_SLOT_COUNT_WITH_OFFHAND = 37;
    private static final int MAX_ROTATION_ATTEMPTS = 8;
    private static final String ROTATION_OWNER = "potion-combiner-rotation";
    private final KeybindRegistry keybindRegistry;
    private final PotionPresetStore potionPresetStore;
    private int useCooldownTicks;
    private float targetPitch = 90.0f;
    private int rotationHoldTicks;
    private int rotationAttemptCount;
    private final Map<String, ItemStack> availableStacksById;
    private int rotationRandomizationStep;
    private final List<PotionCatalogEntry> pendingPotions;
    private Angle downwardAngle;
    private final Map<PotionPresetDraft, PotionPresetKeybind> keybindsByPreset;
    private int revision;
    private final List<PotionPresetDraft> presets = new ArrayList<PotionPresetDraft>();

    public PotionPresetController(EventBus eventBus, KeybindRegistry keybindRegistry, PotionPresetStore potionPresetStore) {
        this.keybindsByPreset = new IdentityHashMap<PotionPresetDraft, PotionPresetKeybind>();
        this.pendingPotions = new ArrayList<PotionCatalogEntry>();
        this.availableStacksById = new HashMap<String, ItemStack>();
        this.keybindRegistry = keybindRegistry;
        this.potionPresetStore = potionPresetStore;
        eventBus.subscribe(ClientTickEvent.class, event -> this.tick());
        this.loadPresets();
    }

    public void refreshInventoryIndex() {
        this.availableStacksById.clear();
        PlayerInventory inventory = this.getInventory();
        if (inventory == null) {
            return;
        }
        ServerKind serverKind = PotionCombinerModule.getServerKind();
        List<PotionCatalogEntry> catalog = PotionCatalog.inventoryEntries();
        boolean searchBundles = PotionCombinerModule.isActive();
        for (int slot = 0; slot < 37; ++slot) {
            ItemStack stack = slot == 36 ? inventory.getStack(40) : inventory.getStack(slot);
            this.indexStack(stack, catalog, serverKind);
            BundleContentsComponent bundle = (BundleContentsComponent)stack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (!searchBundles || bundle == null) continue;
            for (int itemIndex = 0; itemIndex < bundle.size(); ++itemIndex) {
                this.indexStack(bundle.get(itemIndex), catalog, serverKind);
            }
        }
    }

    public List<PotionPresetDraft> getPresets() {
        return this.presets;
    }

    public PotionPresetDraft createPreset(String name) {
        PotionPresetDraft preset = new PotionPresetDraft(name);
        this.presets.add(preset);
        this.registerKeybind(preset);
        ++this.revision;
        this.savePresets();
        return preset;
    }

    public List<PotionCatalogEntry> getCatalogSortedByAvailability() {
        ArrayList<PotionCatalogEntry> available = new ArrayList<PotionCatalogEntry>();
        ArrayList<PotionCatalogEntry> unavailable = new ArrayList<PotionCatalogEntry>();
        for (PotionCatalogEntry entry : PotionCatalog.allEntries()) {
            if (this.availableStacksById.containsKey(entry.getId())) {
                available.add(entry);
                continue;
            }
            unavailable.add(entry);
        }
        available.addAll(unavailable);
        return available;
    }

    private void loadPresets() {
        this.presets.clear();
        for (PotionPresetKeybind keybind : this.keybindsByPreset.values()) {
            this.keybindRegistry.unregister(keybind);
        }
        this.keybindsByPreset.clear();
        for (PotionPreset storedPreset : this.potionPresetStore.getList()) {
            PotionPresetDraft preset = new PotionPresetDraft(storedPreset.name());
            preset.setBindInput(BindInput.fromLegacyCode(storedPreset.keyCode()));
            preset.setFavorite(storedPreset.favorite());
            List<String> potionIds = storedPreset.potionIds();
            for (int slot = 0; slot < 4 && slot < potionIds.size(); ++slot) {
                String potionId = potionIds.get(slot);
                preset.setPotionId(slot, potionId == null || potionId.isEmpty() ? null : potionId);
            }
            this.presets.add(preset);
            this.registerKeybind(preset);
        }
        ++this.revision;
    }

    private void tick() {
        this.tickRotationHold();
        this.processPendingPotions();
    }

    public ItemStack resolveStack(PotionCatalogEntry entry) {
        if (entry == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.availableStacksById.get(entry.getId());
        return stack == null ? entry.createDefaultStack() : stack;
    }

    public void savePresets() {
        ArrayList<PotionPreset> storedPresets = new ArrayList<PotionPreset>();
        for (PotionPresetDraft preset : this.presets) {
            ArrayList<String> potionIds = new ArrayList<String>();
            for (int slot = 0; slot < 4; ++slot) {
                String potionId = preset.getPotionId(slot);
                potionIds.add(potionId == null ? "" : potionId);
            }
            storedPresets.add(new PotionPreset(preset.getName(), preset.getBindInput().toLegacyCode(), preset.isFavorite(), potionIds));
        }
        this.potionPresetStore.setList(storedPresets);
        try {
            this.potionPresetStore.save();
        }
        catch (IOException exception) {
            WexSideClient.getInstance().getLogger().error("Failed to save potion presets", (Throwable)exception);
        }
    }

    private void indexStack(ItemStack stack, List<PotionCatalogEntry> catalog, ServerKind serverKind) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents != null) {
            contents.potion().map(entry -> Registries.POTION.getId(entry.value())).ifPresent(id -> this.availableStacksById.putIfAbsent(id.toString(), stack));
        }
        for (PotionCatalogEntry entry2 : catalog) {
            if (!entry2.matches(stack, serverKind)) continue;
            this.availableStacksById.putIfAbsent(entry2.getId(), stack);
        }
    }

    public List<PotionPresetDraft> getFavoritePresets() {
        ArrayList<PotionPresetDraft> favorites = new ArrayList<PotionPresetDraft>();
        for (PotionPresetDraft preset : this.presets) {
            if (!preset.isFavorite()) continue;
            favorites.add(preset);
        }
        return favorites;
    }

    public void deletePreset(PotionPresetDraft preset) {
        if (this.presets.remove(preset)) {
            this.keybindRegistry.unregister(this.keybindsByPreset.remove(preset));
            ++this.revision;
            this.savePresets();
        }
    }

    private float calculateQuantizedPitch() {
        double sensitivity = (Double)MinecraftClient.getInstance().options.getMouseSensitivity().getValue();
        double base = sensitivity * 0.6 + 0.2;
        double mouseStep = base * base * base * 8.0 * 0.15;
        if (mouseStep <= 0.0) {
            return 90.0f;
        }
        int stepCount = Math.max(1, (int)Math.floor(4.0 / mouseStep));
        this.rotationRandomizationStep = (this.rotationRandomizationStep + 1 + ThreadLocalRandom.current().nextInt(stepCount)) % (stepCount + 1);
        return 90.0f - (float)(mouseStep * (double)this.rotationRandomizationStep);
    }

    private void tickRotationHold() {
        if (this.rotationHoldTicks <= 0) {
            return;
        }
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations == null) {
            this.rotationHoldTicks = 0;
            this.downwardAngle = null;
            return;
        }
        --this.rotationHoldTicks;
        if (this.rotationHoldTicks > 0 && this.downwardAngle != null) {
            this.applyDownwardRotation(rotations, this.downwardAngle);
        } else {
            rotations.clearIntent();
            this.downwardAngle = null;
        }
    }

    public boolean isAvailable(PotionCatalogEntry entry) {
        return entry != null && this.availableStacksById.containsKey(entry.getId());
    }

    private void usePotion(ClientPlayerEntity player, InventoryController inventory, PotionCatalogEntry entry, ItemStack stack, ServerKind serverKind, SwapTiming timing, boolean searchBundles, boolean preserveBundleSlot) {
        boolean throwable = this.isThrowablePotion(stack);
        if (entry.matches(player.getOffHandStack(), serverKind)) {
            if (throwable) {
                this.beginDownwardRotation();
            }
            inventory.submit(InventoryTask.builder().action(new UseItemAction()).owner(TASK_OWNER).flag(TaskFlag.DEFAULT).policy(ClickPolicy.SILENT).priority(TaskPriority.NORMAL).build());
            return;
        }
        PlayerInventory playerInventory = player.getInventory();
        int slot = PotionCatalog.findSlot(playerInventory, entry, serverKind);
        if (slot == -1) {
            if (!searchBundles) {
                return;
            }
            int[] bundleLocation = Bundles.findInBundle(playerInventory, item -> entry.matches((ItemStack)item, serverKind));
            if (bundleLocation == null) {
                return;
            }
            Bundles.useFromBundle(player, inventory, TASK_OWNER, bundleLocation[0], bundleLocation[1], preserveBundleSlot, () -> {
                if (throwable) {
                    this.beginDownwardRotation();
                }
                inventory.pressUseForTick();
            });
            return;
        }
        if (slot < 9) {
            if (throwable) {
                this.beginDownwardRotation();
            }
            inventory.submit(InventoryTask.builder().action(new HotbarSelectAction(slot, true)).owner(TASK_OWNER).flag(TaskFlag.DEFAULT).policy(ClickPolicy.SILENT).priority(TaskPriority.NORMAL).build());
            return;
        }
        Runnable afterSwap = () -> {
            if (throwable) {
                this.beginDownwardRotation();
            }
            inventory.pressUseForTick();
        };
        inventory.submit(InventoryTask.builder().action(inventory.createSwapUseRestore(slot, playerInventory.getSelectedSlot(), afterSwap, timing)).owner(TASK_OWNER).flag(TaskFlag.DEFAULT).policy(ClickPolicy.VISIBLE).priority(TaskPriority.NORMAL).blocking(true).build());
    }

    public void queuePreset(PotionPresetDraft preset) {
        if (preset == null || !PotionCombinerModule.isActive2()) {
            return;
        }
        for (PotionCatalogEntry entry : preset.getPotions()) {
            if (this.pendingPotions.contains(entry)) continue;
            this.pendingPotions.add(entry);
        }
    }

    private void registerKeybind(PotionPresetDraft preset) {
        PotionPresetKeybind keybind = new PotionPresetKeybind(this, preset);
        this.keybindsByPreset.put(preset, keybind);
        this.keybindRegistry.register(keybind);
    }

    private boolean isAimingDown() {
        RotationController rotations = WexSideClient.getRotationController();
        return rotations != null && rotations.isObservedPitchWithin(this.targetPitch, 3.0f);
    }

    private boolean isThrowablePotion(ItemStack stack) {
        return stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION);
    }

    private PlayerInventory getInventory() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player == null ? null : player.getInventory();
    }

    private void applyDownwardRotation(RotationController rotations, Angle angle) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        rotations.applyIntent(new RotationIntent((LivingEntity)player, null, angle, AttackUrgency.HIT, CorrectionMode.NONE, false), ROTATION_OWNER);
    }

    public int getRevision() {
        return this.revision;
    }

    public boolean toggleFavorite(PotionPresetDraft preset) {
        if (preset.isFavorite()) {
            preset.setFavorite(false);
            this.savePresets();
            return true;
        }
        if (this.getFavoritePresets().size() >= 5) {
            return false;
        }
        preset.setFavorite(true);
        this.savePresets();
        return true;
    }

    private void beginDownwardRotation() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        RotationController rotations = WexSideClient.getRotationController();
        if (player == null || rotations == null) {
            return;
        }
        this.downwardAngle = new Angle(player.getYaw(), this.targetPitch);
        this.rotationHoldTicks = 3;
        this.applyDownwardRotation(rotations, this.downwardAngle);
    }

    private void processPendingPotions() {
        ItemStack stack2;
        boolean foundInBundle;
        if (this.pendingPotions.isEmpty()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        InventoryController inventory = WexSideClient.getInventoryController();
        if (player == null || inventory == null) {
            this.pendingPotions.clear();
            this.rotationAttemptCount = 0;
            return;
        }
        ((ItemUseCooldownAccessor)MinecraftClient.getInstance()).setItemUseCooldown(0);
        if (this.useCooldownTicks > 0) {
            --this.useCooldownTicks;
            return;
        }
        if (inventory.hasOwnerTasks(TASK_OWNER)) {
            return;
        }
        ServerKind serverKind = PotionCombinerModule.getServerKind();
        boolean searchBundles = PotionCombinerModule.isActive();
        PotionCatalogEntry entry = this.pendingPotions.get(0);
        ItemStack inventoryStack = PotionCatalog.findStack(player.getInventory(), entry, serverKind);
        boolean bl = foundInBundle = searchBundles && Bundles.contains(player.getInventory(), stack -> entry.matches((ItemStack)stack, serverKind));
        if (inventoryStack.isEmpty() && !foundInBundle) {
            ItemAlerts.warnMissing(entry.createDefaultStack(), entry.getDisplayName());
            this.pendingPotions.remove(0);
            this.rotationAttemptCount = 0;
            return;
        }
        ItemStack class_17992 = stack2 = inventoryStack.isEmpty() ? entry.createDefaultStack() : inventoryStack;
        if (ItemAlerts.isBusy(player.currentScreenHandler, stack2, entry.getDisplayName())) {
            this.pendingPotions.remove(0);
            this.rotationAttemptCount = 0;
            return;
        }
        if (this.isThrowablePotion(stack2)) {
            if (this.rotationAttemptCount == 0) {
                this.targetPitch = this.calculateQuantizedPitch();
            }
            if (!this.isAimingDown()) {
                this.beginDownwardRotation();
                if (this.rotationAttemptCount++ < 8) {
                    return;
                }
            }
        }
        this.usePotion(player, inventory, entry, stack2, serverKind, PotionCombinerModule.getSwapTiming(), searchBundles, PotionCombinerModule.isActive3());
        this.pendingPotions.remove(0);
        this.rotationAttemptCount = 0;
        this.useCooldownTicks = 1;
    }
}

