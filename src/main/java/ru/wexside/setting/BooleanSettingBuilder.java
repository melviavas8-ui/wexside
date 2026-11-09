package ru.wexside.setting;

import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.SettingBuilder;

public final class BooleanSettingBuilder
extends SettingBuilder {
    boolean defaultValue;
    boolean initialValue;

    public BooleanSettingBuilder value(boolean value) {
        this.initialValue = value;
        return this;
    }

    public BooleanSetting build() {
        return new BooleanSetting(this);
    }

    public BooleanSettingBuilder defaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    @Override
    protected BooleanSettingBuilder self() {
        return this;
    }
}

