package ru.wexside.module.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.DamageEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.event.MovementSlowdownEvent;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.util.DamageTracker;

public final class NoSlowDownModule
extends Module
implements ConfigSerializable {
    private int spookyTicks;
    private final DamageTracker damageTracker = new DamageTracker();
    private final ModeSetting mode;
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Убирает замедление при использовании предметов").withKeybind().toggle()).build();

    public NoSlowDownModule(EventBus eventBus) {
        super(eventBus, "no_slow_down", "No Slow Down", "Убирает замедление при использовании предметов", ModuleCategory.valueOf("MOVEMENT"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.mode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("FT", "Crossbow", "Spooky-T", "Vanilla", "Matrix", "Grim", "Really-World").defaultOption("FT").name("Mode").id("mode").description("Метод устранения замедления")).build();
        this.registerSetting(this.mode);
    }

    @Override
    protected void initialize() {
        this.listen(MovementSlowdownEvent.class, this::onSlowdown);
        this.listen(IncomingPacketEvent.class, this::onIncomingPacket);
        this.listen(DamageEvent.class, this::onDamage);
    }

    private void onSlowdown(MovementSlowdownEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        if (!player.isUsingItem()) {
            return;
        }
        switch (this.mode.getSelectedOption()) {
            case "FT": {
                this.applyFunTime(event, player);
                break;
            }
            case "Crossbow": {
                if (player.getMainHandStack().getItem() != Items.CROSSBOW) break;
                event.update();
                break;
            }
            case "Spooky-T": {
                this.applySpookyTime(event, player);
                break;
            }
            case "Vanilla": {
                event.update();
                break;
            }
            case "Matrix": {
                this.applyMatrix(event, player);
                break;
            }
            case "Grim": {
                this.applyGrim(event, player);
                break;
            }
            case "Really-World": {
                this.applyReallyWorld(event, player);
            }
        }
    }

    private void applyReallyWorld(MovementSlowdownEvent event, ClientPlayerEntity player) {
        this.damageTracker.consumeAfter(1500L);
        if (this.skipOffhandConsume(player)) {
            return;
        }
        if (player.isOnGround() && !player.isTouchingWater() && !this.damageTracker.isDamageConfirmed()) {
            return;
        }
        if (player.getActiveHand() == Hand.MAIN_HAND) {
            this.sendInteract(player, Hand.OFF_HAND);
            event.update();
            return;
        }
        event.update();
        this.desyncSelectedSlot(player);
    }

    private void onDamage(DamageEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        this.damageTracker.onDamage(event);
    }

    private void applyMatrix(MovementSlowdownEvent event, ClientPlayerEntity player) {
        boolean falling = player.fallDistance > 0.725;
        event.update();
        if (player.isOnGround() && !player.isTouchingWater()) {
            if (player.age % 2 == 0) {
                float scale = player.input.getMovementInput().x == 0.0f ? 0.5f : 0.4f;
                this.scaleHorizontal(player, scale);
            }
        } else if (falling) {
            float scale = player.fallDistance > 1.4 ? 0.95f : 0.97f;
            this.scaleHorizontal(player, scale);
        }
    }

    private void applyFunTime(MovementSlowdownEvent event, ClientPlayerEntity player) {
        if (player.isClimbing()) {
            return;
        }
        if (player.getItemUseTime() < 4) {
            ClientPlayerInteractionManager interactions = MinecraftClient.getInstance().interactionManager;
            if (interactions != null) {
                interactions.clickSlot(0, 0, 0, SlotActionType.PICKUP, (PlayerEntity)player);
            }
            player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
            return;
        }
        event.update();
    }

    private void sendInteract(ClientPlayerEntity player, Hand hand) {
        player.networkHandler.sendPacket((Packet)new PlayerInteractItemC2SPacket(hand, 0, player.getYaw(), player.getPitch()));
    }

    private void applySpookyTime(MovementSlowdownEvent event, ClientPlayerEntity player) {
        ++this.spookyTicks;
        if (this.spookyTicks % 2 == 0) {
            Hand hand = player.getActiveItem().getMaxUseTime((LivingEntity)player) > 0 ? Hand.OFF_HAND : Hand.MAIN_HAND;
            this.sendInteract(player, hand);
            event.update();
        }
    }

    private boolean isMoving(ClientPlayerEntity player) {
        Vec2f movement = player.input.getMovementInput();
        return movement.x != 0.0f || movement.y != 0.0f;
    }

    private void scaleHorizontal(ClientPlayerEntity player, float scale) {
        Vec3d velocity = player.getVelocity();
        player.setVelocity(velocity.x * (double)scale, velocity.y, velocity.z * (double)scale);
    }

    private void applyGrim(MovementSlowdownEvent event, ClientPlayerEntity player) {
        if (this.skipOffhandConsume(player)) {
            return;
        }
        if (player.getActiveHand() == Hand.MAIN_HAND) {
            this.sendInteract(player, Hand.OFF_HAND);
            event.update();
            return;
        }
        event.update();
        this.desyncSelectedSlot(player);
    }

    private boolean skipOffhandConsume(ClientPlayerEntity player) {
        UseAction action = player.getOffHandStack().getUseAction();
        return (action == UseAction.EAT || action == UseAction.DRINK) && player.getActiveHand() == Hand.MAIN_HAND;
    }

    private void desyncSelectedSlot(ClientPlayerEntity player) {
        if (this.isMoving(player)) {
            int selected = player.getInventory().getSelectedSlot();
            player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(selected % 8 + 1));
            player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(selected));
        }
    }

    private void onIncomingPacket(IncomingPacketEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        this.damageTracker.onIncomingPacket(event);
    }
}

