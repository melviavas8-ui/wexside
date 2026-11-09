package ru.wexside.misc;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import ru.wexside.setting.BindSetting;

public final class ItemBindBox {
    private final BindSetting bindSetting;
    private final Predicate<ItemStack> predicate;
    private final BooleanSupplier booleanSupplier;
    private final Item item2;

    public ItemBindBox(Item iiIilIIilI2, BindSetting bindSetting, Predicate<ItemStack> predicate, BooleanSupplier booleanSupplier) {
        this.item2 = iiIilIIilI2;
        this.bindSetting = bindSetting;
        this.predicate = predicate;
        this.booleanSupplier = booleanSupplier;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ItemBindBox)) {
            return false;
        }
        ItemBindBox itemBindBox = (ItemBindBox)object;
        return Objects.equals(this.item2, itemBindBox.item2) && Objects.equals(this.bindSetting, itemBindBox.bindSetting) && Objects.equals(this.predicate, itemBindBox.predicate);
    }

    public String toString() {
        String string = String.valueOf(this.predicate);
        String string2 = String.valueOf(this.bindSetting);
        String string3 = String.valueOf(this.item2);
        return "ItemBindBox[displayIcon=" + string3 + ", key=" + string2 + ", matcher=" + string + "]";
    }

    public int hashCode() {
        return Objects.hash(this.item2, this.bindSetting, this.predicate);
    }

    public BindSetting getBindSetting() {
        return this.bindSetting;
    }

    public boolean isActive() {
        return this.booleanSupplier == null || this.booleanSupplier.getAsBoolean();
    }

    public Item getItem() {
        return this.item2;
    }

    public boolean isActive2() {
        return !this.bindSetting.getBindInput().isUnbound();
    }

    public Predicate<ItemStack> getPredicate() {
        return this.predicate;
    }
}

