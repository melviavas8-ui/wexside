package ru.wexside.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.item.ItemBadge;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.NameTagSettings;
import ru.wexside.render.BakedItemIcon;
import ru.wexside.render.ItemIconCache;
import ru.wexside.render.OffscreenRenderManager;
import ru.wexside.ui.ColoredTextSegment;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.MsdfFontRenderer;

final class EntityEquipmentOverlayRenderer {
    private static final int slot = -1;
    private static final int slot2 = -5592406;
    private static final int slot3 = -1728053248;
    private static final MsdfFontRenderer font5 = FontRegistry.font2;
    private final List<BakedItemIcon> itemIcons;
    private static final float value = 9.0f;
    private static final float value2 = 11.0f;
    private static final int slot4 = -872415232;
    private final ItemIconCache itemIconCache = new ItemIconCache();
    private static final int slot5 = -43691;
    private static final float value4 = 6.25f;
    private static final float value5 = 10.0f;
    private static final int slot6 = -256;
    private static final float value6 = 7.0f;
    private List<ItemStack> values3;

    void member5307() {
        this.itemIconCache.disposeCache();
        this.itemIcons.clear();
        this.values3 = List.of();
    }

    EntityEquipmentOverlayRenderer() {
        this.itemIcons = new ArrayList<BakedItemIcon>();
        this.values3 = List.of();
    }

    private static boolean JsonElement(ItemStack stack) {
        BlockItem blockItem2;
        Item iiIilIIilI2 = stack.getItem();
        boolean bl = iiIilIIilI2 instanceof BlockItem && (blockItem2 = (BlockItem)iiIilIIilI2).getBlock() instanceof AbstractSkullBlock;
        return bl || stack.getItem() == Items.TOTEM_OF_UNDYING;
    }

    void member11505(List<ItemStack> list, float f) {
        OffscreenRenderManager renderPipeline2;
        this.values3 = list == null ? List.of() : list;
        this.itemIcons.clear();
        if (this.values3.isEmpty()) {
            return;
        }
        this.itemIconCache.advanceFrame();
        for (ItemStack object2 : this.values3) {
            this.itemIcons.add(this.itemIconCache.getOrCreateIcon(object2));
        }
        ArrayList<BakedIconEntry> arrayList = new ArrayList<BakedIconEntry>();
        this.itemIconCache.appendPendingBakes(f, arrayList);
        if (!arrayList.isEmpty() && (renderPipeline2 = WexSideClient.getOffscreenRenderManager()) != null) {
            renderPipeline2.bakeIcons(arrayList);
        }
    }

    private void renderItemBadge(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, ItemBadge itemBadge) {
        Text iIiilliIII2 = itemBadge.label();
        float f3 = EntityEquipmentOverlayRenderer.measureStyledTextWidth(iIiilliIII2, 6.25f);
        float f4 = font5.textHeight(iIiilliIII2.getString(), 6.25f);
        float f5 = f2 - 2.0f - f4;
        float f6 = f - f3 * 0.5f;
        drawApi.fillRectangle(matrix4f, f6 - 1.5f, f5 - 1.0f, f3 + 3.0f, f4 + 2.0f, -1728053248);
        float[] fArray = new float[]{f6};
        iIiilliIII2.visit((lIiIilIIII2, string) -> {
            TextColor illiiiIilI2 = lIiIilIIII2.getColor();
            int n = 0xFF000000 | (illiiiIilI2 != null ? illiiiIilI2.getRgb() : 0xFFFFFF);
            font5.drawText(matrix4f, drawApi, string, fArray[0], f5, 6.25f, n);
            fArray[0] = fArray[0] + font5.textWidth(string, 6.25f);
            return Optional.empty();
        }, Style.EMPTY);
    }

