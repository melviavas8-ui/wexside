package ru.wexside.misc;

import ru.wexside.misc.ContainerDisplay;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.RangeSetting;
import ru.wexside.setting.Setting;
import ru.wexside.setting.TextSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.setting.SettingRow;
import ru.wexside.util.BindSettingRow;
import ru.wexside.util.BooleanSettingRow;
import ru.wexside.util.ColorSettingRow;
import ru.wexside.util.ModeSettingRow;
import ru.wexside.util.MultiSelectSettingRow;
import ru.wexside.util.NumberSettingRow;
import ru.wexside.util.RangeSettingRow;
import ru.wexside.util.TextSettingRow;

public final class SettingRowFactory {
    public static SettingRow<?> createRow(Setting setting, ContainerDisplay containerDisplay) {
        if (setting instanceof BindSetting) {
            BindSetting bindSetting = (BindSetting)setting;
            return new BindSettingRow(new GuiBounds(0.0f, 0.0f, 0.0f, 11.0f), bindSetting, containerDisplay);
        }
        if (setting instanceof BooleanSetting) {
            BooleanSetting booleanSetting = (BooleanSetting)setting;
            return new BooleanSettingRow(new GuiBounds(0.0f, 0.0f, 0.0f, 11.0f), booleanSetting, containerDisplay);
        }
        if (setting instanceof ColorSetting) {
            ColorSetting colorSetting = (ColorSetting)setting;
            return new ColorSettingRow(new GuiBounds(0.0f, 0.0f, 0.0f, 11.0f), colorSetting, containerDisplay);
        }
        if (setting instanceof TextSetting) {
            TextSetting textSetting = (TextSetting)setting;
            return new TextSettingRow(new GuiBounds(0.0f, 0.0f, 0.0f, 11.0f), textSetting, containerDisplay);
        }
        if (setting instanceof ModeSetting) {
            ModeSetting modeSetting = (ModeSetting)setting;
            return new ModeSettingRow(new GuiBounds(0.0f, 0.0f, 0.0f, 11.0f), modeSetting, containerDisplay);
        }
        if (setting instanceof MultiSelectSetting) {
            MultiSelectSetting multiSelectSetting = (MultiSelectSetting)setting;
            return new MultiSelectSettingRow(new GuiBounds(0.0f, 0.0f, 0.0f, 11.0f), multiSelectSetting, containerDisplay);
        }
        if (setting instanceof NumberSetting) {
            NumberSetting numberSetting = (NumberSetting)setting;
            return new NumberSettingRow(new GuiBounds(0.0f, 0.0f, 0.0f, 11.0f), numberSetting, containerDisplay);
        }
        if (setting instanceof RangeSetting) {
            RangeSetting rangeSetting = (RangeSetting)setting;
            return new RangeSettingRow(new GuiBounds(0.0f, 0.0f, 0.0f, 11.0f), rangeSetting, containerDisplay);
        }
        return null;
    }
}

