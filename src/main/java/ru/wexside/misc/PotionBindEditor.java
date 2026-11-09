package ru.wexside.misc;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.WexSideClient;
import ru.wexside.input.BindInput;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.KeybindCaptureField;
import ru.wexside.misc.KeybindDescriptor;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.PotionEditorState;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.GuiDrawApi;

public final class PotionBindEditor
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    public static final float value = 18.0f;
    static final String string = "Триггер кнопка";
    static final String string2 = "Зелья выбрасываются при нажатии бинда";
    private final KeybindCaptureField keybindCaptureField;
    private final float value2;
    private final float value3;
    private final float value4;
    private final float value5;

    public PotionBindEditor(PotionEditorState potionEditorState, float f) {
        super(new GuiBounds(0.0f, 0.0f, f, 18.0f));
        this.keybindCaptureField = new KeybindCaptureField(KeybindDescriptor.createDelegatingBinding("Триггер кнопка", "Зелья выбрасываются при нажатии бинда", () -> potionEditorState.getWorkingCopy().getBindInput(), bind -> potionEditorState.getWorkingCopy().setBindInput(bind)));
        this.value4 = 7.0f;
        this.value2 = 6.5f;
        this.value5 = 10.0f;
        this.value3 = 3.0f;
        this.addChild(this.keybindCaptureField);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        this.update2();
        return super.onMousePressed((int)((float)n - this.getBounds().getX()), (int)((float)n2 - this.getBounds().getY()), n3);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(bounds2.getX(), bounds2.getY(), 0.0f);
        this.update2();
        FontRegistry.font2.drawText(matrix4f2, drawApi, string, 0.0f, 0.0f, 7.0f, ThemeColors.textPrimary());
        FontRegistry.font2.drawText(matrix4f2, drawApi, string2, 0.0f, 10.0f, 6.5f, ThemeColors.textMuted());
        this.keybindCaptureField.render(f, matrix4f2);
        return bounds2.getY() + 18.0f;
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        this.keybindCaptureField.onMouseReleased((int)((float)n - this.getBounds().getX()), (int)((float)n2 - this.getBounds().getY()), n3);
    }

    @Override
    public boolean onKeyPressed(int n) {
        return this.keybindCaptureField.onKeyPressed(n);
    }

    @Override
    public void update2() {
        float f = this.keybindCaptureField.getPreferredWidth();
        this.keybindCaptureField.getBounds().setSize(f, this.keybindCaptureField.getPreferredHeight());
        this.keybindCaptureField.getBounds().setPosition(this.getBounds().getWidth() - f, 3.0f);
    }
}

