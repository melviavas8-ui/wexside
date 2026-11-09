package ru.wexside.util;

import java.util.function.Supplier;
import ru.wexside.misc.AbstractColorTextEditor;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.TextField;
import ru.wexside.setting.ColorSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.HexColorTextCallback;
import ru.wexside.util.AlphaPercentageTextAdapter;
import ru.wexside.util.ColorUtils;

final class HexColorEditor
extends AbstractColorTextEditor
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final float value;
    private final TextField textField;
    private final float value2;
    private final float value3;
    private final float value4;
    private final TextField textField2;
    private final ColorSetting colorSetting;

    HexColorEditor(GuiBounds bounds2, ColorSetting colorSetting) {
        super(bounds2);
        this.value = 12.0f;
        this.value4 = 1.0f;
        this.value2 = 49.5f;
        this.value3 = 20.0f;
        this.colorSetting = colorSetting;
        Supplier<String> supplier = this::getString;
        this.textField2 = new TextField(new GuiBounds(0.0f, 0.0f, 49.5f, 12.0f), new HexColorTextCallback(colorSetting));
        this.textField = new TextField(new GuiBounds(50.5f, 0.0f, 20.0f, 12.0f), new AlphaPercentageTextAdapter(this, supplier), "%");
        this.addChild(this.textField2);
        this.addChild(this.textField);
        this.getBounds().setSize(this.getPreferredWidth(), this.getPreferredHeight());
    }

    @Override
    public float getPreferredWidth() {
        return 70.5f;
    }

    int clampPercentage(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String getString() {
        String string = this.getAlphaPercentage();
        String string2 = this.colorSetting.getAstolfoHex();
        return string2 + " " + string + "%";
    }

    String getAlphaPercentage() {
        int n = ColorUtils.unpackRgba(this.colorSetting.getColor())[3];
        return String.valueOf(Math.round((float)n / 255.0f * 100.0f));
    }

    ColorSetting getColorSetting() {
        return this.colorSetting;
    }

    @Override
    public float getPreferredHeight() {
        return 12.0f;
    }
}

