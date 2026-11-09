package ru.wexside.server;

import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import ru.wexside.misc.BossBarMapAccessor;

public final class FunTimeServerContext {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final long PVP_STATUS_GRACE_PERIOD_MS = 3000L;
    private static volatile long lastPvpStatusTime;

    private FunTimeServerContext() {
    }

    public static int getBalance() {
        ClientPlayerEntity player = FunTimeServerContext.CLIENT.player;
        if (player == null || FunTimeServerContext.CLIENT.world == null) {
            return -1;
        }
        try {
            Scoreboard scoreboard = FunTimeServerContext.CLIENT.world.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective == null) {
                return -1;
            }
            ReadableScoreboardScore score = scoreboard.getScore((ScoreHolder)player, objective);
            return score == null ? -1 : score.getScore();
        }
        catch (RuntimeException ignored) {
            return -1;
        }
    }

    public static boolean isOnHub() {
        if (FunTimeServerContext.CLIENT.inGameHud == null) {
            return false;
        }
        BossBarMapAccessor bossBars = (BossBarMapAccessor)FunTimeServerContext.CLIENT.inGameHud.getBossBarHud();
        for (ClientBossBar bossBar : bossBars.getMap().values()) {
            if (!bossBar.getName().getString().equals("Вы играете на ФанТайм!")) continue;
            return true;
        }
        return false;
    }

    public static boolean isConnected() {
        ServerInfo server = CLIENT.getCurrentServerEntry();
        return server != null && server.address != null && server.address.toLowerCase(Locale.ROOT).contains("funtime");
    }

    public static boolean isPvpLocked() {
        if (FunTimeServerContext.CLIENT.world == null || FunTimeServerContext.CLIENT.player == null) {
            lastPvpStatusTime = 0L;
            return false;
        }
        BossBarMapAccessor bossBars = (BossBarMapAccessor)FunTimeServerContext.CLIENT.inGameHud.getBossBarHud();
        for (ClientBossBar bossBar : bossBars.getMap().values()) {
            String title = bossBar.getName().getString().toLowerCase(Locale.ROOT);
            if (!title.contains("pvp") && !title.contains("пвп")) continue;
            lastPvpStatusTime = System.currentTimeMillis();
            return true;
        }
        return lastPvpStatusTime != 0L && System.currentTimeMillis() - lastPvpStatusTime < 3000L;
    }

    public static int getAnarchyNumber() {
        if (FunTimeServerContext.CLIENT.world == null) {
            return -1;
        }
        try {
            ScoreboardObjective objective = FunTimeServerContext.CLIENT.world.getScoreboard().getNullableObjective("TAB-Scoreboard");
            if (objective == null) {
                return -1;
            }
            String title = objective.getDisplayName().getString();
            int markerIndex = title.indexOf("Анархия-");
            if (markerIndex < 0) {
                return -1;
            }
            String suffix = title.substring(markerIndex + "Анархия-".length()).trim();
            StringBuilder number = new StringBuilder();
            for (int index = 0; index < suffix.length() && Character.isDigit(suffix.charAt(index)); ++index) {
                number.append(suffix.charAt(index));
            }
            return number.isEmpty() ? -1 : Integer.parseInt(number.toString());
        }
        catch (RuntimeException ignored) {
            return -1;
        }
    }

    public static void reset() {
        lastPvpStatusTime = 0L;
    }
}

