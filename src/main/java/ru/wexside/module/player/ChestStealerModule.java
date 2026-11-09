package ru.wexside.module.player;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.ClientChat;
import ru.wexside.misc.ElapsedTimer;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.server.FunTimeServerContext;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;

public class ChestStealerModule
extends Module
implements ConfigSerializable {
    private static final int MISS_PERIOD = 30;
    private static final int MISS_JITTER = 15;
    private static final double MISS_CHANCE = 0.1;
    private final BooleanSetting enabledSetting;
    private final BooleanSetting random;
    private final BooleanSetting missSlots;
    private final BooleanSetting closeIfEmpty;
    private final BooleanSetting leaveAfterLoot;
    private final NumberSetting delay;
    private final ElapsedTimer delayTimer = new ElapsedTimer();
    private int tickCounter;

    public ChestStealerModule(EventBus eventBus) {
        super(eventBus, "chest_stealer", "Chest Stealer", "Автоматически забирает предметы из открытых сундуков", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.random = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Random").id("random").description("Случайный порядок и джиттер задержки").aliases("random", "рандом")).build();
        this.registerSetting(this.random);
        this.missSlots = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Miss Slots").id("miss_slots").description("Иногда промахиваться по пустым слотам").aliases("miss slots", "промахи")).build();
        this.registerSetting(this.missSlots);
        this.closeIfEmpty = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Close If Empty").id("close_if_empty").description("Закрывать сундук когда он пуст").aliases("close if empty", "закрывать пустой")).build();
        this.registerSetting(this.closeIfEmpty);
        this.leaveAfterLoot = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Leave After Loot").id("leave_after_loot").description("Выходить с сервера после лута").aliases("leave after loot", "выходить после лута")).build();
        this.registerSetting(this.leaveAfterLoot);
        this.delay = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 500.0).defaultValue(50.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Delay").id("delay").description("Задержка между предметами (мс)").aliases("delay", "задержка")).build();
        this.registerSetting(this.delay);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        int slot;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (!this.enabledSetting.isEnabled() || player == null || client.world == null) {
            return;
        }
        ScreenHandler handler = player.currentScreenHandler;
        if (!(handler instanceof GenericContainerScreenHandler)) {
            return;
        }
        GenericContainerScreenHandler container = (GenericContainerScreenHandler)handler;
        Inventory chest = container.getInventory();
        this.tickCounter = (this.tickCounter + 1) % 600;
        if (player.getInventory().getEmptySlot() == -1) {
            ClientChat.send((Text)Text.literal((String)"Сундук закрыт: инвентарь полон.").formatted(Formatting.RED));
            this.enabledSetting.setEnabled(false);
            player.closeHandledScreen();
            return;
        }
        if (chest.isEmpty()) {
            if (this.closeIfEmpty.isEnabled()) {
                player.closeHandledScreen();
            }
            if (this.leaveAfterLoot.isEnabled() && !FunTimeServerContext.isPvpLocked()) {
                this.leaveServer();
            }
            return;
        }
        long wait = this.delay.getIntValue();
        if (this.random.isEnabled()) {
            wait += (long)ThreadLocalRandom.current().nextInt(15);
        }
        if (this.delayTimer.hasElapsed(wait) && (slot = this.nextFilledSlot(chest)) != -1) {
            this.click(container.syncId, slot, SlotActionType.QUICK_MOVE, player);
            this.delayTimer.update();
        }
        if (this.missSlots.isEnabled()) {
            this.maybeMiss(container, chest, player);
        }
    }

    private void leaveServer() {
        this.enabledSetting.setEnabled(false);
        MinecraftClient client = MinecraftClient.getInstance();
        client.disconnectWithSavingScreen();
        client.setScreen((Screen)new TitleScreen());
    }

    private int nextFilledSlot(Inventory chest) {
        int size = chest.size();
        if (!this.random.isEnabled()) {
            for (int i = 0; i < size; ++i) {
                if (chest.getStack(i).isEmpty()) continue;
                return i;
            }
            return -1;
        }
        ArrayList<Integer> filled = new ArrayList<Integer>();
        for (int i = 0; i < size; ++i) {
            if (chest.getStack(i).isEmpty()) continue;
            filled.add(i);
        }
        if (filled.isEmpty()) {
            return -1;
        }
        return (Integer)filled.get(ThreadLocalRandom.current().nextInt(filled.size()));
    }

    private void click(int member9959, int slot, SlotActionType action, ClientPlayerEntity player) {
        ClientPlayerInteractionManager interactions = MinecraftClient.getInstance().interactionManager;
        if (interactions != null) {
            interactions.clickSlot(member9959, slot, 0, action, (PlayerEntity)player);
        }
    }

    private void maybeMiss(GenericContainerScreenHandler container, Inventory chest, ClientPlayerEntity player) {
        if (this.tickCounter % 30 != 0) {
            return;
        }
        int size = chest.size();
        for (int i = 0; i < size; ++i) {
            if (!chest.getStack(i).isEmpty() || ThreadLocalRandom.current().nextDouble() >= 0.1) continue;
            this.click(container.syncId, i, SlotActionType.PICKUP, player);
            return;
        }
    }
}

