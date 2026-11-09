package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import ru.wexside.misc.PlayerShieldStateAccessor;
import ru.wexside.misc.SprintResetMode;
import ru.wexside.module.movement.AutoSprintModule;

public class ShieldDesync {
    private boolean restartSprintNextTick;
    private SprintResetMode mode = SprintResetMode.PACKET;

    public boolean sendsSprintPackets() {
        return this.mode.isActive();
    }

    public void restoreSprint() {
        if (!this.restartSprintNextTick) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && !player.isUsingItem() && !((PlayerShieldStateAccessor)player).isShieldUseForced()) {
            player.networkHandler.sendPacket((Packet)new ClientCommandC2SPacket((Entity)player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            player.setSprinting(true);
            ((PlayerShieldStateAccessor)player).setShieldUseForced(true);
        }
        this.restartSprintNextTick = false;
    }

    public void prepareForAttack() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.isUsingItem() || !player.isSprinting()) {
            return;
        }
        this.stopSprinting(player);
        if (this.mode == SprintResetMode.LEGIT || this.mode == SprintResetMode.SEMI_LEGIT) {
            MinecraftClient.getInstance().options.sprintKey.setPressed(false);
            AutoSprintModule.tick();
        }
    }

    public void setMode(SprintResetMode mode) {
        this.mode = mode;
    }

    public boolean delaysAttackUntilSprintStops() {
        return this.mode.isAvailable();
    }

    private void stopSprinting(ClientPlayerEntity player) {
        if (!((PlayerShieldStateAccessor)player).isShieldUseForced()) {
            return;
        }
        player.setSprinting(false);
        if (this.mode != SprintResetMode.LEGIT) {
            player.networkHandler.sendPacket((Packet)new ClientCommandC2SPacket((Entity)player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            ((PlayerShieldStateAccessor)player).setShieldUseForced(false);
        }
        this.restartSprintNextTick = true;
    }
}

