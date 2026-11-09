package ru.wexside.module.render;

import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.AspectRatioEvent;
import ru.wexside.event.EventBus;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;

public final class AspectRatioModule
extends Module
implements ConfigSerializable {
    private final ModeSetting ratio;
    private final NumberSetting number;
    private final BooleanSetting enabledSetting;
    private static final String string = "Custom";

    private void onGame(AspectRatioEvent gameEvent) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        float f = switch (this.ratio.getSelectedOption()) {
            case "21:9" -> 2.3333333f;
            case "16:10" -> 1.6f;
            case "16:9" -> 1.7777778f;
            case "4:3" -> 1.3333334f;
            default -> this.number.getFloatValue();
        };
        gameEvent.setAspectRatio(f);
    }

    @Override
    protected void initialize() {
        this.listen(AspectRatioEvent.class, this::onGame);
    }

    public AspectRatioModule(EventBus eventBus) {
        super(eventBus, "aspect_ratio", "Aspect Ratio", "Изменение соотношения сторон", ModuleCategory.valueOf("RENDER"), new String[0]);
        NumberSetting numberSetting;
        ModeSetting modeSetting;
        BooleanSetting booleanSetting;
        this.enabledSetting = booleanSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Изменение соотношения сторон").withKeybind().toggle()).build();
        this.registerSetting(booleanSetting);
        this.ratio = modeSetting = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options(string, "21:9", "16:10", "16:9", "4:3").defaultOption(string).name("Ratio").id("ratio").description("Соотношение сторон").aliases("ratio", "соотношение")).build();
        this.registerSetting(modeSetting);
        this.number = numberSetting = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 2.0).defaultValue(1.5).multiplier(1.0).precision(1).animationSpeed(20.0f).name("Value").id("value").description("Значение соотношения").aliases("value", "значение").visibleWhen(() -> string.equals(this.ratio.getSelectedOption()))).build();
        this.registerSetting(numberSetting);
    }
}

