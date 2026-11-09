package ru.wexside.server;

import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import ru.wexside.server.ServerHelperAction;

public final class ServerHelperActions {
    public static final ServerHelperAction GODS_AURA = ServerHelperActions.action("GodsAura", "Божья аура", Items.NETHER_STAR, -732322, "Божья аура", List.of("Божья аура", "God's Aura"), false, false, 0.0f, false, false);
    public static final ServerHelperAction TRAP = ServerHelperActions.action("Trap", "Трапка", Items.COBWEB, -2631721, "Трапка", List.of("Трапка", "Trap"), false, false, 4.0f, true, false);
    public static final ServerHelperAction PLAST = ServerHelperActions.action("Plast", "Пласт", Items.SLIME_BALL, -8921737, "Пласт", List.of("Пласт", "Plast"), false, false, 4.0f, true, false);
    public static final ServerHelperAction DISORIENTATION = ServerHelperActions.action("Disorientation", "Дезориентация", Items.ENDER_EYE, -6595099, "Дезориентация", List.of("Дезориентация", "Disorientation"), false, false, 5.0f, true, false);
    public static final ServerHelperAction VISIBLE_DUST = ServerHelperActions.action("VisibleDust", "Явная пыль", Items.GLOWSTONE_DUST, -11930, "Явная пыль", List.of("Явная пыль", "Visible Dust"), false, false, 5.0f, true, false);
    public static final ServerHelperAction FREEZE_BALL = ServerHelperActions.action("FreezeBall", "Снежок заморозки", Items.SNOWBALL, -8333057, "Снежок заморозки", List.of("Снежок заморозки", "Freeze Ball"), false, false, 5.0f, true, false);
    public static final ServerHelperAction FIERY_TORNADO = ServerHelperActions.action("FieryTornado", "Огненный смерч", Items.FIRE_CHARGE, -38091, "Огненный смерч", List.of("Огненный смерч", "Fiery Tornado"), false, false, 6.0f, true, false);
    public static final ServerHelperAction WIND_CHARGE = ServerHelperActions.action("WindCharge", "Wind Charge", Items.WIND_CHARGE, -1509377, "Wind Charge", List.of("Wind Charge", "Заряд ветра"), false, false, 6.0f, false, true);
    public static final ServerHelperAction ASSASSIN_POTION = ServerHelperActions.potion("PotionAssassin", "Зелье Ассасина", -9675545, false);
    public static final ServerHelperAction HOLY_WATER = ServerHelperActions.potion("PotionHolyWater", "Святая Вода", -9127425, false);
    public static final ServerHelperAction RAGE_POTION = ServerHelperActions.potion("PotionRage", "Зелье Гнева", -2740175, false);
    public static final ServerHelperAction PALADIN_POTION = ServerHelperActions.potion("PotionPaladin", "Зелье Палладина", -144530, false);
    public static final ServerHelperAction POPPER = ServerHelperActions.potion("PotionPopper", "Хлопушка", -35211, true);
    public static final ServerHelperAction RADIATION_POTION = ServerHelperActions.potion("PotionRadiation", "Зелье Радиации", -11145276, true);
    public static final ServerHelperAction DROWSINESS_POTION = ServerHelperActions.potion("PotionDrowsiness", "Зелье Снотворного", -6120450, true);
    public static final List<ServerHelperAction> ALL = List.of(GODS_AURA, TRAP, PLAST, DISORIENTATION, VISIBLE_DUST, FREEZE_BALL, FIERY_TORNADO, WIND_CHARGE, ASSASSIN_POTION, HOLY_WATER, RAGE_POTION, PALADIN_POTION, POPPER, RADIATION_POTION, DROWSINESS_POTION);
    public static final List<ServerHelperAction> COOLDOWN_TRACKED = ALL.stream().filter(action -> action.activationDistance() > 0.0f && !action.donationItem()).toList();

    private ServerHelperActions() {
    }

    public static ServerHelperAction byId(String id) {
        if (id == null) {
            return null;
        }
        return ALL.stream().filter(action -> action.id().equals(id)).findFirst().orElse(null);
    }

    private static ServerHelperAction action(String id, String displayName, Item icon, int color, String generalTag, List<String> alternateTags, boolean donationItem, boolean splashPotion, float distance, boolean debuff, boolean matchByItem) {
        return new ServerHelperAction(id, displayName, displayName, icon, color, generalTag, alternateTags, donationItem, splashPotion, distance, debuff, matchByItem);
    }

    private static ServerHelperAction potion(String id, String name, int color, boolean debuff) {
        return ServerHelperActions.action(id, name, Items.SPLASH_POTION, color, name, List.of(name), true, true, 5.0f, debuff, false);
    }
}

