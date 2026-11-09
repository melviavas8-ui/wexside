package ru.wexside.misc;

import java.util.function.IntConsumer;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiInteractionState;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseHitTest;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.PotionCatalog;
import ru.wexside.misc.PotionCatalogEntry;
import ru.wexside.misc.PotionEditorState;
import ru.wexside.misc.ThemeColors;
import ru.wexside.render.BakedItemIcon;
import ru.wexside.render.ItemIconRenderer;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.PotionPresetController;

public final class PotionSlotButton
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
MouseHitTest {
    private final int slot;
    private final IntConsumer intConsumer;
    private float value3;
    private final ItemIconRenderer itemIconRenderer;
    private final String string2;
    private final PotionEditorState potionEditorState;
    private float value7;
    private final PotionPresetController potionPresetController2;
    private float value9;
    private final String string3;

    public PotionSlotButton(int n, PotionEditorState potionEditorState, PotionPresetController potionPresetController2, ItemIconRenderer itemIconRenderer, IntConsumer intConsumer) {
        super(new GuiBounds(0.0f, 0.0f, 44.0f, 30.0f));
        this.string2 = "Я";
        this.string3 = "С";
        this.slot = n;
        this.potionEditorState = potionEditorState;
        this.potionPresetController2 = potionPresetController2;
        this.itemIconRenderer = itemIconRenderer;
        this.intConsumer = intConsumer;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (n3 != 0 || !this.getBounds().contains(n, n2)) {
            return false;
        }
        this.intConsumer.accept(this.slot);
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        GuiInteractionState guiInteractionState = GuiInteractionState.getInstance();
        boolean bl = this.potionEditorState.getSelectorSlot() == this.slot;
        boolean bl2 = this.potionEditorState.getSelectedSlot() == this.slot;
        this.value9 = FrameInterpolator.lerpTowards(this.value9, this.containsMouse(guiInteractionState.getScaledMouseX(), guiInteractionState.getScaledMouseY()) ? 1.0f : 0.0f, 20.0f);
        this.value3 = FrameInterpolator.lerpTowards(this.value3, bl || bl2 ? 1.0f : 0.0f, 25.0f);
        this.value7 = FrameInterpolator.lerpTowards(this.value7, bl2 ? 1.0f : 0.0f, 25.0f);
        int n = ColorUtils.lerp(ColorUtils.lerp(ThemeColors.borderPrimary(), ThemeColors.borderStrong(), this.value9), ThemeColors.accent(), this.value3);
        int n2 = ColorUtils.multiplyAlpha(ThemeColors.accentTint(), this.value7);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), 44.0f, 30.0f, 16.0f, 1.0f, n2, n);
        PotionCatalogEntry potionCatalogEntry = this.getPotionCatalogEntry();
        if (potionCatalogEntry == null) {
            String string = bl ? "С" : "Я";
            int n3 = ColorUtils.lerp(ThemeColors.textPlaceholder(), ThemeColors.accent(), this.value3);
            FontRegistry.font3.drawGlyphText(matrix4f, drawApi, string, bounds2.getX() + 22.0f - FontRegistry.font3.glyphCenterX(string.charAt(0), 0.0f, 10.0f), bounds2.getY() + 15.0f - FontRegistry.font3.glyphCenterY(string.charAt(0), 0.0f, 10.0f), 10.0f, n3);
            return bounds2.getY() + 30.0f;
        }
        BakedItemIcon iiIlilllII2 = this.itemIconRenderer.getCachedIcon(this.potionPresetController2.resolveStack(potionCatalogEntry));
        this.itemIconRenderer.drawIcon(drawApi, matrix4f, iiIlilllII2, bounds2.getX() + 12.0f, bounds2.getY() + 5.0f, 20.0f, -1);
        return bounds2.getY() + 30.0f;
    }

    @Override
    public void update2() {
        PotionCatalogEntry potionCatalogEntry = this.getPotionCatalogEntry();
        if (potionCatalogEntry != null) {
            this.itemIconRenderer.getCachedIcon(this.potionPresetController2.resolveStack(potionCatalogEntry));
        }
    }

    public PotionCatalogEntry getPotionCatalogEntry() {
        return PotionCatalog.findById(this.potionEditorState.getWorkingCopy().getPotionId(this.slot));
    }

    @Override
    public boolean containsMouse(int n, int n2) {
        float f = this.getAbsoluteX();
        float f2 = this.getAbsoluteY();
        return (float)n >= f && (float)n <= f + 44.0f && (float)n2 >= f2 && (float)n2 <= f2 + 30.0f;
    }
}

