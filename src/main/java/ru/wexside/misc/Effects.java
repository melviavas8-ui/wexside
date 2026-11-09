package ru.wexside.misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.ThemeColors;
import ru.wexside.misc.ThemeManager;
import ru.wexside.render.IconAtlasEntry;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.util.AbstractHudElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class Effects
extends AbstractHudElement {
    private static final float MINIMUM_WIDTH = 110.0f;
    private static final float HEADER_HEIGHT = 18.0f;
    private static final float ROW_HEIGHT = 10.5f;
    private static final float CARD_HEIGHT = 23.0f;
    private final ModeSetting displayMode;
    private final Map<String, AnimatedEffect> animatedEffects = new LinkedHashMap<String, AnimatedEffect>();
    private float animatedWidth = 110.0f;
    private float animatedHeight = 18.0f;

    public Effects(BooleanSupplier visible) {
        super("Effects", visible);
        this.displayMode = ((ModeSettingBuilder)((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().id("display")).name("Вид")).options("Карточки", "Панель").defaultOption("Карточки").build();
        this.getHudElementConfig().addSetting(this.displayMode);
    }

    @Override
    protected float getWidth() {
        return this.animatedWidth;
    }

    @Override
    protected float getHeight() {
        return this.animatedHeight;
    }

    @Override
    protected boolean isContentVisible() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && !player.getStatusEffects().isEmpty();
    }

    @Override
    protected void updateLayout() {
        this.synchronizeEffects();
        boolean cards = this.isCardMode();
        float targetWidth = cards ? 0.0f : this.titleWidth();
        float rowsHeight = 0.0f;
        this.animatedEffects.values().removeIf(effect -> {
            effect.updateAnimation();
            if (!effect.isExpired()) {
                return false;
            }
            effect.close();
            return true;
        });
        for (AnimatedEffect effect2 : this.animatedEffects.values()) {
            if (effect2.getVisibility() <= 0.001f) continue;
            targetWidth = Math.max(targetWidth, effect2.getWidth(cards));
            rowsHeight += effect2.getVisibility() * ((cards ? 23.0f : 10.5f) + 3.0f);
        }
        if (cards) {
            this.animatedHeight = FrameInterpolator.lerpTowards(this.animatedHeight, Math.max(0.0f, rowsHeight - 3.0f), 30.0f);
        } else {
            float targetHeight = 18.0f + (rowsHeight > 0.0f ? 4.5f + rowsHeight + 2.0f : 0.0f);
            targetWidth = Math.max(110.0f, targetWidth);
            this.animatedHeight = FrameInterpolator.lerpTowards(this.animatedHeight, targetHeight, 30.0f);
        }
        this.animatedWidth = FrameInterpolator.lerpTowards(this.animatedWidth, Math.max(cards ? 1.0f : 110.0f, targetWidth), 30.0f);
        this.collectIconBakes();
    }

    @Override
    protected void renderContent(GuiDrawApi renderer, Matrix4f matrix, float x, float y, float width, float height, float scale) {
        if (this.isCardMode()) {
            this.renderCards(renderer, matrix, x, y, width, scale);
            return;
        }
        this.renderPanelSurface(renderer, matrix, x, y, width, height, 8.0f, scale);
        renderer.beginStencil(1);
        renderer.drawRoundedRectangle(matrix, x, y, width, height, 8.0f * scale, -1);
        renderer.applyStencilMask(1);
        int titleColor = ThemeColors.hudTextPrimary();
        FontRegistry.font7.drawText(matrix, renderer, "Effects", x + 5.0f * scale, y + 4.5f * scale, 8.0f * scale, titleColor);
        FontRegistry.font3.drawGlyphText(matrix, renderer, "u", x + width - 13.0f * scale, y + 6.0f * scale, 8.0f * scale, titleColor);
        float rowY = y + 22.5f * scale;
        for (AnimatedEffect effect : this.animatedEffects.values()) {
            float visibility = effect.getVisibility();
            if (visibility <= 0.001f) continue;
            effect.renderPanelRow(renderer, matrix, x, rowY, width, scale, visibility);
            rowY += visibility * 13.5f * scale;
        }
        renderer.endStencil();
    }

    private void renderCards(GuiDrawApi renderer, Matrix4f matrix, float x, float y, float width, float scale) {
        boolean alignRight = x + width * 0.5f > (float)MinecraftClient.getInstance().getWindow().getScaledWidth() * 0.5f;
        float rowY = y;
        for (AnimatedEffect effect : this.animatedEffects.values()) {
            float visibility = effect.getVisibility();
            if (visibility <= 0.001f) continue;
            float cardWidth = effect.getWidth(true) * scale;
            float cardX = alignRight ? x + width - cardWidth : x;
            effect.renderCard(renderer, matrix, cardX, rowY, cardWidth, scale, visibility);
            rowY += visibility * 26.0f * scale;
        }
    }

    private void synchronizeEffects() {
        for (AnimatedEffect effect : this.animatedEffects.values()) {
            effect.setPresent(false);
        }
        List<StatusEffectInstance> effects = this.collectEffects();
        if (effects.isEmpty() && this.isEditorScreen()) {
            effects = this.previewEffects();
        }
        for (StatusEffectInstance effect : effects) {
            String key = effect.getEffectType().getIdAsString() + "#" + effect.getAmplifier();
            this.animatedEffects.compute(key, (ignored, animated) -> {
                if (animated == null) {
                    return new AnimatedEffect(this, effect);
                }
                animated.setEffect(effect);
                animated.setPresent(true);
                return animated;
            });
        }
    }

    private List<StatusEffectInstance> collectEffects() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return List.of();
        }
        ArrayList<StatusEffectInstance> effects = new ArrayList<StatusEffectInstance>();
        for (StatusEffectInstance effect2 : player.getStatusEffects()) {
            effects.add(new StatusEffectInstance(effect2));
        }
        effects.sort(Comparator.comparing(effect -> ((StatusEffect)effect.getEffectType().value()).getName().getString()));
        return effects;
    }

    private List<StatusEffectInstance> previewEffects() {
        return List.of(new StatusEffectInstance(StatusEffects.SPEED, -1, 2), new StatusEffectInstance(StatusEffects.STRENGTH, -1, 2), new StatusEffectInstance(StatusEffects.REGENERATION, 1880, 0), new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, -1, 2));
    }

    private boolean isCardMode() {
        return "Карточки".equals(this.displayMode.getSelectedOption());
    }

    private float titleWidth() {
        return 5.0f + FontRegistry.font7.textWidth("Effects", 8.0f) + 19.0f;
    }

    private static String effectName(StatusEffectInstance effect) {
        StatusEffect type = (StatusEffect)effect.getEffectType().value();
        String amplifier = effect.getAmplifier() > 0 ? " " + Effects.amplifierText(effect.getAmplifier() + 1) : "";
        return type.getName().getString() + amplifier;
    }

    private static String amplifierText(int level) {
        return switch (level) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(level);
        };
    }

    private static String effectDuration(StatusEffectInstance effect) {
        if (effect.isInfinite()) {
            return "\u221e";
        }
        int seconds = Math.max(0, effect.getDuration() / 20);
        return "%d:%02d".formatted(seconds / 60, seconds % 60);
    }

    private void collectIconBakes() {
        float framebufferScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        ArrayList<BakedIconEntry> bakes = new ArrayList<BakedIconEntry>();
        for (AnimatedEffect effect : this.animatedEffects.values()) {
            effect.collectIconBake(framebufferScale, bakes);
        }
        if (!bakes.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(bakes);
        }
    }

    private final class AnimatedEffect {
        private StatusEffectInstance effect;
        private boolean present = true;
        private float visibility = 1.0f;
        private final IconAtlasEntry iconTexture = new IconAtlasEntry(true);

        private AnimatedEffect(Effects effects, StatusEffectInstance effect) {
            this.effect = effect;
        }

        private void setEffect(StatusEffectInstance effect) {
            this.effect = effect;
        }

        private void setPresent(boolean present) {
            this.present = present;
        }

        private void updateAnimation() {
            this.visibility = FrameInterpolator.lerpTowards(this.visibility, this.present ? 1.0f : 0.0f, 30.0f);
        }

        private boolean isExpired() {
            return !this.present && this.visibility <= 0.001f;
        }

        private float getVisibility() {
            return this.visibility;
        }

        private float getWidth(boolean cards) {
            String name = Effects.effectName(this.effect);
            String duration = Effects.effectDuration(this.effect);
            if (cards) {
                return 30.0f + Math.max(FontRegistry.font4.textWidth(name, 7.0f), FontRegistry.font6.textWidth(duration, 6.0f));
            }
            return 19.0f + FontRegistry.font4.textWidth(name, 6.5f) + FontRegistry.font6.textWidth(duration, 5.5f) + 12.0f;
        }

        private void renderPanelRow(GuiDrawApi renderer, Matrix4f matrix, float x, float y, float width, float scale, float alpha) {
            float slide = (1.0f - alpha) * 4.0f * scale;
            float contentX = x + slide;
            int primary = ColorUtils.multiplyAlpha(ThemeColors.hudTextPrimary(), alpha);
            int durationColor = ColorUtils.multiplyAlpha(((StatusEffect)this.effect.getEffectType().value()).isBeneficial() ? ThemeColors.hudTextMuted() : ThemeColors.danger(), alpha);
            this.renderIcon(renderer, matrix, contentX + 5.0f * scale, y + 1.75f * scale, 7.0f * scale, alpha);
            String name = Effects.effectName(this.effect);
            float nameHeight = FontRegistry.font4.textHeight(name, 6.5f) * scale;
            FontRegistry.font4.drawText(matrix, renderer, name, contentX + 15.0f * scale, y + (10.5f * scale - nameHeight) * 0.5f, 6.5f * scale, primary);
            String duration = Effects.effectDuration(this.effect);
            float durationWidth = FontRegistry.font6.textWidth(duration, 5.5f) * scale;
            float badgeWidth = durationWidth + 8.0f * scale;
            float badgeX = x + width - 5.0f * scale - badgeWidth + slide;
            renderer.drawRoundedRectangle(matrix, badgeX, y, badgeWidth, 10.5f * scale, 6.0f * scale, ColorUtils.multiplyAlpha(ThemeColors.controlFill(), alpha));
            renderer.drawRoundedOutline(matrix, badgeX, y, badgeWidth, 10.5f * scale, 6.0f * scale, scale, ColorUtils.multiplyAlpha(ThemeColors.separatorHover(), alpha));
            FontRegistry.font6.drawText(matrix, renderer, duration, badgeX + (badgeWidth - durationWidth) * 0.5f, y + (10.5f * scale - FontRegistry.font6.textHeight(duration, 5.5f) * scale) * 0.5f, 5.5f * scale, durationColor);
        }

        private void renderCard(GuiDrawApi renderer, Matrix4f matrix, float x, float y, float width, float scale, float alpha) {
            float slide = (1.0f - alpha) * 4.0f * scale;
            float height = 23.0f * scale;
            renderer.drawRoundedShadow(matrix, x += slide, y, width, height, 14.0f * scale, 12.0f * scale, ColorUtils.rgba(0, 0, 0, Math.round(11.0f * alpha)));
            int background = ColorUtils.multiplyAlpha(ThemeColors.hudBackground(), alpha);
            int outline = ColorUtils.multiplyAlpha(ThemeColors.withHoverOverlay(ThemeColors.notificationOutline()), alpha);
            if (ThemeManager.getThemeManager().isHudBlurEnabled()) {
                renderer.drawBlurredRoundedRectangle(matrix, x, y, width, height, 7.0f * scale);
            }
            renderer.drawRoundedRectangle(matrix, x, y, width, height, 7.0f * scale, background);
            renderer.drawRoundedOutline(matrix, x, y, width, height, 7.0f * scale, scale, outline);
            this.renderIcon(renderer, matrix, x + 6.0f * scale, y + 4.5f * scale, 14.0f * scale, alpha);
            String name = Effects.effectName(this.effect);
            String duration = Effects.effectDuration(this.effect);
            float nameHeight = FontRegistry.font4.textHeight(name, 7.0f);
            float durationHeight = FontRegistry.font6.textHeight(duration, 6.0f);
            float textHeight = nameHeight + 1.0f + durationHeight;
            float textX = x + 24.0f * scale;
            float textY = y + (23.0f - textHeight) * 0.5f * scale;
            FontRegistry.font4.drawText(matrix, renderer, name, textX, textY, 7.0f * scale, ColorUtils.multiplyAlpha(ThemeColors.hudTextPrimary(), alpha));
            int durationColor = ((StatusEffect)this.effect.getEffectType().value()).isBeneficial() ? ThemeColors.hudTextMuted() : ThemeColors.danger();
            FontRegistry.font6.drawText(matrix, renderer, duration, textX, textY + (nameHeight + 1.0f) * scale, 6.0f * scale, ColorUtils.multiplyAlpha(durationColor, alpha));
        }

        private void collectIconBake(float framebufferScale, List<BakedIconEntry> output) {
            if (!this.iconTexture.needsBake(framebufferScale)) {
                return;
            }
            RegistryEntry effectType = this.effect.getEffectType();
            output.add(new BakedIconEntry(this.iconTexture, (context, x, y, size) -> context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, InGameHud.getEffectTexture((RegistryEntry)effectType), x, y, size, size)));
        }

        private void renderIcon(GuiDrawApi renderer, Matrix4f matrix, float x, float y, float size, float alpha) {
            if (!this.iconTexture.isAllocated()) {
                return;
            }
            int textureSize = this.iconTexture.getIconSize();
            int texture = renderer.bindTexture(this.iconTexture.getTextureId(), textureSize, textureSize);
            renderer.drawTexture(matrix, x, y, size, size, 0.0f, 1.0f, 1.0f, 0.0f, texture, ColorUtils.rgba(255, 255, 255, Math.round(alpha * 255.0f)));
        }

        private void close() {
            this.iconTexture.close();
        }
    }
}

