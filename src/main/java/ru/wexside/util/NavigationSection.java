package ru.wexside.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
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
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.NavigationEntry;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class NavigationSection
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final String string3;
    private float compactProgress;
    private final float value2;
    private final float value3;
    private boolean expanded = true;
    private final float value4;
    private final float value5;
    private final float value6;
    private final List<NavigationEntry> entries = new ArrayList<NavigationEntry>();
    private float expansionProgress = 1.0f;
    private final float value8;
    private final String string4;
    private boolean compactMode;

    public NavigationSection(String string, String string2, GuiBounds bounds2) {
        super(bounds2);
        this.value6 = 13.0f;
        this.value8 = 2.0f;
        this.value5 = 4.0f;
        this.value4 = 5.25f;
        this.value2 = 10.5f;
        this.value3 = 4.5f;
        this.string3 = string;
        this.string4 = string2;
    }

    public boolean containsHeaderPoint(int n, int n2) {
        if (this.isHiddenInCompactMode()) {
            return false;
        }
        float f = this.getBounds().getX();
        float f2 = this.getBounds().getY();
        float f3 = this.getFloatType2();
        float f4 = this.getBounds().getWidth();
        return (float)n >= f && (float)n <= f + f4 && (float)n2 >= f2 && (float)n2 <= f2 + f3;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        for (NavigationEntry navigationEntry : this.entries) {
            navigationEntry.onMouseScroll(n, n2, d);
        }
    }

    @Override
    public void update() {
        for (NavigationEntry navigationEntry : this.entries) {
            navigationEntry.update();
        }
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (this.isHiddenInCompactMode()) {
            return false;
        }
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        if (this.isHeaderHovered(n, n2)) {
            if (this.compactProgress <= 0.5f) {
                this.expanded = !this.expanded;
            }
            return true;
        }
        if (this.containsHeaderPoint(n, n2)) {
            return true;
        }
        if (!this.isExpanded()) {
            return true;
        }
        for (NavigationEntry navigationEntry : this.entries) {
            if (!navigationEntry.getBounds().contains(n, n2) || !navigationEntry.onMousePressed(n, n2, n3)) continue;
            return true;
        }
        return false;
    }

    public String getString() {
        return this.string3;
    }

    public void setCollapseState(float f, boolean bl) {
        this.compactProgress = f;
        this.compactMode = bl;
    }

    private float lerp(float f, float f2, float f3) {
        return f * (1.0f - f3) + f2 * f3;
    }

    @Override
    public boolean isActive2() {
        if (this.isHiddenInCompactMode()) {
            return false;
        }
        return this.getFloatType2() > 0.01f || this.isExpanded();
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        this.expansionProgress = FrameInterpolator.lerpTowards(this.expansionProgress, this.expanded ? 1.0f : 0.0f, 15.0f);
        if (this.isHiddenInCompactMode()) {
            this.hideEntryBounds(bounds2, bounds2.getY());
            bounds2.setSize(bounds2.getWidth(), 0.0f);
            return bounds2.getY();
        }
        float f2 = this.getFloatType2();
        float f3 = bounds2.getY() + f2;
        this.renderSectionHeader(matrix4f, bounds2);
        if (!this.isExpanded()) {
            this.hideEntryBounds(bounds2, f3);
            bounds2.setSize(bounds2.getWidth(), f2);
            return f3;
        }
        f3 = this.renderEntries(f, matrix4f, bounds2, f3);
        bounds2.setSize(bounds2.getWidth(), f3 - bounds2.getY());
        return f3;
    }

    public List<NavigationEntry> getList() {
        return Collections.unmodifiableList(this.entries);
    }

    private void hideEntryBounds(GuiBounds bounds2, float f) {
        for (NavigationEntry navigationEntry : this.entries) {
            if (navigationEntry instanceof NavigationEntry) {
                NavigationEntry navigationEntry2 = navigationEntry;
                navigationEntry2.setCompactProgress(this.compactProgress);
            }
            navigationEntry.getBounds().setPosition(bounds2.getX(), f);
            navigationEntry.getBounds().setSize(bounds2.getWidth(), 0.0f);
        }
    }

    public void addEntry(NavigationEntry navigationEntry) {
        this.entries.add(navigationEntry);
    }

    public float getFloatType() {
        return this.compactProgress;
    }

    private float getFloatType2() {
        return 10.5f * (1.0f - this.compactProgress);
    }

    public float getFloatType3() {
        Objects.requireNonNull(this);
        return 5.25f;
    }

    public float getFloatType4() {
        Objects.requireNonNull(this);
        return 4.0f;
    }

    public boolean isHeaderHovered(int n, int n2) {
        if (this.isHiddenInCompactMode()) {
            return false;
        }
        if (this.compactProgress > 0.5f) {
            return false;
        }
        float f = this.getBounds().getX();
        float f2 = this.getBounds().getY();
        float f3 = FontRegistry.font4.textWidth(this.string4, 5.25f);
        float f4 = f3 + 6.0f + 4.5f;
        float f5 = this.getFloatType9();
        return (float)n >= f && (float)n <= f + f4 && (float)n2 >= f2 && (float)n2 <= f2 + f5;
    }

    public float getExpandAnimation() {
        return this.expansionProgress;
    }

    @Override
    public void setBooleanType(boolean bl) {
        this.expanded = bl;
    }

    private float getFloatType5() {
        return this.lerp(2.0f, 4.0f, this.compactProgress);
    }

    public float getFloatType6() {
        Objects.requireNonNull(this);
        return 2.0f;
    }

    public float getFloatType7() {
        Objects.requireNonNull(this);
        return 4.5f;
    }

    private float renderEntries(float f, Matrix4f matrix4f, GuiBounds bounds2, float f2) {
        float f3 = this.getFloatType5();
        for (int i = 0; i < this.entries.size(); ++i) {
            NavigationEntry navigationEntry = this.entries.get(i);
            if (navigationEntry instanceof NavigationEntry) {
                NavigationEntry navigationEntry2 = navigationEntry;
                navigationEntry2.setCompactProgress(this.compactProgress);
            }
            navigationEntry.getBounds().setPosition(bounds2.getX(), f2);
            navigationEntry.getBounds().setSize(bounds2.getWidth(), 13.0f);
            f2 = navigationEntry.render(f, matrix4f);
            if (i >= this.entries.size() - 1) continue;
            f2 += f3;
        }
        return f2;
    }

    public float getFloatType8() {
        Objects.requireNonNull(this);
        return 10.5f;
    }

    private boolean isHiddenInCompactMode() {
        return this.compactMode && !this.expanded;
    }

    public List<NavigationEntry> getList2() {
        return this.entries;
    }

    private float getFloatType9() {
        return 5.25f * (1.0f - this.compactProgress);
    }

    public boolean isCompactMode() {
        return this.compactMode;
    }

    public float getFloatType10() {
        Objects.requireNonNull(this);
        return 13.0f;
    }

    public String getString2() {
        return this.string4;
    }

    private void renderSectionHeader(Matrix4f matrix4f, GuiBounds bounds2) {
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        float f = 1.0f - this.compactProgress;
        int n = (int)Math.max(0.0f, Math.min(255.0f, f * 255.0f));
        if (n <= 2) {
            return;
        }
        int n2 = ColorUtils.withAlpha(ThemeColors.textMuted(), (float)n);
        FontRegistry.font4.drawText(matrix4f, drawApi, this.string4, bounds2.getX(), bounds2.getY(), 5.25f, n2);
        float f2 = bounds2.getX() + 2.0f + FontRegistry.font4.textWidth(this.string4, 5.25f);
        float f3 = bounds2.getY() + 1.0f;
        float f4 = FontRegistry.font3.textWidth("D", 4.5f);
        float f5 = FontRegistry.font3.textHeight("D", 4.5f);
        float f6 = f2 + f4 / 2.0f;
        float f7 = f3 + f5 / 2.0f;
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(f6, f7, 0.0f).rotateZ((float)Math.toRadians(90.0f * this.expansionProgress)).translate(-f6, -f7, 0.0f);
        FontRegistry.font3.drawGlyphText(matrix4f2, drawApi, "D", f2, f3, 4.5f, n2);
    }
}

