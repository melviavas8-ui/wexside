package ru.wexside.model.esp;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import ru.wexside.WexSideClient;
import ru.wexside.misc.FriendList;
import ru.wexside.model.esp.EspRelation;
import ru.wexside.model.esp.EspTargetType;

public final class EspTargetClassifier {
    private EspTargetClassifier() {
    }

    public static EspTargetType targetType(Entity entity, ClientPlayerEntity localPlayer) {
        if (entity == localPlayer) {
            return EspTargetType.SELF;
        }
        if (entity instanceof PlayerEntity) {
            return EspTargetType.PLAYERS;
        }
        if (entity instanceof ItemEntity) {
            return EspTargetType.ITEMS;
        }
        return EspTargetType.ENTITIES;
    }

    public static EspRelation relation(Entity entity) {
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            FriendList friends = WexSideClient.getFriends();
            if (friends != null && friends.contains(player.getName().getString())) {
                return EspRelation.FRIEND;
            }
        }
        return EspRelation.DEFAULT;
    }
}

