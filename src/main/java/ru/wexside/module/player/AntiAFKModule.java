package ru.wexside.module.player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.AttackUrgency;
import ru.wexside.misc.CorrectionMode;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.util.Angle;
import ru.wexside.util.RotationController;
import ru.wexside.util.RotationIntent;

public class AntiAFKModule
extends Module
implements ConfigSerializable {
    private static final int SWAP_INTERVAL = 5;
    private static final int CYCLE_TICKS = 1200;
    private static final float YAW_STEP = 6.2f;
    private static final int JUMP_INTERVAL = 40;
    private static final int MESSAGE_INTERVAL = 400;
    private static final int FLAG_INTERVAL = 600;
    private static final String ROTATION_OWNER = "Simple";
    private static final String JUMP = "Jump";
    private static final String ROTATION = "Rotation";
    private static final String JUMP_ROTATION = "Jump+Rotation";
    private static final String MESSAGE = "Message";
    private static final String SWAP_SLOT = "Swap-Slot";
    private static final String ANTI_CHEAT_FLAG = "Anti-Cheat-Flag";
    private static final String STRAFE = "Strafe";
    private static final String CHAT_MESSAGE = "/fackatrongeralol";
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
    private final MultiSelectSetting actions;
    private int tickCounter;
    private int rememberedSlot = -1;
    private float yawOffset;
    private boolean jumpHeld;
    private boolean strafeHeld;
    private boolean rotating;

    public AntiAFKModule(EventBus eventBus) {
        super(eventBus, "anti_afk", "Anti AFK", "Предотвращает отключение за бездействие", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.registerSetting(this.enabledSetting);
        MultiSelectSetting actionsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options(JUMP, ROTATION, JUMP_ROTATION, MESSAGE, SWAP_SLOT, ANTI_CHEAT_FLAG, STRAFE).selectAll(false).optionListEnabled(false).name("Actions").id("actions").description("Действия для предотвращения AFK").aliases("actions", "действия")).build();
        actionsSetting.setOptions(new String[0]);
        this.actions = actionsSetting;
        this.registerSetting(actionsSetting);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onTick);
        this.listen(WorldSessionEvent.class, event -> this.reset());
    }

    private void onTick(ClientTickEvent event) {
        boolean rotateOrStrafe;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (!this.enabledSetting.isEnabled() || player == null || MinecraftClient.getInstance().world == null) {
            this.reset();
            return;
        }
        this.tickCounter = (this.tickCounter + 1) % 1200;
        List<String> selected = this.actions.getSelectedOptions();
        boolean jumpRotation = selected.contains(JUMP_ROTATION);
        boolean bl = rotateOrStrafe = selected.contains(ROTATION) || selected.contains(STRAFE) || jumpRotation;
        if (selected.contains(JUMP) || rotateOrStrafe) {
            if (this.tickCounter % 40 == 0) {
                MinecraftClient.getInstance().options.jumpKey.setPressed(true);
                this.jumpHeld = true;
            } else {
                this.releaseJump();
            }
        } else {
            this.releaseJump();
        }
        if (jumpRotation) {
            this.spin(player);
        } else {
            this.releaseLook();
        }
        if (selected.contains(MESSAGE) && this.tickCounter % 400 == 0) {
            this.sendMessage(player);
        }
        if (selected.contains(SWAP_SLOT)) {
            this.swapSlot(player);
        } else {
            this.restoreSlot();
        }
        if (selected.contains(ANTI_CHEAT_FLAG) && this.tickCounter % 600 == 0) {
            this.sendFlagPacket(player);
        }
        if (selected.contains(STRAFE)) {
            MinecraftClient.getInstance().options.leftKey.setPressed(true);
            this.strafeHeld = true;
        } else {
            this.releaseStrafe();
        }
    }

    private void reset() {
        this.tickCounter = 0;
        this.yawOffset = 0.0f;
        this.releaseAll();
    }

    private void releaseAll() {
        this.restoreSlot();
        this.releaseStrafe();
        this.releaseJump();
        this.releaseLook();
    }

    private void sendFlagPacket(ClientPlayerEntity player) {
        ClientPlayNetworkHandler network = player.networkHandler;
        if (network == null) {
            return;
        }
        Angle angle = this.currentAngle(player);
        network.sendPacket((Packet)new PlayerMoveC2SPacket.Full(player.getX() + 1.0, player.getY() + 1.0, player.getZ() + 1.0, angle.getYaw(), angle.getPitch(), true, false));
    }

    private void swapSlot(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        if (this.rememberedSlot == -1) {
            this.rememberedSlot = inventory.getSelectedSlot();
        }
        if (this.tickCounter % 5 == 0) {
            inventory.setSelectedSlot(ThreadLocalRandom.current().nextInt(9));
        }
    }

    private void sendMessage(ClientPlayerEntity player) {
        ClientPlayNetworkHandler network = player.networkHandler;
        if (network == null) {
            return;
        }
        if (CHAT_MESSAGE.startsWith("/")) {
            network.sendChatCommand(CHAT_MESSAGE.substring(1));
        } else {
            network.sendChatMessage(CHAT_MESSAGE);
        }
    }

    private void restoreSlot() {
        if (this.rememberedSlot == -1) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.getInventory().setSelectedSlot(this.rememberedSlot);
        }
        this.rememberedSlot = -1;
    }

    private boolean rotationOwnedByOther(RotationController rotations, ClientPlayerEntity player) {
        RotationIntent intent = rotations.getCurrentIntent();
        return intent != null && intent.hasTarget() && intent.target() != player;
    }

    private void spin(ClientPlayerEntity player) {
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations == null || this.rotationOwnedByOther(rotations, player)) {
            return;
        }
        this.yawOffset = (this.yawOffset + 6.2f) % 360.0f;
        rotations.applyIntent(new RotationIntent((LivingEntity)player, null, new Angle(this.yawOffset, player.getPitch()), AttackUrgency.HIT, CorrectionMode.FREE, true), ROTATION_OWNER);
        this.rotating = true;
        Angle applied = rotations.getAngle();
        if (applied != null) {
            player.setYaw(applied.getYaw());
        }
    }

    private Angle currentAngle(ClientPlayerEntity player) {
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations != null && rotations.isActive() && rotations.getAngle() != null) {
            return rotations.getAngle();
        }
        return new Angle(player.getYaw(), player.getPitch());
    }

    private void releaseJump() {
        if (!this.jumpHeld) {
            return;
        }
        MinecraftClient.getInstance().options.jumpKey.setPressed(false);
        this.jumpHeld = false;
    }

    private void releaseStrafe() {
        if (!this.strafeHeld) {
            return;
        }
        MinecraftClient.getInstance().options.leftKey.setPressed(false);
        this.strafeHeld = false;
    }

    private void releaseLook() {
        if (!this.rotating) {
            return;
        }
        this.rotating = false;
        RotationController rotations = WexSideClient.getRotationController();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (rotations != null && rotations.isActive() && !this.rotationOwnedByOther(rotations, player)) {
            rotations.clearIntent();
        }
    }
}

