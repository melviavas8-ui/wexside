package ru.wexside.ui;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.PotionCatalogEntry;
import ru.wexside.misc.PotionPresetDraft;
import ru.wexside.misc.ThemeColors;
import ru.wexside.render.BakedItemIcon;
import ru.wexside.render.ItemIconRenderer;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.PotionPresetController;

public final class PotionPresetRow
extends GuiElement {
    private final PotionPresetDraft preset;
    private final PotionPresetController potionService;
    private final ItemIconRenderer icons;
    private final Runnable onSelected;
    private boolean selected;

    public PotionPresetRow(PotionPresetDraft preset, PotionPresetController potionService, ItemIconRenderer icons, float width, Runnable onSelected) {
        super(new GuiBounds(0.0f, 0.0f, width, 12.0f));
        this.preset = preset;
        this.potionService = potionService;
        this.icons = icons;
        this.onSelected = onSelected;
    }

    public PotionPresetDraft getPreset() {
        return this.preset;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean onMousePressed(int mouseX, int mouseY, int button) {
        if (button != 0 || !this.getBounds().contains(mouseX, mouseY)) {
            return false;
        }
        this.onSelected.run();
        return true;
    }

    @Override
    public float render(float delta, Matrix4f matrix) {
        GuiDrawApi renderer = WexSideClient.getGuiRenderer();
        GuiBounds bounds = this.getBounds();
        renderer.drawRoundedRectangle(matrix, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 4.0f, this.selected ? ThemeColors.formatFieldFill() : ThemeColors.borderPrimary());
        float iconX = bounds.getX() + 2.0f;
        for (PotionCatalogEntry potion : this.preset.getPotions()) {
            BakedItemIcon icon = this.icons.getCachedIcon(this.potionService.resolveStack(potion));
            if (icon == null) continue;
            this.icons.drawIcon(renderer, matrix, icon, iconX, bounds.getY() + 2.0f, 8.0f, -1);
            iconX += 9.0f;
        }
        FontRegistry.font2.drawText(matrix, renderer, this.preset.getName(), Math.max(bounds.getX() + 3.0f, iconX + 2.0f), bounds.getY() + 3.0f, 6.0f, ThemeColors.textPrimary());
        return bounds.getY() + bounds.getHeight();
    }
}

