package ru.wexside.module.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.FriendList;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.server.FunTimeServerContext;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.util.entity.NpcDetector;

public class AutoLeaveModule
extends Module
implements ConfigSerializable {
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private final BooleanSetting ignoreFriends;

    public AutoLeaveModule(EventBus eventBus) {
        super(eventBus, "auto_leave", "Auto Leave", "Автоматически выходит с сервера при обнаружении игроков поблизости", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.ignoreFriends = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Ignore Friends").id("ignore_friends").description("Игнорировать друзей").aliases("friends", "друзья")).build();
        this.registerSetting(this.ignoreFriends);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onTick);
    }

    private void onTick(ClientTickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!this.enabledSetting.isEnabled() || client.world == null || client.player == null) {
            return;
        }
        for (PlayerEntity other : client.world.getPlayers()) {
            if (other == client.player || this.ignoreFriends.isEnabled() && this.isFriend(other) || this.isNpc(other)) continue;
            if (FunTimeServerContext.isPvpLocked()) {
                return;
            }
            this.leave();
            return;
        }
    }

    private void leave() {
        this.enabledSetting.setEnabled(false);
        MinecraftClient client = MinecraftClient.getInstance();
        MutableText reason = Text.literal((String)"AutoLeave");
        client.disconnect((Screen)new DisconnectedScreen((Screen)new TitleScreen(), (Text)reason, (Text)reason), false);
    }

    private boolean isFriend(PlayerEntity player) {
        FriendList friends = WexSideClient.getFriends();
        return friends != null && friends.contains(Formatting.strip((String)player.getName().getString()));
    }

    private boolean isNpc(PlayerEntity player) {
        NpcDetector npcDetector = WexSideClient.getNpcDetector();
        return npcDetector != null && npcDetector.isNpc((LivingEntity)player);
    }
}

