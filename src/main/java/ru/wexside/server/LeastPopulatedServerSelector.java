package ru.wexside.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import ru.wexside.misc.ClientChat;
import ru.wexside.misc.ServerPopulationEntry;

public final class LeastPopulatedServerSelector {
    private static final int TIMEOUT_TICKS = 1600;
    private static final int MENU_SETTLE_TICKS = 6;
    private static final int PAGE_SETTLE_TICKS = 30;
    private static final int MENU_CHANGE_TIMEOUT_TICKS = 60;
    private static final int MAX_PAGES = 12;
    private static final int[] COMMAND_CATEGORIES = new int[]{1, 2, 3, 5, 10};
    private final Pattern populationPattern = Pattern.compile("Онлайн режима:\\s*(\\d+)");
    private final Pattern numberPattern = Pattern.compile("(\\d+)");
    private final List<ServerPopulationEntry> discoveredServers = new ArrayList<ServerPopulationEntry>();
    private final Set<String> discoveredNames = new HashSet<String>();
    private State state = State.IDLE;
    private State stateAfterMenuChange;
    private ServerPopulationEntry selectedServer;
    private int elapsedTicks;
    private int menuTicks;
    private int page;
    private int categoryIndex;
    private int expectedSyncId = -1;
    private int menuChangeTicks;

    public void tick() {
        if (this.state == State.IDLE) {
            return;
        }
        if (++this.elapsedTicks > 1600) {
            this.fail("таймаут");
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !(client.currentScreen instanceof HandledScreen)) {
            this.fail("меню закрыто");
            return;
        }
        switch (this.state.ordinal()) {
            case 1: {
                this.openVersionOrCategory();
                break;
            }
            case 2: {
                this.awaitMenuChange();
                break;
            }
            case 3: {
                this.scanCurrentCategory(false);
                break;
            }
            case 4: {
                this.scanCurrentCategory(true);
            }
        }
    }

