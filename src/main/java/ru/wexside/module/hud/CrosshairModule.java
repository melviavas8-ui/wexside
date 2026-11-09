package ru.wexside.module.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.misc.TransitionAnimation;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.ColorSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.Easing;
import ru.wexside.util.GuiDrawApi;

public final class CrosshairModule
extends Module
implements ConfigSerializable {
    private static volatile CrosshairModule instance;
    private static final float OUTLINE_SIZE = 1.0f;
    private static final float ATTACK_WAVE_SCALE = 5.0f;
    private final BooleanSetting enabledSetting;
    private final ColorSetting color;
    private final BooleanSetting rayTrace;
    private final ColorSetting hoverColor;
    private final BooleanSetting tLike;
    private final BooleanSetting dot;
    private final BooleanSetting displayCooldown;
    private final NumberSetting length;
    private final NumberSetting gap;
    private final TransitionAnimation hoverAnimation;

    public CrosshairModule(EventBus eventBus) {
        super(eventBus, "crosshair", "Crosshair", "Кастомный прицел", ModuleCategory.valueOf("DISPLAY"), new String[0]);
        instance = this;
        this.hoverAnimation = new TransitionAnimation(Easing.LINEAR);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Кастомный прицел").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        ColorSetting crosshairColor = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("Color").id("color").description("Цвет прицела").aliases("color", "цвет")).build();
        this.applyPalette(crosshairColor);
        this.color = crosshairColor;
        this.registerSetting(crosshairColor);
        this.rayTrace = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("RayTrace").id("ray_trace").description("Меняет цвет при наведении на Entity").aliases("raytrace", "наведение")).build();
        this.registerSetting(this.rayTrace);
        ColorSetting hover = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("RayTrace Color").id("hover_color").description("Цвет при наведении на Entity").aliases("hover color", "цвет наведения").visibleWhen(this.rayTrace::isEnabled)).build();
        this.applyPalette(hover);
        this.hoverColor = hover;
        this.registerSetting(hover);
        this.tLike = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("T-Like").id("t_like").description("Форма прицела T-образная").aliases("tlike", "т-образный")).build();
        this.registerSetting(this.tLike);
        this.dot = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Dot").id("dot").description("Точка в центре прицела").aliases("dot", "точка")).build();
        this.registerSetting(this.dot);
        this.displayCooldown = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Display Cooldown").id("display_cooldown").description("Перезарядка атаки на прицеле")).build();
        this.registerSetting(this.displayCooldown);
        this.length = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 8.0).defaultValue(4.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Length").id("length").description("Длина линий прицела").aliases("length", "длина")).build();
        this.registerSetting(this.length);
        this.gap = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 12.0).defaultValue(3.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Gap").id("gap").description("Расстояние между линиями прицела")).build();
        this.registerSetting(this.gap);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
        this.listen(HudRenderEvent.class, event -> this.onRender());
    }

    private void onTick() {
        if (!this.enabledSetting.isEnabled()) {
            this.hoverAnimation.setActive(false);
            return;
        }
        this.hoverAnimation.setActive(this.isHoveringLivingEntity());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void onRender() {
        if (!this.enabledSetting.isEnabled() || CrosshairModule.shouldHideCustomCrosshair()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.options == null || client.options.getPerspective() != Perspective.FIRST_PERSON) {
            return;
        }
        float scale = client.getWindow().getScaleFactor();
        float centerX = (float)client.getWindow().getScaledWidth() / 2.0f;
        float centerY = (float)client.getWindow().getScaledHeight() / 2.0f;
        float lineLength = (float)this.length.getValue();
        float cooldownExpansion = 0.0f;
        if (this.displayCooldown.isEnabled()) {
            float tickDelta = client.getRenderTickCounter().getTickProgress(true);
            float cooldown = player.getAttackCooldownProgress(tickDelta);
            cooldownExpansion = MathHelper.sin((double)(cooldown * cooldown * (float)Math.PI)) * 5.0f;
        }
        float lineGap = (float)this.gap.getValue() + cooldownExpansion;
        int rgb = this.color.getColor(0.0f);
        float hoverMix = this.hoverAnimation.getPrimaryProgress();
        if (hoverMix > 0.0f && this.rayTrace.isEnabled()) {
            rgb = ColorUtils.lerp(rgb, this.hoverColor.getColor(1.0f), hoverMix);
        }
        int outline = ColorUtils.lightContrastColor;
        GuiDrawApi renderer = WexSideClient.getHudRenderer();
        Matrix4f matrix = new Matrix4f().scale(scale);
        renderer.begin();
        try {
            if (this.dot.isEnabled()) {
                this.drawQuad(renderer, matrix, centerX - 1.0f, centerY - 1.0f, centerX + 1.0f, centerY + 1.0f, outline);
                this.drawQuad(renderer, matrix, centerX - 0.5f, centerY - 0.5f, centerX + 0.5f, centerY + 0.5f, rgb);
            }
            if (!this.tLike.isEnabled()) {
                this.drawLine(renderer, matrix, centerX, centerY - lineGap - lineLength, centerX, centerY - lineGap, rgb, outline);
            }
            this.drawLine(renderer, matrix, centerX, centerY + lineGap, centerX, centerY + lineGap + lineLength, rgb, outline);
            this.drawLine(renderer, matrix, centerX - lineGap - lineLength, centerY, centerX - lineGap, centerY, rgb, outline);
            this.drawLine(renderer, matrix, centerX + lineGap, centerY, centerX + lineGap + lineLength, centerY, rgb, outline);
        }
        finally {
            renderer.end();
        }
    }

    private boolean isHoveringLivingEntity() {
        HitResult hit = MinecraftClient.getInstance().crosshairTarget;
        if (!(hit instanceof EntityHitResult)) {
            return false;
        }
        EntityHitResult entityHit = (EntityHitResult)hit;
        Entity entity = entityHit.getEntity();
        return entity instanceof LivingEntity;
    }

    private static boolean shouldHideCustomCrosshair() {
        return MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
    }

    public static boolean isEnabled3() {
        if (CrosshairModule.shouldHideCustomCrosshair()) {
            return true;
        }
        CrosshairModule module = instance;
        if (module == null || !module.enabledSetting.isEnabled()) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        return client.options != null && client.options.getPerspective() == Perspective.FIRST_PERSON;
    }

    private void drawQuad(GuiDrawApi renderer, Matrix4f matrix, float x1, float y1, float x2, float y2, int color) {
        renderer.fillRectangle(matrix, x1, y1, x2 - x1, y2 - y1, color);
    }

    private void drawLine(GuiDrawApi renderer, Matrix4f matrix, float x1, float y1, float x2, float y2, int color, int outline) {
        this.drawQuad(renderer, matrix, x1 - 0.5f, y1 - 0.5f, x2 + 0.5f, y2 + 0.5f, outline);
        this.drawQuad(renderer, matrix, x1, y1, x2, y2, color);
    }

    private void applyPalette(ColorSetting colorSetting) {
        colorSetting.setPrimaryColor(0, -11753627);
        colorSetting.setPrimaryColor(1, -1543135);
        colorSetting.setPrimaryColor(2, -9279489);
        colorSetting.setPrimaryColor(3, -46001);
        colorSetting.setPrimaryColor(4, -13218);
        colorSetting.setPrimaryColor(5, -10582785);
        colorSetting.setPrimaryColor(6, -2732032);
    }
}

