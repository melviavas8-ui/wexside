package ru.wexside.util;

import java.util.Arrays;
import java.util.List;
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
import ru.wexside.misc.MultiSelectPopup;
import ru.wexside.misc.PopupManager;
import ru.wexside.misc.PopupOwner;
import ru.wexside.misc.TextLayoutUtils;
import ru.wexside.misc.ThemeColors;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.PopupPanel;
import ru.wexside.util.GuiDrawApi;

public final class KeybindFilter
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
PopupOwner {
    private final MultiSelectPopup multiSelectPopup;
    private float popupProgress;
    private PopupManager popupManager;
    private final MultiSelectSetting multiSelectSetting;

    public KeybindFilter() {
        super(new GuiBounds(0.0f, 0.0f, 90.0f, 13.5f));
        String[] stringArray = (String[])Arrays.stream(ModuleCategory.values()).map(ModuleCategory::getName).toArray(String[]::new);
        this.multiSelectSetting = ((MultiSelectSettingBuilder)((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().id("bindings.categories")).name("Категории")).options(stringArray).optionListEnabled(false).build();
        this.multiSelectPopup = new MultiSelectPopup(this.multiSelectSetting);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (n3 != 0 || !this.getBounds().contains(n, n2)) {
            return false;
        }
        if (this.popupManager != null) {
            this.popupManager.toggle(this);
        }
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.popupProgress = FrameInterpolator.lerpTowards(this.popupProgress, this.multiSelectPopup.isActive2() ? 1.0f : 0.0f, 15.0f);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 8.0f, 0.75f, ThemeColors.controlFill(), ThemeColors.borderPrimary());
        FontRegistry.font3.drawGlyphText(matrix4f, drawApi, "ч", bounds2.getX() + 4.0f, bounds2.getY() + 3.75f, 6.0f, ThemeColors.textSecondary());
        float f2 = FontRegistry.font3.textWidth("ч", 6.0f);
        float f3 = bounds2.getX() + 4.0f + f2 + 2.5f;
        String string = TextLayoutUtils.trimToWidth(this.getString(), FontRegistry.font4, 6.0f, 60.0f);
        float f4 = FontRegistry.font4.textHeight(string, 6.0f);
        float f5 = bounds2.getY() + (bounds2.getHeight() - f4) / 2.0f;
        FontRegistry.font4.drawText(matrix4f, drawApi, string, f3, f5, 6.0f, ThemeColors.textSecondary());
        this.renderExpandChevron(matrix4f, drawApi, bounds2);
        return bounds2.getY() + bounds2.getHeight();
    }

    public List<String> getList() {
        return this.multiSelectSetting.getSelectedOptions();
    }

    private String getString() {
        List<String> list = this.multiSelectSetting.getSelectedOptions();
        if (list == null || list.isEmpty()) {
            return "Все категории";
        }
        return String.join((CharSequence)", ", list);
    }

    private void renderExpandChevron(Matrix4f matrix4f, GuiDrawApi drawApi, GuiBounds bounds2) {
        float f = FontRegistry.font3.textWidth("F", 6.5f);
        float f2 = FontRegistry.font3.textHeight("F", 6.5f);
        float f3 = bounds2.getX() + bounds2.getWidth() - f - 4.5f;
        float f4 = bounds2.getY() + (bounds2.getHeight() - f2) / 2.0f;
        float f5 = f3 + f / 2.0f;
        float f6 = f4 + f2 / 2.0f;
        float f7 = 90.0f - 180.0f * this.popupProgress;
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(f5, f6, 0.0f).rotateZ((float)Math.toRadians(f7)).translate(-f5, -f6, 0.0f);
        FontRegistry.font3.drawGlyphText(matrix4f2, drawApi, "F", f3, f4, 6.5f, ThemeColors.textMuted());
    }

    @Override
    public PopupPanel getPopup() {
        return this.multiSelectPopup;
    }

    @Override
    public boolean containsScreenPoint(int n, int n2) {
        return this.getBounds().contains(n, n2);
    }

    @Override
    public void update2() {
        GuiBounds bounds2 = this.getBounds();
        this.multiSelectPopup.getBounds().setPosition(bounds2.getX(), bounds2.getY() + bounds2.getHeight() + 1.0f);
    }

    @Override
    public void setPopupManager(PopupManager popupManager) {
        this.popupManager = popupManager;
    }
}

