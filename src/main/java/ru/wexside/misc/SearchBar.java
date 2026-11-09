package ru.wexside.misc;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.NavigationState;
import ru.wexside.misc.SearchQueryState;
import ru.wexside.misc.SearchQueryTextAdapter;
import ru.wexside.misc.TextInputController;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class SearchBar
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final SearchQueryState searchQueryState;
    private final String string4;
    private float value;
    private final NavigationState navigationState;
    private final Runnable runnable;
    private float value5;
    private final TextInputController textInputController;

    public SearchBar(GuiBounds bounds2, NavigationState navigationState, SearchQueryState searchQueryState, Runnable runnable) {
        super(bounds2);
        this.string4 = "Поиск по имени элемента или модуля...";
        this.navigationState = navigationState;
        this.searchQueryState = searchQueryState;
        this.runnable = runnable;
        this.textInputController = new TextInputController(new SearchQueryTextAdapter(searchQueryState));
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
        this.textInputController.tick();
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (!this.navigationState.isActive2()) {
            return false;
        }
        return this.textInputController.onMousePressed(this.getBounds(), n, n2, n3);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        float f2 = this.navigationState.isActive2() ? 1.0f : 0.0f;
        this.value5 = FrameInterpolator.lerpTowards(this.value5, f2, 45.0f);
        if (this.value5 <= 0.01f) {
            return bounds2.getY() + bounds2.getHeight();
        }
        int n = (int)(255.0f * this.value5);
        float f3 = bounds2.getY();
        int n2 = ColorUtils.withAlpha(ThemeColors.textPrimary(), (float)n);
        FontRegistry.font3.drawGlyphText(matrix4f, drawApi, "Ф", bounds2.getX() + 1.0f, f3, 7.5f, n2);
        float f4 = bounds2.getX() + 1.0f + FontRegistry.font3.textWidth("Ф", 7.5f) + 2.5f;
        String string = this.textInputController.getText();
        boolean bl = string.isBlank();
        boolean bl2 = bl && !this.textInputController.isFocused();
        String string2 = bl2 ? "Поиск по имени элемента или модуля..." : string;
        this.value = FrameInterpolator.lerpTowards(this.value, this.textInputController.isAllSelected() ? 1.0f : 0.0f, 30.0f);
        int n3 = bl2 ? ThemeColors.textMuted() : ThemeColors.textPrimary();
        int n4 = bl2 ? n3 : ColorUtils.lerp(n3, ThemeColors.adjustForTheme(n3), this.value);
        int n5 = ColorUtils.withAlpha(n4, (float)n);
        float f5 = bounds2.getY();
        FontRegistry.font2.drawText(matrix4f, drawApi, string2, f4, f5, 6.5f, n5);
        if (this.textInputController.isCaretVisible()) {
            float f6 = f4 + (bl ? 0.0f : FontRegistry.font2.textWidth(string, 6.5f));
            FontRegistry.font2.drawText(matrix4f, drawApi, "|", f6, f5, 6.5f, ColorUtils.withAlpha(ThemeColors.textPrimary(), (float)n));
        }
        return bounds2.getY() + bounds2.getHeight();
    }

    @Override
    public void onMouseReleased(int n, int n2, int n3) {
        this.textInputController.blurIfOutside(this.getBounds(), n, n2);
    }

    @Override
    public boolean onCharTyped(char c) {
        if (!this.navigationState.isActive2()) {
            return false;
        }
        return this.textInputController.onCharTyped(c);
    }

    @Override
    public void update2() {
        this.textInputController.blur();
        super.update2();
    }

    @Override
    public boolean onKeyPressed(int n) {
        if (!this.navigationState.isActive2()) {
            return false;
        }
        if (n == 257 || n == 335) {
            if (this.runnable != null) {
                this.runnable.run();
            }
            return true;
        }
        return this.textInputController.onKeyPressed(n);
    }

    public void blur() {
        this.textInputController.blur();
    }

    public void focus() {
        this.textInputController.focus();
    }
}

