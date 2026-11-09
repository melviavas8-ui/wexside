package ru.wexside.module.hud;

import net.minecraft.client.MinecraftClient;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.event.MousePressedEvent;
import ru.wexside.event.MouseReleasedEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.ArmorHUD;
import ru.wexside.misc.Cooldowns;
import ru.wexside.misc.Effects;
import ru.wexside.misc.HudDragInputHandler;
import ru.wexside.misc.HudDragRegistry;
import ru.wexside.misc.InventoryHUD;
import ru.wexside.misc.KeybindsHud;
import ru.wexside.misc.Schedules;
import ru.wexside.misc.StaffList;
import ru.wexside.misc.TotemCounter;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.notification.NotificationCategory;
import ru.wexside.notification.NotificationCenter;
import ru.wexside.notification.NotificationPreferences;
import ru.wexside.notification.NotificationTracker;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.TargetHUD;
import ru.wexside.util.Watermark;

public final class HUDModule
extends Module
implements ConfigSerializable,
NotificationPreferences {
    private static volatile HUDModule instance;
    private final MultiSelectSetting elements;
    private final BooleanSetting targetHudRaycast;
    private final BooleanSetting targetHudHurtTint;
    private final BooleanSetting notifications;
    private final MultiSelectSetting notificationEvents;
    private final MultiSelectSetting notificationSounds;
    private final NumberSetting notificationVolume;
    private final Watermark watermark;
    private final Cooldowns cooldowns;
    private final Schedules schedules;
    private final StaffList staffList;
    private final TargetHUD targetHud;
    private final Effects effects;
    private final InventoryHUD inventoryHud;
    private final ArmorHUD armorHud;
    private final TotemCounter totemCounter;
    private final KeybindsHud keybindsHud;
    private final HudDragRegistry dragRegistry;
    private final HudDragInputHandler dragController;
    private final NotificationTracker notificationTracker;

    public HUDModule(EventBus eventBus) {
        super(eventBus, "hud", "HUD", "HUD-элементы клиента", ModuleCategory.valueOf("DISPLAY"), "hud", "худ");
        MultiSelectSetting soundsSetting;
        MultiSelectSetting eventsSetting;
        MultiSelectSetting elementsSetting;
        instance = this;
        this.elements = elementsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Watermark", "Cooldowns", "Schedules", "Staff List", "Target HUD", "Keybinds", "Effects", "Inventory HUD", "Armor HUD", "Totem Counter").selectAll(true).optionListEnabled(false).name("Elements").id("elements").description("Какие HUD-элементы рисовать")).build();
        this.registerSetting(this.elements);
        this.targetHudRaycast = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Target HUD Raycast").id("targethudraycast").description("Цель по лучу взгляда вне боя")).build();
        this.registerSetting(this.targetHudRaycast);
        this.targetHudHurtTint = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Target HUD Hurt Tint").id("targethudhurttint").description("Красная подсветка головы при уроне")).build();
        this.registerSetting(this.targetHudHurtTint);
        this.notifications = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Notifications").id("notifications").description("Уведомления о событиях")).build();
        this.registerSetting(this.notifications);
        this.notificationEvents = eventsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Use", "Drink", "Effect", "Function", "Config", "Missing", "Delay", "Totem", "Swap", "Pickup", "Chat", "Structures").selectAll(false).optionListEnabled(false).name("Notification Events").id("notificationevents").description("События уведомлений").visibleWhen(this.notifications::isEnabled)).build();
        this.registerSetting(this.notificationEvents);
        this.notificationSounds = soundsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Use", "Drink", "Effect", "Function", "Config", "Missing", "Delay", "Totem", "Swap", "Pickup", "Chat", "Structures").selectAll(false).optionListEnabled(false).name("Notification Sounds").id("notificationsounds").description("Звуки уведомлений").visibleWhen(this.notifications::isEnabled)).build();
        this.registerSetting(this.notificationSounds);
        this.notificationVolume = ((NumberSettingBuilder)NumberSetting.builder().range(10.0, 100.0).defaultValue(50.0).multiplier(1.0).precision(0).animationSpeed(20.0f).markers(30.0).snapTo(5.0).name("Sound Volume").id("notificationvolume").description("Громкость звука уведомлений").visibleWhen(() -> this.notifications.isEnabled() && !this.notificationSounds.getSelectedOptions().isEmpty())).build();
        this.registerSetting(this.notificationVolume);
        this.watermark = new Watermark(this::isWatermarkEnabled);
        this.cooldowns = new Cooldowns(this::isCooldownsEnabled);
        this.schedules = new Schedules(this::isSchedulesEnabled);
        this.staffList = new StaffList(this::isStaffListEnabled);
        this.targetHud = new TargetHUD(this::isTargetHudEnabled, this.targetHudRaycast::isEnabled, this.targetHudHurtTint::isEnabled);
        this.effects = new Effects(this::isEffectsEnabled);
        this.inventoryHud = new InventoryHUD(this::isInventoryHudEnabled);
        this.armorHud = new ArmorHUD(this::isArmorHudEnabled);
        this.totemCounter = new TotemCounter(this::isTotemCounterEnabled);
        this.keybindsHud = new KeybindsHud(this::isKeybindsEnabled);
        this.dragRegistry = new HudDragRegistry();
        this.dragController = new HudDragInputHandler(this.dragRegistry);
        this.notificationTracker = new NotificationTracker();
    }

    @Override
    protected void initialize() {
        this.listen(HudRenderEvent.class, event -> this.tick());
        this.listen(ClientTickEvent.class, event -> {
            if (this.notifications.isEnabled()) {
                this.notificationTracker.tick();
            }
        });
        this.listen(WorldSessionEvent.class, event -> {});
        this.listen(MousePressedEvent.class, event -> this.handleHudMousePress(event.button()));
        this.listen(MouseReleasedEvent.class, event -> this.dragController.onMouseReleased(event.button()));
    }

    private void handleHudMousePress(int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!this.dragController.isEditorScreen()) {
            return;
        }
        double scale = client.getWindow().getScaleFactor();
        this.dragController.onMousePressed((float)(client.mouse.getX() / scale), (float)(client.mouse.getY() / scale), button);
    }

    private void tick() {
        this.watermark.renderFrame();
        NotificationCenter notificationCenter = WexSideClient.getNotificationCenter();
        if (notificationCenter != null) {
            notificationCenter.render(1.0f);
        }
        this.cooldowns.renderFrame();
        this.schedules.renderFrame();
        this.staffList.renderFrame();
        this.targetHud.renderFrame();
        this.effects.renderFrame();
        this.inventoryHud.renderFrame();
        this.armorHud.renderFrame();
        this.totemCounter.renderFrame();
        this.keybindsHud.renderFrame();
        this.dragController.updateScreenState();
        this.dragRegistry.render();
    }

    private boolean isElementEnabled(String name) {
        return this.elements.getSelectedOptions().contains(name);
    }

    private boolean isWatermarkEnabled() {
        return this.isElementEnabled("Watermark");
    }

    private boolean isCooldownsEnabled() {
        return this.isElementEnabled("Cooldowns");
    }

    private boolean isSchedulesEnabled() {
        return this.isElementEnabled("Schedules");
    }

    private boolean isStaffListEnabled() {
        return this.isElementEnabled("Staff List");
    }

    private boolean isTargetHudEnabled() {
        return this.isElementEnabled("Target HUD");
    }

    private boolean isKeybindsEnabled() {
        return this.isElementEnabled("Keybinds");
    }

    private boolean isEffectsEnabled() {
        return this.isElementEnabled("Effects");
    }

    private boolean isInventoryHudEnabled() {
        return this.isElementEnabled("Inventory HUD");
    }

    private boolean isArmorHudEnabled() {
        return this.isElementEnabled("Armor HUD");
    }

    private boolean isTotemCounterEnabled() {
        return this.isElementEnabled("Totem Counter");
    }

    public HudDragRegistry getHudDragRegistry() {
        return this.dragRegistry;
    }

    public static boolean isHudContextActive() {
        return MinecraftClient.getInstance().currentScreen == null;
    }

    @Override
    public boolean isCategoryVisible(NotificationCategory category) {
        return this.notificationEvents.getSelectedOptions().contains(category.settingName());
    }

    @Override
    public boolean isSoundEnabled(NotificationCategory category) {
        return this.notificationSounds.getSelectedOptions().contains(category.settingName());
    }

    @Override
    public float soundVolume() {
        return (float)(this.notificationVolume.getValue() / 100.0);
    }

    @Override
    public boolean isEnabled() {
        return this.notifications.isEnabled();
    }
}

