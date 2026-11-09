package ru.wexside.util;

import java.util.Objects;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.setting.SettingComponent;
import ru.wexside.util.GuiDrawApi;

public final class LabeledSettingComponent
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final String string2;
    private final SettingComponent<?> settingComponent;
    private final float value2;
    private final float value3;

    public LabeledSettingComponent(String string, SettingComponent<?> settingComponent2) {
        super(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f));
        this.value2 = 6.0f;
        this.value3 = 6.0f;
        this.string2 = string;
        this.settingComponent = settingComponent2;
        this.addChild(settingComponent2);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        this.settingComponent.onMouseScroll(n, n2, d);
    }

    @Override
    public void update() {
        this.settingComponent.update();
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        float f2 = bounds2.getX() + bounds2.getWidth() - this.settingComponent.getPreferredWidth();
        float f3 = bounds2.getY() + (bounds2.getHeight() - this.settingComponent.getPreferredHeight()) / 2.0f;
        this.settingComponent.getBounds().setPosition(f2, f3);
        this.settingComponent.getBounds().setSize(this.settingComponent.getPreferredWidth(), this.settingComponent.getPreferredHeight());
        float f4 = bounds2.getY() + (bounds2.getHeight() - FontRegistry.font2.textHeight(this.string2, 6.0f)) / 2.0f;
        FontRegistry.font2.drawText(matrix4f, drawApi, this.string2, bounds2.getX(), f4, 6.0f, ThemeColors.textPrimary());
        this.settingComponent.render(f, matrix4f);
        return bounds2.getY() + bounds2.getHeight();
    }

    public float getFloatType() {
        return FontRegistry.font2.textWidth(this.string2, 6.0f) + 6.0f + this.settingComponent.getPreferredWidth();
    }

    public String getString2() {
        return this.string2;
    }

    public float getSpacing() {
        Objects.requireNonNull(this);
        return 6.0f;
    }

    public float getFloatType3() {
        Objects.requireNonNull(this);
        return 6.0f;
    }

    public SettingComponent<?> getSettingComponent() {
        return this.settingComponent;
    }

    public float getFloatType2() {
        float f = FontRegistry.font2.textHeight(this.string2, 6.0f);
        return Math.max(f, this.settingComponent.getPreferredHeight());
    }
}

