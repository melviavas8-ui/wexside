package ru.wexside.misc;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.ContainerDisplay;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.ModuleBrowser;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.PopupManager;
import ru.wexside.misc.PopupOwner;
import ru.wexside.misc.Table;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.PopupPanel;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class LayoutModeButton
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
PopupOwner {
    private Table table;
    private final ModuleBrowser moduleBrowser;
    private float value;
    private boolean enabled;
    private final ContainerDisplay containerDisplay;
    private PopupManager popupManager;
    private final String string2;
    private final String string3;
    private float value2;

    public LayoutModeButton(GuiBounds bounds2, ContainerDisplay containerDisplay, ModuleBrowser moduleBrowser) {
        super(bounds2);
        this.string3 = "р";
        this.string2 = "Отображение";
        this.containerDisplay = containerDisplay;
        this.moduleBrowser = moduleBrowser;
        this.value2 = moduleBrowser.isActive2() ? 1.0f : 0.0f;
        this.getBounds().setSize(this.getPreferredWidth(), 11.5f);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (!this.moduleBrowser.isActive2() || !this.getBounds().contains(n, n2)) {
            return false;
        }
        if (n3 == 0 && this.table != null && this.popupManager != null) {
            this.popupManager.toggle(this);
        }
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.value2 = FrameInterpolator.lerpTowards(this.value2, this.moduleBrowser.isActive2() ? 1.0f : 0.0f, 45.0f);
        this.value = FrameInterpolator.lerpTowards(this.value, this.isActive3() ? 1.0f : 0.0f, 30.0f);
        if (this.value2 <= 0.01f) {
            return bounds2.getY() + bounds2.getHeight();
        }
        int n = (int)(255.0f * this.value2);
        int n2 = ColorUtils.withAlpha(ThemeColors.backgroundHover(), (float)((int)(255.0f * this.value * this.value2)));
        int n3 = ColorUtils.withAlpha(ThemeColors.textSecondary(), (float)n);
        int n4 = ColorUtils.withAlpha(ThemeColors.borderPrimary(), (float)n);
        int n5 = ColorUtils.withAlpha(ThemeColors.textMuted(), (float)n);
        float f2 = 5.75f;
        float f3 = 5.75f;
        float f4 = 4.75f;
        float f5 = 7.0f;
        float f6 = 7.5f;
        float f7 = FontRegistry.font3.textWidth("р", f2);
        float f8 = FontRegistry.font2.textWidth("Отображение", f3);
        float f9 = f7 + 3.0f + f8 + 2.0f + f5;
        float f10 = bounds2.getX() + (bounds2.getWidth() - f9) / 2.0f;
        float f11 = bounds2.getY() + (bounds2.getHeight() - FontRegistry.font3.textHeight("р", f2)) / 2.0f;
        float f12 = f10 + f7 + 3.0f;
        float f13 = bounds2.getY() + (bounds2.getHeight() - FontRegistry.font2.textHeight("Отображение", f3)) / 2.0f;
        float f14 = f12 + f8 + 3.0f;
        float f15 = bounds2.getY() + (bounds2.getHeight() - f6) / 2.0f;
        String string = Integer.toString(this.containerDisplay.getVisibleDescriptionCount());
        float f16 = f14 + (f5 - FontRegistry.font2.textWidth(string, f4)) / 2.0f;
        float f17 = f15 + (f6 - FontRegistry.font2.textHeight(string, f4)) / 2.0f;
        drawApi.drawRoundedRectangle(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, n2);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 7.0f, 1.0f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), n4);
        drawApi.drawRoundedRectangleOutlined(matrix4f, f14, f15, f5, f6, 5.0f, 1.0f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), n4);
        FontRegistry.font3.drawGlyphText(matrix4f, drawApi, "р", f10, f11, f2, n3);
        FontRegistry.font2.drawText(matrix4f, drawApi, "Отображение", f12, f13, f3, n3);
        FontRegistry.font2.drawText(matrix4f, drawApi, string, f16, f17, f4, n5);
        return bounds2.getY() + bounds2.getHeight();
    }

    public float getPreferredWidth() {
        float f = 5.75f;
        float f2 = 5.75f;
        float f3 = 7.0f;
        float f4 = FontRegistry.font3.textWidth("р", f);
        float f5 = FontRegistry.font2.textWidth("Отображение", f2);
        return f4 + 3.0f + f5 + 2.0f + f3 + 6.0f;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public boolean isActive3() {
        this.enabled = this.table != null && this.table.isActive2();
        return this.enabled;
    }

    public Table getTable() {
        return this.table;
    }

    @Override
    public PopupPanel getPopup() {
        return this.table;
    }

    @Override
    public void update2() {
        if (this.table == null) {
            return;
        }
        this.table.getBounds().setPosition(this.getBounds().getX() + this.getBounds().getWidth() + 4.0f, this.getBounds().getY());
    }

    @Override
    public void setPopupManager(PopupManager popupManager) {
        this.popupManager = popupManager;
    }
}

