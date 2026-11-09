package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.IconPlacement;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.KeybindDescriptor;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.ThemeColors;
import ru.wexside.module.Module;
import ru.wexside.module.misc.EspFeatureModule;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.HudIconStyle;
import ru.wexside.util.KeybindRow;

public final class ModuleKeybindGroup
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private final HudIconStyle hudIconStyle;
    private final List<KeybindRow> keybindRows;
    private final Module module;

    public ModuleKeybindGroup(Module module, List<KeybindDescriptor> list) {
        super(new GuiBounds(0.0f, 0.0f, 0.0f, 0.0f));
        this.module = module;
        this.hudIconStyle = new HudIconStyle(ModuleKeybindGroup.moduleBreadcrumb(module));
        this.keybindRows = new ArrayList<KeybindRow>(list.size());
        for (KeybindDescriptor callback10 : list) {
            KeybindRow keybindRow = new KeybindRow(callback10);
            this.keybindRows.add(keybindRow);
            this.addChild(keybindRow);
        }
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        return this.renderFilteredGroup(f, matrix4f, false);
    }

    public float getHorizontalPadding() {
        Objects.requireNonNull(this);
        return 7.0f;
    }

    public float measureGroupHeight(boolean bl) {
        float f = this.hudIconStyle.getContentHeight();
        float f2 = 7.0f + f + 4.0f;
        float f3 = Math.max(0.0f, this.getBounds().getWidth() - 14.0f);
        boolean bl2 = false;
        for (KeybindRow keybindRow : this.keybindRows) {
            if (bl && !keybindRow.getCallback10().isActive()) continue;
            f2 += keybindRow.measureRowHeight(f3);
            f2 += 4.0f;
            bl2 = true;
        }
        if (bl2) {
            f2 -= 4.0f;
        }
        return f2 += 7.0f;
    }

    public float getItemSpacing() {
        Objects.requireNonNull(this);
        return 4.0f;
    }

    public float getHeaderSpacing() {
        Objects.requireNonNull(this);
        return 4.0f;
    }

    public float getCornerRadius() {
        Objects.requireNonNull(this);
        return 8.0f;
    }

    public float renderFilteredGroup(float f, Matrix4f matrix4f, boolean bl) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        float f2 = this.measureGroupHeight(bl);
        bounds2.setSize(bounds2.getWidth(), f2);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), f2, 8.0f, 0.75f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), ThemeColors.borderPrimary());
        float f3 = bounds2.getX() + 7.0f;
        float f4 = bounds2.getY() + 7.0f;
        this.hudIconStyle.render(matrix4f, drawApi, f3, f4, 0.0f, IconPlacement.ICON_LEFT);
        float f5 = f4 + this.hudIconStyle.getContentHeight() + 4.0f;
        float f6 = bounds2.getX() + 7.0f;
        float f7 = bounds2.getWidth() - 14.0f;
        for (KeybindRow keybindRow : this.keybindRows) {
            if (bl && !keybindRow.getCallback10().isActive()) continue;
            float f8 = keybindRow.measureRowHeight(f7);
            keybindRow.getBounds().setPosition(f6, f5);
            keybindRow.getBounds().setSize(f7, f8);
            keybindRow.render(f, matrix4f);
            f5 += f8 + 4.0f;
        }
        return bounds2.getY() + f2;
    }

    private static String moduleBreadcrumb(Module module) {
        String string;
        if (module instanceof EspFeatureModule) {
            EspFeatureModule cls0919Module = (EspFeatureModule)module;
            String string2 = cls0919Module.getString();
            string = string2 + " / ";
        } else {
            string = "";
        }
        String string3 = string;
        String string4 = module.getDisplayName();
        String string5 = string3;
        String string6 = module.getCategory().getName();
        return (string6 + " / " + string5 + string4).toUpperCase();
    }

    public List<KeybindRow> getList() {
        return this.keybindRows;
    }

    public float getBackgroundOpacity() {
        Objects.requireNonNull(this);
        return 0.75f;
    }

    public HudIconStyle getHudIconStyle() {
        return this.hudIconStyle;
    }

    public boolean hasMatchingRows(boolean bl) {
        if (!bl) {
            return !this.keybindRows.isEmpty();
        }
        for (KeybindRow keybindRow : this.keybindRows) {
            if (!keybindRow.getCallback10().isActive()) continue;
            return true;
        }
        return false;
    }

    public float getVerticalPadding() {
        Objects.requireNonNull(this);
        return 7.0f;
    }

    public Module getModule() {
        return this.module;
    }
}

