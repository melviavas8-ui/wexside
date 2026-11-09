package ru.wexside.module.render;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EntityAttackEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.TargetEspEffect;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.module.combat.AttackAuraModule;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.ColorSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.util.AuraTargetEspRenderer;
import ru.wexside.util.CylinderTargetEspRenderer;
import ru.wexside.util.MarkerTargetEspRenderer;
import ru.wexside.util.SkullTargetEspRenderer;
import ru.wexside.util.SwordTargetEspRenderer;

public final class TargetESPModule
extends Module
implements ConfigSerializable {
    private static final String SWORD = "Sword";
    private static volatile TargetESPModule instance;
    private final BooleanSetting enabledSetting;
    private final ModeSetting mode;
    private final ColorSetting color;
    private final BooleanSetting useAttackAuraTarget;
    private final Map<String, TargetEspEffect> renderers;
    private WorldRenderEvent pendingWorldRender;
    private AttackAuraModule attackAura;
    private LivingEntity lastTarget;
    private String lastMode;

    public TargetESPModule(EventBus eventBus) {
        super(eventBus, "target_esp", "Target ESP", "Подсветка атакуемой цели", ModuleCategory.valueOf("RENDER"), new String[0]);
        instance = this;
        this.renderers = this.createRenderers();
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Включить подсветку цели").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.mode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options(SWORD, "Cylinder", "Marker", "Skull", "Aura").defaultOption(SWORD).name("Mode").id("mode").description("Стиль подсветки")).build();
        this.registerSetting(this.mode);
        ColorSetting colorSetting = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("Color").id("color").description("Цвет Target ESP")).build();
        colorSetting.setPrimaryColor(0, -11753627);
        colorSetting.setPrimaryColor(1, -1543135);
        colorSetting.setPrimaryColor(2, -9279489);
        colorSetting.setPrimaryColor(3, -46001);
        colorSetting.setPrimaryColor(4, -13218);
        colorSetting.setPrimaryColor(5, -10582785);
        colorSetting.setPrimaryColor(6, -2732032);
        this.color = colorSetting;
        this.registerSetting(colorSetting);
        this.useAttackAuraTarget = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Attack Aura only").id("use_attack_aura_target").description("Отображать только при использовании Attack Aura")).build();
        this.registerSetting(this.useAttackAuraTarget);
    }

    @Override
    protected void initialize() {
        this.listen(EntityAttackEvent.class, this::onAttack);
        this.listen(WorldRenderEvent.class, this::onWorldRender);
        this.listen(WorldSessionEvent.class, event -> this.resetRenderers());
    }

    public static void tick3() {
        TargetESPModule module = instance;
        if (module != null) {
            module.flushPendingRender();
        }
    }

    private void onAttack(EntityAttackEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        Entity class_12972 = event.getEntity();
        if (class_12972 instanceof LivingEntity) {
            LivingEntity livingEntity;
            this.lastTarget = livingEntity = (LivingEntity)class_12972;
        }
        this.currentRenderer().setEntityAttackEvent(event);
    }

    private void onWorldRender(WorldRenderEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            this.resetRenderers();
            this.pendingWorldRender = null;
            return;
        }
        if (this.currentRenderer().isActive()) {
            this.pendingWorldRender = event;
        } else {
            this.pendingWorldRender = null;
            this.render(event);
        }
    }

    private void flushPendingRender() {
        WorldRenderEvent event = this.pendingWorldRender;
        this.pendingWorldRender = null;
        if (event == null || !this.enabledSetting.isEnabled()) {
            return;
        }
        this.render(event);
    }

    private void render(WorldRenderEvent event) {
        this.syncMode();
        this.auraTarget();
        this.currentRenderer().setWorldRenderEvent(event);
    }

    private void syncMode() {
        String selected = this.mode.getSelectedOption();
        if (selected == null) {
            return;
        }
        if (this.lastMode == null) {
            this.lastMode = selected;
            return;
        }
        if (!selected.equals(this.lastMode)) {
            this.resetRenderers();
            this.lastMode = selected;
        }
    }

    private void resetRenderers() {
        this.renderers.values().forEach(TargetEspEffect::update);
        this.lastTarget = null;
    }

    private TargetEspEffect currentRenderer() {
        return this.renderers.getOrDefault(this.mode.getSelectedOption(), this.renderers.get(SWORD));
    }

    private Map<String, TargetEspEffect> createRenderers() {
        LinkedHashMap<String, TargetEspEffect> map = new LinkedHashMap<String, TargetEspEffect>();
        map.put(SWORD, new SwordTargetEspRenderer());
        map.put("Cylinder", new CylinderTargetEspRenderer());
        map.put("Marker", new MarkerTargetEspRenderer());
        map.put("Skull", new SkullTargetEspRenderer());
        map.put("Aura", new AuraTargetEspRenderer());
        return map;
    }

    private AttackAuraModule attackAura() {
        if (this.attackAura != null) {
            return this.attackAura;
        }
        if (WexSideClient.getInstance() == null || WexSideClient.getInstance().getModuleManager() == null) {
            return null;
        }
        this.attackAura = WexSideClient.getInstance().getModuleManager().getModule(AttackAuraModule.class);
        return this.attackAura;
    }

    private LivingEntity auraTarget() {
        if (!this.useAttackAuraTarget.isEnabled()) {
            return null;
        }
        AttackAuraModule module = this.attackAura();
        return module != null ? module.getLivingEntity() : null;
    }

    public static TargetESPModule getInstance() {
        return instance;
    }

    public LivingEntity getCurrentTarget() {
        LivingEntity target;
        LivingEntity aura = this.auraTarget();
        LivingEntity class_13092 = target = aura != null ? aura : this.lastTarget;
        if (target != null && (!target.isAlive() || target.isRemoved())) {
            this.lastTarget = null;
            return null;
        }
        return target;
    }

    public int getPrimaryColor() {
        return this.color.getColor(0.0f);
    }

    public int getSecondaryColor() {
        return this.color.getColor(0.5f);
    }
}

