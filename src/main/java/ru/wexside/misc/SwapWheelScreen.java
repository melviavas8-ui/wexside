package ru.wexside.misc;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import ru.wexside.misc.SwapIcon;
import ru.wexside.setting.BindSetting;

public class SwapWheelScreen
extends Screen {
    private static List<ItemStack> items = List.of();
    private final BindSetting bind;
    private final List<BindSetting> binds;
    private final int segments;
    private final Consumer<ItemStack> onSelect;
    private final Consumer<ItemStack> onForget;
    private final Function<ItemStack, SwapIcon> icon;

    public SwapWheelScreen(BindSetting bind, List<BindSetting> binds, int segments, Consumer<ItemStack> onSelect, Consumer<ItemStack> onForget, Function<ItemStack, SwapIcon> icon) {
        super((Text)Text.empty());
        this.bind = bind;
        this.binds = binds;
        this.segments = segments;
        this.onSelect = onSelect;
        this.onForget = onForget;
        this.icon = icon;
    }

    public BindSetting bind() {
        return this.bind;
    }

    public List<BindSetting> binds() {
        return this.binds;
    }

    public int segments() {
        return this.segments;
    }

    public Consumer<ItemStack> onSelect() {
        return this.onSelect;
    }

    public Consumer<ItemStack> onForget() {
        return this.onForget;
    }

    public Function<ItemStack, SwapIcon> icon() {
        return this.icon;
    }

    public static List<ItemStack> items() {
        return items;
    }

    public static ItemStack stackAt(int index) {
        if (index < 0 || index >= items.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(index);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public static int indexAt(int code) {
        return code;
    }
}

