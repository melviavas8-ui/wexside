package ru.wexside.misc;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.FontRegistry;
import ru.wexside.render.BakedItemIcon;
import ru.wexside.render.ItemIconCache;
import ru.wexside.util.AbstractHudElement;
import ru.wexside.util.GuiDrawApi;

public final class TotemCounter
extends AbstractHudElement {
    static final int slot = -1;
    static final float value3 = 12.0f;
    private BakedItemIcon bakedItemIcon;
    static final float value4 = 16.0f;
    private int slot2;
    static final int slot3 = -3670016;
    static final float value5 = 10.0f;
    static final float value6 = 7.0f;
    private final ItemIconCache itemIconCache = new ItemIconCache();
    private final ItemStack stack = new ItemStack((ItemConvertible)Items.TOTEM_OF_UNDYING);

    public TotemCounter(BooleanSupplier booleanSupplier) {
        super("Totem Counter", booleanSupplier);
    }

    @Override
    protected float getWidth() {
        return 16.0f;
    }

    @Override
    protected void updateLayout() {
        this.itemIconCache.disposeCache();
        this.slot2 = this.countTotems();
        this.itemIconCache.advanceFrame();
        this.bakedItemIcon = this.itemIconCache.getOrCreateIcon(this.stack);
        float f = MinecraftClient.getInstance().getWindow().getScaleFactor();
        ArrayList<BakedIconEntry> arrayList = new ArrayList<BakedIconEntry>();
        this.itemIconCache.appendPendingBakes(f, arrayList);
        if (!arrayList.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(arrayList);
        }
    }

    @Override
    protected void renderContent(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5) {
        if (this.bakedItemIcon == null) {
            return;
        }
        this.itemIconCache.drawBakedIcon(drawApi, matrix4f, this.bakedItemIcon, f, f2, 16.0f * f5);
        FontRegistry.font5.drawText(matrix4f, drawApi, String.valueOf(this.slot2), f + 10.0f * f5, f2 + 12.0f * f5, 7.0f * f5, this.slot2 == 0 ? -3670016 : -1);
        this.itemIconCache.trimCache();
    }

    @Override
    protected float getHeight() {
        return 16.0f;
    }

    private int countTotems() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return 0;
        }
        int count = player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) ? player.getOffHandStack().getCount() : 0;
        for (int index = 0; index < player.getInventory().size(); ++index) {
            ItemStack item = player.getInventory().getStack(index);
            if (!item.isOf(Items.TOTEM_OF_UNDYING)) continue;
            count += item.getCount();
        }
        return count;
    }
}

