package ru.wexside.module.misc;

import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.ClickPolicy;
import ru.wexside.misc.ClickSlotAction;
import ru.wexside.misc.ElapsedTimer;
import ru.wexside.misc.HotbarSelectAction;
import ru.wexside.misc.Inventories;
import ru.wexside.misc.InventoryTask;
import ru.wexside.misc.TaskFlag;
import ru.wexside.misc.TaskPriority;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.InventoryController;

public final class ServerJoinerModule
extends Module
implements ConfigSerializable {
    private static final String SERVER_SPOOKY_TIME = "SpookyTime";
    private static final String SERVER_REALLY_WORLD = "ReallyWorld";
    private static final String MODULE_OWNER = "server_joiner";
    private static final long GRIEF_CLICK_DELAY_MS = 150L;
    private static final long REALLY_WORLD_RETRY_MS = 500L;
    private static final long REALLY_WORLD_QUEUE_MS = 5000L;
    private final ElapsedTimer clickTimer = new ElapsedTimer();
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private final ModeSetting server;
    private final NumberSetting griefNumber;
    private volatile long reallyWorldRetryAtMs;
    private boolean firstTick = true;

    public ServerJoinerModule(EventBus eventBus) {
        super(eventBus, MODULE_OWNER, "Server Joiner", "Автоматический вход на выбранный сервер: SpookyTime дуэли / ReallyWorld гриф", ModuleCategory.valueOf("MISC"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.server = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options(SERVER_SPOOKY_TIME, SERVER_REALLY_WORLD).defaultOption(SERVER_SPOOKY_TIME).name("Сервер").id("server").description("Сервер для автоматического входа")).build();
        this.registerSetting(this.server);
        this.griefNumber = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 42.0).defaultValue(1.0).multiplier(1.0).precision(0).animationSpeed(20.0f).snapTo(1.0).name("Номер грифа").id("grief_number").description("Номер гриф-сервера ReallyWorld").visibleWhen(() -> SERVER_REALLY_WORLD.equals(this.server.getSelectedOption()))).build();
        this.registerSetting(this.griefNumber);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onClientTick);
        this.listen(IncomingPacketEvent.class, this::onIncomingPacket);
        this.listen(WorldSessionEvent.class, this::onWorldChange);
    }

    private void onIncomingPacket(IncomingPacketEvent event) {
        if (!this.enabledSetting.isEnabled() || !SERVER_REALLY_WORLD.equals(this.server.getSelectedOption())) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof GameJoinS2CPacket) {
            this.enabledSetting.setEnabled(false);
            return;
        }
        String message = this.extractMessage(packet);
        if (message == null) {
            return;
        }
        boolean waitMessage = message.contains("Подождите");
        boolean retryMessage = waitMessage || message.contains("К сожалению сервер переполнен") || message.contains("большой поток игроков") || message.contains("Сервер перезагружается");
        boolean bl = retryMessage;
        if (!retryMessage) {
            return;
        }
        this.reallyWorldRetryAtMs = waitMessage ? System.currentTimeMillis() + 5000L : System.currentTimeMillis();
    }

    private void onClientTick(ClientTickEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            this.firstTick = true;
            this.reallyWorldRetryAtMs = 0L;
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        boolean initialTick = this.firstTick;
        this.firstTick = false;
        if (SERVER_REALLY_WORLD.equals(this.server.getSelectedOption())) {
            this.handleReallyWorld(initialTick);
        } else {
            this.handleSpookyTime();
        }
    }

    private void handleSpookyTime() {
        HandledScreen handledScreen;
        Screen screen;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) {
            this.selectHotbarItem("выбор сервера", true);
        }
        if ((screen = client.currentScreen) instanceof HandledScreen && (handledScreen = (HandledScreen)screen).getTitle().getString().contains("Выберите режим:")) {
            this.clickModeSelectorSlot();
        }
        if (this.findHotbarSlot("вход в очередь") != -1) {
            this.enabledSetting.setEnabled(false);
        }
    }

    private void handleReallyWorld(boolean initialTick) {
        if (initialTick) {
            this.selectCompassSlot();
            this.clickTimer.update();
            return;
        }
        if (this.reallyWorldRetryAtMs != 0L && System.currentTimeMillis() >= this.reallyWorldRetryAtMs) {
            this.reallyWorldRetryAtMs = 0L;
            this.selectCompassSlot();
            this.clickTimer.update();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        if (client.currentScreen == null) {
            if (player.age < 5 && this.clickTimer.hasElapsed(500L)) {
                this.selectCurrentHotbarSlot();
                this.clickTimer.update();
            }
            return;
        }
        if (client.currentScreen instanceof HandledScreen) {
            this.clickMatchingGriefSlot(player);
        }
    }

    private void clickMatchingGriefSlot(ClientPlayerEntity player) {
        int griefNumber = this.griefNumber.getIntValue();
        for (int slotIndex = 0; slotIndex < player.currentScreenHandler.slots.size(); ++slotIndex) {
            String name;
            Slot slot = (Slot)player.currentScreenHandler.slots.get(slotIndex);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || !(name = stack.getName().getString()).contains("ГРИФЕРСКОЕ ВЫЖИВАНИЕ") && !this.matchesGriefNumber(name, griefNumber) || !this.clickTimer.hasElapsed(150L)) continue;
            this.clickContainerSlot(slotIndex);
            this.clickTimer.update();
        }
    }

    private boolean matchesGriefNumber(String name, int griefNumber) {
        String marker = "ГРИФ #" + griefNumber;
        int index = name.indexOf(marker);
        if (index == -1) {
            return false;
        }
        int afterMarker = index + marker.length();
        if (afterMarker >= name.length()) {
            return true;
        }
        return !Character.isDigit(name.charAt(afterMarker));
    }

    private void clickModeSelectorSlot() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        if (player == null || interactionManager == null) {
            return;
        }
        interactionManager.clickSlot(player.currentScreenHandler.syncId, 13, 0, SlotActionType.PICKUP, (PlayerEntity)player);
    }

    private String extractMessage(Packet<?> packet) {
        Text text = null;
        if (packet instanceof GameMessageS2CPacket) {
            GameMessageS2CPacket chatPacket = (GameMessageS2CPacket)packet;
            if (chatPacket.overlay()) {
                return null;
            }
            text = chatPacket.content();
        }
        if (text == null) {
            return null;
        }
        return Formatting.strip((String)text.getString());
    }

    private void selectCompassSlot() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        int slot = Inventories.findHotbarSlot(Items.COMPASS);
        if (slot == -1) {
            slot = player.getInventory().getSelectedSlot();
        }
        this.selectHotbarSlot(slot, false);
    }

    private void selectCurrentHotbarSlot() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        this.selectHotbarSlot(player.getInventory().getSelectedSlot(), false);
    }

    private void selectHotbarItem(String namePart, boolean rightClick) {
        int slot = this.findHotbarSlot(namePart);
        if (slot == -1) {
            return;
        }
        this.selectHotbarSlot(slot, rightClick);
    }

    private void selectHotbarSlot(int slot, boolean rightClick) {
        InventoryController member6090 = WexSideClient.getInventoryController();
        if (member6090 == null) {
            return;
        }
        member6090.submit(InventoryTask.builder().action(new HotbarSelectAction(slot, rightClick)).owner(MODULE_OWNER).flag2(TaskFlag.DEFAULT).policy(ClickPolicy.SILENT).priority(TaskPriority.NORMAL).build());
    }

    private void clickContainerSlot(int slot) {
        InventoryController member6090 = WexSideClient.getInventoryController();
        if (member6090 == null) {
            return;
        }
        member6090.submit(InventoryTask.builder().action(new ClickSlotAction(slot, 0)).owner(MODULE_OWNER).flag2(TaskFlag.DEFAULT).policy(ClickPolicy.SILENT).priority(TaskPriority.NORMAL).build());
    }

    private int findHotbarSlot(String namePart) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return -1;
        }
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isEmpty() || !stack.getName().getString().toLowerCase(Locale.ROOT).contains(namePart)) continue;
            return slot;
        }
        return -1;
    }

    private void onWorldChange(WorldSessionEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || SERVER_REALLY_WORLD.equals(this.server.getSelectedOption())) {
            return;
        }
        if (client.world.getRegistryKey().getValue().toString().contains(":spawn")) {
            this.enabledSetting.setEnabled(false);
        }
    }
}

