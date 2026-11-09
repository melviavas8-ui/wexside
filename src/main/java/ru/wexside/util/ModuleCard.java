package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.ContainerColumnLayout;
import ru.wexside.misc.ContainerDisplay;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.ModuleTogglePulse;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.SettingKeybindButton;
import ru.wexside.misc.SettingRowFactory;
import ru.wexside.misc.SettingsColumnLayout;
import ru.wexside.misc.ThemeColors;
import ru.wexside.misc.ThemeManager;
import ru.wexside.module.Module;
import ru.wexside.setting.Setting;
import ru.wexside.ui.FloatingPanel;
import ru.wexside.ui.FloatingPanelProvider;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.setting.SettingComponent;
import ru.wexside.ui.setting.SettingRow;
import ru.wexside.util.ClippedLayerRenderer;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.ExpandButton;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.SettingDescriptionRenderer;
import ru.wexside.util.SettingsContainer;

public final class ModuleCard
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private int knownSettingCount;
    private final List<GuiElement> settingElements = new ArrayList<GuiElement>();
    private final SettingsContainer settingsContainer;
    private float expansionProgress;
    private final ExpandButton expandButton;
    private final SettingKeybindButton settingKeybindButton;
    private final SettingDescriptionRenderer settingDescriptionRenderer;
    private final Module module;
    private final SettingComponent<?> settingComponent;
    private final ContainerDisplay containerDisplay;

    public ModuleCard(GuiBounds bounds2, Module module, ContainerDisplay containerDisplay) {
        super(bounds2);
        this.module = module;
        this.containerDisplay = containerDisplay;
        this.expandButton = new ExpandButton(new GuiBounds(0.0f, 0.0f, 13.0f, 11.0f));
        this.settingsContainer = new SettingsContainer(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f));
        this.settingDescriptionRenderer = new SettingDescriptionRenderer(bounds2, module.getDisplayName(), module.getDescription(), containerDisplay);
        this.settingElements.add(this.expandButton);
        SettingRow<?> headerSettingRow = null;
        for (Setting setting : module.getSettings()) {
            SettingRow<?> row = SettingRowFactory.createRow(setting, containerDisplay);
            if (row == null) continue;
            if (row.isHeaderControl()) {
                headerSettingRow = row;
                continue;
            }
            this.settingsContainer.addChild(row);
        }
        SettingComponent<?> headerControl = null;
        SettingKeybindButton keybindEditor = null;
        if (headerSettingRow != null && headerSettingRow.getSettingComponent() != null) {
            headerControl = headerSettingRow.getSettingComponent();
            this.settingElements.add(headerControl);
            if (((Setting)headerSettingRow.getSetting()).hasKeybind()) {
                keybindEditor = new SettingKeybindButton((Setting)headerSettingRow.getSetting());
                super.addChild(keybindEditor);
            }
        }
        this.settingComponent = headerControl;
        this.settingKeybindButton = keybindEditor;
        super.addChild(this.settingsContainer);
        this.knownSettingCount = module.getSettings().size();
        this.update5();
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        if (!this.isExpanded()) {
            return;
        }
        this.settingsContainer.setSettingsColumnLayout(this.getSettingsColumnLayout());
        this.settingsContainer.onMouseScroll(n, n2, d);
    }

    @Override
    public void update() {
        for (GuiElement element2 : this.settingElements) {
            element2.update();
        }
        if (this.settingKeybindButton != null) {
            this.settingKeybindButton.update();
        }
        this.settingsContainer.setSettingsColumnLayout(this.getSettingsColumnLayout());
        this.settingsContainer.update();
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        this.update5();
        this.settingsContainer.setSettingsColumnLayout(this.getSettingsColumnLayout());
        boolean bl = this.getBounds().contains(n, n2);
        this.update6();
        if (bl) {
            for (GuiElement element2 : this.settingElements) {
                boolean bl2;
                if (element2 == this.expandButton && !this.isActive2()) continue;
                boolean bl3 = bl2 = element2 == this.expandButton && this.expandButton.isExpanded();
                if (!element2.onMousePressed(n, n2, n3)) continue;
                if (element2 == this.expandButton && bl2 && !this.expandButton.isExpanded()) {
                    this.settingsContainer.update2();
                    this.closeFloatingPanels(this);
                }
                return true;
            }
            if (this.settingKeybindButton != null && this.settingKeybindButton.onMousePressed(n, n2, n3)) {
                return true;
            }
            if (!this.isExpanded()) {
                return true;
            }
        }
        if (bl && this.isExpanded() && this.settingsContainer.onMousePressed(n, n2, n3)) {
            return true;
        }
        return bl;
    }

    @Override
    public float render(float f, Matrix4f matrix4f2) {
        this.update4();
        this.update5();
        this.settingsContainer.setSettingsColumnLayout(this.getSettingsColumnLayout());
        GuiBounds bounds2 = this.getBounds();
        this.updateHeaderWidth();
        float f2 = 34.5f;
        float f3 = 5.0f;
        float f4 = 12.0f;
        float f5 = this.settingDescriptionRenderer.getPreferredHeight();
        this.expansionProgress = FrameInterpolator.lerpTowards(this.expansionProgress, this.isExpanded() ? 1.0f : 0.0f, 20.0f);
        float f6 = bounds2.getY() + f5 + 2.0f - f3;
        float f7 = this.module.getDescription().isBlank() ? f6 : bounds2.getY() + 20.0f + this.settingDescriptionRenderer.getFloatType4() + 2.0f - f4;
        float f8 = f6 + (f7 - f6) * this.expansionProgress;
        float f9 = this.settingsContainer.getFloatType();
        float f10 = Math.max(f2, f7 - bounds2.getY() + f9);
        float f11 = f5 + (f10 - f5) * this.expansionProgress;
        float f12 = Math.max(0.0f, f11 - (f8 - bounds2.getY()));
        float f13 = bounds2.getX() + 2.0f;
        float f14 = bounds2.getWidth() - 4.0f;
        boolean bl = this.settingsContainer.hasSettings() && f9 > 0.01f && this.expansionProgress > 0.01f;
        boolean bl2 = bl && this.expansionProgress < 0.99f;
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.settingsContainer.getBounds().setPosition(f13, f8);
        this.settingsContainer.getBounds().setSize(f14, f12);
        drawApi.beginStencil(1);
        drawApi.drawRoundedRectangle(matrix4f2, bounds2.getX(), bounds2.getY() - 0.25f, bounds2.getWidth(), f11, 8.0f, ColorUtils.rgba(0, 0, 0, 0));
        drawApi.applyStencilMask(1);
        drawApi.drawRoundedRectangleOutlined(matrix4f2, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), f11, 8.0f, 0.75f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), ThemeColors.borderPrimary());
        this.settingDescriptionRenderer.renderWithCollapse(matrix4f2, drawApi, this.expansionProgress);
        this.update6();
        for (GuiElement element2 : this.settingElements) {
            if (element2 == this.expandButton && !this.isActive2()) continue;
            element2.render(f, matrix4f2);
        }
        if (this.settingKeybindButton != null) {
            this.settingKeybindButton.render(f, matrix4f2);
        }
        if (bl && f12 > 0.01f) {
            float f15 = bl2 ? f9 : f12;
            this.settingsContainer.getBounds().setPosition(0.0f, 0.0f);
            this.settingsContainer.getBounds().setSize(f14, f15);
            ClippedLayerRenderer.renderLayer(drawApi, matrix4f2, f13, f8, f14, f9, 0.0f, bl2, ColorUtils.withAlpha(-1, 255.0f * this.expansionProgress), matrix4f -> this.settingsContainer.render(f, (Matrix4f)matrix4f));
            this.settingsContainer.getBounds().setPosition(f13, f8);
            this.settingsContainer.getBounds().setSize(f14, f12);
        }
        this.settingsContainer.update2();
        float f16 = ModuleTogglePulse.progress(this.module);
        if (f16 > 0.0f) {
            int n = ThemeManager.getThemeManager().isDarkTheme() ? 235 : 140;
            drawApi.drawShimmer(matrix4f2, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), f11, 8.0f, f16, ColorUtils.withAlpha(ThemeColors.accent(), (float)n));
        }
        drawApi.endStencil();
        bounds2.setSize(bounds2.getWidth(), f11);
        return bounds2.getY() + f11;
    }

    @Override
    public boolean onCharTyped(char c) {
        for (GuiElement element2 : this.settingElements) {
            if (!element2.onCharTyped(c)) continue;
            return true;
        }
        this.settingsContainer.setSettingsColumnLayout(this.getSettingsColumnLayout());
        return this.isExpanded() && this.settingsContainer.onCharTyped(c);
    }

    public boolean isExpanded() {
        return this.expandButton.isExpanded();
    }

    @Override
    public void update2() {
        for (GuiElement element2 : this.settingElements) {
            element2.update2();
        }
        if (this.settingKeybindButton != null) {
            this.settingKeybindButton.update2();
        }
        this.settingsContainer.update2();
    }

    @Override
    public void setBooleanType(boolean bl) {
        boolean bl2 = this.expandButton.isExpanded();
        this.expandButton.setExpanded(bl);
        if (bl2 && !bl) {
            this.settingsContainer.update2();
            this.closeFloatingPanels(this);
        }
    }

    @Override
    public boolean onKeyPressed(int n) {
        for (GuiElement element2 : this.settingElements) {
            if (!element2.onKeyPressed(n)) continue;
            return true;
        }
        this.settingsContainer.setSettingsColumnLayout(this.getSettingsColumnLayout());
        return this.isExpanded() && this.settingsContainer.onKeyPressed(n);
    }

    public float getFloatType() {
        float f = this.settingDescriptionRenderer.getPreferredHeight();
        if (this.expansionProgress < 0.001f) {
            return f;
        }
        float f2 = 34.5f;
        float f3 = 5.0f;
        float f4 = 12.0f;
        float f5 = this.module.getDescription().isBlank() ? f + 2.0f - f3 : 20.0f + this.settingDescriptionRenderer.getFloatType4() + 2.0f - f4;
        float f6 = Math.max(f2, f5 + this.settingsContainer.getFloatType());
        return f + (f6 - f) * this.expansionProgress;
    }

    public void collapse() {
        this.setBooleanType(false);
        this.expansionProgress = 0.0f;
        this.getBounds().setSize(this.getBounds().getWidth(), this.settingDescriptionRenderer.getPreferredHeight());
    }

    public Module getModule() {
        return this.module;
    }

    private void updateHeaderWidth() {
        GuiBounds bounds2 = this.getBounds();
        this.settingDescriptionRenderer.setWrapWidth(Math.max(0.0f, bounds2.getWidth() - 2.0f * this.settingDescriptionRenderer.getContentOffset()));
    }

    @Override
    public boolean isActive2() {
        return this.settingsContainer.hasSettings();
    }

    private void update4() {
        List<Setting> list = this.module.getSettings();
        if (list.size() == this.knownSettingCount) {
            return;
        }
        for (int i = this.knownSettingCount; i < list.size(); ++i) {
            SettingRow<?> settingRow2 = SettingRowFactory.createRow(list.get(i), this.containerDisplay);
            if (settingRow2 == null || settingRow2.isHeaderControl()) continue;
            this.settingsContainer.addChild(settingRow2);
        }
        this.knownSettingCount = list.size();
    }

    private SettingsColumnLayout getSettingsColumnLayout() {
        return this.containerDisplay.getContainerColumnLayout() == ContainerColumnLayout.SINGLE_COLUMN ? SettingsColumnLayout.TWO_COLUMNS : SettingsColumnLayout.SINGLE_COLUMN;
    }

    private void update5() {
        boolean bl = this.isActive2();
        boolean bl2 = this.expandButton.isExpanded();
        this.expandButton.setInteractive(bl);
        if (!bl) {
            this.expandButton.setExpanded(false);
            if (bl2) {
                this.settingsContainer.update2();
                this.closeFloatingPanels(this);
            }
        }
    }

    private void closeFloatingPanels(GuiElement element) {
        FloatingPanelProvider provider;
        FloatingPanel panel;
        if (element instanceof FloatingPanelProvider && (panel = (provider = (FloatingPanelProvider)((Object)element)).getFloatingPanel()) != null) {
            panel.setBooleanType(false);
        }
        for (GuiElement child : element.getChildren()) {
            this.closeFloatingPanels(child);
        }
    }

    private void update6() {
        GuiBounds bounds2 = this.getBounds();
        float f = 7.0f;
        float f2 = 10.5f;
        float f3 = bounds2.getY() + 7.0f;
        float f4 = 5.0f;
        for (GuiElement element2 : this.settingElements) {
            if (element2 == this.expandButton && !this.isActive2()) continue;
            float f5 = element2 == this.expandButton ? 12.5f : ((SettingComponent)element2).getPreferredWidth();
            float f6 = element2 == this.expandButton ? f2 : ((SettingComponent)element2).getPreferredHeight();
            float f7 = bounds2.getX() + bounds2.getWidth() - f - f5;
            float f8 = f3 + (f2 - f6) / 2.0f;
            element2.getBounds().setPosition(f7, f8);
            element2.getBounds().setSize(f5, f6);
            f += f5 + f4;
        }
        if (this.settingKeybindButton != null && this.settingComponent != null) {
            this.settingKeybindButton.setBounds(this.settingComponent.getBounds());
            this.settingKeybindButton.setPopupAnchorPosition(this.settingKeybindButton.getBounds().getX(), this.settingKeybindButton.getBounds().getY());
            this.settingKeybindButton.setClipBounds(this.settingComponent.getBounds().getX(), this.settingComponent.getBounds().getY(), this.settingComponent.getBounds().getWidth(), this.settingComponent.getBounds().getHeight());
        }
    }
}

