package ru.wexside.misc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.ThemeColors;
import ru.wexside.mixin.ItemCooldownEntryAccessorMixin;
import ru.wexside.mixin.ItemCooldownManagerAccessorMixin;
import ru.wexside.render.BakedItemIcon;
import ru.wexside.render.ItemIconRenderer;
import ru.wexside.util.AbstractHudElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class Cooldowns
extends AbstractHudElement {
    private static final float MINIMUM_WIDTH = 110.0f;
    private static final float HEADER_HEIGHT = 18.0f;
    private static final float ROW_HEIGHT = 10.5f;
    private final Map<String, AnimatedCooldown> animatedCooldowns = new LinkedHashMap<String, AnimatedCooldown>();
    private final ItemIconRenderer itemIcons = new ItemIconRenderer();
    private float animatedWidth = 110.0f;
    private float animatedHeight = 18.0f;

    public Cooldowns(BooleanSupplier visible) {
        super("Cooldowns", visible);
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
        return !this.collectActualCooldowns().isEmpty();
    }

    @Override
    protected void updateLayout() {
        this.synchronizeCooldowns();
        float targetWidth = this.titleWidth();
        float rowsHeight = 0.0f;
        this.itemIcons.beginFrame();
        this.animatedCooldowns.values().removeIf(cooldown -> {
            cooldown.updateAnimation();
            return cooldown.isExpired();
        });
        for (AnimatedCooldown cooldown2 : this.animatedCooldowns.values()) {
            if (cooldown2.getVisibility() <= 0.001f) continue;
            targetWidth = Math.max(targetWidth, cooldown2.getWidth());
            rowsHeight += cooldown2.getVisibility() * 13.5f;
            this.itemIcons.get(cooldown2.getStack());
        }
        float targetHeight = 18.0f + (rowsHeight > 0.0f ? 4.5f + rowsHeight + 2.0f : 0.0f);
        this.animatedWidth = FrameInterpolator.lerpTowards(this.animatedWidth, Math.max(110.0f, targetWidth), 30.0f);
        this.animatedHeight = FrameInterpolator.lerpTowards(this.animatedHeight, targetHeight, 30.0f);
        this.itemIcons.collectBakes();
        this.itemIcons.evictUnused();
    }

    @Override
    protected void renderContent(GuiDrawApi renderer, Matrix4f matrix, float x, float y, float width, float height, float scale) {
        int titleColor = ThemeColors.hudTextPrimary();
        this.renderPanelSurface(renderer, matrix, x, y, width, height, 8.0f, scale);
        renderer.beginStencil(1);
        renderer.drawRoundedRectangle(matrix, x, y, width, height, 8.0f * scale, -1);
        renderer.applyStencilMask(1);
        FontRegistry.font7.drawText(matrix, renderer, "Cooldowns", x + 5.0f * scale, y + 4.5f * scale, 8.0f * scale, titleColor);
        FontRegistry.font3.drawGlyphText(matrix, renderer, "ъ", x + width - 13.0f * scale, y + 6.0f * scale, 8.0f * scale, titleColor);
        float rowY = y + 22.5f * scale;
        for (AnimatedCooldown cooldown : this.animatedCooldowns.values()) {
            float visibility = cooldown.getVisibility();
            if (visibility <= 0.001f) continue;
            cooldown.render(renderer, matrix, x, rowY, width, scale, visibility);
            rowY += visibility * 13.5f * scale;
        }
        renderer.endStencil();
    }

    private void synchronizeCooldowns() {
        for (AnimatedCooldown cooldown : this.animatedCooldowns.values()) {
            cooldown.setPresent(false);
        }
        List<CooldownEntry> cooldowns = this.collectActualCooldowns();
        if (cooldowns.isEmpty() && this.isEditorScreen()) {
            cooldowns = this.previewCooldowns();
        }
        for (CooldownEntry cooldown : cooldowns) {
            this.animatedCooldowns.compute(cooldown.id(), (ignored, animated) -> {
                if (animated == null) {
                    return new AnimatedCooldown(cooldown);
                }
                animated.setEntry(cooldown);
                animated.setPresent(true);
                return animated;
            });
        }
    }

    private List<CooldownEntry> collectActualCooldowns() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return List.of();
        }
        ArrayList<CooldownEntry> cooldowns = new ArrayList<CooldownEntry>();
        ItemCooldownManager manager = player.getItemCooldownManager();
        if (!(manager instanceof ItemCooldownManagerAccessorMixin)) {
            return cooldowns;
        }
        ItemCooldownManagerAccessorMixin state = (ItemCooldownManagerAccessorMixin)manager;
        HashSet<Identifier> seen = new HashSet<Identifier>();
        for (int slot = 0; slot < player.getInventory().size(); ++slot) {
            ItemCooldownEntryAccessorMixin entry;
            int remainingTicks;
            Object rawEntry;
            Identifier group;
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isEmpty() || !manager.isCoolingDown(stack) || !seen.add(group = manager.getGroup(stack)) || !((rawEntry = state.wexside$getEntries().get(group)) instanceof ItemCooldownEntryAccessorMixin) || (remainingTicks = (entry = (ItemCooldownEntryAccessorMixin)rawEntry).wexside$getEndTick() - state.wexside$getTick()) <= 0) continue;
            cooldowns.add(new CooldownEntry(group.toString(), stack.copy(), remainingTicks));
        }
        return cooldowns;
    }

    private List<CooldownEntry> previewCooldowns() {
        return List.of(new CooldownEntry("preview/pearl", new ItemStack((ItemConvertible)Items.ENDER_PEARL), 200), new CooldownEntry("preview/gapple", new ItemStack((ItemConvertible)Items.GOLDEN_APPLE), 160), new CooldownEntry("preview/chorus", new ItemStack((ItemConvertible)Items.CHORUS_FRUIT), 120), new CooldownEntry("preview/totem", new ItemStack((ItemConvertible)Items.TOTEM_OF_UNDYING), 80));
    }

    private float titleWidth() {
        return 5.0f + FontRegistry.font7.textWidth("Cooldowns", 8.0f) + 19.0f;
    }

    private final class AnimatedCooldown {
        private CooldownEntry entry;
        private boolean present = true;
        private float visibility = 1.0f;

        private AnimatedCooldown(CooldownEntry entry) {
            this.entry = entry;
        }

        private void setEntry(CooldownEntry entry) {
            this.entry = entry;
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

        private ItemStack getStack() {
            return this.entry.stack();
        }

        private float getWidth() {
            String duration = this.durationText();
            return 15.0f + FontRegistry.font4.textWidth(this.entry.stack().getName().getString(), 6.5f) + 4.0f + FontRegistry.font6.textWidth(duration, 5.5f) + 5.0f;
        }

        private void render(GuiDrawApi renderer, Matrix4f matrix, float x, float y, float width, float scale, float alpha) {
            int primary = ColorUtils.multiplyAlpha(ThemeColors.hudTextPrimary(), alpha);
            float slide = (1.0f - alpha) * 4.0f * scale;
            BakedItemIcon icon = Cooldowns.this.itemIcons.get(this.entry.stack());
            Cooldowns.this.itemIcons.render(renderer, matrix, icon, x + 5.0f * scale + slide, y + 1.75f * scale, 7.0f * scale, ColorUtils.withAlpha(-1, 255.0f * alpha));
            String name = this.entry.stack().getName().getString();
            float nameHeight = FontRegistry.font4.textHeight(name, 6.5f) * scale;
            FontRegistry.font4.drawText(matrix, renderer, name, x + 15.0f * scale + slide, y + (10.5f * scale - nameHeight) * 0.5f, 6.5f * scale, primary);
            String duration = this.durationText();
            float durationWidth = FontRegistry.font6.textWidth(duration, 5.5f) * scale;
            FontRegistry.font6.drawText(matrix, renderer, duration, x + width - 5.0f * scale - durationWidth + slide, y + (10.5f * scale - FontRegistry.font6.textHeight(duration, 5.5f) * scale) * 0.5f, 5.5f * scale, ColorUtils.multiplyAlpha(ThemeColors.hudTextMuted(), alpha));
        }

        private String durationText() {
            float seconds = (float)this.entry.remainingTicks() / 20.0f;
            if (seconds < 1.0f) {
                return (float)Math.round(seconds * 10.0f) / 10.0f + " сек.";
            }
            if (seconds < 60.0f) {
                return "%d сек.".formatted((int)seconds);
            }
            return "%d:%02d".formatted((int)seconds / 60, (int)seconds % 60);
        }
    }

    private record CooldownEntry(String id, ItemStack stack, int remainingTicks) {
    }
}

