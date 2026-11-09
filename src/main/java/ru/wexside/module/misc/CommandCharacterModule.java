package ru.wexside.module.misc;

import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.TextSetting;
import ru.wexside.setting.TextSettingBuilder;

public final class CommandCharacterModule
extends Module
implements ConfigSerializable {
    private static final String DEFAULT = ".";
    private static volatile CommandCharacterModule instance;
    private final TextSetting character;

    public CommandCharacterModule(EventBus eventBus) {
        super(eventBus, "command_character", "Command Character", "Свой символ для команд клиента", ModuleCategory.valueOf("MISC"), new String[0]);
        instance = this;
        this.character = ((TextSettingBuilder)TextSetting.getTextSettingBuilder().value(DEFAULT).maxLength(1).name("Character").id("character").description("Символ, с которого начинаются команды клиента")).build();
        this.registerSetting(this.character);
    }

    @Override
    protected void initialize() {
    }

    public static String getString() {
        CommandCharacterModule module = instance;
        if (module == null) {
            return DEFAULT;
        }
        String value = module.character.getValue();
        return value == null || value.isBlank() ? DEFAULT : value;
    }
}

