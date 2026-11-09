package ru.wexside.util;

import java.util.function.Supplier;
import ru.wexside.misc.TextInputModel;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.HexColorEditor;

final class AlphaPercentageTextAdapter
implements TextInputModel {
    private final HexColorEditor owner;
    private final Supplier<String> labelSupplier;

    AlphaPercentageTextAdapter(HexColorEditor owner, Supplier<String> labelSupplier) {
        this.owner = owner;
        this.labelSupplier = labelSupplier;
    }

    @Override
    public boolean accepts(char character, String currentText) {
        return Character.isDigit(character);
    }

    @Override
    public int getMaximumLength() {
        return 3;
    }

    private String normalize(String string) {
        if (string == null || string.isBlank()) {
            return "";
        }
        int n = Integer.parseInt(string);
        return String.valueOf(this.owner.clampPercentage(n));
    }

    @Override
    public void setText(String string) {
        if (string == null || string.isBlank()) {
            return;
        }
        int n = this.owner.clampPercentage(Integer.parseInt(this.normalize(string)));
        int[] nArray = ColorUtils.unpackRgba(this.owner.getColorSetting().getColor());
        int n2 = Math.round((float)n / 100.0f * 255.0f);
        this.owner.getColorSetting().setEditingColor(ColorUtils.rgba(nArray[0], nArray[1], nArray[2], n2));
    }

    @Override
    public String getText() {
        return this.owner.getAlphaPercentage();
    }

    @Override
    public String getClipboardText() {
        return this.labelSupplier.get();
    }
}

