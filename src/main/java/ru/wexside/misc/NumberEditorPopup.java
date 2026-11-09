package ru.wexside.misc;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LabeledGuiElement;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.PopupHeader;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.PopupPanel;
import ru.wexside.util.GuiDrawApi;

public class NumberEditorPopup
extends PopupPanel
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final float value;
    private final float value2;
    private final float value3;
    private final float value4;
    private final String string2;
    private final PopupHeader popupHeader;
    private final float value5;
    private final float value6;
    private final float value7;
    private final String string3;
    private final List<LabeledGuiElement> entries;
    private final float value8;

    protected NumberEditorPopup(String string, LabeledGuiElement ... cls0755Array) {
        super(new GuiBounds(0.0f, 0.0f, 125.0f, 47.0f));
        this.value5 = 125.0f;
        this.value2 = 5.0f;
        this.value6 = 5.0f;
        this.value8 = 6.0f;
        this.value4 = 5.0f;
        this.value3 = 7.0f;
        this.value7 = 6.25f;
        this.value = 3.0f;
        this.string2 = "Настройка ползунка";
        this.string3 = "B";
        this.popupHeader = new PopupHeader(new GuiBounds(5.0f, 5.0f, 115.0f, 0.0f), "Настройка ползунка", "B", string);
        this.entries = Arrays.asList(cls0755Array);
        for (LabeledGuiElement labeledGuiElement : this.entries) {
            this.addChild(labeledGuiElement.getElement());
        }
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        int n3 = (int)((float)n - this.getBounds().getX());
        int n4 = (int)((float)n2 - this.getBounds().getY());
        for (LabeledGuiElement entry : this.entries) {
            entry.getElement().onMouseScroll(n3, n4, d);
        }
    }

    @Override
    public void update() {
        for (LabeledGuiElement entry : this.entries) {
            entry.getElement().update();
        }
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        int n4 = (int)((float)n - this.getBounds().getX());
        int n5 = (int)((float)n2 - this.getBounds().getY());
        super.onMouseReleased(n4, n5, n3);
    }

    protected float getFloatType4() {
        return 6.0f;
    }

    @Override
    protected void updateLayout() {
        float f = this.getFloatType11();
        for (LabeledGuiElement labeledGuiElement : this.entries) {
            GuiElement element2 = labeledGuiElement.getElement();
            float f2 = 120.0f - element2.getBounds().getWidth();
            element2.getBounds().setPosition(f2, f);
            element2.getBounds().setSize(element2.getBounds().getWidth(), element2.getBounds().getHeight());
            f += element2.getBounds().getHeight() + 5.0f;
        }
        float f3 = this.entries.isEmpty() ? this.getFloatType11() + 7.0f : f - 5.0f + 7.0f;
        this.getBounds().setSize(125.0f, f3);
    }

    @Override
    protected void renderPopup(float f, Matrix4f matrix4f, GuiDrawApi drawApi) {
        this.popupHeader.BlockHitResult(matrix4f, drawApi);
        for (LabeledGuiElement labeledGuiElement : this.entries) {
            GuiElement element2 = labeledGuiElement.getElement();
            FontRegistry.font2.drawText(matrix4f, drawApi, labeledGuiElement.ModelPartBuilder(), 5.0f, element2.getBounds().getY() + 3.0f, 6.25f, ThemeColors.textPrimary());
            element2.render(f, matrix4f);
        }
    }

    protected float getFloatType() {
        return 6.0f;
    }

    public float getFloatType2() {
        Objects.requireNonNull(this);
        return 5.0f;
    }

    public float getFloatType3() {
        Objects.requireNonNull(this);
        return 7.0f;
    }

    public float getFloatType5() {
        Objects.requireNonNull(this);
        return 5.0f;
    }

    public String getString() {
        Objects.requireNonNull(this);
        return "Настройка ползунка";
    }

    public String getString2() {
        Objects.requireNonNull(this);
        return "B";
    }

    public float getFloatType6() {
        Objects.requireNonNull(this);
        return 6.25f;
    }

    public float getFloatType7() {
        Objects.requireNonNull(this);
        return 3.0f;
    }

    public List<LabeledGuiElement> getList() {
        return this.entries;
    }

    public float getFloatType8() {
        Objects.requireNonNull(this);
        return 6.0f;
    }

    public float getFloatType9() {
        Objects.requireNonNull(this);
        return 5.0f;
    }

    public float getFloatType10() {
        Objects.requireNonNull(this);
        return 125.0f;
    }

    private float getFloatType11() {
        return 5.0f + this.popupHeader.getHeight() + 6.0f;
    }

    public PopupHeader getPopupHeader() {
        return this.popupHeader;
    }
}

