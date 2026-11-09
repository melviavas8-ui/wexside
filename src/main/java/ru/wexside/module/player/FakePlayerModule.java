package ru.wexside.module.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public class FakePlayerModule
extends Module
implements ConfigSerializable {
    private static final int FAKE_ID = -1338;
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private ClientWorld trackedWorld;
    private OtherClientPlayerEntity fakePlayer;
    private boolean spawned;

    public FakePlayerModule(EventBus eventBus) {
        super(eventBus, "fake_player", "Fake Player", "Спавнит вашего двойника", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onTick);
        this.listen(WorldSessionEvent.class, event -> this.despawn());
    }

    private void onTick(ClientTickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (this.trackedWorld != world) {
            this.trackedWorld = world;
            if (this.spawned) {
                this.forceDespawn();
                return;
            }
        }
        if (this.spawned && (client.player == null || !client.player.isAlive())) {
            this.forceDespawn();
            return;
        }
        if (this.enabledSetting.isEnabled() && !this.spawned) {
            this.spawn();
        } else if (!this.enabledSetting.isEnabled() && this.spawned) {
            this.despawn();
        }
    }

    private void spawn() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || !player.isAlive()) {
            this.enabledSetting.setEnabled(false);
            return;
        }
        this.trackedWorld = client.world;
        this.copyFrom(player);
        this.spawned = true;
    }

    private void despawn() {
        this.removeFake();
        this.spawned = false;
    }

    private void forceDespawn() {
        this.despawn();
        this.enabledSetting.setEnabled(false);
    }

    private void removeFake() {
        OtherClientPlayerEntity fake = this.fakePlayer;
        this.fakePlayer = null;
        if (fake == null) {
            return;
        }
        try {
            fake.remove(Entity.RemovalReason.DISCARDED);
            fake.discard();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void copyFrom(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        OtherClientPlayerEntity fake = new OtherClientPlayerEntity(client.world, player.getGameProfile());
        fake.copyPositionAndRotation((Entity)player);
        fake.copyFrom((Entity)player);
        this.copyInventory(fake.getInventory(), player.getInventory());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            fake.equipStack(slot, player.getEquippedStack(slot).copy());
        }
        fake.setId(-1338);
        client.world.addEntity((Entity)fake);
        this.fakePlayer = fake;
    }

    private void copyInventory(PlayerInventory dest, PlayerInventory source) {
        for (int i = 0; i < source.size(); ++i) {
            dest.setStack(i, source.getStack(i).copy());
        }
    }
}

