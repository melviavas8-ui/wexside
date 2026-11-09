package ru.wexside.module.player;

import net.minecraft.client.MinecraftClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.TextSetting;
import ru.wexside.setting.TextSettingBuilder;

public final class NameProtectModule
extends Module
implements ConfigSerializable {
    private static volatile NameProtectModule instance;
    private final BooleanSetting enabledSetting;
    private final TextSetting fakeName;

    public NameProtectModule(EventBus eventBus) {
        super(eventBus, "name_protect", "Name Protect", "Визуально меняет твой ник во всём тексте", ModuleCategory.valueOf("PLAYER"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Возможность визуально менять ник").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.fakeName = ((TextSettingBuilder)TextSetting.getTextSettingBuilder().value("wexside").maxLength(32).name("Ник").id("fake_name").description("Никнейм, которым заменяется твой настоящий")).build();
        this.registerSetting(this.fakeName);
    }

    @Override
    protected void initialize() {
    }

    public static boolean isEnabled() {
        NameProtectModule module = instance;
        return module != null && module.enabledSetting.isEnabled();
    }

    public static String compute(String text) {
        NameProtectModule module = instance;
        if (module == null || !module.enabledSetting.isEnabled() || text == null || text.isEmpty()) {
            return text;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSession() == null) {
            return text;
        }
        String username = client.getSession().getUsername();
        if (username == null || username.isEmpty() || text.indexOf(username) < 0) {
            return text;
        }
        String replacement = module.fakeName.getValue();
        return text.replace(username, replacement == null ? "" : replacement);
    }

    public static String getString() {
        NameProtectModule module = instance;
        if (module == null) {
            return "";
        }
        String name = module.fakeName.getValue();
        return name == null ? "" : name;
    }
}

