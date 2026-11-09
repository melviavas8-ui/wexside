package ru.wexside.misc;

import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LabeledGuiElement;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.NumberEditorPopup;
import ru.wexside.setting.RangeSetting;
import ru.wexside.util.CompactTextField;
import ru.wexside.util.RangeValueTextAdapter;

public final class RangeEditorPopup
extends NumberEditorPopup
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    public RangeEditorPopup(RangeSetting rangeSetting) {
        super(rangeSetting.getDisplayName(), new LabeledGuiElement("Мин. значение", new CompactTextField(new RangeValueTextAdapter(rangeSetting, true))), new LabeledGuiElement("Макс. значение", new CompactTextField(new RangeValueTextAdapter(rangeSetting, false))));
    }
}

