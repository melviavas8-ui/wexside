package ru.wexside.module.misc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.BlockedSoundList;
import ru.wexside.misc.ClientChat;
import ru.wexside.misc.SoundMapAccessor;
import ru.wexside.misc.SoundSystemAccessor;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;

public final class SoundRemoverModule
extends Module
implements ConfigSerializable {
    static volatile SoundRemoverModule soundRemoverModule2;
    private static final Map<String, String[]> SOUND_GROUPS;
    private final BooleanSetting enabledSetting;
    private final MultiSelectSetting groups;
    private final BooleanSetting logSounds;
    private final Set<String> loggedSoundIds = new HashSet<String>();
    private String lastSyncSignature;

    public SoundRemoverModule(EventBus eventBus) {
        super(eventBus, "sound_remover", "Sound Remover", "Заглушает любые звуки по отдельности: группы ванильных + кастомные серверные", ModuleCategory.valueOf("MISC"), new String[0]);
        soundRemoverModule2 = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Заглушать выбранные звуки").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        MultiSelectSetting groupsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Взрывы", "Фейерверки", "Опыт и уровни", "Урон", "Смерть", "Эндер Дракон", "Варден", "Визер", "Гаст/огненный шар", "Молнии и гроза", "Погода и дождь", "Шаги", "Интерфейс", "Голоса мобов", "Двери и люки", "Сундуки/хранилища", "Порталы").selectAll(false).optionListEnabled(false).name("Groups").id("groups").description("Группы ванильных звуков для заглушения").aliases("groups", "группы")).build();
        groupsSetting.setOptions(new String[0]);
        this.groups = groupsSetting;
        this.registerSetting(groupsSetting);
        this.logSounds = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Log sounds").id("log_sounds").description("Печатать в чат каждый слышимый звук с кнопкой [заглушить] \u2014 для поиска id кастомных серверных звуков")).build();
        this.registerSetting(this.logSounds);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onClientTick);
    }

    private void onClientTick(ClientTickEvent event) {
        String enabledFlag = this.enabledSetting.isEnabled() ? "1" : "0";
        String signature = enabledFlag + "|" + String.join((CharSequence)",", this.groups.getSelectedOptions());
        if (signature.equals(this.lastSyncSignature)) {
            return;
        }
        this.lastSyncSignature = signature;
        if (this.enabledSetting.isEnabled()) {
            this.muteActiveSounds();
        }
    }

    private void muteActiveSounds() {
        ArrayList<SoundInstance> instances;
        MinecraftClient client = MinecraftClient.getInstance();
        SoundManager soundManager = client.getSoundManager();
        if (!(soundManager instanceof SoundSystemAccessor)) {
            return;
        }
        SoundSystemAccessor callback54 = (SoundSystemAccessor)soundManager;
        SoundSystem soundEngine = callback54.getSoundSystem();
        if (!(soundEngine instanceof SoundMapAccessor)) {
            return;
        }
        SoundMapAccessor callback38 = (SoundMapAccessor)soundEngine;
        Map<SoundInstance, ?> activeSounds = callback38.getMap();
        if (activeSounds == null || activeSounds.isEmpty()) {
            return;
        }
        try {
            instances = new ArrayList<SoundInstance>();
            for (SoundInstance key : activeSounds.keySet()) {
                if (!(key instanceof SoundInstance)) continue;
                SoundInstance instance = key;
                instances.add(instance);
            }
        }
        catch (RuntimeException ignored) {
            return;
        }
        HashSet<Identifier> blockedIds = new HashSet<Identifier>();
        for (SoundInstance instance : instances) {
            Identifier id = instance.getId();
            if (id == null || !SoundRemoverModule.shouldBlock(id)) continue;
            blockedIds.add(id);
        }
        for (Identifier id : blockedIds) {
            soundManager.stopSounds(id, null);
        }
    }

    public static void handle(Identifier soundId, boolean muted) {
        SoundRemoverModule module = soundRemoverModule2;
        if (module == null || soundId == null || !module.enabledSetting.isEnabled()) {
            return;
        }
        if (!module.logSounds.isEnabled()) {
            module.loggedSoundIds.clear();
            return;
        }
        String normalized = soundId.toString().toLowerCase(Locale.ROOT);
        if (!module.loggedSoundIds.add(normalized)) {
            return;
        }
        MutableText message = SoundRemoverModule.buildLogMessage(soundId.toString(), muted);
        MinecraftClient.getInstance().execute(() -> ClientChat.send((Text)message));
    }

    public static boolean compute4(Identifier soundId) {
        SoundRemoverModule module = soundRemoverModule2;
        if (module == null || soundId == null || !module.enabledSetting.isEnabled()) {
            return false;
        }
        String normalized = soundId.toString().toLowerCase(Locale.ROOT);
        if (SoundRemoverModule.matchesSelectedGroups(module.groups.getSelectedOptions(), normalized)) {
            return true;
        }
        BlockedSoundList customBlocked = WexSideClient.getBlockedSoundList();
        return customBlocked != null && customBlocked.contains(normalized);
    }

    private static boolean matchesSelectedGroups(List<String> groups, String soundId) {
        if (groups == null || groups.isEmpty()) {
            return false;
        }
        for (String group : groups) {
            if (!SoundRemoverModule.matchesGroup(group, soundId)) continue;
            return true;
        }
        return false;
    }

    private static boolean matchesGroup(String group, String soundId) {
        String[] fragments = SOUND_GROUPS.get(group);
        return fragments != null && SoundRemoverModule.containsAny(soundId, fragments);
    }

    private static boolean containsAny(String soundId, String ... fragments) {
        for (String fragment : fragments) {
            if (!soundId.contains(fragment)) continue;
            return true;
        }
        return false;
    }

    private static MutableText buildLogMessage(String soundId, boolean muted) {
        MutableText message = Text.literal((String)soundId).formatted(Formatting.GRAY);
        message.append((Text)Text.literal((String)" "));
        if (muted) {
            message.append((Text)SoundRemoverModule.clickableText("[вернуть]", Formatting.GREEN, ".sound remove " + soundId, "Кликни, чтобы вернуть звук"));
        } else {
            message.append((Text)SoundRemoverModule.clickableText("[заглушить]", Formatting.YELLOW, ".sound add " + soundId, "Кликни, чтобы заглушить звук"));
        }
        return message;
    }

    private static MutableText clickableText(String label, Formatting color, String command, String hover) {
        return Text.literal((String)label).setStyle(Style.EMPTY.withColor(color).withUnderline(Boolean.valueOf(true)).withClickEvent((ClickEvent)new ClickEvent.RunCommand(command)).withHoverEvent((HoverEvent)new HoverEvent.ShowText((Text)Text.literal((String)hover))));
    }

    private static boolean shouldBlock(Identifier soundId) {
        return SoundRemoverModule.compute4(soundId);
    }

    private static Map<String, String[]> buildSoundGroups() {
        LinkedHashMap<String, String[]> groups = new LinkedHashMap<String, String[]>();
        groups.put("Взрывы", new String[]{"explode", "explosion", "tnt", "creeper", "blast"});
        groups.put("Фейерверки", new String[]{"firework", "fireworks"});
        groups.put("Опыт и уровни", new String[]{"experience", "levelup", "orb.pickup", "random.levelup"});
        groups.put("Урон", new String[]{"hurt", "damage", "fall", "hit", "injured"});
        groups.put("Смерть", new String[]{"death", "die"});
        groups.put("Эндер Дракон", new String[]{"dragon", "enderdragon"});
        groups.put("Варден", new String[]{"warden"});
        groups.put("Визер", new String[]{"wither"});
        groups.put("Гаст/огненный шар", new String[]{"ghast", "fireball", "blaze"});
        groups.put("Молнии и гроза", new String[]{"thunder", "lightning"});
        groups.put("Погода и дождь", new String[]{"weather", "rain", "ambient.weather"});
        groups.put("Шаги", new String[]{"step", "footstep", "walk"});
        groups.put("Интерфейс", new String[]{"ui.", "click", "button", "inventory", "screen", "toast"});
        groups.put("Голоса мобов", new String[]{"entity.", "mob", "ambient"});
        groups.put("Двери и люки", new String[]{"door", "trapdoor", "fence_gate", "gate"});
        groups.put("Сундуки/хранилища", new String[]{"chest", "shulker", "barrel", "ender_chest"});
        groups.put("Порталы", new String[]{"portal", "warp"});
        return Map.copyOf(groups);
    }

    static {
        SOUND_GROUPS = SoundRemoverModule.buildSoundGroups();
    }
}

