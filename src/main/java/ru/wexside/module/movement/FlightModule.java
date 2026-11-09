package ru.wexside.module.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.AttackUrgency;
import ru.wexside.misc.CorrectionMode;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.module.ModuleManager;
import ru.wexside.module.combat.AttackAuraModule;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.Setting;
import ru.wexside.util.Angle;
import ru.wexside.util.RotationController;
import ru.wexside.util.RotationIntent;

public final class FlightModule
extends Module
implements ConfigSerializable {
    private static final long PLACE_INTERVAL_MS = 20L;
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Позволяет летать на FT (требуется голова в руке)").withKeybind().toggle()).build();
    private long lastPlaceTime;
    private BlockPos columnPos;

    public FlightModule(EventBus eventBus) {
        super(eventBus, "flight", "Flight", "Позволяет летать на FT (требуется голова в руке)", ModuleCategory.valueOf("MOVEMENT"), new String[0]);
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::onClientTick);
    }

    private void onClientTick(ClientTickEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            this.columnPos = null;
            this.releaseLook();
            return;
        }
        this.disableAura();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            return;
        }
        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            this.releaseLook();
            return;
        }
        if (this.columnPos == null) {
            Direction facing = player.getHorizontalFacing();
            Vec3d ahead = new Vec3d(player.getX(), player.getY(), player.getZ()).add((double)facing.getOffsetX(), 0.0, (double)facing.getOffsetZ());
            this.columnPos = BlockPos.ofFloored((Position)ahead);
        }
        boolean placed = false;
        BlockPos pos = this.columnPos;
        while (pos.getY() < world.getBottomY() + world.getHeight()) {
            if (world.getBlockState(pos).isAir() && !world.getBlockState(pos.down()).isAir()) {
                long now = System.currentTimeMillis();
                if (now - this.lastPlaceTime < 20L) break;
                Vec3d hitPos = Vec3d.ofCenter((Vec3i)pos);
                BlockHitResult hit = new BlockHitResult(hitPos, Direction.UP, pos.down(), false);
                this.applyLook(player, Angle.fromVectors(player.getEyePos(), hitPos));
                ClientPlayerInteractionManager interactions = client.interactionManager;
                if (interactions != null) {
                    interactions.interactBlock(player, Hand.MAIN_HAND, hit);
                }
                this.lastPlaceTime = now;
                placed = true;
                break;
            }
            pos = pos.up();
        }
        if (!placed) {
            this.releaseLook();
        }
    }

    private void applyLook(ClientPlayerEntity player, Angle angle) {
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations == null) {
            return;
        }
        RotationIntent intent = new RotationIntent((LivingEntity)player, null, angle, AttackUrgency.HIT, CorrectionMode.FOCUSED, false);
        rotations.applyIntent(intent, "Simple");
    }

    private void releaseLook() {
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations == null || !rotations.isActive()) {
            return;
        }
        RotationIntent intent = rotations.getCurrentIntent();
        if (intent != null && intent.target() == MinecraftClient.getInstance().player) {
            rotations.clearIntent();
        }
    }

    private void disableAura() {
        ModuleManager moduleManager = WexSideClient.getInstance().getModuleManager();
        if (moduleManager == null) {
            return;
        }
        AttackAuraModule attackAura = moduleManager.getModule(AttackAuraModule.class);
        if (attackAura == null || !attackAura.isActive()) {
            return;
        }
        Setting toggle = attackAura.getToggleSetting();
        if (toggle instanceof BooleanSetting) {
            BooleanSetting enabled = (BooleanSetting)toggle;
            enabled.setEnabled(false);
        }
    }
}

