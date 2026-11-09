package ru.wexside.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joml.Matrix4f;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ViewModeButton;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;

public final class ViewModeSelector
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private float verticalGap;
    private float horizontalGap;
    private final List<ViewModeButton> modeButtons = new ArrayList<ViewModeButton>();
    private int columnCount;
    private int selectedIndex = -1;

    public ViewModeSelector(GuiBounds bounds2, int n, ViewModeButton ... cls0735Array) {
        super(bounds2);
        this.columnCount = Math.max(1, n);
        this.horizontalGap = 5.0f;
        this.verticalGap = 4.0f;
        this.modeButtons.addAll(Arrays.asList(cls0735Array));
        this.modeButtons.forEach(arg_0 -> this.addChild((GuiElement)arg_0));
        if (!this.modeButtons.isEmpty()) {
            this.setIntType2(0);
        }
        this.layoutModeButtons();
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        for (int i = 0; i < this.modeButtons.size(); ++i) {
            ViewModeButton viewModeButton = this.modeButtons.get(i);
            if (!viewModeButton.onMousePressed(n, n2, n3)) continue;
            this.setIntType2(i);
            return true;
        }
        return false;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        this.layoutModeButtons();
        float f2 = this.getBounds().getY();
        for (ViewModeButton viewModeButton : this.modeButtons) {
            f2 = viewModeButton.render(f, matrix4f);
        }
        return f2;
    }

    public float getHorizontalGap() {
        return this.horizontalGap;
    }

    public void setViewModeButton(ViewModeButton viewModeButton) {
        this.modeButtons.add(viewModeButton);
        this.addChild(viewModeButton);
        if (this.selectedIndex == -1) {
            this.setIntType2(0);
        } else {
            this.layoutModeButtons();
        }
    }

    public void setGridGaps(float f, float f2) {
        this.horizontalGap = f;
        this.verticalGap = f2;
        this.layoutModeButtons();
    }

    public int getIntType() {
        return this.columnCount;
    }

    public float getVerticalGap() {
        return this.verticalGap;
    }

    public void setIntType(int n) {
        this.columnCount = Math.max(1, n);
        this.layoutModeButtons();
    }

    public void setIntType2(int n) {
        if (n < 0 || n >= this.modeButtons.size()) {
            return;
        }
        this.selectedIndex = n;
        for (int i = 0; i < this.modeButtons.size(); ++i) {
            this.modeButtons.get(i).setBooleanType(i == n);
        }
        this.layoutModeButtons();
    }

    public int getIntType2() {
        return this.selectedIndex;
    }

    public List<ViewModeButton> getList() {
        return this.modeButtons;
    }

    private void layoutModeButtons() {
        float f;
        int n;
        int n2;
        if (this.modeButtons.isEmpty()) {
            this.getBounds().setSize(0.0f, 0.0f);
            return;
        }
        float f2 = this.getBounds().getX();
        float f3 = this.getBounds().getY();
        for (n2 = 0; n2 < this.modeButtons.size(); ++n2) {
            n = n2 % this.columnCount;
            int n3 = n2 / this.columnCount;
            f = f2 + (float)n * (50.0f + this.horizontalGap);
            float f4 = f3 + (float)n3 * (18.5f + this.verticalGap);
            this.modeButtons.get(n2).getBounds().setPosition(f, f4);
            this.modeButtons.get(n2).getBounds().setSize(50.0f, 18.5f);
        }
        n2 = Math.min(this.columnCount, this.modeButtons.size());
        n = (int)Math.ceil((float)this.modeButtons.size() / (float)this.columnCount);
        float f5 = (float)n2 * 50.0f + (float)Math.max(0, n2 - 1) * this.horizontalGap;
        f = (float)n * 18.5f + (float)Math.max(0, n - 1) * this.verticalGap;
        this.getBounds().setSize(f5, f);
    }
}