    public void start() {
        if (this.state != State.IDLE) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !(client.currentScreen instanceof HandledScreen)) {
            return;
        }
        this.resetProgress();
        this.state = State.OPEN_VERSION;
        ClientChat.send("Ищем самую пустую анархию...");
    }

    private void openVersionOrCategory() {
        String title = this.currentTitle();
        if (title.contains("Выберите сервер") || title.contains("тип режима")) {
            int categorySlot = this.findCategorySlot(COMMAND_CATEGORIES[0]);
            if (categorySlot < 0) {
                this.fail("нет вкладки команды");
                return;
            }
            this.categoryIndex = 0;
            this.page = 0;
            this.clickAndWait(categorySlot, State.SCAN_SERVERS);
        } else if (title.contains("Выберите режим")) {
            int versionSlot = this.findSlotContaining("1.21.11");
            if (versionSlot < 0) {
                this.fail("нет типа 1.21.11");
                return;
            }
            this.clickAndWait(versionSlot, State.OPEN_VERSION);
        } else {
            this.fail("неожиданный экран: " + title);
        }
    }

    private void scanCurrentCategory(boolean locatingSelectedServer) {
        if (!this.currentTitle().contains("Выберите сервер")) {
            this.fail("ожидал экран серверов");
            return;
        }
        ++this.menuTicks;
        if (!(this.menuTicks >= 30 || this.menuTicks >= 6 && this.containsAnarchyServers())) {
            return;
        }
        if (locatingSelectedServer) {
            int serverSlot = this.findExactSlot(this.selectedServer.name());
            if (serverSlot >= 0) {
                this.clickSlot(serverSlot);
                ClientChat.send("Заходим на " + this.selectedServer.name() + " (онлайн " + this.selectedServer.onlinePlayers() + ")");
                this.finish();
                return;
            }
        } else {
            this.collectCurrentPage();
        }
        int nextPageSlot = this.findSlotContaining("Следующая страница");
        if (nextPageSlot >= 0 && this.page < 12) {
            ++this.page;
            this.clickAndWait(nextPageSlot, locatingSelectedServer ? State.FIND_SELECTED_SERVER : State.SCAN_SERVERS);
            return;
        }
        if (locatingSelectedServer) {
            this.fail("не нашёл сервер " + this.selectedServer.name());
            return;
        }
        ++this.categoryIndex;
        this.page = 0;
        if (this.categoryIndex < COMMAND_CATEGORIES.length) {
            int categorySlot = this.findCategorySlot(COMMAND_CATEGORIES[this.categoryIndex]);
            if (categorySlot < 0) {
                this.fail("нет вкладки команды " + COMMAND_CATEGORIES[this.categoryIndex]);
                return;
            }
            this.clickAndWait(categorySlot, State.SCAN_SERVERS);
            return;
        }
        this.selectedServer = this.findLeastPopulatedServer();
        if (this.selectedServer == null) {
            this.fail("серверов не найдено");
            return;
        }
        int selectedCategorySlot = this.findCategorySlot(COMMAND_CATEGORIES[this.selectedServer.categoryIndex()]);
        if (selectedCategorySlot < 0) {
            this.fail("нет вкладки команды для захода");
            return;
        }
        this.clickAndWait(selectedCategorySlot, State.FIND_SELECTED_SERVER);
    }

    private Integer readPopulation(ItemStack stack) {
        LoreComponent lore = (LoreComponent)stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return null;
        }
        for (Text line : lore.lines()) {
            Matcher matcher = this.populationPattern.matcher(line.getString());
            if (!matcher.find()) continue;
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private void collectCurrentPage() {
        ScreenHandler handler = this.currentHandler();
        if (handler == null) {
            return;
        }
        for (Slot slot : handler.slots) {
            ItemStack stack;
            String name;
            Integer population;
            if (!slot.hasStack() || (population = (name = (stack = slot.getStack()).getName().getString()).contains("Анархия-") ? this.readPopulation(stack) : null) == null || !this.discoveredNames.add(name)) continue;
            this.discoveredServers.add(new ServerPopulationEntry(name, population, this.categoryIndex));
        }
    }

    private boolean containsAnarchyServers() {
        ScreenHandler handler = this.currentHandler();
        if (handler == null) {
            return false;
        }
        for (Slot slot : handler.slots) {
            if (!slot.hasStack() || !slot.getStack().getName().getString().contains("Анархия-") || this.readPopulation(slot.getStack()) == null) continue;
            return true;
        }
        return false;
    }

    private int findExactSlot(String name) {
        ScreenHandler handler = this.currentHandler();
        if (handler == null) {
            return -1;
        }
        for (Slot slot : handler.slots) {
            if (!slot.hasStack() || !slot.getStack().getName().getString().equals(name)) continue;
            return slot.id;
        }
        return -1;
    }

    private int findSlotContaining(String text) {
        ScreenHandler handler = this.currentHandler();
        if (handler == null) {
            return -1;
        }
        for (Slot slot : handler.slots) {
            if (!slot.hasStack() || !slot.getStack().getName().getString().contains(text)) continue;
            return slot.id;
        }
        return -1;
    }

    private int findCategorySlot(int category) {
        ScreenHandler handler = this.currentHandler();
        if (handler == null) {
            return -1;
        }
        for (Slot slot : handler.slots) {
            String name;
            if (!slot.hasStack() || !(name = slot.getStack().getName().getString()).contains("Команды") || this.lastNumber(name) != category) continue;
            return slot.id;
        }
        return -1;
    }

    private int lastNumber(String text) {
        Matcher matcher = this.numberPattern.matcher(text);
        int value = -1;
        while (matcher.find()) {
            value = Integer.parseInt(matcher.group(1));
        }
        return value;
    }

    private void clickAndWait(int slot, State nextState) {
        ScreenHandler handler = this.currentHandler();
        this.expectedSyncId = handler == null ? -1 : handler.syncId;
        this.stateAfterMenuChange = nextState;
        this.menuChangeTicks = 0;
        this.menuTicks = 0;
        this.clickSlot(slot);
        this.state = State.WAIT_FOR_MENU;
    }

    private void awaitMenuChange() {
        ScreenHandler handler = this.currentHandler();
        if (handler != null && handler.syncId != this.expectedSyncId) {
            this.state = this.stateAfterMenuChange;
            this.menuChangeTicks = 0;
            this.menuTicks = 0;
            return;
        }
        if (++this.menuChangeTicks > 60) {
            this.fail("окно не пришло");
        }
    }

    private void clickSlot(int slot) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ScreenHandler handler = this.currentHandler();
        if (player != null && handler != null && client.interactionManager != null) {
            client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, (PlayerEntity)player);
        }
    }

    private ServerPopulationEntry findLeastPopulatedServer() {
        ServerPopulationEntry best = null;
        for (ServerPopulationEntry entry : this.discoveredServers) {
            if (best != null && entry.onlinePlayers() >= best.onlinePlayers()) continue;
            best = entry;
        }
        return best;
    }

    private String currentTitle() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.currentScreen == null ? "" : client.currentScreen.getTitle().getString();
    }

    private ScreenHandler currentHandler() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player == null ? null : player.currentScreenHandler;
    }

    private void fail(String reason) {
        ClientChat.send("Пустая анархия: " + reason);
        this.finish();
    }

    private void finish() {
        this.state = State.IDLE;
        this.resetProgress();
    }

    private void resetProgress() {
        this.discoveredServers.clear();
        this.discoveredNames.clear();
        this.selectedServer = null;
        this.stateAfterMenuChange = null;
        this.elapsedTicks = 0;
        this.menuTicks = 0;
        this.page = 0;
        this.categoryIndex = 0;
        this.expectedSyncId = -1;
        this.menuChangeTicks = 0;
    }

    private static enum State {
        IDLE,
        OPEN_VERSION,
        WAIT_FOR_MENU,
        SCAN_SERVERS,
        FIND_SELECTED_SERVER;

    }
}

