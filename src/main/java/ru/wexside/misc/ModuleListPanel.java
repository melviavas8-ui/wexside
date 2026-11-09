package ru.wexside.misc;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.ContainerColumnLayout;
import ru.wexside.misc.ContainerDisplay;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.GuiListLayoutAdapter;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.ListLayout;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.module.ModuleManager;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.ModuleCard;
import ru.wexside.util.ScrollController;
import ru.wexside.util.Scrollbar;
import ru.wexside.util.TwoColumnLayout;

public final class ModuleListPanel
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final TwoColumnLayout twoColumnLayout;
    private Module module;
    private final ModuleCategory moduleCategory;
    private final ContainerDisplay containerDisplay;
    private final Scrollbar scrollbar;
    private final ScrollController scrollController = new ScrollController(18.0f, 30.0f);

    public ModuleListPanel(GuiBounds bounds2, ModuleCategory moduleCategory, ModuleManager moduleManager, ContainerDisplay containerDisplay) {
        super(bounds2);
        this.scrollbar = new Scrollbar();
        this.twoColumnLayout = new TwoColumnLayout(2, 6.0f, 6.0f);
        this.moduleCategory = moduleCategory;
        this.containerDisplay = containerDisplay;
        moduleManager.getModules().stream().filter(module -> module.getCategory() == moduleCategory).forEach(module -> this.addChild(new ModuleCard(new GuiBounds(8.5f, 8.5f, 0.0f, 34.5f), (Module)module, containerDisplay)));
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
        if (this.getBounds().contains(n, n2)) {
            this.scrollController.scrollByWheel(d, this.getBounds().getHeight());
        }
        for (GuiElement element2 : this.children) {
            element2.onMouseScroll(n, n2, d);
        }
    }

    @Override
    public void update() {
        for (GuiElement element2 : this.children) {
            element2.update();
        }
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        if (this.scrollbar.onMousePressed(n, n2, n3)) {
            return true;
        }
        this.layoutChildren();
        return super.onMousePressed(n, n2, n3);
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        if (this.children.isEmpty()) {
            float f2 = bounds2.getY() + 15.0f;
            FontRegistry.font4.drawText(matrix4f, drawApi, this.getCategoryTitle(this.moduleCategory), bounds2.getX() + 8.0f, bounds2.getY() + 8.0f, 8.0f, ThemeColors.textPrimary());
            FontRegistry.font2.drawText(matrix4f, drawApi, "No modules in this category yet.", bounds2.getX() + 8.0f, f2 + 8.0f, 6.0f, ThemeColors.textMuted());
            return bounds2.getY() + bounds2.getHeight();
        }
        float f3 = this.isActive() ? this.measureSingleColumnHeight() : this.measureContentHeight();
        this.scrollController.update(bounds2.getHeight(), f3);
        drawApi.beginStencil(1);
        drawApi.drawRoundedRectangleRadii(matrix4f, bounds2.getX() + 1.0f, bounds2.getY() + 1.0f, this.getBounds().getWidth() - 1.5f, this.getBounds().getHeight() - 1.5f, 10.5f, 0.0f, 0.0f, 0.0f, ColorUtils.rgba(0, 0, 0, 45));
        drawApi.applyStencilMask(1);
        float f4 = this.isActive() ? this.renderSingleColumn(f, matrix4f) : this.renderTwoColumns(f, matrix4f);
        drawApi.endStencil();
        this.scrollController.setContentHeight(bounds2.getHeight(), f4);
        this.scrollbar.render(drawApi, matrix4f, bounds2.getX() + bounds2.getWidth(), bounds2.getY(), bounds2.getHeight(), this.scrollController, this.getLastMouseX(), this.getLastMouseY());
        this.setFloatType(bounds2.getHeight());
        return bounds2.getY() + bounds2.getHeight();
    }

    @Override
    public void update2() {
        this.scrollController.scrollToTop();
    }

    public void update3() {
        ModuleCard moduleCard;
        for (GuiElement element2 : this.children) {
            if (!(element2 instanceof ModuleCard)) continue;
            moduleCard = (ModuleCard)element2;
            GuiBounds bounds2 = moduleCard.getBounds();
            bounds2.setSize(bounds2.getWidth(), moduleCard.getFloatType());
        }
        this.layoutChildren();
        for (GuiElement element2 : this.children) {
            if (!(element2 instanceof ModuleCard)) continue;
            moduleCard = (ModuleCard)element2;
            if (this.intersectsViewport(moduleCard)) {
                moduleCard.setBooleanType(false);
                continue;
            }
            moduleCard.collapse();
        }
        this.scrollController.scrollToTop();
    }

    private float measureContentHeight() {
        return 16.0f + this.twoColumnLayout.measureColumns(this.createLayoutAdapter(0.0f));
    }

    private boolean intersectsViewport(GuiElement element2) {
        float f = 1.0f;
        float f2 = element2.getBounds().getY();
        float f3 = f2 + element2.getBounds().getHeight();
        float f4 = this.getBounds().getY() - f;
        float f5 = this.getBounds().getY() + this.getBounds().getHeight() + f;
        return f3 >= f4 && f2 <= f5;
    }

    private float renderSingleColumn(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        float f2 = this.scrollController.getOffset();
        float f3 = bounds2.getX() + 8.0f;
        float f4 = bounds2.getY() + 8.0f + f2;
        float f5 = 319.0f;
        float f6 = f4;
        for (GuiElement element2 : this.children) {
            f6 = this.renderVisibleChild(element2, f, matrix4f, f3, f4, f5);
            f4 = f6 + 6.0f;
        }
        return this.calculateContentHeight(f6 - f2);
    }

    private float renderVisibleChild(GuiElement element2, float f, Matrix4f matrix4f, float f2, float f3, float f4) {
        element2.getBounds().setPosition(f2, f3);
        element2.getBounds().setSize(f4, element2.getBounds().getHeight());
        float f5 = element2.getBounds().getY();
        float f6 = f5 + element2.getBounds().getHeight();
        float f7 = 1.0f;
        float f8 = this.getBounds().getY() - f7;
        float f9 = this.getBounds().getY() + this.getBounds().getHeight() + f7;
        if (f6 < f8 || f5 > f9) {
            return f6;
        }
        return element2.render(f, matrix4f);
    }

    public void setModule(Module module) {
        this.module = module;
    }

    private void setFloatType(float f) {
        Module module = this.module;
        if (module == null) {
            return;
        }
        this.module = null;
        if (this.children.isEmpty()) {
            return;
        }
        if (this.isActive()) {
            float f2 = 8.0f;
            for (GuiElement element2 : this.children) {
                ModuleCard moduleCard;
                if (element2 instanceof ModuleCard && (moduleCard = (ModuleCard)element2).getModule() == module) {
                    this.scrollController.scrollTo(-f2, f);
                    return;
                }
                f2 += element2.getBounds().getHeight() + 6.0f;
            }
            return;
        }
        float f3 = 8.0f;
        float f4 = 8.0f;
        for (int i = 0; i < this.children.size(); ++i) {
            ModuleCard moduleCard;
            float f5;
            GuiElement element3 = (GuiElement)this.children.get(i);
            boolean bl = (i & 1) == 0;
            float f6 = f5 = bl ? f3 : f4;
            if (element3 instanceof ModuleCard && (moduleCard = (ModuleCard)element3).getModule() == module) {
                this.scrollController.scrollTo(-f5, f);
                return;
            }
            if (bl) {
                f3 += element3.getBounds().getHeight() + 6.0f;
                continue;
            }
            f4 += element3.getBounds().getHeight() + 6.0f;
        }
    }

    private ListLayout createLayoutAdapter(float f) {
        return new GuiListLayoutAdapter(this, f);
    }

    private void layoutChildren() {
        if (this.children.isEmpty()) {
            return;
        }
        if (this.isActive()) {
            this.layoutSingleColumn();
        } else {
            this.layoutTwoColumns();
        }
    }

    private float measureSingleColumnHeight() {
        float f = 8.0f;
        for (GuiElement element2 : this.children) {
            f += element2.getBounds().getHeight() + 6.0f;
        }
        return f - 6.0f + 8.0f;
    }

    private boolean isActive() {
        return this.containerDisplay.getContainerColumnLayout() == ContainerColumnLayout.SINGLE_COLUMN;
    }

    private float renderTwoColumns(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        float f2 = bounds2.getX() + 8.0f;
        float f3 = bounds2.getY() + 8.0f;
        float f4 = 319.0f;
        float f5 = this.twoColumnLayout.renderColumns(this.createLayoutAdapter(f), matrix4f, f2, f3, f4, this.scrollController.getOffset(), bounds2.getY() - 1.0f, bounds2.getY() + bounds2.getHeight() + 1.0f);
        return f5 <= 0.0f ? 0.0f : f5 + 16.0f;
    }

    private void layoutSingleColumn() {
        GuiBounds bounds2 = this.getBounds();
        float f = bounds2.getX() + 8.0f;
        float f2 = bounds2.getY() + 8.0f + this.scrollController.getOffset();
        float f3 = 319.0f;
        for (GuiElement element2 : this.children) {
            element2.getBounds().setPosition(f, f2);
            element2.getBounds().setSize(f3, element2.getBounds().getHeight());
            f2 += element2.getBounds().getHeight() + 6.0f;
        }
    }

    private void layoutTwoColumns() {
        float f;
        GuiBounds bounds2 = this.getBounds();
        float f2 = 319.0f;
        float f3 = (f2 - 6.0f) / 2.0f;
        float f4 = bounds2.getX() + 8.0f;
        float f5 = f4 + f3 + 6.0f;
        float f6 = f = bounds2.getY() + 8.0f + this.scrollController.getOffset();
        float f7 = f;
        for (int i = 0; i < this.children.size(); ++i) {
            GuiElement element2 = (GuiElement)this.children.get(i);
            boolean bl = (i & 1) == 0;
            float f8 = bl ? f4 : f5;
            float f9 = bl ? f6 : f7;
            element2.getBounds().setPosition(f8, f9);
            element2.getBounds().setSize(f3, element2.getBounds().getHeight());
            if (bl) {
                f6 = f9 + element2.getBounds().getHeight() + 6.0f;
                continue;
            }
            f7 = f9 + element2.getBounds().getHeight() + 6.0f;
        }
    }

    private float calculateContentHeight(float f) {
        GuiBounds bounds2 = this.getBounds();
        return f <= bounds2.getY() + 8.0f ? 0.0f : f - bounds2.getY() + 8.0f;
    }

    private String getCategoryTitle(ModuleCategory moduleCategory) {
        return switch (moduleCategory) {
            default -> throw new MatchException(null, null);
            case ModuleCategory.COMBAT -> "Combat Modules";
            case ModuleCategory.MOVEMENT -> "Movement Modules";
            case ModuleCategory.RENDER -> "Render Modules";
            case ModuleCategory.PLAYER -> "Player Modules";
            case ModuleCategory.DISPLAY -> "Display Modules";
            case ModuleCategory.MISC -> "Miscellaneous Modules";
            case ModuleCategory.HIDDEN -> "Hidden Modules";
        };
    }
}

