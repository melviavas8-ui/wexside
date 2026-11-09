package ru.wexside.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.BoundsSupplier;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutGroup;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.PopupOwner;
import ru.wexside.misc.PositionedLayoutGroup;
import ru.wexside.misc.PreparedLayer;
import ru.wexside.misc.SettingsColumnLayout;
import ru.wexside.misc.ThemeColors;
import ru.wexside.misc.VisibilityCondition;
import ru.wexside.render.RenderFrameClock;
import ru.wexside.setting.Setting;
import ru.wexside.ui.FloatingPanelProvider;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.setting.SettingRow;
import ru.wexside.util.ClippedLayerRenderer;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class SettingsContainer
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
BoundsSupplier {
    private SettingsColumnLayout settingsColumnLayout;
    private float rowGap = 2.0f;
    private float separatorThickness = 0.5f;
    private Map<String, Float> map2 = new HashMap<String, Float>();
    private Map<String, Integer> map3 = new HashMap<String, Integer>();
    private float columnGap = 10.0f;
    private float bottomPadding = 2.0f;
    private float separatorWidth = 142.5f;

    public SettingsContainer(GuiBounds bounds2) {
        super(bounds2);
        this.settingsColumnLayout = SettingsColumnLayout.SINGLE_COLUMN;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        for (PositionedLayoutGroup positionedLayoutGroup : this.getList2()) {
            if (!this.isGroupInteractive(positionedLayoutGroup.getLayoutGroup())) continue;
            for (GuiElement element2 : positionedLayoutGroup.getLayoutGroup().getChildren()) {
                element2.onMouseScroll(n, n2, d);
            }
        }
    }

    @Override
    public void update() {
        for (PositionedLayoutGroup positionedLayoutGroup : this.getList2()) {
            if (!this.isGroupInteractive(positionedLayoutGroup.getLayoutGroup())) continue;
            for (GuiElement element2 : positionedLayoutGroup.getLayoutGroup().getChildren()) {
                element2.update();
            }
        }
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        GuiBounds bounds2 = this.getBounds();
        int n4 = (int)((float)n - bounds2.getX());
        int n5 = (int)((float)n2 - bounds2.getY());
        for (PositionedLayoutGroup positionedLayoutGroup : this.getList2()) {
            if (!this.isGroupInteractive(positionedLayoutGroup.getLayoutGroup())) continue;
            for (GuiElement element2 : positionedLayoutGroup.getLayoutGroup().getChildren()) {
                if (!element2.onMousePressed(n4, n5, n3)) continue;
                return true;
            }
        }
        return bounds2.contains(n, n2);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        List<PositionedLayoutGroup> list = this.getList2();
        if (list.isEmpty() || bounds2.getHeight() <= 0.01f) {
            return bounds2.getY() + bounds2.getHeight();
        }
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(bounds2.getX(), bounds2.getY(), 0.0f);
        for (PositionedLayoutGroup positionedLayoutGroup : list) {
            this.renderLayoutGroup(f, matrix4f2, drawApi, positionedLayoutGroup);
        }
        return bounds2.getY() + bounds2.getHeight();
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        GuiBounds bounds2 = this.getBounds();
        int n4 = (int)((float)n - bounds2.getX());
        int n5 = (int)((float)n2 - bounds2.getY());
        for (PositionedLayoutGroup positionedLayoutGroup : this.getList2()) {
            if (!this.isGroupInteractive(positionedLayoutGroup.getLayoutGroup())) continue;
            for (GuiElement element2 : positionedLayoutGroup.getLayoutGroup().getChildren()) {
                element2.onMouseReleased(n4, n5, n3);
            }
        }
    }

    @Override
    public boolean onCharTyped(char c) {
        for (PositionedLayoutGroup positionedLayoutGroup : this.getList2()) {
            if (!this.isGroupInteractive(positionedLayoutGroup.getLayoutGroup())) continue;
            for (GuiElement element2 : positionedLayoutGroup.getLayoutGroup().getChildren()) {
                if (!element2.onCharTyped(c)) continue;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyPressed(int n) {
        for (PositionedLayoutGroup positionedLayoutGroup : this.getList2()) {
            if (!this.isGroupInteractive(positionedLayoutGroup.getLayoutGroup())) continue;
            for (GuiElement element2 : positionedLayoutGroup.getLayoutGroup().getChildren()) {
                if (!element2.onKeyPressed(n)) continue;
                return true;
            }
        }
        return false;
    }

    @Override
    public void addChild(GuiElement element2) {
        super.addChild(element2);
    }

    public void setSettingsColumnLayout(SettingsColumnLayout settingsColumnLayout) {
        this.settingsColumnLayout = settingsColumnLayout == null ? SettingsColumnLayout.SINGLE_COLUMN : settingsColumnLayout;
    }

    public boolean hasSettings() {
        return !this.getChildren().isEmpty();
    }

    @Override
    public void update2() {
        for (PositionedLayoutGroup positionedLayoutGroup : this.getList2()) {
            for (GuiElement element2 : positionedLayoutGroup.getLayoutGroup().getChildren()) {
                if (!(element2 instanceof SettingRow)) continue;
                SettingRow settingRow2 = (SettingRow)element2;
                settingRow2.resetVisibilityAnimation();
            }
        }
    }

    private void renderLayoutGroup(float f, Matrix4f matrix4f2, GuiDrawApi drawApi, PositionedLayoutGroup positionedLayoutGroup) {
        boolean bl;
        LayoutGroup layoutGroup2 = positionedLayoutGroup.getLayoutGroup();
        if (layoutGroup2.getChildren().isEmpty() || layoutGroup2.getAnimationProgress() <= 0.0f) {
            return;
        }
        float f2 = positionedLayoutGroup.getX();
        float f3 = positionedLayoutGroup.getY();
        float f4 = positionedLayoutGroup.getWidth();
        boolean bl2 = positionedLayoutGroup.hasLeadingGap();
        float f5 = this.animatedGroupHeight(layoutGroup2, bl2);
        float f6 = this.measureGroupHeight(layoutGroup2, bl2);
        int n = ColorUtils.withAlpha(-1, 255.0f * layoutGroup2.getAnimationProgress());
        boolean bl3 = bl = layoutGroup2.getAnimationProgress() > 0.0f && layoutGroup2.getAnimationProgress() < 1.0f;
        if (!bl) {
            ClippedLayerRenderer.renderLayer(drawApi, matrix4f2, f2, f3, f4, f6, 0.0f, false, -1, matrix4f -> this.renderGroupContents(layoutGroup2, f, (Matrix4f)matrix4f, f2, f3, f4, bl2));
            return;
        }
        PreparedLayer preparedLayer = drawApi.prepareDedicatedLayer(matrix4f2, f2, f3, f4, f6, 0.0f);
        drawApi.beginLayerFrame(preparedLayer.getTexture());
        Matrix4f matrix4f3 = new Matrix4f((Matrix4fc)preparedLayer.getContentMatrix()).translate(preparedLayer.contentX(), preparedLayer.contentY(), 0.0f);
        this.renderGroupContents(layoutGroup2, f, matrix4f3, f2, f3, f4, bl2);
        drawApi.endLayerFrame();
        float f7 = Math.min(1.0f, f5 / Math.max(f6, 1.0E-4f));
        drawApi.drawLayerTexture(matrix4f2, preparedLayer.getTexture(), preparedLayer.drawX(), preparedLayer.drawY(), preparedLayer.drawWidth(), f5, 0.0f, preparedLayer.maxV() * f7, preparedLayer.maxU(), 0.0f, n);
        drawApi.flushPending();
    }

    public float getFloatType() {
        List<PositionedLayoutGroup> list = this.getList2();
        if (list.isEmpty()) {
            return 0.0f;
        }
        float f = 0.0f;
        for (PositionedLayoutGroup positionedLayoutGroup : list) {
            float f2 = positionedLayoutGroup.getY() + this.animatedGroupHeight(positionedLayoutGroup.getLayoutGroup(), positionedLayoutGroup.hasLeadingGap());
            f = Math.max(f, f2);
        }
        return f + this.bottomPadding;
    }

    private float animateGroupVisibility(String string, boolean bl) {
        float f;
        int n = RenderFrameClock.currentFrame();
        Integer n2 = this.map3.get(string);
        Float f2 = this.map2.get(string);
        float f3 = f = bl ? 1.0f : 0.0f;
        if (f2 == null) {
            this.map2.put(string, Float.valueOf(f));
            this.map3.put(string, n);
            return f;
        }
        if (n2 != null && n2 == n) {
            return f2.floatValue();
        }
        float f4 = FrameInterpolator.lerpTowards(f2.floatValue(), f, 20.0f);
        if (Math.abs(f - f4) <= 0.001f) {
            f4 = f;
        }
        this.map2.put(string, Float.valueOf(f4));
        this.map3.put(string, n);
        return f4;
    }

    private float measureGroupHeight(LayoutGroup layoutGroup2, boolean bl) {
        float f = 0.0f;
        for (int i = 0; i < layoutGroup2.getChildren().size(); ++i) {
            f += this.measureElementHeight(layoutGroup2.getChildren().get(i));
            if (i >= layoutGroup2.getChildren().size() - 1) continue;
            f += this.rowGap;
        }
        if (bl) {
            f += this.rowGap;
        }
        return f;
    }

    private void renderGroupContents(LayoutGroup layoutGroup2, float f, Matrix4f matrix4f, float f2, float f3, float f4, boolean bl) {
        float f5;
        int n = ColorUtils.withAlpha(ThemeColors.borderPrimary(), 255.0f * layoutGroup2.getAnimationProgress());
        float f6 = f5 = bl ? this.rowGap : 0.0f;
        if (bl) {
            float f7 = (f4 - this.separatorWidth) / 2.0f;
            float f8 = (f5 - this.separatorThickness) / 2.0f;
            WexSideClient.getGuiRenderer().fillRectangle(matrix4f, f7, f8, this.separatorWidth, this.separatorThickness, n);
        }
        for (int i = 0; i < layoutGroup2.getChildren().size(); ++i) {
            GuiElement element2 = layoutGroup2.getChildren().get(i);
            GuiBounds bounds2 = element2.getBounds();
            float f9 = bounds2.getX();
            float f10 = bounds2.getY();
            float f11 = bounds2.getWidth();
            float f12 = bounds2.getHeight();
            float f13 = this.measureElementHeight(element2);
            float f14 = f9 - f2;
            float f15 = f10 - f3;
            bounds2.setPosition(f14, f15);
            bounds2.setSize(f4, f13);
            element2.render(f, matrix4f);
            if (i < layoutGroup2.getChildren().size() - 1) {
                float f16 = (f4 - this.separatorWidth) / 2.0f;
                float f17 = f15 + f13 + (this.rowGap - this.separatorThickness) / 2.0f;
                WexSideClient.getGuiRenderer().fillRectangle(matrix4f, f16, f17, this.separatorWidth, this.separatorThickness, n);
            }
            bounds2.setPosition(f9, f10);
            bounds2.setSize(f11, f12);
            if (!(element2 instanceof SettingRow)) continue;
            SettingRow settingRow2 = (SettingRow)element2;
            settingRow2.refreshLayout();
            settingRow2.updateLayoutState();
        }
    }

    private List<PositionedLayoutGroup> layoutTwoColumns(List<LayoutGroup> list) {
        ArrayList<PositionedLayoutGroup> arrayList = new ArrayList<PositionedLayoutGroup>(list.size());
        float f = Math.max(0.0f, (this.getBounds().getWidth() - this.columnGap) / 2.0f);
        float f2 = f + this.columnGap;
        float f3 = 0.0f;
        float f4 = 0.0f;
        boolean bl = false;
        boolean bl2 = false;
        for (int i = 0; i < list.size(); ++i) {
            LayoutGroup layoutGroup2 = list.get(i);
            boolean bl3 = (i & 1) == 0;
            float f5 = bl3 ? 0.0f : f2;
            float f6 = bl3 ? f3 : f4;
            boolean bl4 = bl3 ? bl : bl2;
            float f7 = f6 + this.animatedLeadingGap(layoutGroup2, bl4);
            this.positionGroupElements(layoutGroup2, f5, f7, f);
            arrayList.add(new PositionedLayoutGroup(layoutGroup2, f5, f6, f, bl4));
            float f8 = this.animatedGroupHeight(layoutGroup2, bl4);
            if (bl3) {
                f3 += f8;
                bl = true;
                continue;
            }
            f4 += f8;
            bl2 = true;
        }
        return arrayList;
    }

    private void closeNestedPopups(GuiElement element2) {
        FloatingPanelProvider panelProvider;
        PopupOwner popupProvider;
        if (element2 instanceof PopupOwner && (popupProvider = (PopupOwner)((Object)element2)).getPopup() != null) {
            popupProvider.getPopup().setBooleanType(false);
        }
        if (element2 instanceof FloatingPanelProvider && (panelProvider = (FloatingPanelProvider)((Object)element2)).getFloatingPanel() != null) {
            panelProvider.getFloatingPanel().setBooleanType(false);
        }
        for (GuiElement element3 : element2.getChildren()) {
            this.closeNestedPopups(element3);
        }
    }

    private boolean isGroupInteractive(LayoutGroup layoutGroup2) {
        if (layoutGroup2.getChildren().isEmpty()) {
            return false;
        }
        GuiElement element2 = layoutGroup2.getChildren().getFirst();
        if (!(element2 instanceof SettingRow)) {
            return true;
        }
        return layoutGroup2.isTargetVisible() && layoutGroup2.getAnimationProgress() >= 1.0f;
    }

    private float measureElementHeight(GuiElement element2) {
        if (element2 instanceof SettingRow) {
            SettingRow settingRow2 = (SettingRow)element2;
            return settingRow2.getPreferredHeight();
        }
        return Math.max(11.0f, element2.getBounds().getHeight());
    }

    private void resetHiddenElement(GuiElement element2) {
        element2.update2();
        element2.getBounds().setPosition(0.0f, 0.0f);
        element2.getBounds().setSize(0.0f, 0.0f);
        this.closeNestedPopups(element2);
    }

    private boolean sharesVisibilityCondition(GuiElement element2, VisibilityCondition visibilityCondition) {
        if (!(element2 instanceof SettingRow)) {
            return false;
        }
        SettingRow settingRow2 = (SettingRow)element2;
        VisibilityCondition visibilityCondition2 = ((Setting)settingRow2.getSetting()).getVisibilityCondition();
        if (visibilityCondition2 == null) {
            return false;
        }
        return visibilityCondition.getString().equals(visibilityCondition2.getString());
    }

    private List<PositionedLayoutGroup> layoutSingleColumn(List<LayoutGroup> list) {
        ArrayList<PositionedLayoutGroup> arrayList = new ArrayList<PositionedLayoutGroup>(list.size());
        float f = this.getBounds().getWidth();
        float f2 = 0.0f;
        for (int i = 0; i < list.size(); ++i) {
            LayoutGroup layoutGroup2 = list.get(i);
            boolean bl = i > 0;
            float f3 = f2 + this.animatedLeadingGap(layoutGroup2, bl);
            this.positionGroupElements(layoutGroup2, 0.0f, f3, f);
            arrayList.add(new PositionedLayoutGroup(layoutGroup2, 0.0f, f2, f, bl));
            f2 += this.animatedGroupHeight(layoutGroup2, bl);
        }
        return arrayList;
    }

    private List<LayoutGroup> buildLayoutGroups() {
        ArrayList<LayoutGroup> arrayList = new ArrayList<LayoutGroup>();
        int n = 0;
        while (n < this.children.size()) {
            int n2;
            GuiElement element2 = (GuiElement)this.children.get(n);
            if (!(element2 instanceof SettingRow)) {
                arrayList.add(new LayoutGroup(List.of(element2), 1.0f, true, false));
                ++n;
                continue;
            }
            SettingRow iiIllIIlII2 = (SettingRow)element2;
            VisibilityCondition visibilityCondition = ((Setting)iiIllIIlII2.getSetting()).getVisibilityCondition();
            if (visibilityCondition == null) {
                LayoutGroup object2 = this.groupForSettingRow(element2, iiIllIIlII2);
                if (object2 != null) {
                    arrayList.add(object2);
                }
                ++n;
                continue;
            }
            ArrayList<GuiElement> groupedRows = new ArrayList<GuiElement>();
            for (n2 = n; n2 < this.children.size() && this.sharesVisibilityCondition((GuiElement)this.children.get(n2), visibilityCondition); ++n2) {
                groupedRows.add((GuiElement)this.children.get(n2));
            }
            if (groupedRows.size() <= 1) {
                LayoutGroup layoutGroup2 = this.groupForSettingRow(element2, iiIllIIlII2);
                if (layoutGroup2 != null) {
                    arrayList.add(layoutGroup2);
                }
                ++n;
                continue;
            }
            boolean bl = false;
            for (GuiElement groupedRow : groupedRows) {
                SettingRow settingRow = (SettingRow)groupedRow;
                if (!settingRow.isTargetVisible()) {
                    this.closeNestedPopups(groupedRow);
                    continue;
                }
                bl = true;
            }
            float f = this.animateGroupVisibility(visibilityCondition.getString(), bl);
            if (bl || f > 0.0f) {
                arrayList.add(new LayoutGroup(List.copyOf(groupedRows), f, bl, true));
            } else {
                for (GuiElement groupedRow : groupedRows) {
                    this.resetHiddenElement(groupedRow);
                }
            }
            n = n2;
        }
        return arrayList;
    }

    private void positionGroupElements(LayoutGroup layoutGroup2, float f, float f2, float f3) {
        float f4 = f2;
        for (int i = 0; i < layoutGroup2.getChildren().size(); ++i) {
            GuiElement element2 = layoutGroup2.getChildren().get(i);
            float f5 = this.measureElementHeight(element2);
            element2.getBounds().setPosition(f, f4);
            element2.getBounds().setSize(f3, f5);
            if (element2 instanceof SettingRow) {
                SettingRow settingRow2 = (SettingRow)element2;
                settingRow2.updateLayoutState();
            }
            f4 += f5;
            if (i >= layoutGroup2.getChildren().size() - 1) continue;
            f4 += this.rowGap;
        }
    }

    private List<PositionedLayoutGroup> getList2() {
        List<LayoutGroup> list = this.buildLayoutGroups();
        if (list.isEmpty()) {
            return List.of();
        }
        return this.settingsColumnLayout == SettingsColumnLayout.TWO_COLUMNS ? this.layoutTwoColumns(list) : this.layoutSingleColumn(list);
    }

    private LayoutGroup groupForSettingRow(GuiElement element2, SettingRow<?> settingRow2) {
        if (!settingRow2.isTargetVisible()) {
            this.closeNestedPopups(element2);
        }
        if (!settingRow2.shouldRemainInLayout()) {
            this.resetHiddenElement(element2);
            return null;
        }
        return new LayoutGroup(List.of(element2), settingRow2.visibilityProgress(), settingRow2.isTargetVisible(), false);
    }

    private float animatedGroupHeight(LayoutGroup layoutGroup2, boolean bl) {
        return this.measureGroupHeight(layoutGroup2, bl) * layoutGroup2.getAnimationProgress();
    }

    private float animatedLeadingGap(LayoutGroup layoutGroup2, boolean bl) {
        return bl ? this.rowGap * layoutGroup2.getAnimationProgress() : 0.0f;
    }
}

