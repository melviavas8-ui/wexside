package ru.wexside.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.EmptyStatePanel;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.PotionEditorState;
import ru.wexside.misc.PotionPresetDraft;
import ru.wexside.render.ItemIconRenderer;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.PotionPresetRow;
import ru.wexside.util.ClippedContentRenderer;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.PotionPresetController;
import ru.wexside.util.ScrollController;
import ru.wexside.util.Scrollbar;

public final class PotionPresetList
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final ClippedContentRenderer clippedContentRenderer;
    private final Consumer<PotionPresetDraft> consumer;
    private final PotionPresetController potionPresetController2;
    private final float value;
    private final ItemIconRenderer itemIconRenderer;
    private final float value2;
    static final float value3 = 138.5f;
    private int slot = -1;
    private final List<PotionPresetRow> presetRows;
    private final float value4;
    private final Scrollbar scrollbar;
    private final EmptyStatePanel emptyStatePanel = new EmptyStatePanel(List.of("У вас еще нет пресетов"), 12.0f, 3.0f);
    private final ScrollController scrollController;
    private final PotionEditorState potionEditorState;
    static final float value5 = 4.0f;

    public PotionPresetList(GuiBounds bounds2, PotionPresetController potionPresetController2, PotionEditorState potionEditorState, ItemIconRenderer itemIconRenderer, Consumer<PotionPresetDraft> consumer) {
        super(bounds2);
        this.value2 = 3.5f;
        this.value4 = 12.0f;
        this.value = 3.0f;
        this.clippedContentRenderer = new ClippedContentRenderer(0.0f, 12.0f, 12.0f, false);
        this.scrollController = new ScrollController(18.0f, 30.0f);
        this.scrollbar = new Scrollbar();
        this.presetRows = new ArrayList<PotionPresetRow>();
        this.potionPresetController2 = potionPresetController2;
        this.potionEditorState = potionEditorState;
        this.itemIconRenderer = itemIconRenderer;
        this.consumer = consumer;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        if (this.getBounds().contains(n, n2)) {
            this.scrollController.scrollByWheel(d, this.getBounds().getHeight());
        }
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        if (this.scrollbar.onMousePressed(n, n2, n3)) {
            return true;
        }
        for (PotionPresetRow row : this.presetRows) {
            if (!row.onMousePressed(n, n2, n3)) continue;
            return true;
        }
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f2) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.update2();
        if (this.presetRows.isEmpty()) {
            this.emptyStatePanel.getBounds().setPosition(bounds2.getX(), bounds2.getY());
            this.emptyStatePanel.getBounds().setSize(bounds2.getWidth(), bounds2.getHeight());
            this.emptyStatePanel.render(f, matrix4f2);
            return bounds2.getY() + bounds2.getHeight();
        }
        float f2 = (float)this.presetRows.size() * 16.0f - 4.0f;
        this.scrollController.update(bounds2.getHeight(), f2);
        this.clippedContentRenderer.render(drawApi, matrix4f2, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), this.scrollController.getOffset(), this.scrollController.getMinimumOffset(bounds2.getHeight()), matrix4f -> {
            float rowY = bounds2.getY() + this.scrollController.getOffset();
            for (PotionPresetRow row : this.presetRows) {
                row.getBounds().setPosition(bounds2.getX(), rowY);
                row.setSelected(this.potionEditorState.getOriginalPreset() == row.getPreset());
                if (rowY + 12.0f >= bounds2.getY() && rowY <= bounds2.getY() + bounds2.getHeight()) {
                    row.render(f, (Matrix4f)matrix4f);
                }
                rowY += 16.0f;
            }
        });
        this.scrollController.setContentHeight(bounds2.getHeight(), f2);
        this.scrollbar.render(drawApi, matrix4f2, bounds2.getX() + bounds2.getWidth() + 3.5f, bounds2.getY(), bounds2.getHeight(), this.scrollController, this.getLastMouseX(), this.getLastMouseY());
        return bounds2.getY() + bounds2.getHeight();
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        for (PotionPresetRow row : this.presetRows) {
            row.onMouseReleased(n, n2, n3);
        }
    }

    @Override
    public boolean onKeyPressed(int n) {
        for (PotionPresetRow row : this.presetRows) {
            if (!row.onKeyPressed(n)) continue;
            return true;
        }
        return false;
    }

    public void update4() {
        this.update2();
        this.itemIconRenderer.advanceFrame();
    }

    @Override
    public void update2() {
        if (this.slot == this.potionPresetController2.getRevision()) {
            return;
        }
        this.slot = this.potionPresetController2.getRevision();
        this.presetRows.clear();
        for (PotionPresetDraft potionPresetDraft : this.potionPresetController2.getPresets()) {
            PotionPresetRow row = new PotionPresetRow(potionPresetDraft, this.potionPresetController2, this.itemIconRenderer, 138.5f, () -> this.consumer.accept(potionPresetDraft));
            row.setParent(this.getParent());
            this.presetRows.add(row);
        }
    }
}

