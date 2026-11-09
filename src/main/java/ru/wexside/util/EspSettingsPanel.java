package ru.wexside.util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.ContainerDisplay;
import ru.wexside.misc.EspRelationSelector;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.model.esp.EspRelation;
import ru.wexside.model.esp.EspTargetType;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleManager;
import ru.wexside.render.EspPreviewRenderer;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.EspFeatureRegistry;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.ModuleCard;
import ru.wexside.util.ScrollController;
import ru.wexside.util.SegmentedControl;
import ru.wexside.util.SegmentedControlStyle;

public final class EspSettingsPanel
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final String string2;
    private final String string3;
    private final ModuleManager moduleManager;
    private final GuiBounds bounds3;
    private float relationSelectorOpacity = 1.0f;
    private final EnumMap<EspTargetType, EnumMap<EspRelation, List<ModuleCard>>> enumMap;
    private final SegmentedControl segmentedControl;
    private final EspPreviewRenderer entity;
    private EspTargetType espTargetType;
    private final ScrollController scrollController;
    private final EspRelationSelector espRelationSelector;
    private final int slot;
    private final ContainerDisplay containerDisplay = new ContainerDisplay();
    private final GuiBounds bounds4;

    public EspSettingsPanel(GuiBounds bounds3, String string, ModuleManager moduleManager, ContainerDisplay containerDisplay) {
        super(bounds3);
        this.scrollController = new ScrollController(18.0f, 30.0f);
        this.entity = new EspPreviewRenderer();
        this.espRelationSelector = new EspRelationSelector();
        this.string2 = "Вращайте модель зажав на ней ЛКМ";
        this.slot = ColorUtils.rgba(167, 168, 174, 255);
        this.bounds3 = new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.bounds4 = new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.enumMap = new EnumMap(EspTargetType.class);
        this.espTargetType = EspTargetType.PLAYERS;
        this.string3 = string;
        this.moduleManager = moduleManager;
        SegmentedControlStyle segmentedControlStyle = new SegmentedControlStyle().withSegmentWidth(180.0f / (float)EspTargetType.values().length).withSegmentHeight(12.0f).withBackgroundRadius(8.0f).withLabelSize(6.0f).withIconSize(6.5f).withIconTextGap(2.5f);
        this.segmentedControl = new SegmentedControl(new GuiBounds(0.0f, 0.0f, 180.0f, 12.0f), segmentedControlStyle, new String[0]);
        for (EspTargetType espTargetType : EspTargetType.values()) {
            this.segmentedControl.addOption(espTargetType.getTitle(), espTargetType.getIcon());
        }
        this.segmentedControl.setIntConsumer(this::setIntType);
        this.segmentedControl.setIntType2(0);
        this.espRelationSelector.setConsumer(espRelation -> this.update4());
        this.initializeTargetCards();
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        if (this.bounds4.contains(n, n2) && this.getModuleCards() != null) {
            this.scrollController.scrollByWheel(d, this.bounds4.getHeight());
        }
    }

    @Override
    public void update() {
        this.segmentedControl.update();
        List<ModuleCard> list = this.getModuleCards();
        if (list != null) {
            for (ModuleCard moduleCard : list) {
                moduleCard.update();
            }
        }
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (n3 != 0 || !this.getBounds().contains(n, n2)) {
            return false;
        }
        if (this.segmentedControl.onMousePressed(n, n2, n3)) {
            return true;
        }
        if (this.isPlayerTargetSelected() && this.espRelationSelector.onMousePressed(n, n2, n3)) {
            return true;
        }
        if (this.bounds3.contains(n, n2)) {
            this.entity.beginRotation(n, n2, this.bounds3);
            return true;
        }
        List<ModuleCard> list = this.getModuleCards();
        if (list != null && this.bounds4.contains(n, n2)) {
            for (ModuleCard moduleCard : list) {
                if (!moduleCard.onMousePressed(n, n2, n3)) continue;
                return true;
            }
        }
        return this.getBounds().contains(n, n2);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        float f2;
        float f3;
        float f4;
        GuiBounds bounds3 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        float f5 = bounds3.getX() + 8.0f;
        float f6 = bounds3.getY() + 8.0f;
        float f7 = 162.5f;
        float f8 = f5 + f7 + 6.5f;
        float f9 = Math.max(0.0f, bounds3.getX() + bounds3.getWidth() - 8.0f - f8);
        float f10 = f6;
        float f11 = bounds3.getY() + bounds3.getHeight() - 8.0f;
        this.bounds3.setPosition(f8 + 6.0f, f10 + 24.0f);
        this.bounds3.setSize(f9 - 12.0f, f11 - f10 - 30.0f);
        this.entity.updatePreviewRotation();
        float f12 = Math.min(this.bounds3.getWidth(), this.bounds3.getHeight());
        this.entity.requestPreviewRender(this.espTargetType, this.espRelationSelector.getEspRelation(), f12 * Math.max(1.0f, Math.abs(matrix4f.m00())));
        float f13 = Math.min(f7, 180.0f);
        this.segmentedControl.getSegmentedControlStyle().withSegmentWidth(f13 / (float)EspTargetType.values().length);
        this.segmentedControl.getBounds().setPosition(f5, f6);
        this.segmentedControl.getBounds().setSize(f13, 12.0f);
        this.segmentedControl.render(f, matrix4f);
        float f14 = f6 + 12.0f + 6.0f;
        float f15 = bounds3.getY() + bounds3.getHeight() - f14;
        this.bounds4.setPosition(f5, f14);
        this.bounds4.setSize(f7, Math.max(0.0f, f15));
        List<ModuleCard> list = this.getModuleCards();
        if (list != null && !list.isEmpty() && f15 > 0.0f) {
            f4 = this.measureCardsHeight(list);
            this.scrollController.update(f15, f4);
            drawApi.beginStencil(1);
            drawApi.drawRoundedRectangle(matrix4f, f5, f14, f7, f15, 0.0f, ColorUtils.rgba(0, 0, 0, 0));
            drawApi.applyStencilMask(1);
            f3 = this.scrollController.getOffset();
            float f16 = f2 = f14 + f3;
            for (ModuleCard moduleCard : list) {
                moduleCard.getBounds().setPosition(f5, f2);
                moduleCard.getBounds().setSize(f7, moduleCard.getBounds().getHeight());
                f16 = moduleCard.render(f, matrix4f);
                f2 = f16 + 4.0f;
            }
            drawApi.endStencil();
            float f17 = f16 - f3 - f14 + 8.0f;
            this.scrollController.setContentHeight(f15, Math.max(0.0f, f17));
        }
        drawApi.drawRoundedRectangleOutlined(matrix4f, f8, f10, f9, f11 - f10, 8.0f, 0.75f, ColorUtils.rgba(0, 0, 0, 0), ThemeColors.borderPrimary());
        f4 = f10 + 13.25f;
        f3 = FontRegistry.font2.textHeight("ESP Preview", 6.75f);
        FontRegistry.font2.drawText(matrix4f, drawApi, "ESP Preview", f8 + 7.5f, f4 - f3 / 2.0f, 6.75f, ThemeColors.textSecondary());
        this.entity.render(drawApi, matrix4f, this.bounds3);
        this.relationSelectorOpacity = FrameInterpolator.lerpTowards(this.relationSelectorOpacity, this.isPlayerTargetSelected() ? 1.0f : 0.0f, 20.0f);
        this.espRelationSelector.setPosition(f8 + f9 - 7.0f - this.espRelationSelector.enabled(), f4 - 5.75f);
        this.espRelationSelector.renderWithOpacity(f, matrix4f, this.relationSelectorOpacity);
        f2 = FontRegistry.font2.textHeight("Вращайте модель зажав на ней ЛКМ", 6.1f);
        FontRegistry.font2.drawText(matrix4f, drawApi, "Вращайте модель зажав на ней ЛКМ", f8 + 7.5f, f11 - 10.25f - f2 / 2.0f, 6.1f, this.slot);
        return bounds3.getY() + bounds3.getHeight();
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        List<ModuleCard> list = this.getModuleCards();
        if (list != null) {
            for (ModuleCard moduleCard : list) {
                moduleCard.onMouseReleased(n, n2, n3);
            }
        }
    }

    @Override
    public boolean onCharTyped(char c) {
        List<ModuleCard> list = this.getModuleCards();
        if (list != null) {
            for (ModuleCard moduleCard : list) {
                if (!moduleCard.onCharTyped(c)) continue;
                return true;
            }
        }
        return false;
    }

    @Override
    public void update2() {
        this.scrollController.scrollToTop();
    }

    public void resetPreview() {
        super.update2();
        this.entity.close();
    }

    @Override
    public boolean onKeyPressed(int n) {
        List<ModuleCard> list = this.getModuleCards();
        if (list != null) {
            for (ModuleCard moduleCard : list) {
                if (!moduleCard.onKeyPressed(n)) continue;
                return true;
            }
        }
        return false;
    }

    private void initializeTargetCards() {
        EspFeatureRegistry espFeatures = WexSideClient.getEspFeatureRegistry();
        if (espFeatures == null) {
            return;
        }
        for (EspTargetType espTargetType : EspTargetType.values()) {
            EnumMap<EspRelation, List<ModuleCard>> enumMap = new EnumMap<EspRelation, List<ModuleCard>>(EspRelation.class);
            if (espTargetType == EspTargetType.PLAYERS) {
                for (EspRelation espRelation : EspRelation.values()) {
                    enumMap.put(espRelation, this.createModuleCards(espFeatures.getModules(espTargetType, espRelation)));
                }
            } else {
                List<ModuleCard> list = this.createModuleCards(espFeatures.getModules(espTargetType, EspRelation.DEFAULT));
                for (EspRelation espRelation : EspRelation.values()) {
                    enumMap.put(espRelation, list);
                }
            }
            this.enumMap.put(espTargetType, enumMap);
        }
    }

    private List<ModuleCard> getModuleCards() {
        EnumMap<EspRelation, List<ModuleCard>> enumMap = this.enumMap.get((Object)this.espTargetType);
        return enumMap == null ? null : enumMap.get((Object)this.espRelationSelector.getEspRelation());
    }

    private void setEspTargetType(EspTargetType espTargetType) {
        EnumMap<EspRelation, List<ModuleCard>> enumMap = this.enumMap.get((Object)espTargetType);
        if (enumMap == null) {
            return;
        }
        for (List<ModuleCard> list : enumMap.values()) {
            for (ModuleCard moduleCard : list) {
                moduleCard.update2();
            }
        }
    }

    private float measureCardsHeight(List<ModuleCard> list) {
        float f = 0.0f;
        for (ModuleCard moduleCard : list) {
            f += moduleCard.getBounds().getHeight() + 4.0f;
        }
        return Math.max(0.0f, f - 4.0f) + 8.0f;
    }

    private boolean isPlayerTargetSelected() {
        return this.espTargetType == EspTargetType.PLAYERS;
    }

    private void setIntType(int n) {
        EspTargetType espTargetType = this.espTargetType;
        EspTargetType[] cls0277Array = EspTargetType.values();
        if (n >= 0 && n < cls0277Array.length) {
            this.espTargetType = cls0277Array[n];
        }
        if (!this.isPlayerTargetSelected()) {
            this.espRelationSelector.setEspRelation(EspRelation.DEFAULT);
        }
        this.setEspTargetType(espTargetType);
        this.scrollController.scrollToTop();
    }

    private List<ModuleCard> createModuleCards(List<Module> list) {
        ArrayList<ModuleCard> arrayList = new ArrayList<ModuleCard>();
        for (Module module : list) {
            ModuleCard moduleCard = new ModuleCard(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f), module, this.containerDisplay);
            arrayList.add(moduleCard);
            this.addChild(moduleCard);
        }
        return arrayList;
    }

    private void update4() {
        this.setEspTargetType(this.espTargetType);
        this.scrollController.scrollToTop();
    }

    public void setModule(Module module) {
        EspFeatureRegistry espFeatures = WexSideClient.getEspFeatureRegistry();
        if (espFeatures == null || module == null) {
            return;
        }
        EspTargetType espTargetType = espFeatures.getTargetType(module);
        if (espTargetType == null) {
            return;
        }
        this.segmentedControl.setIntType2(espTargetType.ordinal());
        this.espTargetType = espTargetType;
        this.espRelationSelector.setEspRelation(this.isPlayerTargetSelected() ? espFeatures.getRelation(module) : EspRelation.DEFAULT);
        this.scrollController.scrollToTop();
    }
}

