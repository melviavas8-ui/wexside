package ru.wexside.ui;

import java.util.function.BooleanSupplier;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.GuiDrawApi;

public final class CreateConfigButton
extends GuiElement {
    private final BooleanSupplier enabled;
    private final Runnable action;

    public CreateConfigButton(GuiBounds bounds, BooleanSupplier enabled, Runnable action) {
        super(bounds);
        this.enabled = enabled;
        this.action = action;
    }

    @Override
    public boolean onMousePressed(int mouseX, int mouseY, int button) {
        if (button != 0 || !this.getBounds().contains(mouseX, mouseY)) {
            return false;
        }
        if (this.enabled.getAsBoolean()) {
            this.action.run();
        }
        return true;
    }

    @Override
    public float render(float delta, Matrix4f matrix) {
        GuiBounds bounds = this.getBounds();
        GuiDrawApi renderer = WexSideClient.getGuiRenderer();
        int color = this.enabled.getAsBoolean() ? ThemeColors.accent() : ThemeColors.borderPrimary();
        renderer.drawRoundedOutline(matrix, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 6.0f, 0.75f, ThemeColors.withHoverOverlay(ThemeColors.notificationOutline()));
        String label = "Создать конфигурацию";
        float width = FontRegistry.font4.textWidth(label, 5.5f);
        float height = FontRegistry.font4.textHeight(label, 5.5f);
        FontRegistry.font4.drawText(matrix, renderer, label, bounds.getX() + (bounds.getWidth() - width) / 2.0f, bounds.getY() + (bounds.getHeight() - height) / 2.0f, 5.5f, color);
        return bounds.getY() + bounds.getHeight();
    }
}

