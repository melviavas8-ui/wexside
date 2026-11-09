package ru.wexside.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.ItemStackRenderer;
import ru.wexside.misc.ThemeColors;
import ru.wexside.util.AbstractHudElement;
import ru.wexside.util.GuiDrawApi;

public final class InventoryHUD
extends AbstractHudElement {
    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final float WIDTH = 210.0f;
    private static final float HEIGHT = 90.0f;
    private final List<ItemStackRenderer> slots = new ArrayList<ItemStackRenderer>(27);

    public InventoryHUD(BooleanSupplier visible) {
        super("Inventory HUD", visible);
        for (int index = 0; index < 27; ++index) {
            this.slots.add(new ItemStackRenderer());
        }
    }

    @Override
    protected float getWidth() {
        return 210.0f;
    }

    @Override
    protected float getHeight() {
        return 90.0f;
    }

    @Override
    protected void updateLayout() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        for (int index = 0; index < this.slots.size(); ++index) {
            ItemStack stack = player == null ? ItemStack.EMPTY : player.getInventory().getStack(9 + index);
            this.slots.get(index).setStack(stack);
        }
        this.collectIconBakes();
    }

    @Override
    protected void renderContent(GuiDrawApi renderer, Matrix4f matrix, float x, float y, float width, float height, float scale) {
        int color = ThemeColors.hudTextPrimary();
        this.renderPanelSurface(renderer, matrix, x, y, width, height, 8.0f, scale);
        renderer.beginStencil(1);
        renderer.drawRoundedRectangle(matrix, x, y, width, height, 8.0f * scale, -1);
        renderer.applyStencilMask(1);
        FontRegistry.font7.drawText(matrix, renderer, "Inventory", x + 8.0f * scale, y + 4.5f * scale, 8.0f * scale, color);
        FontRegistry.font3.drawGlyphText(matrix, renderer, "Й", x + width - 19.0f * scale, y + 6.0f * scale, 8.0f * scale, color);
        float slotWidth = this.slots.get(0).getWidth();
        float slotHeight = this.slots.get(0).getHeight();
        float columnStep = (slotWidth + 3.06f) * scale;
        float rowStep = (slotHeight + 3.5f) * scale;
        for (int index = 0; index < this.slots.size(); ++index) {
            float slotX = x + 5.0f * scale + (float)(index % 9) * columnStep;
            float slotY = y + 21.0f * scale + (float)(index / 9) * rowStep;
            this.slots.get(index).renderItemSlot(renderer, matrix, slotX, slotY, scale);
        }
        renderer.endStencil();
    }

    private void collectIconBakes() {
        float framebufferScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        ArrayList<BakedIconEntry> bakes = new ArrayList<BakedIconEntry>();
        for (ItemStackRenderer slot : this.slots) {
            BakedIconEntry bake = slot.createIconBakeEntry(framebufferScale);
            if (bake == null) continue;
            bakes.add(bake);
        }
        if (!bakes.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(bakes);
        }
    }
}

