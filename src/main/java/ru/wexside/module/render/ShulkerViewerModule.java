package ru.wexside.module.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.event.ItemHoverEvent;
import ru.wexside.event.TooltipRenderEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.render.ItemIconCache;
import ru.wexside.render.RenderProjection;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.BindSettingBuilder;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.util.GuiDrawApi;

public final class ShulkerViewerModule
extends Module
implements ConfigSerializable {
    private static final float PANEL_WIDTH = 172.0f;
    private static final int DEFAULT_TINT = 9068428;
    private static final int COLUMNS = 9;
    private static volatile ShulkerViewerModule instance;
    private final BooleanSetting enabledSetting;
    private final BooleanSetting onGround;
    private final BooleanSetting onGroundTexture;
    private final BindSetting viewKey;
    private final ItemIconCache icons = new ItemIconCache();
    private ItemStack hoveredShulker;

    public ShulkerViewerModule(EventBus eventBus) {
        super(eventBus, "shulker_viewer", "Shulker Viewer", "Просмотр содержимого шалкера ", ModuleCategory.valueOf("RENDER"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Включить просмотр содержимого шалкеров").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.onGround = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("OnGround").id("on_ground").description("Предпросмотр шалкеров, лежащих в мире")).build();
        this.registerSetting(this.onGround);
        this.onGroundTexture = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Texture").id("on_ground_texture").description("Текстурная рамка предпросмотра на земле").aliases("texture", "текстура").visibleWhen(this.onGround::isEnabled)).build();
        this.registerSetting(this.onGroundTexture);
        this.viewKey = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).name("View key").id("view_key").description("Удерживай клавишу, чтобы увидеть содержимое наведённого шалкера").aliases("view key", "клавиша просмотра")).build();
        this.registerSetting(this.viewKey);
    }

    @Override
    protected void initialize() {
        this.listen(ItemHoverEvent.class, this::onItemHover);
        this.listen(TooltipRenderEvent.class, this::onTooltip);
        this.listen(HudRenderEvent.class, this::onHudRender);
        this.listen(WorldSessionEvent.class, event -> this.icons.disposeCache());
    }

    public static boolean compute(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Identifier id = Registries.ITEM.getId(stack.getItem());
        String path = id == null ? "" : id.getPath();
        return path.contains("shulker_box");
    }

    public static String getString() {
        ShulkerViewerModule module = instance;
        if (module == null || !module.enabledSetting.isEnabled() || module.viewKey.getBindInput().isUnbound()) {
            return null;
        }
        return module.viewKey.getKeyDisplayName();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void onHudRender(HudRenderEvent event) {
        if (!this.enabledSetting.isEnabled() || !this.onGround.isEnabled()) {
            this.icons.disposeCache();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
        if (client.world == null || client.player == null || cameraPos == null) {
            return;
        }
        ArrayList<GroundPreview> previews = new ArrayList<GroundPreview>();
        for (Entity entity : client.world.getEntities()) {
            Vector2f screen;
            List<ItemStack> contents;
            ContainerComponent container;
            ItemEntity itemEntity;
            ItemStack stack;
            DyeColor color;
            if (!(entity instanceof ItemEntity)) continue;
            itemEntity = (ItemEntity)entity;
            stack = itemEntity.getStack();
            color = this.shulkerColor(stack);
            if (color == null && !this.isShulkerBlock(stack)) continue;
            if (cameraPos.squaredDistanceTo(entity.getEntityPos()) > 1024.0) continue;
            container = (ContainerComponent)stack.get(DataComponentTypes.CONTAINER);
            if (container == null) continue;
            contents = container.stream().toList();
            if (contents.isEmpty()) continue;
            screen = this.worldToScreen(entity.getX(), entity.getY() + (double)entity.getHeight() / 2.0, entity.getZ());
            if (screen == null) continue;
            previews.add(new GroundPreview(screen.x, screen.y, this.brighten(color), contents));
        }
        if (previews.isEmpty()) {
            return;
        }
        float scale = client.getWindow().getScaleFactor();
        this.icons.advanceFrame();
        for (GroundPreview preview : previews) {
            for (ItemStack stack : preview.stacks) {
                preview.icons.add(stack.isEmpty() ? null : this.icons.getOrCreateIcon(stack));
            }
        }
        ArrayList<BakedIconEntry> commands = new ArrayList<BakedIconEntry>();
        this.icons.appendPendingBakes(scale, commands);
        if (!commands.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(commands);
        }
        GuiDrawApi renderer = WexSideClient.getHudRenderer();
        Matrix4f matrix = new Matrix4f().scale(scale);
        renderer.begin();
        try {
            for (GroundPreview preview : previews) {
                this.drawPanel(renderer, matrix, preview);
            }
            for (GroundPreview preview : previews) {
                this.drawIcons(renderer, matrix, preview);
            }
        }
        finally {
            renderer.end();
        }
        this.icons.trimCache();
    }

    private void onItemHover(ItemHoverEvent event) {
        ItemStack stack = event.getStack();
        if (!this.enabledSetting.isEnabled() || !ShulkerViewerModule.compute(stack)) {
            return;
        }
        this.hoveredShulker = stack;
        if (this.viewKey.isPressed()) {
            event.update();
        }
    }

    private void onTooltip(TooltipRenderEvent event) {
        ItemStack stack = this.hoveredShulker;
        this.hoveredShulker = null;
        if (!this.enabledSetting.isEnabled() || stack == null || !this.viewKey.isPressed()) {
            return;
        }
        ContainerComponent container = (ContainerComponent)stack.get(DataComponentTypes.CONTAINER);
        if (container == null) {
            return;
        }
        this.drawTooltip(event.getDrawContext(), event.getMouseX(), event.getMouseY(), stack, container.stream().toList());
    }

    private void drawTooltip(DrawContext context, int mouseX, int mouseY, ItemStack stack, List<ItemStack> contents) {
        MinecraftClient client = MinecraftClient.getInstance();
        int rows = Math.max(3, (contents.size() + 9 - 1) / 9);
        int slots = rows * 9;
        int width = 174;
        int height = rows * 18 + 12;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int x = mouseX + 8;
        int y = mouseY + 8;
        if (x + width > screenWidth) {
            x = mouseX - 8 - width;
        }
        if (y + height > screenHeight) {
            y = Math.max(0, screenHeight - height);
        }
        int tint = this.itemTint(stack);
        int fill = 0xE6000000 | this.scaleRgb(tint, 0.35f);
        int slotFill = 0x33FFFFFF;
        context.fill(x, y, x + width, y + height, fill);
        context.fill(x, y, x + width, y + 1, 0xFF000000 | tint);
        context.fill(x, y + height - 1, x + width, y + height, 0xFF000000 | tint);
        context.fill(x, y, x + 1, y + height, 0xFF000000 | tint);
        context.fill(x + width - 1, y, x + width, y + height, 0xFF000000 | tint);
        for (int i = 0; i < slots; ++i) {
            ItemStack item;
            int slotX = x + 6 + i % 9 * 18;
            int slotY = y + 6 + i / 9 * 18;
            context.fill(slotX, slotY, slotX + 16, slotY + 16, slotFill);
            if (i >= contents.size() || (item = contents.get(i)).isEmpty()) continue;
            context.drawItem(item, slotX, slotY);
            context.drawStackOverlay(client.textRenderer, item, slotX, slotY);
        }
    }

    private void drawPanel(GuiDrawApi renderer, Matrix4f matrix, GroundPreview preview) {
        int rows = Math.max(3, (preview.stacks.size() + 9 - 1) / 9);
        float x = preview.x - 86.0f;
        float y = preview.y + 6.0f;
        float height = rows * 18 + 10;
        renderer.fillRectangle(matrix, x, y, 172.0f, height, -1072689136);
        int border = 0xFF000000 | preview.tint & 0xFFFFFF;
        renderer.fillRectangle(matrix, x, y, 172.0f, 1.0f, border);
        renderer.fillRectangle(matrix, x, y + height - 1.0f, 172.0f, 1.0f, border);
        renderer.fillRectangle(matrix, x, y, 1.0f, height, border);
        renderer.fillRectangle(matrix, x + 172.0f - 1.0f, y, 1.0f, height, border);
        for (int row = 0; row < rows; ++row) {
            for (int column = 0; column < 9; ++column) {
                renderer.fillRectangle(matrix, x + 6.0f + (float)(18 * column), y + 6.0f + (float)(18 * row), 16.0f, 16.0f, 0x33FFFFFF);
            }
        }
    }

    private void drawIcons(GuiDrawApi renderer, Matrix4f matrix, GroundPreview preview) {
        float x = preview.x - 86.0f;
        float y = preview.y + 6.0f;
        for (int i = 0; i < preview.stacks.size(); ++i) {
            Object icon = preview.icons.get(i);
            if (icon == null) continue;
            float slotX = x + 6.0f + (float)(18 * (i % 9));
            float slotY = y + 6.0f + (float)(18 * (i / 9));
            this.icons.drawBakedIcon(renderer, matrix, icon, slotX, slotY, 16.0f);
        }
    }

    private DyeColor shulkerColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (!(item instanceof BlockItem)) {
            return null;
        }
        BlockItem blockItem = (BlockItem)item;
        Block class_22482 = blockItem.getBlock();
        if (!(class_22482 instanceof ShulkerBoxBlock)) {
            return null;
        }
        ShulkerBoxBlock shulker = (ShulkerBoxBlock)class_22482;
        return shulker.getColor();
    }

    private boolean isShulkerBlock(ItemStack stack) {
        BlockItem blockItem;
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof BlockItem && (blockItem = (BlockItem)item).getBlock() instanceof ShulkerBoxBlock;
    }

    private int itemTint(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        String path = id == null ? "" : id.getPath();
        for (DyeColor color : DyeColor.values()) {
            if (!path.startsWith(color.asString() + "_")) continue;
            return color.getSignColor() & 0xFFFFFF;
        }
        return 9068428;
    }

    private int brighten(DyeColor color) {
        int rgb = color != null ? color.getEntityColor() & 0xFFFFFF : 9068428;
        int red = Math.min(255, (rgb >> 16 & 0xFF) + 38);
        int green = Math.min(255, (rgb >> 8 & 0xFF) + 38);
        int blue = Math.min(255, (rgb & 0xFF) + 38);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private int scaleRgb(int rgb, float factor) {
        int red = Math.round((float)(rgb >> 16 & 0xFF) * factor);
        int green = Math.round((float)(rgb >> 8 & 0xFF) * factor);
        int blue = Math.round((float)(rgb & 0xFF) * factor);
        return red << 16 | green << 8 | blue;
    }

    private Vector2f worldToScreen(double x, double y, double z) {
        return RenderProjection.project(new Vec3d(x, y, z));
    }

    private static final class GroundPreview {
        final float x;
        final float y;
        final int tint;
        final List<ItemStack> stacks;
        final List<Object> icons = new ArrayList<Object>();

        GroundPreview(float x, float y, int tint, List<ItemStack> contents) {
            this.x = x;
            this.y = y;
            this.tint = tint;
            this.stacks = contents;
        }
    }
}

