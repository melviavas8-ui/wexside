package ru.wexside.util;

import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.BoundsSupplier;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.MultiSelectAnimation;
import ru.wexside.misc.MultiSelectPopup;
import ru.wexside.misc.PopupManager;
import ru.wexside.misc.PopupOwner;
import ru.wexside.misc.ThemeColors;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.ui.PopupPanel;
import ru.wexside.ui.setting.SettingComponent;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.PopupPlacement;

public final class MultiSelectSettingComponent
extends SettingComponent
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider,
PopupOwner {
    private final MultiSelectPopup multiSelectPopup;
    private PopupManager popupManager;
    private long longType;
    private float popupProgress;
    private boolean popupOpen;
    private final PopupPlacement popupPlacement;

    public MultiSelectSettingComponent(MultiSelectSetting multiSelectSetting) {
        super(new GuiBounds(0.0f, 0.0f, 55.0f, 14.0f), multiSelectSetting);
        this.multiSelectPopup = new MultiSelectPopup(multiSelectSetting);
        this.popupPlacement = new PopupPlacement(3.0f, 1.0f);
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        if (n3 == 0 && ((MultiSelectSetting)this.getSetting()).getAction() != null && this.actionButtonBounds(this.getBounds()).contains(n, n2)) {
            ((MultiSelectSetting)this.getSetting()).getAction().run();
            if (((MultiSelectSetting)this.getSetting()).getAnimation() == MultiSelectAnimation.SPIN) {
                this.longType = System.currentTimeMillis();
            }
            return true;
        }
        if (!this.getBounds().contains(n, n2)) {
            return false;
        }
        if (n3 == 0 && this.popupManager != null) {
            this.popupManager.toggle(this);
        }
        return true;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        this.popupOpen = this.multiSelectPopup.isActive2();
        this.popupProgress = FrameInterpolator.lerpTowards(this.popupProgress, this.popupOpen ? 1.0f : 0.0f, 15.0f);
        drawApi.drawRoundedRectangleOutlined(matrix4f, bounds2.getX(), bounds2.getY(), bounds2.getWidth(), bounds2.getHeight(), 8.0f, 1.0f, ThemeColors.controlFill(), ThemeColors.borderPrimary());
        this.renderActionIcon(matrix4f, drawApi, bounds2);
        String string = this.getString();
        float f2 = this.fitSelectionFontSize(string, bounds2);
        float f3 = bounds2.getY() + 3.25f + (FontRegistry.font4.textHeight(string, 6.0f) - FontRegistry.font4.textHeight(string, f2)) / 2.0f;
        FontRegistry.font4.drawText(matrix4f, drawApi, string, bounds2.getX() + 5.0f, f3, f2, ColorUtils.withAlpha(ThemeColors.textSecondary(), 255.0f));
        this.renderDropdownChevron(matrix4f, drawApi, bounds2);
        return bounds2.getY() + this.getPreferredHeight();
    }

    @Override
    public float getPreferredWidth() {
        return 55.0f;
    }

    private GuiBounds getContainerBounds() {
        for (GuiElement element2 = this.getParent(); element2 != null; element2 = element2.getParent()) {
            if (!(element2 instanceof BoundsSupplier)) continue;
            BoundsSupplier callback13 = (BoundsSupplier)((Object)element2);
            return callback13.getBounds();
        }
        return null;
    }

    public MultiSelectPopup getMultiSelectPopup() {
        return this.multiSelectPopup;
    }

    @Override
    public PopupPanel getPopup() {
        return this.multiSelectPopup;
    }

    private float fitSelectionFontSize(String string, GuiBounds bounds2) {
        float f = FontRegistry.font3.textWidth("F", 6.5f);
        float f2 = bounds2.getX() + bounds2.getWidth() - f - 4.5f;
        float f3 = f2 - (bounds2.getX() + 5.0f) - 2.0f;
        float f4 = FontRegistry.font4.textWidth(string, 6.0f);
        if (f3 <= 0.0f || f4 <= f3) {
            return 6.0f;
        }
        return Math.max(4.0f, 6.0f * f3 / f4);
    }

    private void renderDropdownChevron(Matrix4f matrix4f, GuiDrawApi drawApi, GuiBounds bounds2) {
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

    private String getString() {
        int n;
        int n2 = ((MultiSelectSetting)this.getSetting()).getSelectedOptions() == null ? 0 : ((MultiSelectSetting)this.getSetting()).getSelectedOptions().size();
        String[] stringArray = ((MultiSelectSetting)this.getSetting()).getOptions();
        int n3 = n = stringArray == null ? 0 : stringArray.length;
        int n4 = n2;
        return "Selected " + n4 + "/" + n3;
    }

    private float getActionIconRotation() {
        if (((MultiSelectSetting)this.getSetting()).getAnimation() != MultiSelectAnimation.SPIN || this.longType == 0L) {
            return 0.0f;
        }
        float f = (float)(System.currentTimeMillis() - this.longType) / 450.0f;
        if (f >= 1.0f) {
            return 0.0f;
        }
        float f2 = 1.0f - (1.0f - f) * (1.0f - f) * (1.0f - f);
        return 360.0f * f2;
    }

    private GuiBounds actionButtonBounds(GuiBounds bounds2) {
        return new GuiBounds(bounds2.getX() - 1.0f - 14.0f, bounds2.getY(), 14.0f, 14.0f);
    }

    private void renderActionIcon(Matrix4f matrix4f, GuiDrawApi drawApi, GuiBounds bounds2) {
        String string = ((MultiSelectSetting)this.getSetting()).getActionLabel();
        if (((MultiSelectSetting)this.getSetting()).getAction() == null || string == null || string.isEmpty()) {
            return;
        }
        GuiBounds bounds3 = this.actionButtonBounds(bounds2);
        float f = FontRegistry.font3.textWidth(string, 7.0f);
        float f2 = FontRegistry.font3.textHeight(string, 7.0f);
        float f3 = bounds3.getX() + (bounds3.getWidth() - f) / 2.0f;
        float f4 = bounds3.getY() + (bounds3.getHeight() - f2) / 2.0f;
        Matrix4f matrix4f2 = matrix4f;
        float f5 = this.getActionIconRotation();
        if (f5 != 0.0f) {
            float f6 = f3 + f / 2.0f;
            float f7 = f4 + f2 / 2.0f;
            matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).translate(f6, f7, 0.0f).rotateZ((float)Math.toRadians(f5)).translate(-f6, -f7, 0.0f);
        }
        FontRegistry.font3.drawGlyphText(matrix4f2, drawApi, string, f3, f4, 7.0f, ThemeColors.textMuted());
    }

    @Override
    public boolean containsScreenPoint(int n, int n2) {
        GuiBounds bounds2 = this.getContainerBounds();
        if (bounds2 != null) {
            return new GuiBounds(bounds2.getX() + this.getBounds().getX(), bounds2.getY() + this.getBounds().getY(), this.getBounds().getWidth(), this.getBounds().getHeight()).contains(n, n2);
        }
        return new GuiBounds(this.getAbsoluteX(), this.getAbsoluteY(), this.getBounds().getWidth(), this.getBounds().getHeight()).contains(n, n2);
    }

    @Override
    public void update2() {
        float anchorY;
        float anchorX;
        GuiBounds bounds2 = this.getContainerBounds();
        if (bounds2 != null) {
            anchorX = bounds2.getX() + this.getBounds().getX();
            anchorY = bounds2.getY() + this.getBounds().getY();
        } else {
            anchorX = this.getAbsoluteX();
            anchorY = this.getAbsoluteY();
        }
        MinecraftClient client = MinecraftClient.getInstance();
        float viewportWidth = client.currentScreen == null ? (float)client.getWindow().getScaledWidth() : (float)client.currentScreen.width;
        float viewportHeight = client.currentScreen == null ? (float)client.getWindow().getScaledHeight() : (float)client.currentScreen.height;
        this.popupPlacement.place(this.multiSelectPopup.getBounds(), anchorX, anchorY, this.getBounds().getHeight(), viewportWidth, viewportHeight);
    }

    @Override
    public void setPopupManager(PopupManager popupManager) {
        this.popupManager = popupManager;
    }

    @Override
    public float getPreferredHeight() {
        return 14.0f;
    }
}

