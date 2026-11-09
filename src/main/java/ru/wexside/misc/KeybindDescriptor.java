package ru.wexside.misc;

import java.util.function.Consumer;
import java.util.function.Supplier;
import ru.wexside.input.BindInput;
import ru.wexside.misc.BindSettingAdapter;
import ru.wexside.misc.DelegatingKeybind;
import ru.wexside.misc.SettingKeybindAdapter;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.SettingKeybind;

public interface KeybindDescriptor {
    public static KeybindDescriptor createDelegatingBinding(String string, String string2, Supplier<BindInput> supplier, Consumer<BindInput> consumer) {
        return new DelegatingKeybind(string, string2, supplier, consumer);
    }

    public String getString();

    public String getString2();

    public static KeybindDescriptor fromBindSetting(BindSetting bindSetting) {
        return new BindSettingAdapter(bindSetting);
    }

    public void setBindInput(BindInput var1);

    public BindInput getBindInput();

    public static KeybindDescriptor fromSettingKeybind(SettingKeybind settingKeybind) {
        return new SettingKeybindAdapter(settingKeybind);
    }

    default public boolean isActive() {
        return !this.getBindInput().isUnbound();
    }
}

