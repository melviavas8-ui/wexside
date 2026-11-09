package ru.wexside.module.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EntityAttackEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.ClientChat;
import ru.wexside.misc.FriendList;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.BindSettingBuilder;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public final class FriendsModule
extends Module
implements ConfigSerializable {
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Добавление друзей биндом и отключение урона по друзьям").withKeybind().toggle()).build();
    private final BooleanSetting clickFriend;
    private final BindSetting friendKey;
    private final BooleanSetting noFriendDamage;

    public FriendsModule(EventBus eventBus) {
        super(eventBus, "friends", "Friends", "Добавление друзей биндом и отключение урона по друзьям", ModuleCategory.valueOf("MISC"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.clickFriend = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Click Friend").id("click_friend").description("Добавлять/удалять друга биндом по наводимому игроку")).build();
        this.registerSetting(this.clickFriend);
        this.friendKey = ((BindSettingBuilder)BindSetting.getBindSettingBuilder().keyboard(0).onReleased(this::onFriendKeyPress).name("Friend Key").id("friend_key").description("Бинд добавления/удаления друга").aliases("friend key", "бинд друга").visibleWhen(this.clickFriend::isEnabled)).build();
        this.registerSetting(this.friendKey);
        this.noFriendDamage = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("No Friend Damage").id("no_friend_damage").description("Отменяет атаку по игрокам из списка друзей").aliases("no friend damage", "без урона по друзьям")).build();
        this.registerSetting(this.noFriendDamage);
    }

    @Override
    protected void initialize() {
        this.listen(EntityAttackEvent.class, this::onEntityAttack);
    }

    private void onFriendKeyPress(BindSetting ignored) {
        PlayerEntity player;
        if (!this.enabledSetting.isEnabled() || !this.clickFriend.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        Entity target = client.targetedEntity;
        if (!(target instanceof PlayerEntity) || (player = (PlayerEntity)target) == client.player) {
            return;
        }
        FriendList friends = WexSideClient.getFriends();
        if (friends == null) {
            return;
        }
        String name = player.getName().getString();
        if (friends.contains(name)) {
            friends.remove(name);
            ClientChat.send("Друг " + name + " удалён из списка друзей");
        } else {
            friends.add(name);
            ClientChat.send("Друг " + name + " добавлен в список друзей");
        }
    }

    private void onEntityAttack(EntityAttackEvent event) {
        if (!this.enabledSetting.isEnabled() || !this.noFriendDamage.isEnabled()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof PlayerEntity && this.isFriend(entity)) {
            event.update();
        }
    }

    private boolean isFriend(Entity entity) {
        FriendList friends = WexSideClient.getFriends();
        return friends != null && friends.contains(entity.getName().getString());
    }
}