    private void renderEquipmentIcons(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2) {
        float f3;
        int n;
        int n2 = this.itemIcons.size();
        for (n = 0; n < n2; ++n) {
            f3 = f + ((float)n - (float)(n2 - 1) / 2.0f) * 11.0f;
            boolean bl = EntityEquipmentOverlayRenderer.JsonElement(this.values3.get(n));
            drawApi.fillRectangle(matrix4f, f3 - 5.0f, f2, 10.0f, 10.0f, bl ? -872415232 : -1728053248);
        }
        for (n = 0; n < n2; ++n) {
            f3 = f + ((float)n - (float)(n2 - 1) / 2.0f) * 11.0f;
            this.itemIconCache.drawBakedIcon(drawApi, matrix4f, this.itemIcons.get(n), f3 - 4.5f, f2 + 0.5f, 9.0f);
        }
    }

    void member2669(GuiDrawApi drawApi, Matrix4f matrix4f, GuiBounds bounds2, NameTagSettings talisman, String string, int n, int n2, ItemBadge itemBadge, String string2) {
        String string3 = string == null || string.isEmpty() ? "?" : string;
        ArrayList<ColoredTextSegment> arrayList = new ArrayList<ColoredTextSegment>();
        arrayList.add(new ColoredTextSegment(string3, -1));
        if (talisman.isHealthVisible() && n >= 0) {
            arrayList.add(new ColoredTextSegment(" ", -1));
            EntityEquipmentOverlayRenderer.appendSegment(arrayList, -43691, String.valueOf(n));
        }
        if (talisman.isSphereVisible() && string2 != null && !string2.isEmpty()) {
            arrayList.add(new ColoredTextSegment(" ", -1));
            EntityEquipmentOverlayRenderer.appendSegment(arrayList, -43691, string2);
        }
        if (talisman.isMoneyVisible() && n2 != 0) {
            arrayList.add(new ColoredTextSegment(" ", -1));
            int n3 = n2;
            EntityEquipmentOverlayRenderer.appendSegment(arrayList, -256, "~" + n3 + "$");
        }
        float f = 0.0f;
        for (ColoredTextSegment segment : arrayList) {
            f += font5.textWidth(segment.text(), 7.0f);
        }
        float f2 = font5.textHeight(string3, 7.0f);
        float f3 = bounds2.getX() + bounds2.getWidth() * 0.5f;
        float f4 = f3 - f * 0.5f;
        float f5 = bounds2.getY() - 4.0f - f2;
        drawApi.fillRectangle(matrix4f, f4 - 1.5f, f5 - 1.0f, f + 3.0f, f2 + 2.0f, -1728053248);
        float f6 = f4;
        for (ColoredTextSegment segment : arrayList) {
            font5.drawText(matrix4f, drawApi, segment.text(), f6, f5, 7.0f, segment.color());
            f6 += font5.textWidth(segment.text(), 7.0f);
        }
        float f7 = f5;
        if (talisman.areItemsVisible() && !this.itemIcons.isEmpty()) {
            float f8 = f7 - 2.0f - 10.0f;
            this.renderEquipmentIcons(drawApi, matrix4f, f3, f8);
            f7 = f8;
        }
        if (talisman.isTalismanVisible() && itemBadge != null) {
            this.renderItemBadge(drawApi, matrix4f, f3, f7, itemBadge);
        }
        this.itemIconCache.trimCache();
    }

    private static void appendSegment(List<ColoredTextSegment> list, int n, String string) {
        list.add(new ColoredTextSegment("[", -5592406));
        list.add(new ColoredTextSegment(string, n));
        list.add(new ColoredTextSegment("]", -5592406));
    }

    private static float measureStyledTextWidth(Text iIiilliIII2, float f) {
        float[] fArray = new float[]{0.0f};
        iIiilliIII2.visit((lIiIilIIII2, string) -> {
            fArray[0] = fArray[0] + font5.textWidth(string, f);
            return Optional.empty();
        }, Style.EMPTY);
        return fArray[0];
    }
}

