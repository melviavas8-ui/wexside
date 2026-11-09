package ru.wexside.misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameMode;
import org.joml.Matrix4f;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.ThemeColors;
import ru.wexside.util.AbstractHudElement;
import ru.wexside.util.GuiDrawApi;

public final class StaffList
extends AbstractHudElement {
    private static final List<String> RANK_MARKERS = List.of("хелпер", "helper", "модер", "moder", "developer", "админ", "admin", "ютуб", "youtube", "помощник", "сотрудник", "заместитель", "куратор", "стажёр", "staff");
    private final List<StaffEntry> staff = new ArrayList<StaffEntry>();
    private float animatedWidth = 80.0f;
    private float animatedHeight = 18.0f;

    public StaffList(BooleanSupplier visible) {
        super("Staff List", visible);
    }

    @Override
    protected float getWidth() {
        return this.animatedWidth;
    }

    @Override
    protected float getHeight() {
        return this.animatedHeight;
    }

    @Override
    protected boolean isContentVisible() {
        this.collectStaff();
        return !this.staff.isEmpty();
    }

    @Override
    protected void updateLayout() {
        this.collectStaff();
        float targetWidth = StaffList.titleWidth();
        for (StaffEntry entry : this.staff) {
            String row = entry.name() + "  " + entry.rank();
            targetWidth = Math.max(targetWidth, FontRegistry.font4.textWidth(row, 6.5f) + 18.0f);
        }
        targetWidth = Math.max(80.0f, targetWidth);
        float targetHeight = 18.0f + (this.staff.isEmpty() ? 0.0f : 4.5f + (float)this.staff.size() * 13.5f + 5.0f);
        this.animatedWidth = FrameInterpolator.lerpTowards(this.animatedWidth, targetWidth, 30.0f);
        this.animatedHeight = FrameInterpolator.lerpTowards(this.animatedHeight, targetHeight, 30.0f);
    }

    @Override
    protected void renderContent(GuiDrawApi renderer, Matrix4f matrix, float x, float y, float width, float height, float scale) {
        int color = ThemeColors.hudTextPrimary();
        this.renderPanelSurface(renderer, matrix, x, y, width, height, 8.0f, scale);
        renderer.beginStencil(1);
        renderer.drawRoundedRectangle(matrix, x, y, width, height, 8.0f * scale, -1);
        renderer.applyStencilMask(1);
        FontRegistry.font7.drawText(matrix, renderer, "Staff List", x + 5.0f * scale, y + 4.5f * scale, 8.0f * scale, color);
        FontRegistry.font3.drawGlyphText(matrix, renderer, "Ж", x + width - 13.0f * scale, y + 6.0f * scale, 8.0f * scale, color);
        float rowY = y + 22.5f * scale;
        for (StaffEntry entry : this.staff) {
            String rank = ((String)(entry.spectator() ? entry.rank() + " [S]" : entry.rank())).toUpperCase(Locale.ROOT);
            float rankTextWidth = FontRegistry.font6.textWidth(rank, 5.5f);
            float badgeWidth = (rankTextWidth + 8.0f) * scale;
            float badgeX = x + width - 5.0f * scale - badgeWidth;
            FontRegistry.font4.drawText(matrix, renderer, entry.name(), x + 5.0f * scale, rowY + (10.5f - FontRegistry.font4.textHeight(entry.name(), 6.5f)) * 0.5f * scale, 6.5f * scale, color);
            renderer.drawRoundedOutline(matrix, badgeX, rowY, badgeWidth, 10.5f * scale, 6.0f * scale, scale, ThemeColors.separator());
            FontRegistry.font6.drawText(matrix, renderer, rank, badgeX + (badgeWidth - rankTextWidth * scale) * 0.5f, rowY + (10.5f - FontRegistry.font6.textHeight(rank, 5.5f)) * 0.5f * scale, 5.5f * scale, ThemeColors.hudTextMuted());
            rowY += 13.5f * scale;
        }
        renderer.endStencil();
    }

    private static float titleWidth() {
        return 5.0f + FontRegistry.font7.textWidth("Staff List", 8.0f) + 19.0f;
    }

    private void collectStaff() {
        this.staff.clear();
        ClientPlayNetworkHandler network = MinecraftClient.getInstance().getNetworkHandler();
        if (network == null) {
            return;
        }
        for (PlayerListEntry player : network.getPlayerList()) {
            String name = player.getProfile().name();
            String rank = StaffList.teamLabel(player.getScoreboardTeam());
            if (name == null || !StaffList.containsRankMarker(rank)) continue;
            this.staff.add(new StaffEntry(name, rank, player.getGameMode() == GameMode.SPECTATOR));
        }
        this.staff.sort(Comparator.comparing(StaffEntry::name, String.CASE_INSENSITIVE_ORDER));
        if (this.staff.isEmpty() && this.isEditorScreen()) {
            this.staff.add(new StaffEntry("markushv", "\u26a1 Админ", false));
            this.staff.add(new StaffEntry("annihilatorq", "\u26a1 Страж", false));
            this.staff.add(new StaffEntry("Hunger", "\u26a1 Модератор", false));
        }
    }

    private static String teamLabel(Team team) {
        if (team == null) {
            return "";
        }
        return (team.getPrefix().getString() + team.getSuffix().getString()).replaceAll("\u00a7.", "").trim();
    }

    private static boolean containsRankMarker(String rank) {
        String normalized = rank.toLowerCase(Locale.ROOT);
        return RANK_MARKERS.stream().anyMatch(normalized::contains);
    }

    private record StaffEntry(String name, String rank, boolean spectator) {
    }
}

