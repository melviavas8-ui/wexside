package ru.wexside.module.render;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.OverlayRenderEvent;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;

public final class NoRenderModule
extends Module
implements ConfigSerializable {
    static final String DYNAMIC_FOV = "Dynamic-FOV";
    static final String FLUID_ZOOM = "Fluid-Zoom";
    static final String FIRE_OVERLAY = "Fire-Overlay";
    static final String SCOREBOARD = "Scoreboard";
    static final String BLOCK_OVERLAY = "Block-Overlay";
    static final String TOTEM_OVERLAY = "Totem-Overlay";
    static final String GLOW = "Glow";
    static final String CAMERA_HURT = "Camera-Hurt";
    static final String VIEW_BOBBING = "View-Bobbing";
    static final String EFFECTS_ICONS = "Effects-Icons";
    static final String BAD_EFFECTS = "Bad-Effects";
    static final String BOSS_BAR = "Boss-Bar";
    static final String UNDER_LAVA = "Under-Lava";
    static final String WORLD_LAVA = "World-Lava";
    private static volatile NoRenderModule instance;
    private final BooleanSetting enabledSetting;
    private final MultiSelectSetting remove;
    private boolean lavaHidden;

    public NoRenderModule(EventBus eventBus) {
        super(eventBus, "no_render", "No Render", "Отключение элементов рендера", ModuleCategory.valueOf("RENDER"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Включить отключение элементов рендера").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        MultiSelectSetting removeSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options(FIRE_OVERLAY, BLOCK_OVERLAY, CAMERA_HURT, TOTEM_OVERLAY, EFFECTS_ICONS, BAD_EFFECTS, BOSS_BAR, SCOREBOARD, GLOW, FLUID_ZOOM, UNDER_LAVA, WORLD_LAVA, VIEW_BOBBING, DYNAMIC_FOV).selectAll(false).optionListEnabled(false).name("Remove").id("remove").description("Отключаемые элементы")).build();
        removeSetting.setOptions(new String[0]);
        this.remove = removeSetting;
        this.registerSetting(removeSetting);
    }

    @Override
    protected void initialize() {
        this.listen(OverlayRenderEvent.class, this::onOverlay);
        this.listen(ClientTickEvent.class, this::onTick);
    }

    public static boolean isEnabled() {
        return NoRenderModule.compute(DYNAMIC_FOV);
    }

    public static boolean isEnabled2() {
        return NoRenderModule.compute(BAD_EFFECTS);
    }

    public static boolean isViewBobbingDisabled() {
        return NoRenderModule.compute(VIEW_BOBBING);
    }

    public static boolean isFluidZoomDisabled() {
        return NoRenderModule.compute(FLUID_ZOOM);
    }

    public static boolean isUnderLavaDisabled() {
        return NoRenderModule.compute(UNDER_LAVA);
    }

    public static boolean isWorldLavaDisabled() {
        return NoRenderModule.compute(WORLD_LAVA);
    }

    public static boolean compute(String option) {
        NoRenderModule module = instance;
        return module != null && module.enabledSetting.isEnabled() && module.remove.getSelectedOptions().contains(option);
    }

    private void onTick(ClientTickEvent event) {
        boolean hideLava;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        boolean bl = hideLava = this.enabledSetting.isEnabled() && this.remove.getSelectedOptions().contains(WORLD_LAVA);
        if (hideLava != this.lavaHidden) {
            if (client.worldRenderer != null) {
                client.worldRenderer.reload();
            }
            this.lavaHidden = hideLava;
        }
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        List<String> selected = this.remove.getSelectedOptions();
        if (selected.contains(BAD_EFFECTS)) {
            this.stripEffect(client.player, (RegistryEntry<StatusEffect>)StatusEffects.NAUSEA);
            this.stripEffect(client.player, (RegistryEntry<StatusEffect>)StatusEffects.BLINDNESS);
            this.stripEffect(client.player, (RegistryEntry<StatusEffect>)StatusEffects.DARKNESS);
        }
        if (selected.contains(GLOW)) {
            for (Entity entity : client.world.getEntities()) {
                if (!entity.isGlowing()) continue;
                entity.setGlowing(false);
            }
        }
    }

    private void stripEffect(ClientPlayerEntity player, RegistryEntry<StatusEffect> effect) {
        if (player.hasStatusEffect(effect)) {
            player.removeStatusEffect(effect);
        }
    }

    private void onOverlay(OverlayRenderEvent event) {
        String option;
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        switch (event.type()) {
            default: {
                throw new MatchException(null, null);
            }
            case FIRE: {
                option = FIRE_OVERLAY;
                break;
            }
            case BLOCK: {
                option = BLOCK_OVERLAY;
                break;
            }
            case CAMERA_HURT: {
                option = CAMERA_HURT;
                break;
            }
            case TOTEM: {
                option = TOTEM_OVERLAY;
                break;
            }
            case STATUS_EFFECTS: {
                option = EFFECTS_ICONS;
                break;
            }
            case BOSS_BAR: {
                option = BOSS_BAR;
                break;
            }
            case SCOREBOARD: {
                option = SCOREBOARD;
            }
        }
        if (option != null && this.remove.getSelectedOptions().contains(option)) {
            event.update();
        }
    }
}

