package ru.wexside.module.hud;

import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.HotbarSlotRenderer;
import ru.wexside.misc.ThemeColors;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.render.BakedItemIcon;
import ru.wexside.render.ItemIconCache;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public class HotbarModule
extends Module
implements ConfigSerializable {
    private static final float HOTBAR_WIDTH = 180.0f;
    private static final float SLOT_SIZE = 20.0f;
    private static final float ICON_SIZE = 16.0f;
    private static final float SLOT_DEPTH = 10.0f;
    private static final float BOTTOM_MARGIN = 3.0f;
    private static final float HOTBAR_HEIGHT = 20.0f;
    private static final float OFFHAND_GAP = 4.0f;
    private static final int HOTBAR_SLOTS = 9;
    static volatile HotbarModule hotbarModule2;
    private final BooleanSetting enabledSetting;
    private final HotbarSlotRenderer offhandRenderer;
    private final ItemIconCache iconCache;
    private final HotbarSlotRenderer[] slotRenderers;

    public HotbarModule(EventBus eventBus) {
        super(eventBus, "hotbar", "Hotbar", "Кастомный хотбар", ModuleCategory.valueOf("DISPLAY"), new String[0]);
        hotbarModule2 = this;
        this.iconCache = new ItemIconCache();
        this.offhandRenderer = new HotbarSlotRenderer(0);
        this.slotRenderers = new HotbarSlotRenderer[9];
        for (int slot = 0; slot < 9; ++slot) {
            this.slotRenderers[slot] = new HotbarSlotRenderer(slot);
        }
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Кастомный хотбар").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
        this.listen(HudRenderEvent.class, event -> this.render());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void render() {
        if (!HotbarModule.isEnabled()) {
            return;
        }
        ClientPlayerEntity player = HotbarModule.getPlayer();
        GuiDrawApi renderer = WexSideClient.getHudRenderer();
        if (player == null || renderer == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        ItemStack offhandStack = player.getOffHandStack();
        boolean hasOffhand = !offhandStack.isEmpty();
        Hand activeHand = player.isUsingItem() ? player.getActiveHand() : null;
        Window window = MinecraftClient.getInstance().getWindow();
        float hotbarX = (float)window.getScaledWidth() / 2.0f - 90.0f;
        float hotbarY = (float)window.getScaledHeight() - 3.0f - 20.0f;
        boolean offhandOnLeft = player.getMainArm() == Arm.RIGHT;
        float offhandX = offhandOnLeft ? hotbarX - 4.0f - 20.0f : hotbarX + 180.0f + 4.0f;
        this.iconCache.advanceFrame();
        BakedItemIcon[] slotIcons = new BakedItemIcon[9];
        for (int slot = 0; slot < 9; ++slot) {
            slotIcons[slot] = this.iconCache.getOrCreateIcon(inventory.getStack(slot));
        }
        BakedItemIcon offhandIcon = hasOffhand ? this.iconCache.getOrCreateIcon(offhandStack) : null;
        ArrayList<BakedIconEntry> queuedDraws = new ArrayList<BakedIconEntry>();
        this.iconCache.appendPendingBakes(2.0f, queuedDraws);
        if (!queuedDraws.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(queuedDraws);
        }
        Matrix4f matrix = new Matrix4f().scale(2.0f);
        renderer.begin();
        try {
            this.drawSlotBackground(renderer, matrix, hotbarX, hotbarY, 180.0f);
            for (int slot = 0; slot < 9; ++slot) {
                float slotX = hotbarX + (float)slot * 20.0f;
                this.slotRenderers[slot].renderSlot(renderer, matrix, this.iconCache, slotIcons[slot], inventory.getStack(slot), slotX, hotbarY, 20.0f, 16.0f, 10.0f, slot == selectedSlot, activeHand == Hand.MAIN_HAND && slot == selectedSlot);
            }
            if (hasOffhand) {
                this.drawSlotBackground(renderer, matrix, offhandX, hotbarY, 20.0f);
                this.offhandRenderer.renderSlot(renderer, matrix, this.iconCache, offhandIcon, offhandStack, offhandX, hotbarY, 20.0f, 16.0f, 10.0f, false, activeHand == Hand.OFF_HAND);
            }
        }
        finally {
            renderer.end();
            this.iconCache.trimCache();
        }
    }

    public static boolean isEnabled() {
        HotbarModule module = hotbarModule2;
        if (module == null || !module.enabledSetting.isEnabled()) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        GameOptions options = client.options;
        if (options == null || options.hudHidden) {
            return false;
        }
        if (client.interactionManager != null && client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        return HotbarModule.getPlayer() != null;
    }

    public static float getFloatType() {
        if (!HotbarModule.isEnabled()) {
            return 0.0f;
        }
        float scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        return 46.0f / scale - 22.0f;
    }

    private static ClientPlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }

    private void drawSlotBackground(GuiDrawApi renderer, Matrix4f matrix, float x, float y, float width) {
        renderer.drawRoundedRectangle(matrix, x, y, width, 20.0f, 10.0f, ColorUtils.multiplyAlpha(ThemeColors.visualizerSlot(), 1.0f));
    }
}

