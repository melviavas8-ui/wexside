package ru.wexside.util;

import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.ClientProfile;
import ru.wexside.misc.ClientRole;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.ThemeColors;
import ru.wexside.render.TextGradient;
import ru.wexside.util.AbstractHudElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class Watermark
extends AbstractHudElement {
    private final float value;
    static final long member12674 = 2200L;
    static final float value2 = 0.0f;
    private final float value3;
    private final float value4;
    private final float value5;
    private static final int LOGO_TEXT_COLOR = -1;
    static final float value6 = 0.17f;
    private final float value7;
    private final float value8;
    private static final int LOGO_GRADIENT_END = -7709441;
    private final float value9;
    private final float value10;
    private final float value11;
    private final float value12;
    private final float value13;
    static final float value14 = 0.9f;
    private final float value15;
    private static final int LOGO_GRADIENT_START = -11546113;
    private static final int LOGO_GRADIENT_HIGHLIGHT = -1;
    private final float value16;

    public Watermark(BooleanSupplier booleanSupplier) {
        super("Watermark", booleanSupplier);
        this.value11 = 5.0f;
        this.value15 = 7.5f;
        this.value10 = 3.0f;
        this.value12 = 6.5f;
        this.value3 = 5.0f;
        this.value7 = 0.5f;
        this.value5 = 5.0f;
        this.value9 = 5.0f;
        this.value13 = 6.5f;
        this.value16 = 2.0f;
        this.value4 = 6.0f;
        this.value = 14.0f;
        this.value8 = 6.0f;
    }

    @Override
    protected float getWidth() {
        ClientRole clientRole = this.getClientRole();
        String string2 = clientRole != null ? clientRole.getIconGlyph() : null;
        return 15.5f + FontRegistry.font4.textWidth("wexside", 6.5f) + this.measureInfoSegment(string2, this.getHudName()) + this.measureInfoSegment("j", this.getString2()) + this.measureInfoSegment("з", this.getPingText()) + 5.0f;
    }

    @Override
    protected void renderContent(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5) {
        float f6;
        int n = ThemeColors.hudTextPrimary();
        int n2 = ThemeColors.separator();
        this.renderPanelSurface(drawApi, matrix4f, f, f2, f3, f4, 6.0f, f5);
        float f7 = f6 = f + 5.0f * f5;
        float f8 = FontRegistry.font4.textWidth("wexside", 6.5f) * f5;
        float f9 = 7.5f * f5 + 3.0f * f5 + f8;
        TextGradient textGradient = this.createTextGradient();
        FontRegistry.font3.drawGradientTextInRange(matrix4f, drawApi, "@", f6, this.centerVertically(f2, f5, 7.5f), 7.5f * f5, 0.0f, -1, textGradient, f7, f9);
        FontRegistry.font4.drawGradientTextInRange(matrix4f, drawApi, "wexside", f6 += 7.5f * f5 + 3.0f * f5, this.centerVertically(f2, f5, FontRegistry.font4.textHeight("wexside", 6.5f)), 6.5f * f5, 0.0f, -1, textGradient, f7, f9);
        f6 += f8;
        ClientRole clientRole = this.getClientRole();
        if (clientRole != null && clientRole.getDisplayName() != null) {
            int n3 = clientRole.getColor() != 0 ? clientRole.getColor() : n;
            f6 = this.renderInfoSegment(drawApi, matrix4f, f6, f2, f5, n3, n2, clientRole.getIconGlyph(), this.getHudName());
        }
        f6 = this.renderInfoSegment(drawApi, matrix4f, f6, f2, f5, n, n2, "j", this.getString2());
        this.renderInfoSegment(drawApi, matrix4f, f6, f2, f5, n, n2, "з", this.getPingText());
    }

    @Override
    protected float getHeight() {
        return 14.0f;
    }

    private TextGradient createTextGradient() {
        float f = (float)(System.currentTimeMillis() % 2200L) / 2200.0f;
        return f2 -> {
            int n = ColorUtils.lerp(-11546113, -7709441, f2);
            float f3 = f2 - f;
            f3 -= (float)Math.floor(f3 + 0.5f);
            float f4 = (float)Math.exp(-(f3 * f3) / 0.057800002f);
            return ColorUtils.lerp(n, -1, f4 * 0.9f);
        };
    }

    private float renderInfoSegment(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, int n, int n2, String string, String string2) {
        drawApi.fillRectangle(matrix4f, f += 5.0f * f3, this.centerVertically(f2, f3, 5.0f), 0.5f * f3, 5.0f * f3, n2);
        f += 0.5f * f3 + 5.0f * f3;
        if (string != null) {
            FontRegistry.font3.drawGlyphText(matrix4f, drawApi, string, f, this.centerVertically(f2, f3, 6.5f), 6.5f * f3, n);
            f += 6.5f * f3 + 2.0f * f3;
        }
        FontRegistry.font4.drawText(matrix4f, drawApi, string2, f, this.centerVertically(f2, f3, FontRegistry.font4.textHeight(string2, 6.0f)), 6.0f * f3, n);
        return f + FontRegistry.font4.textWidth(string2, 6.0f) * f3;
    }

    private ClientRole getClientRole() {
        ClientProfile clientProfile2 = WexSideClient.getInstance().getClientProfile();
        return clientProfile2 != null ? clientProfile2.getRole() : null;
    }

    private String getHudName() {
        String username;
        ClientProfile clientProfile2 = WexSideClient.getInstance().getClientProfile();
        if (clientProfile2 != null && (username = clientProfile2.getUsername()) != null && !username.isBlank()) {
            return username;
        }
        return "PasterEnd";
    }

    private float measureInfoSegment(String string, String string2) {
        float f = 10.5f;
        if (string != null) {
            f += 8.5f;
        }
        return f + FontRegistry.font4.textWidth(string2, 6.0f);
    }

    private String getPingText() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null || mc.player == null) {
            return "0";
        }
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return String.valueOf(entry != null ? entry.getLatency() : 0);
    }

    private String getString2() {
        int n = MinecraftClient.getInstance().getCurrentFps();
        return n + " fps";
    }
}

