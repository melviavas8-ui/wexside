package ru.wexside.misc;

import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.TransitionAnimation;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.Easing;

public abstract class MovablePanel
extends GuiElement
implements MouseScrollHandler,
GuiRenderable,
BoundsProvider,
MouseButtonHandler,
CharacterInputHandler,
LayoutUpdater,
KeyPressHandler {
    private int horizontalSelection;
    private int verticalSelection;
    private boolean auxiliaryState;
    private boolean visible;
    private boolean open;
    private final TransitionAnimation transition = new TransitionAnimation(Easing.EASE_OUT_CUBIC, Easing.EASE_IN_CUBIC);

    protected MovablePanel(int x, int y, int width, int height) {
        super(new GuiBounds(x, y, width, height));
    }

    public TransitionAnimation getTransition() {
        return this.transition;
    }

    public GuiBounds getVisibleBounds() {
        return new GuiBounds(0.0f, 0.0f, this.getBounds().getWidth(), this.getBounds().getHeight());
    }

    public boolean isActive() {
        return this.open;
    }

    public void setAnchorSelection(int horizontalSelection, int verticalSelection) {
        this.horizontalSelection = horizontalSelection;
        this.verticalSelection = verticalSelection;
    }

    public void setVerticalSelection(int verticalSelection) {
        this.verticalSelection = verticalSelection;
    }

    public int getVerticalSelection() {
        return this.verticalSelection;
    }

    public void setHorizontalSelection(int horizontalSelection) {
        this.horizontalSelection = horizontalSelection;
    }

    public int getHorizontalSelection() {
        return this.horizontalSelection;
    }

    @Override
    public void setBooleanType(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isActive2() {
        return this.visible;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void setBooleanType3(boolean auxiliaryState) {
        this.auxiliaryState = auxiliaryState;
    }

    public boolean isActive3() {
        return this.auxiliaryState;
    }

    public void setPosition(int x, int y) {
        this.getBounds().setPosition(x, y);
    }

    public void open() {
        this.open = true;
    }

    public void update4() {
    }
}

