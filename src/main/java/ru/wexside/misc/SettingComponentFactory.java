package ru.wexside.misc;

import ru.wexside.misc.BindSettingComponent;
import ru.wexside.misc.BooleanSettingComponent;
import ru.wexside.misc.ColorSettingComponent;
import ru.wexside.misc.CompactTextFieldStyle;
import ru.wexside.misc.ExpandedTextFieldStyle;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.RangeSetting;
import ru.wexside.setting.Setting;
import ru.wexside.setting.TextSetting;
import ru.wexside.ui.setting.SettingComponent;
import ru.wexside.util.ModeSettingComponent;
import ru.wexside.util.MultiSelectSettingComponent;
import ru.wexside.util.NumberSettingComponent;
import ru.wexside.util.RangeSettingComponent;
import ru.wexside.util.TextSettingComponent;

public final class SettingComponentFactory {
    public static SettingComponent<?> createComponent(Setting setting) {
        if (setting instanceof BindSetting) {
            BindSetting bindSetting = (BindSetting)setting;
            return new BindSettingComponent(bindSetting);
        }
        if (setting instanceof BooleanSetting) {
            BooleanSetting booleanSetting = (BooleanSetting)setting;
            return new BooleanSettingComponent(booleanSetting);
        }
        if (setting instanceof ColorSetting) {
            ColorSetting colorSetting = (ColorSetting)setting;
            return new ColorSettingComponent(colorSetting);
        }
        if (setting instanceof TextSetting) {
            TextSetting textSetting = (TextSetting)setting;
            return new TextSettingComponent(textSetting, textSetting.isExpanded() ? new ExpandedTextFieldStyle() : new CompactTextFieldStyle());
        }
        if (setting instanceof ModeSetting) {
            ModeSetting modeSetting = (ModeSetting)setting;
            return new ModeSettingComponent(modeSetting);
        }
        if (setting instanceof MultiSelectSetting) {
            MultiSelectSetting multiSelectSetting = (MultiSelectSetting)setting;
            return new MultiSelectSettingComponent(multiSelectSetting);
        }
        if (setting instanceof NumberSetting) {
            NumberSetting numberSetting = (NumberSetting)setting;
            return new NumberSettingComponent(numberSetting);
        }
        if (setting instanceof RangeSetting) {
            RangeSetting rangeSetting = (RangeSetting)setting;
            return new RangeSettingComponent(rangeSetting);
        }
        return null;
    }
}

