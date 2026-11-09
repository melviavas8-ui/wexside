package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LabeledSegmentedControl;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.ModuleKeybindEntryFactory;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.SettingsListLayout;
import ru.wexside.module.ModuleCategory;
import ru.wexside.module.ModuleManager;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.KeybindFilter;
import ru.wexside.util.ModuleKeybindGroup;
import ru.wexside.util.ScrollController;
import ru.wexside.util.Scrollbar;
import ru.wexside.util.TwoColumnLayout;

public final class KeybindsPanel
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final TwoColumnLayout twoColumnLayout;
    private final ScrollController scrollController = new ScrollController(18.0f, 30.0f);
    private final List<ModuleKeybindGroup> moduleGroups;
    private final KeybindFilter keybindFilter;
    private final LabeledSegmentedControl labeledSegmentedControl;
    private final Scrollbar scrollbar = new Scrollbar();

    public KeybindsPanel(GuiBounds bounds2, ModuleManager moduleManager) {
        super(bounds2);
        this.labeledSegmentedControl = new LabeledSegmentedControl("Л", "Все кейбинды", "Активные кейбинды");
        this.keybindFilter = new KeybindFilter();
        this.moduleGroups = ModuleKeybindEntryFactory.createGroups(moduleManager);
        this.twoColumnLayout = new TwoColumnLayout(2, 6.0f, 6.0f);
        this.addChild(this.labeledSegmentedControl);
        this.addChild(this.keybindFilter);
        for (ModuleKeybindGroup moduleKeybindGroup : this.moduleGroups) {
            this.addChild(moduleKeybindGroup);
        }
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        if (this.getBounds().contains(n, n2)) {
            this.scrollController.scrollByWheel(d, this.getFloatType());
        }
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (n3 != 0 || !this.getBounds().contains(n, n2)) {
            return false;
        }
        if (this.scrollbar.onMousePressed(n, n2, n3)) {
            return true;
        }
        if (this.labeledSegmentedControl.onMousePressed(n, n2, n3)) {
            return true;
        }
        if (this.keybindFilter.onMousePressed(n, n2, n3)) {
            return true;
        }
        for (ModuleKeybindGroup moduleKeybindGroup : this.getTabs()) {
            if (!moduleKeybindGroup.onMousePressed(n, n2, n3)) continue;
            return true;
        }
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.layoutTabs(bounds2);
        this.labeledSegmentedControl.render(f, matrix4f);
        this.keybindFilter.render(f, matrix4f);
        boolean bl = this.onlyActiveBindings();
        List<ModuleKeybindGroup> list = this.filteredModuleGroups(bl);
        SettingsListLayout settingsListLayout = new SettingsListLayout(list, f, bl);
        float f2 = bounds2.getY() + 8.0f + this.labeledSegmentedControl.getBounds().getHeight() + 6.0f;
        float f3 = this.getFloatType();
        this.scrollController.update(f3, this.twoColumnLayout.measureColumns(settingsListLayout) + 8.0f);
        drawApi.beginStencil(1);
        drawApi.drawRoundedRectangleRadii(matrix4f, bounds2.getX() + 1.0f, f2, bounds2.getWidth() - 1.5f, f3 - 0.5f, 10.5f, 0.0f, 0.0f, 0.0f, ColorUtils.rgba(0, 0, 0, 45));
        drawApi.applyStencilMask(1);
        float f4 = bounds2.getX() + 8.0f;
        float f5 = bounds2.getWidth() - 16.0f;
        float f6 = this.twoColumnLayout.renderColumns(settingsListLayout, matrix4f, f4, f2, f5, this.scrollController.getOffset(), bounds2.getY() - 1.0f, bounds2.getY() + bounds2.getHeight() + 1.0f);
        drawApi.endStencil();
        this.scrollController.setContentHeight(f3, f6 <= 0.0f ? 0.0f : f6 + 8.0f);
        this.scrollbar.render(drawApi, matrix4f, bounds2.getX() + bounds2.getWidth(), f2, f3, this.scrollController, this.getLastMouseX(), this.getLastMouseY());
        return bounds2.getY() + bounds2.getHeight();
    }

    @Override
    public void update2() {
        this.scrollController.scrollToTop();
    }

    private static boolean categoryMatchesFilter(ModuleCategory moduleCategory, List<String> list) {
        for (String string : list) {
            if (!moduleCategory.getName().equalsIgnoreCase(string)) continue;
            return true;
        }
        return false;
    }

    private List<ModuleKeybindGroup> getTabs() {
        return this.filteredModuleGroups(this.onlyActiveBindings());
    }

    private List<ModuleKeybindGroup> filteredModuleGroups(boolean bl) {
        List<String> list = this.keybindFilter.getList();
        boolean bl2 = list != null && !list.isEmpty();
        ArrayList<ModuleKeybindGroup> arrayList = new ArrayList<ModuleKeybindGroup>(this.moduleGroups.size());
        for (ModuleKeybindGroup moduleKeybindGroup : this.moduleGroups) {
            if (bl2 && !KeybindsPanel.categoryMatchesFilter(moduleKeybindGroup.getModule().getCategory(), list) || !moduleKeybindGroup.hasMatchingRows(bl)) continue;
            arrayList.add(moduleKeybindGroup);
        }
        return arrayList;
    }

    private float getFloatType() {
        return Math.max(0.0f, this.getBounds().getHeight() - 8.0f - this.labeledSegmentedControl.getBounds().getHeight() - 6.0f);
    }

    private void layoutTabs(GuiBounds bounds2) {
        this.labeledSegmentedControl.getBounds().setPosition(bounds2.getX() + 8.0f, bounds2.getY() + 8.0f);
        GuiBounds bounds3 = this.keybindFilter.getBounds();
        bounds3.setPosition(bounds2.getX() + bounds2.getWidth() - 8.0f - bounds3.getWidth(), bounds2.getY() + 8.0f);
    }

    private boolean onlyActiveBindings() {
        return this.labeledSegmentedControl.getIntType2() == 1;
    }
}

