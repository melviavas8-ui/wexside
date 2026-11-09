package ru.wexside.misc;

import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.ui.NavigationEntry;

public final class EspNavigationEntry
extends NavigationEntry
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final String string;

    public EspNavigationEntry() {
        super("visuals.esp", "ESP", "Д");
        this.string = "custom:esp";
    }

    @Override
    public String getString() {
        return this.string;
    }
}

