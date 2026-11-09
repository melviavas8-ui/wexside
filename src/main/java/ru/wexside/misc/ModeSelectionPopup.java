package ru.wexside.misc;

import java.util.Arrays;
import ru.wexside.misc.AbstractOptionRow;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.CompactOptionRow;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.setting.ModeSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.SelectionPopup;
import ru.wexside.util.ScrollableOptionList;

public final class ModeSelectionPopup
extends SelectionPopup
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private static final float OPTION_WIDTH = 79.0f;
    private final ModeSetting modeSetting;
    private String[] cachedOptions;

    public ModeSelectionPopup(ModeSetting modeSetting) {
        super(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f), ModeSelectionPopup.createOptionList(modeSetting));
        this.modeSetting = modeSetting;
        this.cachedOptions = modeSetting.getOptions();
    }

    @Override
    public void update2() {
        String[] currentOptions = this.modeSetting.getOptions();
        if (!Arrays.equals(this.cachedOptions, currentOptions)) {
            this.cachedOptions = currentOptions;
            this.optionList.setAbstractOptionRow(ModeSelectionPopup.createOptionRows(this.modeSetting));
        }
        String string = this.modeSetting.getSelectedOption();
        for (AbstractOptionRow abstractOptionRow : this.optionList.getList()) {
            abstractOptionRow.setBooleanType(abstractOptionRow.getString2().equals(string));
        }
    }

    @Override
    protected void selectOption(AbstractOptionRow abstractOptionRow) {
        this.modeSetting.setSelectedOption(abstractOptionRow.getString2());
        this.update2();
        this.setBooleanType(false);
    }

    @Override
    protected void updateSelectionState() {
        this.update2();
    }

    private static AbstractOptionRow[] createOptionRows(ModeSetting modeSetting) {
        String[] options = modeSetting.getOptions();
        AbstractOptionRow[] rows = new AbstractOptionRow[options.length];
        for (int i = 0; i < options.length; ++i) {
            rows[i] = new CompactOptionRow(options[i], null, 79.0f);
        }
        return rows;
    }

    private static ScrollableOptionList createOptionList(ModeSetting modeSetting) {
        return new ScrollableOptionList(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f), ModeSelectionPopup.createOptionRows(modeSetting));
    }
}

