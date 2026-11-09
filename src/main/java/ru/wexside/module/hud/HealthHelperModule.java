package ru.wexside.module.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.misc.SlotHighlight;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.ColorSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.ContainerScreenHelper;

public final class HealthHelperModule
extends Module
implements ConfigSerializable {
    private static volatile HealthHelperModule instance;
    private final BooleanSetting enabledSetting;
    private final ColorSetting buffColor;
    private final BooleanSetting debuffs;
    private final ColorSetting debuffColor;
    private final BooleanSetting gapple;
    private final ColorSetting gappleColor;
    private final BooleanSetting enchantedGapple;
    private final ColorSetting enchantedGappleColor;
    private final BooleanSetting tint;
    private final NumberSetting tintPeriod;

    public HealthHelperModule(EventBus eventBus) {
        super(eventBus, "health_helper", "Health Helper", "Подсвечивает исцеляющие предметы в инвентаре", ModuleCategory.valueOf("DISPLAY"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Подсвечивает исцеляющие предметы в инвентаре").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        ColorSetting buff = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("Color buff").id("buff_color").description("Цвет полезных зелий")).build();
        this.applyDefaultPalette(buff);
        this.buffColor = buff;
        this.registerSetting(buff);
        this.debuffs = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Debuffs").id("debuffs").description("Подсвечивает вредные зелья в инвентаре")).build();
        this.registerSetting(this.debuffs);
        ColorSetting debuff = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(3).name("Color debuff").id("debuff_color").description("Цвет вредных зелий").visibleWhen(this.debuffs::isEnabled)).build();
        this.applyDefaultPalette(debuff);
        this.debuffColor = debuff;
        this.registerSetting(debuff);
        this.gapple = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("GApple").id("gapple").description("Подсвечивает золотые яблоки")).build();
        this.registerSetting(this.gapple);
        ColorSetting gappleColorSetting = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(4).name("Color gapple").id("gapple_color").description("Цвет золотого яблока").visibleWhen(this.gapple::isEnabled)).build();
        this.applyDefaultPalette(gappleColorSetting);
        this.gappleColor = gappleColorSetting;
        this.registerSetting(gappleColorSetting);
        this.enchantedGapple = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("E-GApple").id("e_gapple").description("Подсвечивает зачарованные золотые яблоки")).build();
        this.registerSetting(this.enchantedGapple);
        ColorSetting egappleColorSetting = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(2).name("Color e-gapple").id("e_gapple_color").description("Цвет зачарованного золотого яблока").visibleWhen(this.enchantedGapple::isEnabled)).build();
        this.applyDefaultPalette(egappleColorSetting);
        this.enchantedGappleColor = egappleColorSetting;
        this.registerSetting(egappleColorSetting);
        this.tint = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Tint").id("tint").description("Пульсация прозрачности подсветки")).build();
        this.registerSetting(this.tint);
        this.tintPeriod = ((NumberSettingBuilder)NumberSetting.builder().range(500.0, 2000.0).defaultValue(1200.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Tint period").id("tint_period").description("Период пульсации прозрачности").visibleWhen(this.tint::isEnabled)).build();
        this.registerSetting(this.tintPeriod);
    }

    @Override
    protected void initialize() {
        WexSideClient.getSlotHighlightRegistry().setCallback56(this::collectHighlights);
    }

    private List<SlotHighlight> collectHighlights(HandledScreen<?> screen) {
        if (!this.enabledSetting.isEnabled() || screen == null || HealthHelperModule.shouldSkipPlayer()) {
            return List.of();
        }
        ScreenHandler handler = screen.getScreenHandler();
        if (handler == null || !ContainerScreenHelper.isPlayerInventoryContainer(handler, screen)) {
            return List.of();
        }
        ArrayList<SlotHighlight> highlights = new ArrayList<SlotHighlight>();
        for (int i = 0; i < handler.slots.size(); ++i) {
            Slot slot = (Slot)handler.slots.get(i);
            Integer color = this.resolveColor(slot.getStack());
            if (color == null) continue;
            highlights.add(new SlotHighlight(i, color));
        }
        return highlights;
    }

    public static int getItemHighlightColor(ItemStack stack) {
        HealthHelperModule module = instance;
        if (module == null || !module.enabledSetting.isEnabled() || stack == null || stack.isEmpty() || HealthHelperModule.shouldSkipPlayer()) {
            return 0;
        }
        Integer color = module.resolveColor(stack);
        return color == null ? 0 : color;
    }

    private static boolean shouldSkipPlayer() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && player.isCreative();
    }

    private Integer resolveColor(ItemStack stack) {
        if (stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            return this.enchantedGapple.isEnabled() ? Integer.valueOf(this.resolveTintedColor(this.enchantedGappleColor)) : null;
        }
        if (stack.isOf(Items.GOLDEN_APPLE)) {
            return this.gapple.isEnabled() ? Integer.valueOf(this.resolveTintedColor(this.gappleColor)) : null;
        }
        if (!(stack.isOf(Items.POTION) || stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION))) {
            return null;
        }
        PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null || !contents.hasEffects()) {
            return null;
        }
        if (this.isMostlyHarmful(contents)) {
            return this.debuffs.isEnabled() ? Integer.valueOf(this.resolveTintedColor(this.debuffColor)) : null;
        }
        return this.resolveTintedColor(this.buffColor);
    }

    private boolean isMostlyHarmful(PotionContentsComponent contents) {
        int harmful = 0;
        int beneficial = 0;
        for (StatusEffectInstance effect : contents.getEffects()) {
            StatusEffect type = (StatusEffect)effect.getEffectType().value();
            if (type.getCategory() == StatusEffectCategory.HARMFUL) {
                ++harmful;
                continue;
            }
            if (type.getCategory() != StatusEffectCategory.BENEFICIAL) continue;
            ++beneficial;
        }
        return harmful > beneficial;
    }

    private int resolveTintedColor(ColorSetting colorSetting) {
        int color = this.resolveBaseColor(colorSetting);
        if (this.tint.isEnabled()) {
            color = ColorUtils.multiplyAlpha(color, this.getTintStrength());
        }
        return color;
    }

    private int resolveBaseColor(ColorSetting colorSetting) {
        if (colorSetting.isAstolfoMode()) {
            return colorSetting.getColor();
        }
        if (colorSetting.isDoubleColorMode()) {
            return ColorUtils.lerp(colorSetting.getPrimaryColor(), colorSetting.getSecondaryColor(), HealthHelperModule.pulse(1500L));
        }
        return colorSetting.getPrimaryColor();
    }

    private float getTintStrength() {
        long period = Math.max(1L, (long)this.tintPeriod.getIntValue());
        return (float)(0.3 + 0.7 * HealthHelperModule.pulse(period));
    }

    private static double pulse(long periodMs) {
        double phase = (double)(System.currentTimeMillis() % periodMs) / (double)periodMs;
        return 1.0 - Math.abs(2.0 * phase - 1.0);
    }

    private void applyDefaultPalette(ColorSetting colorSetting) {
        colorSetting.setPrimaryColor(0, -11753627);
        colorSetting.setPrimaryColor(1, -1543135);
        colorSetting.setPrimaryColor(2, -9279489);
        colorSetting.setPrimaryColor(3, -46001);
        colorSetting.setPrimaryColor(4, -13218);
        colorSetting.setPrimaryColor(5, -10582785);
        colorSetting.setPrimaryColor(6, -2732032);
    }
}

