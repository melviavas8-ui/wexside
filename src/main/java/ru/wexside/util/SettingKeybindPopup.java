package ru.wexside.util;

import org.joml.Matrix4f;
import ru.wexside.misc.BindActivationModeSelector;
import ru.wexside.misc.BindingVisibility;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.BoundsSupplier;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.PopupManager;
import ru.wexside.misc.PopupOwner;
import ru.wexside.misc.PopupTreeBinder;
import ru.wexside.misc.SettingComponentFactory;
import ru.wexside.misc.ThemeColors;
import ru.wexside.setting.Setting;
import ru.wexside.setting.SettingKeybind;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.PopupPanel;
import ru.wexside.ui.setting.SettingComponent;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.LabeledSettingComponent;

public final class SettingKeybindPopup
extends PopupPanel
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
BoundsSupplier {
    private final LabeledSettingComponent labeledSettingComponent;
    private final LabeledSettingComponent labeledSettingComponent2;
    private final BindActivationModeSelector activationModeSelector;
    private final BindingVisibility bindingVisibility;
    private final LabeledSettingComponent labeledSettingComponent3;
    private boolean popupTreeBound;
    private final SettingKeybind settingKeybind;
    private boolean wasOpen;

    public SettingKeybindPopup(SettingKeybind settingKeybind) {
        super(new GuiBounds(0.0f, 0.0f, 96.0f, 74.0f));
        this.settingKeybind = settingKeybind;
        this.bindingVisibility = new BindingVisibility(settingKeybind);
        this.labeledSettingComponent = new LabeledSettingComponent("Значение", this.createSettingControl(this.bindingVisibility.getSetting()));
        this.labeledSettingComponent3 = new LabeledSettingComponent("Кнопка", this.createSettingControl(this.bindingVisibility.getBindSetting()));
        this.labeledSettingComponent2 = new LabeledSettingComponent("Видимость", this.createSettingControl(this.bindingVisibility.getBooleanSetting()));
        this.activationModeSelector = new BindActivationModeSelector(this.bindingVisibility.getBindActivationMode(), this.bindingVisibility::setBindActivationMode);
        this.addChild(this.labeledSettingComponent);
        this.addChild(this.labeledSettingComponent3);
        this.addChild(this.labeledSettingComponent2);
        this.addChild(this.activationModeSelector);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        int n3 = (int)((float)n - this.getBounds().getX());
        int n4 = (int)((float)n2 - this.getBounds().getY());
        for (GuiElement element2 : this.children) {
            element2.onMouseScroll(n3, n4, d);
        }
    }

    @Override
    public void update() {
        boolean bl = this.isActive2();
        if (bl && !this.wasOpen) {
            this.bindingVisibility.loadFromKeybind();
            this.settingKeybind.markEditorInitialized();
        }
        if (bl) {
            this.bindingVisibility.saveToKeybind();
            for (GuiElement element2 : this.children) {
                element2.update();
            }
        } else if (this.wasOpen) {
            this.bindingVisibility.saveToKeybind();
        }
        this.wasOpen = bl;
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        int n4 = (int)((float)n - this.getBounds().getX());
        int n5 = (int)((float)n2 - this.getBounds().getY());
        super.onMouseReleased(n4, n5, n3);
    }

    @Override
    public void update2() {
        if (this.isActive2()) {
            this.bindingVisibility.saveToKeybind();
        }
        super.update2();
    }

    public void bindPopupTree(PopupManager popupManager, PopupOwner callback34) {
        if (this.popupTreeBound || popupManager == null || callback34 == null) {
            return;
        }
        PopupTreeBinder.bindTree(this.labeledSettingComponent, popupManager, callback34);
        PopupTreeBinder.bindTree(this.labeledSettingComponent3, popupManager, callback34);
        PopupTreeBinder.bindTree(this.labeledSettingComponent2, popupManager, callback34);
        this.popupTreeBound = true;
    }

    @Override
    protected void updateLayout() {
        float f = Math.max(this.labeledSettingComponent.getFloatType(), Math.max(this.labeledSettingComponent3.getFloatType(), this.labeledSettingComponent2.getFloatType()));
        float f2 = Math.max(96.0f, f + 10.5f);
        float f3 = 21.0f;
        float f4 = f2 - 10.5f;
        this.positionSettingControl(this.labeledSettingComponent, f4, f3);
        this.positionSettingControl(this.labeledSettingComponent3, f4, f3 += this.labeledSettingComponent.getBounds().getHeight() + 4.0f);
        this.positionSettingControl(this.labeledSettingComponent2, f4, f3 += this.labeledSettingComponent3.getBounds().getHeight() + 4.0f);
        this.positionActivationSelector(this.activationModeSelector, f4, f3 += this.labeledSettingComponent2.getBounds().getHeight() + 4.0f);
        this.getBounds().setSize(f2, f3 += this.activationModeSelector.getBounds().getHeight() + 5.0f);
    }

    @Override
    protected void renderPopup(float f, Matrix4f matrix4f, GuiDrawApi drawApi) {
        GuiBounds bounds2 = this.getBounds();
        drawApi.fillRectangle(matrix4f, 0.0f, 15.0f, bounds2.getWidth(), 0.5f, ThemeColors.borderPrimary());
        FontRegistry.font2.drawText(matrix4f, drawApi, "Бинд", 4.0f, 4.0f, 5.75f, ThemeColors.textPlaceholder());
        this.renderChildren(f, matrix4f);
    }

    private void positionSettingControl(LabeledSettingComponent labeledSettingComponent, float f, float f2) {
        labeledSettingComponent.getBounds().setPosition(5.25f, f2);
        labeledSettingComponent.getBounds().setSize(f, labeledSettingComponent.getFloatType2());
    }

    private SettingComponent<?> createSettingControl(Setting setting) {
        SettingComponent<?> settingComponent = SettingComponentFactory.createComponent(setting);
        if (settingComponent == null) {
            String string = setting.getClass().getName();
            throw new IllegalStateException("Unsupported popup control setting: " + string);
        }
        return settingComponent;
    }

    private void positionActivationSelector(BindActivationModeSelector selector, float f, float f2) {
        selector.getBounds().setPosition(5.25f, f2);
        selector.getBounds().setSize(f, selector.getPreferredHeight());
    }

    @Override
    public GuiBounds getBounds() {
        return super.getBounds();
    }
}

