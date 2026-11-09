package ru.wexside.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MenuToolbar;
import ru.wexside.misc.ModuleCategoryNavigationEntry;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.NavigationSelectionListener;
import ru.wexside.misc.NavigationState;
import ru.wexside.module.ModuleCategory;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.NavigationEntry;
import ru.wexside.util.ClippedContentRenderer;
import ru.wexside.util.NavigationSection;
import ru.wexside.util.ScrollController;
import ru.wexside.util.UserProfilePanel;

public final class ClientMenuContent
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final float value;
    private final float value2;
    private final float value3;
    private final float value4;
    private final float value5;
    private final float value6;
    private final float value7;
    private final float value8;
    private final float value9;
    private final float value10;
    private final MenuToolbar menuToolbar;
    private final float value11;
    private final float value12;
    private final float value13;
    private final float value14;
    private final float value15;
    private final float value16;
    private final float value17;
    private final NavigationState navigationState;
    private final float value18;
    private final float value19;
    private final UserProfilePanel userProfilePanel;
    private final ClippedContentRenderer clippedContentRenderer;
    private final float value20;
    private final float value21;
    private final float value22;
    private final float value23;
    private final float value24;
    private final float value25;
    private final List<NavigationSection> navigationSections = new ArrayList<NavigationSection>();
    private final float value26;
    private final ScrollController scrollController = new ScrollController(18.0f, 30.0f);
    private final float value27;
    private final float value28;
    private final float value29;
    private final float value30;
    private final NavigationSelectionListener callback24;
    private final float value31;

    public ClientMenuContent(GuiBounds bounds2, NavigationState navigationState, NavigationSelectionListener callback24, Runnable runnable, BooleanSupplier booleanSupplier) {
        super(bounds2);
        this.value16 = 0.0f;
        this.value30 = 90.0f;
        this.value3 = 25.0f;
        this.value13 = 89.5f;
        this.value2 = 90.0f;
        this.value29 = 65.0f;
        this.value21 = 6.0f;
        this.value10 = 6.0f;
        this.value26 = 71.0f;
        this.value22 = 78.0f;
        this.value14 = 13.0f;
        this.value31 = -0.5f;
        this.value5 = 0.5f;
        this.value24 = 9.0f;
        this.value18 = 4.0f;
        this.value25 = 4.0f;
        this.value27 = 8.5f;
        this.value7 = 6.0f;
        this.value20 = 22.0f;
        this.value4 = 5.0f;
        this.value12 = 5.0f;
        this.value11 = 28.0f;
        this.value17 = 1.0f;
        this.value = 4.0f;
        this.value8 = 4.0f;
        this.value6 = 10.0f;
        this.value19 = 10.0f;
        this.clippedContentRenderer = new ClippedContentRenderer(4.0f, 10.0f, 10.0f, true);
        this.navigationState = navigationState;
        this.callback24 = callback24;
        this.getBounds().setPosition(6.0f, bounds2.getY());
        this.getBounds().setSize(78.0f, bounds2.getHeight());
        this.value23 = 30.0f;
        this.value9 = 8.0f;
        this.userProfilePanel = new UserProfilePanel(new GuiBounds(6.0f, this.value23, 78.0f, this.value9), WexSideClient.getInstance().getClientProfile());
        this.addChild(this.userProfilePanel);
        this.value15 = 12.0f;
        this.value28 = this.getBounds().getY() + this.getBounds().getHeight() - (4.75f + this.value15);
        GuiBounds bounds3 = new GuiBounds(5.5f, this.value28, 78.0f, this.value15);
        GuiBounds bounds4 = new GuiBounds(70.5f, this.value28, 13.0f, this.value15);
        this.menuToolbar = new MenuToolbar(bounds3, bounds4, navigationState, runnable, booleanSupplier);
        this.addChild(this.menuToolbar);
    }

    public String getString() {
        for (NavigationSection navigationSection : this.navigationSections) {
            Iterator<NavigationEntry> iterator = navigationSection.getList().iterator();
            if (!iterator.hasNext()) continue;
            NavigationEntry navigationEntry = iterator.next();
            return navigationEntry.getString();
        }
        return null;
    }

    public float getFloatType() {
        float f = this.navigationState.getOpenProgress();
        return 6.0f * (1.0f - f) + 71.0f * f;
    }

    public float getFloatType2() {
        float f = this.navigationState.getOpenProgress();
        return 78.0f * (1.0f - f) + 13.0f * f;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        if (!this.getBounds().contains(n, n2) || !this.isInsideNavigationY(n2)) {
            return;
        }
        this.scrollController.scrollByWheel(d, this.getFloatType12());
    }

    @Override
    public void update() {
        for (NavigationSection navigationSection : this.navigationSections) {
            for (NavigationEntry navigationEntry : navigationSection.getList()) {
                navigationEntry.setBooleanType(this.navigationState.isCategorySelected(navigationEntry.getString()));
            }
        }
        for (NavigationSection navigationSection : this.navigationSections) {
            navigationSection.update();
        }
        this.userProfilePanel.update();
        this.menuToolbar.update();
    }

    public float getFloatType3() {
        Objects.requireNonNull(this);
        return 89.5f;
    }

    public String categoryEntryId(ModuleCategory moduleCategory) {
        for (NavigationSection navigationSection : this.navigationSections) {
            for (NavigationEntry navigationEntry : navigationSection.getList()) {
                ModuleCategoryNavigationEntry categoryEntry;
                if (!(navigationEntry instanceof ModuleCategoryNavigationEntry) || (categoryEntry = (ModuleCategoryNavigationEntry)navigationEntry).getModuleCategory() != moduleCategory) continue;
                return categoryEntry.getString();
            }
        }
        return null;
    }

    public float getFloatType4() {
        Objects.requireNonNull(this);
        return 90.0f;
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (!this.isInsideNavigationY(n2)) {
            return super.onMousePressed(n, n2, n3);
        }
        for (NavigationSection navigationSection : this.navigationSections) {
            if (!navigationSection.getBounds().contains(n, n2)) continue;
            boolean bl = navigationSection.onMousePressed(n, n2, n3);
            if (navigationSection.containsHeaderPoint(n, n2)) {
                return bl;
            }
            NavigationEntry navigationEntry = this.entryAtPoint(n, n2);
            if (navigationEntry != null) {
                boolean bl2 = this.navigationState.isCategorySelected(navigationEntry.getString());
                this.navigationState.setString(navigationEntry.getString());
                this.callback24.onNavigationSelected(navigationEntry, bl2);
            }
            return bl;
        }
        return super.onMousePressed(n, n2, n3);
    }

    private boolean hasVisibleSectionFrom(int n) {
        for (int i = n; i < this.navigationSections.size(); ++i) {
            if (!this.navigationSections.get(i).isActive2()) continue;
            return true;
        }
        return false;
    }

    private float getFloatType5() {
        float f = this.value28 - 28.0f * this.navigationState.getOpenProgress();
        return f - 4.0f;
    }

    public NavigationSection sectionContainingEntry(String string) {
        for (NavigationSection navigationSection : this.navigationSections) {
            for (NavigationEntry navigationEntry : navigationSection.getList()) {
                if (!navigationEntry.getString().equals(string)) continue;
                return navigationSection;
            }
        }
        return null;
    }

    private float getFloatType6() {
        return this.getFloatType10() + this.userProfilePanel.getFloatType() + 5.0f;
    }

    public String entryIdForType(Class<? extends NavigationEntry> clazz) {
        for (NavigationSection navigationSection : this.navigationSections) {
            for (NavigationEntry navigationEntry : navigationSection.getList()) {
                if (!clazz.isInstance(navigationEntry)) continue;
                return navigationEntry.getString();
            }
        }
        return null;
    }

    public void setNavigationSection(NavigationSection navigationSection) {
        this.navigationSections.add(navigationSection);
    }

    public float getFloatType7() {
        Objects.requireNonNull(this);
        return 78.0f;
    }

    private boolean isInsideNavigationY(int n) {
        return (float)n >= this.getFloatType11() - 1.0f && (float)n <= this.getFloatType5();
    }

    public float getFloatType8() {
        float f = this.navigationState.getOpenProgress();
        return 90.0f * (1.0f - f) + 25.0f * f;
    }

    private NavigationEntry entryAtPoint(int n, int n2) {
        for (NavigationSection navigationSection : this.navigationSections) {
            if (!navigationSection.isExpanded()) continue;
            for (NavigationEntry navigationEntry : navigationSection.getList()) {
                if (!navigationEntry.getBounds().contains(n, n2)) continue;
                return navigationEntry;
            }
        }
        return null;
    }

    private float renderSections(float f, Matrix4f matrix4f, GuiBounds bounds2, float f2, float f3, float f4) {
        float f5 = this.lerp(9.0f, 8.5f, f3);
        float f6 = 4.0f * f3;
        for (int i = 0; i < this.navigationSections.size(); ++i) {
            NavigationSection navigationSection = this.navigationSections.get(i);
            navigationSection.getBounds().setPosition(bounds2.getX(), f4);
            navigationSection.getBounds().setSize(f2, navigationSection.getBounds().getHeight());
            float f7 = f4;
            f4 = navigationSection.render(f, matrix4f);
            if (!navigationSection.isActive2()) {
                f4 = f7;
                continue;
            }
            if (this.hasVisibleSectionFrom(i + 1) && f3 > 0.01f) {
                int n = (int)Math.max(0.0f, Math.min(255.0f, f3 * 255.0f));
                int n2 = 0xD9D9D9 | n << 24;
                float f8 = bounds2.getX() + (f2 - 6.0f) / 2.0f;
                WexSideClient.getGuiRenderer().fillRectangle(matrix4f, f8, f4 + f6, 6.0f, 0.5f, n2);
            }
            if (!this.hasVisibleSectionFrom(i + 1)) continue;
            f4 += f5;
        }
        return f4;
    }

    public void setString(String string) {
        NavigationEntry navigationEntry = this.entryById(string);
        if (navigationEntry == null) {
            return;
        }
        float f = this.getFloatType12();
        float f2 = navigationEntry.getBounds().getY() - this.getFloatType11();
        float f3 = f2 + navigationEntry.getBounds().getHeight();
        float f4 = this.scrollController.getOffset();
        if (f2 + f4 < 0.0f) {
            this.scrollController.scrollTo(-f2, f);
        } else if (f3 + f4 > f) {
            this.scrollController.scrollTo(f - f3, f);
        }
    }

    private float lerp(float f, float f2, float f3) {
        return f * (1.0f - f3) + f2 * f3;
    }

    public float getFloatType9() {
        float f = this.navigationState.getOpenProgress();
        return 0.0f * (1.0f - f) + 65.0f * f;
    }

    private float getFloatType10() {
        return 27.0f;
    }

    private float getFloatType11() {
        return this.lerp(this.getBounds().getY(), this.getFloatType6(), this.navigationState.getOpenProgress());
    }

    public NavigationEntry entryById(String string) {
        for (NavigationSection navigationSection : this.navigationSections) {
            for (NavigationEntry navigationEntry : navigationSection.getList()) {
                if (!navigationEntry.getString().equals(string)) continue;
                return navigationEntry;
            }
        }
        return null;
    }

    private float getFloatType12() {
        return Math.max(0.0f, this.getFloatType5() - this.getFloatType11());
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        this.navigationState.updateOpenProgress();
        float f2 = this.navigationState.getOpenProgress();
        float f3 = this.getFloatType2();
        float f4 = this.getFloatType();
        float f5 = this.getFloatType10();
        bounds2.setPosition(f4, bounds2.getY());
        bounds2.setSize(f3, bounds2.getHeight());
        this.userProfilePanel.getBounds().setPosition(bounds2.getX(), this.value23);
        this.userProfilePanel.getBounds().setSize(f3, this.value9);
        this.userProfilePanel.setCollapsedLayout(f2, f3, f5);
        float f6 = this.getFloatType11();
        float f7 = this.getFloatType12();
        this.scrollController.update(f7, this.scrollController.getContentHeight());
        for (NavigationSection navigationSection : this.navigationSections) {
            navigationSection.setCollapseState(f2, this.navigationState.isActive());
        }
        float[] contentBottom = new float[]{f6 + this.scrollController.getOffset()};
        this.clippedContentRenderer.render(WexSideClient.getGuiRenderer(), matrix4f, bounds2.getX(), f6, f3, f7, this.scrollController.getOffset(), this.scrollController.getMinimumOffset(f7), arg_0 -> this.member5202(contentBottom, f, bounds2, f3, f2, (Matrix4f)arg_0));
        this.scrollController.setContentHeight(f7, contentBottom[0] - this.scrollController.getOffset() - f6);
        this.menuToolbar.setCompactProgress(f2);
        super.render(f, matrix4f);
        return bounds2.getY() + bounds2.getHeight();
    }

    private void member5202(float[] fArray, float f, GuiBounds bounds2, float f2, float f3, Matrix4f matrix4f) {
        fArray[0] = this.renderSections(f, matrix4f, bounds2, f2, f3, fArray[0]);
    }
}

