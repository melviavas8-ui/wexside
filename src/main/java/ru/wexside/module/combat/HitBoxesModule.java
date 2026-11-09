package ru.wexside.module.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.FriendList;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;

public final class HitBoxesModule
extends Module
implements ConfigSerializable {
    private static final float SIZE_SCALE = 2.5f;
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Расширяет хит-боксы сущностей").withKeybind().toggle()).build();
    private final BooleanSetting visible;
    private final NumberSetting size;
    private final BooleanSetting ignoreFriends;

    public HitBoxesModule(EventBus eventBus) {
        super(eventBus, "hit_boxes", "Hit Boxes", "Расширяет хит-боксы сущностей", ModuleCategory.valueOf("COMBAT"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.visible = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Visible").id("visible").description("Обводка хитбокса")).build();
        this.registerSetting(this.visible);
        this.size = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 3.0).defaultValue(0.5).multiplier(1.0).precision(1).animationSpeed(20.0f).name("Size").id("size").description("Размер расширения хит-бокса").aliases("size", "размер")).build();
        this.registerSetting(this.size);
        this.ignoreFriends = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Ignore Friends").id("ignore_friends").description("Не расширять хит-боксы друзей")).build();
        this.registerSetting(this.ignoreFriends);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onClientTick);
    }

    private void onClientTick(ClientTickEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            return;
        }
        float expand = (float)this.size.getValue() * 2.5f;
        for (Entity entity : world.getEntities()) {
            if (!this.shouldExpand(entity, (Entity)client.player)) continue;
            entity.setBoundingBox(this.expandedBox(entity, expand));
        }
    }

    private Box expandedBox(Entity entity, float expand) {
        Box box = entity.getBoundingBox();
        return new Box(entity.getX() - (double)expand, box.minY, entity.getZ() - (double)expand, entity.getX() + (double)expand, box.maxY, entity.getZ() + (double)expand);
    }

    private boolean shouldExpand(Entity entity, Entity player) {
        if (entity == null || !entity.isAlive() || entity == player) {
            return false;
        }
        if (!this.ignoreFriends.isEnabled()) {
            return true;
        }
        FriendList friends = WexSideClient.getFriends();
        return friends == null || !friends.contains(entity.getName().getString());
    }
}

