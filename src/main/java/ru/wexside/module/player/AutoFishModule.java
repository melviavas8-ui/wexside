package ru.wexside.module.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.ElapsedTimer;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.module.player.AutoEatModule;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public class AutoFishModule
extends Module
implements ConfigSerializable {
    private static final long RECAST_DELAY_MS = 1000L;
    private static final double BITE_DISTANCE_SQ = 9.0;
    private final BooleanSetting enabledSetting;
    private final ElapsedTimer cooldown = new ElapsedTimer();
    private double splashX;
    private double splashY;
    private double splashZ;
    private volatile boolean bitePending;

    public AutoFishModule(EventBus eventBus) {
        super(eventBus, "auto_fish", "Auto Fish", "Автоматическая рыбалка", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onTick);
        this.listen(IncomingPacketEvent.class, this::onIncomingPacket);
        this.listen(WorldSessionEvent.class, event -> this.reset());
    }

    private void onIncomingPacket(IncomingPacketEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (!(packet instanceof PlaySoundS2CPacket)) {
            return;
        }
        PlaySoundS2CPacket soundPacket = (PlaySoundS2CPacket)packet;
        if (!((SoundEvent)soundPacket.getSound().value()).equals((Object)SoundEvents.ENTITY_FISHING_BOBBER_SPLASH)) {
            return;
        }
        this.splashX = soundPacket.getX();
        this.splashY = soundPacket.getY();
        this.splashZ = soundPacket.getZ();
        this.bitePending = true;
    }

    private void onTick(ClientTickEvent event) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (!this.enabledSetting.isEnabled() || player == null || MinecraftClient.getInstance().world == null || AutoEatModule.eating || !player.getMainHandStack().isOf(Items.FISHING_ROD)) {
            this.reset();
            return;
        }
        if (this.bitePending) {
            this.bitePending = false;
            FishingBobberEntity bobber = player.fishHook;
            if (bobber != null && bobber.getEntityPos().squaredDistanceTo(this.splashX, this.splashY, this.splashZ) < 9.0) {
                this.useRod(player);
                this.cooldown.update();
                return;
            }
        }
        if (player.fishHook == null && this.cooldown.hasElapsed(1000L)) {
            this.useRod(player);
            this.cooldown.update();
        }
    }

    private void useRod(ClientPlayerEntity player) {
        ClientPlayerInteractionManager interactions = MinecraftClient.getInstance().interactionManager;
        if (interactions == null) {
            return;
        }
        interactions.interactItem((PlayerEntity)player, Hand.MAIN_HAND);
        player.swingHand(Hand.MAIN_HAND);
    }

    private void reset() {
        this.bitePending = false;
    }
}

