package ru.wexside.module.movement;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.event.OutgoingPacketEvent;
import ru.wexside.misc.ElapsedTimer;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.ui.WexsideScreen;
import ru.wexside.util.InventoryController;

public class GuiMoveModule
extends Module
implements ConfigSerializable {
    private volatile boolean waitingForConfirm;
    private volatile boolean flushingFunTime;
    private volatile boolean sendingOwnPackets;
    private volatile int funTimeFlushTicks;
    private final ModeSetting bypass;
    private volatile boolean delayingClicks;
    private volatile int clickDelayTicks;
    private final Queue<ClickSlotC2SPacket> clickQueue = new ConcurrentLinkedQueue<ClickSlotC2SPacket>();
    private final ElapsedTimer confirmTimer = new ElapsedTimer();
    private final Queue<Packet<?>> funTimeQueue = new ConcurrentLinkedQueue();
    private volatile CloseHandledScreenC2SPacket pendingClose;
    private volatile int closeDelayTicks;
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private volatile boolean movingKeysDown;
    private volatile boolean wasDelayingClicks;

    public GuiMoveModule(EventBus eventBus) {
        super(eventBus, "gui_move", "Gui Move", "Позволяет двигаться при открытом GUI", ModuleCategory.valueOf("MOVEMENT"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.bypass = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Off", "FT", "Spooky-T").defaultOption("Off").name("Обход").id("bypass").description("Режим обхода кликов по слотам")).build();
        this.registerSetting(this.bypass);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onClientTick);
        this.listen(OutgoingPacketEvent.class, this::onOutgoingPacket);
        this.listen(IncomingPacketEvent.class, this::onIncomingPacket);
    }

    private void onClientTick(ClientTickEvent event) {
        boolean busy = false;
        if (this.enabledSetting.isEnabled() && this.spookyTimeMode()) {
            this.tickSpookyTime();
            busy = this.delayingClicks || !this.clickQueue.isEmpty() || this.pendingClose != null;
        } else {
            this.resetSpookyTime();
        }
        if (this.enabledSetting.isEnabled() && this.funTimeMode()) {
            this.tickFunTime();
            busy = busy || this.flushingFunTime;
        } else {
            this.resetFunTime();
        }
        this.setInventoryBusy(busy);
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (!this.enabledSetting.isEnabled() || screen == null || this.compute2(screen)) {
            if (screen != null) {
                this.releaseMovementKeys();
            }
            return;
        }
        this.pressMovementKeys();
    }

    private void onOutgoingPacket(OutgoingPacketEvent event) {
        if (!this.enabledSetting.isEnabled() || this.sendingOwnPackets) {
            return;
        }
        if (this.funTimeMode()) {
            this.onFunTimeOutgoing(event);
        } else if (this.spookyTimeMode()) {
            this.onSpookyTimeOutgoing(event);
        }
    }

    private void onIncomingPacket(IncomingPacketEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        if (this.funTimeMode()) {
            CloseScreenS2CPacket close;
            Packet<?> packet = event.getPacket();
            if (packet instanceof CloseScreenS2CPacket && (close = (CloseScreenS2CPacket)packet).getSyncId() == 0) {
                event.update();
            }
            return;
        }
        if (!this.spookyTimeMode() || !this.waitingForConfirm || this.confirmTimer.hasElapsed(2000L)) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof ScreenHandlerSlotUpdateS2CPacket || packet instanceof InventoryS2CPacket) {
            this.waitingForConfirm = false;
            event.update();
        }
    }

    private void resetFunTime() {
        if (!this.funTimeQueue.isEmpty()) {
            this.funTimeQueue.clear();
        }
        this.flushingFunTime = false;
        this.funTimeFlushTicks = 0;
    }

    private void pressMovementKeys() {
        KeyBinding[] keys = this.movementKeys();
        if (keys.length == 0) {
            return;
        }
        Window window = MinecraftClient.getInstance().getWindow();
        for (KeyBinding key : keys) {
            InputUtil.Key bound = InputUtil.fromTranslationKey((String)key.getBoundKeyTranslationKey());
            boolean pressed = bound.getCategory() != InputUtil.Type.MOUSE && InputUtil.isKeyPressed((Window)window, (int)bound.getCode());
            key.setPressed(pressed);
        }
    }

    private boolean spookyTimeMode() {
        return "Spooky-T".equals(this.bypass.getSelectedOption());
    }

    private void onSpookyTimeOutgoing(OutgoingPacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof CloseHandledScreenC2SPacket) {
            CloseHandledScreenC2SPacket close = (CloseHandledScreenC2SPacket)packet;
            if (this.shouldHoldClose()) {
                this.pendingClose = close;
                this.closeDelayTicks = 0;
                event.update();
            }
            return;
        }
        if (!(packet instanceof ClickSlotC2SPacket)) {
            return;
        }
        ClickSlotC2SPacket click = (ClickSlotC2SPacket)packet;
        if (!this.clickBusy() || click.slot() == -1) {
            return;
        }
        this.armClickDelay(click.actionType());
        if (!this.delayingClicks && this.queueClick(click)) {
            this.waitingForConfirm = true;
            this.confirmTimer.update();
            event.update();
        }
    }

    private void resetSpookyTime() {
        if (!this.clickQueue.isEmpty()) {
            this.clickQueue.clear();
        }
        this.clickDelayTicks = 0;
        this.delayingClicks = false;
        this.wasDelayingClicks = false;
        this.waitingForConfirm = false;
        this.movingKeysDown = false;
        this.pendingClose = null;
        this.closeDelayTicks = 0;
    }

    private boolean isKeyDown(Window window, KeyBinding key) {
        InputUtil.Key bound = InputUtil.fromTranslationKey((String)key.getBoundKeyTranslationKey());
        return bound.getCategory() != InputUtil.Type.MOUSE && InputUtil.isKeyPressed((Window)window, (int)bound.getCode());
    }

    private void tickFunTime() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            this.resetFunTime();
            return;
        }
        this.updateMovingKeys();
        if (!this.flushingFunTime) {
            this.funTimeFlushTicks = 0;
            return;
        }
        if (this.funTimeFlushTicks > 2) {
            this.sendingOwnPackets = true;
            try {
                Packet<?> packet;
                while ((packet = this.funTimeQueue.poll()) != null) {
                    player.networkHandler.sendPacket(packet);
                }
            }
            finally {
                this.sendingOwnPackets = false;
            }
            this.flushingFunTime = false;
            this.funTimeFlushTicks = 0;
            return;
        }
        ++this.funTimeFlushTicks;
    }

    private boolean compute2(Screen screen) {
        return screen instanceof ChatScreen || screen instanceof AbstractSignEditScreen || screen instanceof BookEditScreen || screen instanceof WexsideScreen;
    }

    private boolean shouldHoldClose() {
        if (this.clickBusy() || !this.clickQueue.isEmpty()) {
            return true;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && player.isSprinting();
    }

    private boolean holdingItemInInventory(ClientPlayerEntity player) {
        if (!(MinecraftClient.getInstance().currentScreen instanceof InventoryScreen)) {
            return false;
        }
        if (!this.clickBusy()) {
            return false;
        }
        return !player.currentScreenHandler.getCursorStack().isEmpty();
    }

    private boolean queueClick(ClickSlotC2SPacket packet) {
        if (this.clickQueue.contains(packet)) {
            return false;
        }
        return this.clickQueue.add(packet);
    }

    private void flushPendingClose(ClientPlayerEntity player) {
        if (this.pendingClose == null) {
            return;
        }
        if (!this.clickQueue.isEmpty() || this.closeDelayTicks <= 1) {
            ++this.closeDelayTicks;
            return;
        }
        this.sendingOwnPackets = true;
        try {
            player.networkHandler.sendPacket((Packet)this.pendingClose);
        }
        finally {
            this.sendingOwnPackets = false;
        }
        this.pendingClose = null;
        this.closeDelayTicks = 0;
    }

    private int delayFor(SlotActionType actionType) {
        return actionType == SlotActionType.PICKUP ? 1 : (this.clickDelayTicks > 1 ? 2 : 3);
    }

    private void setInventoryBusy(boolean busy) {
        InventoryController member6090 = WexSideClient.getInventoryController();
        if (member6090 == null) {
            return;
        }
        if (busy) {
            member6090.update();
        } else {
            member6090.update2();
        }
    }

    private void onFunTimeOutgoing(OutgoingPacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClickSlotC2SPacket) {
            ClickSlotC2SPacket click = (ClickSlotC2SPacket)packet;
            if (MinecraftClient.getInstance().currentScreen instanceof InventoryScreen && this.movingKeysDown && click.slot() != -1) {
                this.funTimeQueue.add((Packet<?>)click);
                event.update();
            }
        } else if (packet instanceof CloseHandledScreenC2SPacket) {
            CloseHandledScreenC2SPacket close = (CloseHandledScreenC2SPacket)packet;
            if (!this.funTimeQueue.isEmpty()) {
                this.funTimeQueue.add((Packet<?>)close);
                this.flushingFunTime = true;
                event.update();
            }
        }
    }

    private KeyBinding[] movementKeys() {
        GameOptions options = MinecraftClient.getInstance().options;
        if (options == null) {
            return new KeyBinding[0];
        }
        return new KeyBinding[]{options.forwardKey, options.leftKey, options.backKey, options.rightKey, options.jumpKey, options.sprintKey};
    }

    private void flushClickQueue(ClientPlayerEntity player) {
        if (this.clickQueue.isEmpty()) {
            return;
        }
        this.sendingOwnPackets = true;
        try {
            ClickSlotC2SPacket click;
            while ((click = this.clickQueue.poll()) != null) {
                player.networkHandler.sendPacket((Packet)click);
            }
        }
        finally {
            this.sendingOwnPackets = false;
        }
    }

    private boolean funTimeMode() {
        return "FT".equals(this.bypass.getSelectedOption());
    }

    private boolean clickBusy() {
        return this.movingKeysDown || this.delayingClicks;
    }

    private void releaseMovementKeys() {
        for (KeyBinding key : this.movementKeys()) {
            key.setPressed(false);
        }
    }

    private void armClickDelay(SlotActionType actionType) {
        this.clickDelayTicks = this.delayFor(actionType == null ? SlotActionType.PICKUP : actionType) + 1;
    }

    private void updateMovingKeys() {
        GameOptions options = MinecraftClient.getInstance().options;
        if (options == null) {
            this.movingKeysDown = false;
            return;
        }
        Window window = MinecraftClient.getInstance().getWindow();
        this.movingKeysDown = this.isKeyDown(window, options.forwardKey) || this.isKeyDown(window, options.backKey) || this.isKeyDown(window, options.leftKey) || this.isKeyDown(window, options.rightKey);
    }

    private void tickSpookyTime() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            this.resetSpookyTime();
            return;
        }
        this.updateMovingKeys();
        if (this.wasDelayingClicks && this.delayingClicks && this.clickDelayTicks > 0) {
            this.flushClickQueue(player);
        }
        this.wasDelayingClicks = this.delayingClicks;
        if (this.holdingItemInInventory(player)) {
            this.clickDelayTicks = this.delayFor(SlotActionType.PICKUP) + 1;
        } else if (this.clickDelayTicks > 0) {
            --this.clickDelayTicks;
        }
        this.delayingClicks = this.clickDelayTicks > 0;
        this.flushPendingClose(player);
    }
}

