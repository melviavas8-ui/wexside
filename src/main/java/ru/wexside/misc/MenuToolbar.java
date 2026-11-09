package ru.wexside.misc;

import java.util.function.BooleanSupplier;
import org.joml.Matrix4f;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.IconButton;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.NavigationState;
import ru.wexside.misc.ThemeManager;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;

public class MenuToolbar
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final float value;
    private final GuiBounds bounds3;
    private final float value2;
    private final IconButton iconButton;
    private final IconButton iconButton2;
    private final IconButton iconButton3;
    private final float value3;
    private final float value4;
    private final GuiBounds bounds4;
    private final float value5;

    public MenuToolbar(GuiBounds bounds3, GuiBounds bounds5, NavigationState navigationState, Runnable runnable, BooleanSupplier booleanSupplier) {
        super(new GuiBounds(bounds3.getX(), bounds3.getY(), bounds3.getWidth(), bounds3.getHeight()));
        this.value4 = 25.165f;
        this.value = 26.665f;
        this.value2 = 13.0f;
        this.value3 = 12.0f;
        this.value5 = 2.0f;
        this.bounds3 = bounds3;
        this.bounds4 = bounds5;
        this.iconButton = new IconButton(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f), "Ф", runnable, booleanSupplier);
        this.iconButton3 = new IconButton(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f), "Ч", ThemeManager.getThemeManager()::cycleTheme);
        this.iconButton2 = new IconButton(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f), "v", "э", navigationState::update);
        this.addChild(this.iconButton);
        this.addChild(this.iconButton3);
        this.addChild(this.iconButton2);
    }

    public void setCompactProgress(float f) {
        this.getBounds().setPosition(this.lerp(this.bounds3.getX(), this.bounds4.getX(), f), this.bounds3.getY());
        this.getBounds().setSize(this.lerp(this.bounds3.getWidth(), this.bounds4.getWidth(), f), this.bounds3.getHeight());
        GuiBounds[] cls0254Array = this.createHorizontalButtonBounds(this.bounds3);
        GuiBounds[] cls0254Array2 = this.createVerticalButtonBounds(this.bounds4);
        this.interpolateButtonBounds(this.iconButton, cls0254Array[0], cls0254Array2[0], f);
        this.interpolateButtonBounds(this.iconButton3, cls0254Array[1], cls0254Array2[1], f);
        this.interpolateButtonBounds(this.iconButton2, cls0254Array[2], cls0254Array2[2], f);
        this.iconButton.setFloatType(f);
        this.iconButton3.setFloatType(f);
        this.iconButton2.setFloatType(f);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
        this.iconButton.update();
        this.iconButton3.update();
        this.iconButton2.update();
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        return super.onMousePressed(n, n2, n3);
    }

    private float lerp(float f, float f2, float f3) {
        return f * (1.0f - f3) + f2 * f3;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds3 = this.getBounds();
        super.render(f, matrix4f);
        return bounds3.getY() + bounds3.getHeight();
    }

    private void interpolateButtonBounds(IconButton iconButton, GuiBounds bounds3, GuiBounds bounds5, float f) {
        iconButton.getBounds().setPosition(this.lerp(bounds3.getX(), bounds5.getX(), f), this.lerp(bounds3.getY(), bounds5.getY(), f));
        iconButton.getBounds().setSize(this.lerp(bounds3.getWidth(), bounds5.getWidth(), f), this.lerp(bounds3.getHeight(), bounds5.getHeight(), f));
    }

    private GuiBounds[] createVerticalButtonBounds(GuiBounds bounds3) {
        float f = 40.0f;
        float f2 = bounds3.getY() + bounds3.getHeight();
        float f3 = f2 - f;
        float f4 = bounds3.getX() + (bounds3.getWidth() - 13.0f) / 2.0f;
        return new GuiBounds[]{new GuiBounds(f4, f3, 13.0f, 12.0f), new GuiBounds(f4, f3 + 14.0f, 13.0f, 12.0f), new GuiBounds(f4, f3 + 28.0f, 13.0f, 12.0f)};
    }

    private GuiBounds[] createHorizontalButtonBounds(GuiBounds bounds3) {
        float f = bounds3.getX();
        float f2 = bounds3.getY();
        float f3 = bounds3.getHeight();
        return new GuiBounds[]{new GuiBounds(f, f2, 25.165f, f3), new GuiBounds(f + 26.665f, f2, 25.165f, f3), new GuiBounds(f + 53.33f, f2, 25.165f, f3)};
    }
}

