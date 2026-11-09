package ru.wexside.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.ArmorSlotRenderer;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.util.AbstractHudElement;
import ru.wexside.util.GuiDrawApi;

public final class ArmorHUD
extends AbstractHudElement {
    private final float value;
    private final List<ArmorSlotRenderer> armorSlots = List.of(new ArmorSlotRenderer(EquipmentSlot.HEAD), new ArmorSlotRenderer(EquipmentSlot.CHEST), new ArmorSlotRenderer(EquipmentSlot.LEGS), new ArmorSlotRenderer(EquipmentSlot.FEET));

    public ArmorHUD(BooleanSupplier booleanSupplier) {
        super("Armor HUD", booleanSupplier);
        this.value = 3.0f;
    }

    @Override
    protected float getWidth() {
        return (float)this.armorSlots.size() * this.armorSlots.get(0).getWidth() + (float)(this.armorSlots.size() - 1) * 3.0f;
    }

    @Override
    protected void updateLayout() {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        for (ArmorSlotRenderer armorSlotRenderer : this.armorSlots) {
            armorSlotRenderer.updateEquipment(player2);
        }
        this.queueArmorIconBakes();
    }

    @Override
    protected void renderContent(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5) {
        float f6 = (this.armorSlots.get(0).getWidth() + 3.0f) * f5;
        for (int i = 0; i < this.armorSlots.size(); ++i) {
            this.armorSlots.get(i).renderSlot(drawApi, matrix4f, f + (float)i * f6, f2, f5);
        }
    }

    private void queueArmorIconBakes() {
        float f = MinecraftClient.getInstance().getWindow().getScaleFactor();
        ArrayList<BakedIconEntry> arrayList = new ArrayList<BakedIconEntry>();
        for (ArmorSlotRenderer armorSlotRenderer : this.armorSlots) {
            BakedIconEntry bakedIconEntry = armorSlotRenderer.createIconBakeEntry(f);
            if (bakedIconEntry == null) continue;
            arrayList.add(bakedIconEntry);
        }
        if (!arrayList.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(arrayList);
        }
    }

    @Override
    protected float getHeight() {
        return this.armorSlots.get(0).getHeight();
    }

    @Override
    protected boolean isContentVisible() {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        for (ArmorSlotRenderer armorSlotRenderer : this.armorSlots) {
            if (!armorSlotRenderer.hasEquippedItem(player2)) continue;
            return true;
        }
        return false;
    }
}

