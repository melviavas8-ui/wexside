package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.WexSideClient;
import ru.wexside.misc.ActionButton;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LabeledTextField;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.MovablePanel;
import ru.wexside.misc.PotionBindEditor;
import ru.wexside.misc.PotionCatalogEntry;
import ru.wexside.misc.PotionEditorState;
import ru.wexside.misc.PotionPresetDraft;
import ru.wexside.misc.PotionPresetList;
import ru.wexside.misc.PotionSelectorPopup;
import ru.wexside.misc.PotionSlotButton;
import ru.wexside.misc.SectionHeader;
import ru.wexside.misc.SelectedPotionPanel;
import ru.wexside.misc.StyledActionButton;
import ru.wexside.misc.ThemeColors;
import ru.wexside.render.ItemIconRenderer;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.ClippedLayerRenderer;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.PotionPresetController;

public final class PotionCombiner
extends MovablePanel
implements MouseScrollHandler,
GuiRenderable,
BoundsProvider,
MouseButtonHandler,
CharacterInputHandler,
LayoutUpdater,
KeyPressHandler {
    static final float value = 8.0f;
    static final float value2 = 210.0f;
    private final ItemIconRenderer itemIconRenderer;
    static final float value3 = 188.0f;
    static final float value4 = 56.0f;
    private final ActionButton actionButton;
    static final float value5 = 96.0f;
    private final int slot;
    private float selectionProgress;
    private final String string2;
    static final float value7 = 5.0f;
    private final PotionPresetList potionPresetList;
    private final PotionPresetController potionPresetController2;
    static final float value8 = 11.5f;
    private final PotionSelectorPopup potionSelectorPopup;
    private final float value9;
    private final ActionButton actionButton2;
    static final float value10 = 6.0f;
    private float lastLayoutY;
    private float anchoredCenterY;
    static final float value13 = 16.0f;
    static final float value14 = 100.0f;
    private float renderOffsetY;
    private final float value16;
    private final SelectedPotionPanel selectedPotionPanel;
    private final List<PotionSlotButton> potionSlotButtons;
    private final PotionEditorState potionEditorState = new PotionEditorState();
    private final LabeledTextField labeledTextField;
    static final float value17 = 142.0f;
    private float saveEnabledProgress;
    private final SectionHeader sectionHeader;
    private final String string3;
    private final SectionHeader sectionHeader2;
    static final float value19 = 218.0f;
    static final float value20 = 34.5f;
    static final float value21 = 194.0f;
    static final float value22 = 368.0f;
    private final PotionBindEditor potionBindEditor;
    static final float value23 = 8.0f;
    private final StyledActionButton styledActionButton;
    static final float value24 = 2.0f;

    public PotionCombiner(PotionPresetController potionPresetController2) {
        super(0, 0, 368, 188);
        this.value9 = 20.0f;
        this.slot = 3;
        this.value16 = 56.0f;
        this.string2 = "Создание пресета";
        this.string3 = "Редактирование пресета";
        this.itemIconRenderer = new ItemIconRenderer();
        this.potionSlotButtons = new ArrayList<PotionSlotButton>();
        this.anchoredCenterY = Float.NaN;
        this.potionPresetController2 = potionPresetController2;
        this.sectionHeader = new SectionHeader("Создание пресета", "щ", "Potion Combiner", "Выбрасывает указанные зелья из инвентаря по нажатию указанной пользователем клавиши", 194.0f);
        this.sectionHeader2 = new SectionHeader("Настройки пресетов", "Й", "Ваши пресеты", "Созданные вами ранее пресеты для Potion Combiner", 142.0f);
        this.labeledTextField = new LabeledTextField("Имя пресета", "Уникальное имя данного пресета", 32, 194.0f);
        for (int i = 0; i < 4; ++i) {
            this.potionSlotButtons.add(new PotionSlotButton(i, this.potionEditorState, potionPresetController2, this.itemIconRenderer, this::selectPotionSlot));
        }
        this.selectedPotionPanel = new SelectedPotionPanel(this.potionEditorState, 194.0f, () -> this.openPotionSelector(this.potionEditorState.getSelectedSlot()), this::clearSelectedPotion);
        this.potionBindEditor = new PotionBindEditor(this.potionEditorState, 194.0f);
        this.styledActionButton = new StyledActionButton("Сохранить пресет", this::getButtonBackgroundColor, this::getButtonTextColor, this::savePreset);
        this.actionButton = new ActionButton("Удалить пресет", "ь", 96.0f, 16.0f, this::deletePreset);
        this.actionButton2 = new ActionButton("Выйти", "m", 34.5f, 11.5f, this::update9);
        this.potionPresetList = new PotionPresetList(new GuiBounds(218.0f, 56.0f, 142.0f, 0.0f), potionPresetController2, this.potionEditorState, this.itemIconRenderer, this::setPotionPresetDraft);
        this.potionSelectorPopup = new PotionSelectorPopup(potionPresetController2, this.potionEditorState, this.itemIconRenderer, this::setPotionCatalogEntry);
        this.sectionHeader.getBounds().setPosition(8.0f, 8.0f);
        this.sectionHeader2.getBounds().setPosition(218.0f, 8.0f);
        this.labeledTextField.getBounds().setPosition(8.0f, 56.0f);
        this.styledActionButton.getBounds().setSize(194.0f, 16.0f);
        this.addChild(this.sectionHeader);
        this.addChild(this.sectionHeader2);
        this.addChild(this.labeledTextField);
        for (PotionSlotButton potionSlotButton : this.potionSlotButtons) {
            this.addChild(potionSlotButton);
        }
        this.addChild(this.selectedPotionPanel);
        this.addChild(this.potionBindEditor);
        this.addChild(this.styledActionButton);
        this.addChild(this.actionButton);
        this.addChild(this.actionButton2);
        this.addChild(this.potionPresetList);
        this.addChild(this.potionSelectorPopup);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        int n3 = n - (int)this.getBounds().getX();
        int n4 = n2 - (int)this.getBounds().getY();
        if (this.potionSelectorPopup.isActive2() && this.potionSelectorPopup.getBounds().contains(n3, n4)) {
            this.potionSelectorPopup.onMouseScroll(n3, n4, d);
            return;
        }
        this.potionPresetList.onMouseScroll(n3, n4, d);
    }

    @Override
    public void update() {
        this.potionPresetController2.refreshInventoryIndex();
        this.itemIconRenderer.advanceFrame();
        for (PotionSlotButton potionSlotButton : this.potionSlotButtons) {
            potionSlotButton.update2();
        }
        this.potionPresetList.update4();
        this.potionSelectorPopup.prepareVisibleIcons();
        this.itemIconRenderer.bakePendingIcons();
        this.itemIconRenderer.trimUnusedIcons();
        for (PotionSlotButton potionSlotButton : this.potionSlotButtons) {
            potionSlotButton.update();
        }
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        int n4;
        int n5 = n - (int)this.getBounds().getX();
        if (this.potionSelectorPopup.onMousePressed(n5, n4 = n2 - (int)this.getBounds().getY(), n3)) {
            return true;
        }
        if (this.potionEditorState.isActive() && (this.actionButton2.onMousePressed(n5, n4, n3) || this.actionButton.onMousePressed(n5, n4, n3))) {
            return true;
        }
        if (this.potionEditorState.getSelectedSlot() >= 0 && this.selectedPotionPanel.onMousePressed(n5, n4, n3)) {
            return true;
        }
        for (PotionSlotButton potionSlotButton : this.potionSlotButtons) {
            if (!potionSlotButton.onMousePressed(n5, n4, n3)) continue;
            return true;
        }
        return this.labeledTextField.onMousePressed(n5, n4, n3) || this.potionBindEditor.onMousePressed(n5, n4, n3) || this.styledActionButton.onMousePressed(n5, n4, n3) || this.potionPresetList.onMousePressed(n5, n4, n3);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.updateLayout();
        Matrix4f matrix4f2 = Math.abs(this.renderOffsetY) < 0.001f ? matrix4f : new Matrix4f((Matrix4fc)matrix4f).translate(0.0f, this.renderOffsetY, 0.0f);
        drawApi.begin();
        this.renderWindowBackground(drawApi, matrix4f2);
        drawApi.beginStencil(3);
        drawApi.drawRoundedRectangle(matrix4f2, 0.0f, 0.0f, this.getBounds().getWidth(), this.getBounds().getHeight(), 10.5f, ColorUtils.withAlpha(-1, 0.0f));
        drawApi.applyStencilMask(3);
        this.sectionHeader.render(f, matrix4f2);
        this.sectionHeader2.render(f, matrix4f2);
        this.labeledTextField.render(f, matrix4f2);
        for (PotionSlotButton potionSlotButton : this.potionSlotButtons) {
            potionSlotButton.render(f, matrix4f2);
        }
        this.renderSelectedPotionPanel(drawApi, f, matrix4f2);
        this.potionBindEditor.render(f, matrix4f2);
        this.styledActionButton.render(f, matrix4f2);
        if (this.potionEditorState.isActive()) {
            this.actionButton.render(f, matrix4f2);
            this.actionButton2.render(f, matrix4f2);
        }
        this.potionPresetList.render(f, matrix4f2);
        drawApi.endStencil();
        this.potionSelectorPopup.render(f, matrix4f2);
        drawApi.end();
        return 0.0f;
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        int n4 = n - (int)this.getBounds().getX();
        int n5 = n2 - (int)this.getBounds().getY();
        if (this.potionSelectorPopup.isActive2() && !this.potionSelectorPopup.getBounds().contains(n4, n5)) {
            this.closeSelector();
        }
        super.onMouseReleased(n4, n5, n3);
    }

    @Override
    public boolean onCharTyped(char c) {
        return this.potionSelectorPopup.onCharTyped(c) || this.labeledTextField.onCharTyped(c);
    }

    @Override
    public void update2() {
        this.closeSelector();
        super.update2();
    }

    @Override
    public boolean onKeyPressed(int n) {
        return this.potionSelectorPopup.onKeyPressed(n) || this.labeledTextField.onKeyPressed(n) || this.potionBindEditor.onKeyPressed(n) || this.potionPresetList.onKeyPressed(n);
    }

    @Override
    public GuiBounds getVisibleBounds() {
        GuiBounds bounds2 = super.getVisibleBounds();
        if (!this.potionSelectorPopup.isActive()) {
            return bounds2;
        }
        GuiBounds bounds3 = this.potionSelectorPopup.getBounds();
        float f = Math.min(bounds2.getX(), bounds3.getX());
        float f2 = Math.min(bounds2.getY(), bounds3.getY());
        float f3 = Math.max(bounds2.getX() + bounds2.getWidth(), bounds3.getX() + bounds3.getWidth());
        float f4 = Math.max(bounds2.getY() + bounds2.getHeight(), bounds3.getY() + bounds3.getHeight());
        return new GuiBounds(f, f2, f3 - f, f4 - f2);
    }

    @Override
    public void update4() {
        this.setBooleanType3(true);
    }

    private void renderWindowBackground(GuiDrawApi drawApi, Matrix4f matrix4f) {
        float f = this.getBounds().getWidth();
        float f2 = this.getBounds().getHeight();
        drawApi.drawRoundedRectangle(matrix4f, 0.0f, 0.0f, f, f2, 10.5f, ThemeColors.backgroundPrimary());
        drawApi.drawRoundedRectangle(matrix4f, 0.0f, 0.0f, f, f2, 10.5f, ThemeColors.backgroundPrimary());
        drawApi.fillRectangle(matrix4f, 210.0f, 0.0f, 0.5f, f2, ThemeColors.borderPrimary());
    }

    private void updateLayout() {
        this.saveEnabledProgress = FrameInterpolator.lerpTowards(this.saveEnabledProgress, this.canSavePreset() ? 1.0f : 0.0f, 20.0f);
        this.selectionProgress = FrameInterpolator.lerpTowards(this.selectionProgress, this.potionEditorState.getSelectedSlot() >= 0 ? 1.0f : 0.0f, 20.0f);
        float f = 130.0f;
        float f2 = 41.5f;
        float f3 = f + f2 * this.selectionProgress;
        this.selectedPotionPanel.getBounds().setPosition(8.0f, f + 5.0f);
        this.potionBindEditor.getBounds().setPosition(8.0f, f3 + 8.0f);
        float f4 = f3 + 8.0f + 18.0f + 8.0f;
        float f5 = f4 + 16.0f + 8.0f;
        this.setFloatType(f5);
        for (int i = 0; i < this.potionSlotButtons.size(); ++i) {
            this.potionSlotButtons.get(i).getBounds().setPosition(8.0f + (float)i * 50.0f, 100.0f);
        }
        if (this.potionEditorState.isActive()) {
            this.actionButton.getBounds().setPosition(8.0f, f4);
            this.styledActionButton.getBounds().setPosition(106.0f, f4);
            this.styledActionButton.getBounds().setSize(96.0f, 16.0f);
            this.actionButton2.getBounds().setPosition(167.5f, 8.0f);
        } else {
            this.styledActionButton.getBounds().setPosition(8.0f, f4);
            this.styledActionButton.getBounds().setSize(194.0f, 16.0f);
        }
        this.sectionHeader.setString(this.potionEditorState.isActive() ? "Редактирование пресета" : "Создание пресета");
        this.potionPresetList.getBounds().setSize(142.0f, this.getBounds().getHeight() - 56.0f - 8.0f);
    }

    private int getButtonTextColor() {
        return ColorUtils.lerp(ThemeColors.textDisabled(), -1, this.saveEnabledProgress);
    }

    private void selectPotionSlot(int n) {
        if (this.potionEditorState.getWorkingCopy().getPotionId(n) == null) {
            this.openPotionSelector(n);
            return;
        }
        this.closeSelector();
        this.potionEditorState.setSelectedSlot(this.potionEditorState.getSelectedSlot() == n ? -1 : n);
    }

    private void setPotionPresetDraft(PotionPresetDraft potionPresetDraft) {
        this.closeSelector();
        this.potionEditorState.beginEditing(potionPresetDraft);
        this.labeledTextField.setString(potionPresetDraft.getName());
    }

    private void setPotionCatalogEntry(PotionCatalogEntry potionCatalogEntry) {
        int n = this.potionEditorState.getSelectorSlot();
        if (n >= 0) {
            this.potionEditorState.getWorkingCopy().setPotionId(n, potionCatalogEntry.getId());
        }
        this.closeSelector();
    }

    private void savePreset() {
        if (!this.canSavePreset()) {
            return;
        }
        String string = this.labeledTextField.getString().trim();
        this.potionEditorState.getWorkingCopy().setName(string);
        PotionPresetDraft potionPresetDraft = this.potionEditorState.isActive() ? this.potionEditorState.getOriginalPreset() : this.potionPresetController2.createPreset(string);
        this.potionEditorState.applyWorkingCopyTo(potionPresetDraft);
        this.potionPresetController2.savePresets();
        this.resetEditor();
    }

    private int getButtonBackgroundColor() {
        return ColorUtils.lerp(ThemeColors.controlFill(), ThemeColors.accent(), this.saveEnabledProgress);
    }

    private void setFloatType(float f) {
        GuiBounds bounds2 = this.getBounds();
        if (Float.isNaN(this.anchoredCenterY) || Math.abs(bounds2.getY() - this.lastLayoutY) > 0.5f) {
            this.anchoredCenterY = bounds2.getY() + bounds2.getHeight() / 2.0f;
        }
        float f2 = this.anchoredCenterY - f / 2.0f;
        this.renderOffsetY = f2 - bounds2.getY();
        bounds2.setSize(368.0f, f);
        float f3 = Math.round(f2);
        if (Math.abs(f3 - bounds2.getY()) > 0.01f) {
            bounds2.setPosition(bounds2.getX(), f3);
        }
        this.lastLayoutY = f3;
    }

    private void renderSelectedPotionPanel(GuiDrawApi drawApi, float f, Matrix4f matrix4f2) {
        if (this.selectionProgress <= 0.01f) {
            return;
        }
        GuiBounds bounds2 = this.selectedPotionPanel.getBounds();
        float f2 = bounds2.getX();
        float f3 = bounds2.getY();
        bounds2.setPosition(0.0f, 0.0f);
        ClippedLayerRenderer.renderLayer(drawApi, matrix4f2, f2, f3, bounds2.getWidth(), 36.5f * this.selectionProgress, 0.0f, this.selectionProgress < 0.99f, ColorUtils.withAlpha(-1, 255.0f * this.selectionProgress), matrix4f -> this.selectedPotionPanel.render(f, (Matrix4f)matrix4f));
        bounds2.setPosition(f2, f3);
    }

    private void clearSelectedPotion() {
        int n = this.potionEditorState.getSelectedSlot();
        if (n >= 0) {
            this.potionEditorState.getWorkingCopy().setPotionId(n, null);
        }
        this.potionEditorState.setSelectedSlot(-1);
    }

    private void deletePreset() {
        this.potionPresetController2.deletePreset(this.potionEditorState.getOriginalPreset());
        this.resetEditor();
    }

    private void resetEditor() {
        this.closeSelector();
        this.potionEditorState.reset();
        this.labeledTextField.clearAndBlur();
    }

    private void openPotionSelector(int n) {
        if (n < 0) {
            return;
        }
        this.potionEditorState.setSelectorSlot(n);
        this.potionEditorState.setSearchQuery("");
        this.potionPresetController2.refreshInventoryIndex();
        this.potionSelectorPopup.setBounds(this.potionSlotButtons.get(n).getBounds());
        this.potionSelectorPopup.rebuildFilteredOptions();
        this.potionSelectorPopup.setBooleanType(true);
    }

    private void closeSelector() {
        this.potionSelectorPopup.setBooleanType(false);
        this.potionEditorState.setSelectorSlot(-1);
    }

    private boolean canSavePreset() {
        return !this.labeledTextField.getString().trim().isEmpty() && !this.potionEditorState.getWorkingCopy().isEmpty();
    }

    private void update9() {
        this.resetEditor();
    }
}

