package ru.wexside.misc;

import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseHitTest;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.PotionEditorState;
import ru.wexside.misc.PotionSearchTextAdapter;
import ru.wexside.util.SearchTextField;

public final class PotionSearchField
extends SearchTextField
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
MouseHitTest {
    static final String string = "Поиск";
    static final float value = 105.5f;

    public PotionSearchField(PotionEditorState potionEditorState) {
        super(new PotionSearchTextAdapter(potionEditorState), () -> true, string, true);
    }

    public float getPreferredWidth() {
        return 105.5f;
    }
}

