package ru.wexside.util;

import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BoundsProvider;
import ru.wexside.misc.CharacterInputHandler;
import ru.wexside.misc.ClasspathResource;
import ru.wexside.misc.ClientProfile;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.GuiRenderable;
import ru.wexside.misc.KeyPressHandler;
import ru.wexside.misc.LayoutUpdater;
import ru.wexside.misc.MouseButtonHandler;
import ru.wexside.misc.MouseScrollHandler;
import ru.wexside.misc.TextLayoutUtils;
import ru.wexside.misc.TextureResource;
import ru.wexside.misc.ThemeColors;
import ru.wexside.ui.GuiBounds;
import ru.wexside.ui.GuiElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public class UserProfilePanel
extends GuiElement
implements CharacterInputHandler,
MouseScrollHandler,
LayoutUpdater,
KeyPressHandler,
GuiRenderable,
MouseButtonHandler,
BoundsProvider {
    private static final TextureResource DEFAULT_AVATAR = new TextureResource(new ClasspathResource("/assets/wexside/textures/menu/logotypes.png"));
    private float collapsedY;
    private float sidebarWidth;
    private float collapseProgress;
    private final ClientProfile clientProfile2;

    public UserProfilePanel(GuiBounds bounds2, ClientProfile clientProfile2) {
        super(bounds2);
        this.clientProfile2 = clientProfile2;
        this.sidebarWidth = bounds2.getWidth();
        this.collapsedY = bounds2.getY();
    }

    public float getFloatType() {
        return 22.0f;
    }

    @Override
    public void onMouseScroll(int n, int n2, double d) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean onMousePressed(int n, int n2, int n3) {
        return false;
    }

    public void setCollapsedLayout(float f, float f2, float f3) {
        this.collapseProgress = f;
        this.sidebarWidth = f2;
        this.collapsedY = f3;
    }

    private float lerp(float f, float f2, float f3) {
        return f * (1.0f - f3) + f2 * f3;
    }

    @Override
    public float render(float f, Matrix4f matrix4f) {
        TextureResource texture2;
        float f2;
        float f3;
        int n;
        int n2;
        GuiBounds bounds2 = this.getBounds();
        GuiDrawApi drawApi = WexSideClient.getGuiRenderer();
        float f4 = 1.0f - this.collapseProgress;
        int n3 = (int)Math.clamp(f4 * 255.0f, 0.0f, 255.0f);
        int n4 = (int)Math.clamp(this.collapseProgress * 255.0f, 0.0f, 255.0f);
        float f5 = bounds2.getX();
        float f6 = bounds2.getY();
        float f7 = bounds2.getX() + (this.sidebarWidth - 13.0f) / 2.0f;
        float f8 = f7 + 2.5f;
        float f9 = this.collapsedY + 2.5f;
        float f10 = this.lerp(8.0f, 8.0f, this.collapseProgress);
        float f11 = this.lerp(f5, f8, this.collapseProgress);
        float f12 = this.lerp(f6, f9, this.collapseProgress);
        if (n3 > 2) {
            n2 = ColorUtils.withAlpha(ThemeColors.textPrimary(), (float)n3);
            n = ColorUtils.withAlpha(ThemeColors.textMuted(), (float)n3);
            String string = this.clientProfile2.getExpirationDate().replace("-", ".");
            f3 = bounds2.getX() + 10.0f;
            f2 = bounds2.getX() + bounds2.getWidth() - FontRegistry.font4.textWidth(string, 5.5f);
            String string2 = TextLayoutUtils.trimToWidth(this.clientProfile2.getUsername(), FontRegistry.font2, 5.5f, f2 - f3 - 0.0f);
            FontRegistry.font2.drawText(matrix4f, drawApi, string2, f3, bounds2.getY() + 0.75f, 5.5f, n2);
            FontRegistry.font2.drawText(matrix4f, drawApi, string, f2, bounds2.getY() + 0.75f, 5.5f, n);
        }
        if (n4 > 2) {
            n2 = ColorUtils.withAlpha(ThemeColors.borderPrimary(), (float)n4);
            n = ColorUtils.withAlpha(ThemeColors.accent(), (float)n4);
            float f13 = f7 + 6.5f;
            f3 = f9 + 8.0f + 3.0f;
            f2 = f3 + 3.0f;
            drawApi.drawRoundedRectangleOutlined(matrix4f, f7, this.collapsedY, 13.0f, 22.0f, 9.0f, 1.0f, ColorUtils.withAlpha(ThemeColors.backgroundPrimary(), 0.0f), n2);
            drawApi.drawCircle(matrix4f, f13, f2, 0.0f, 360.0f, 1.5f, 3.0f, n2);
            drawApi.drawCircle(matrix4f, f13, f2, 0.0f, 270.0f, 1.5f, 3.0f, n);
        }
        TextureResource texture3 = (texture2 = this.clientProfile2.getAvatarTexture()) != null ? texture2 : DEFAULT_AVATAR;
        int n5 = drawApi.bindTexture(texture3.getTextureId(), texture3.getWidth(), texture3.getHeight());
        drawApi.drawRoundedTextureTinted(matrix4f, f11, f12, f10, f10, 5.0f, n5, -1);
        return bounds2.getY() + bounds2.getHeight();
    }
}

