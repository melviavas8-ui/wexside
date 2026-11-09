package ru.wexside.module.player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.AttackUrgency;
import ru.wexside.misc.BlockBreakingAccessor;
import ru.wexside.misc.CorrectionMode;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.Angle;
import ru.wexside.util.RotationController;
import ru.wexside.util.RotationIntent;

public class NukerModule
extends Module
implements ConfigSerializable {
    private static final String ROTATION_OWNER = "Simple";
    private final BooleanSetting enabledSetting;
    private final NumberSetting speed;
    private final NumberSetting height;
    private final NumberSetting blocks;
    private final BooleanSetting rotate;
    private final List<BlockPos> targets = new ArrayList<BlockPos>();
    private boolean sessionActive;
    private boolean rotating;

    public NukerModule(EventBus eventBus) {
        super(eventBus, "nuker", "Nuker", "Ломает блоки вокруг игрока", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.speed = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 100.0).defaultValue(70.0).multiplier(1.0).precision(0).animationSpeed(20.0f).showMarkers().name("Speed").id("speed").description("Ускорение (в % от изначальной скорости)")).build();
        this.registerSetting(this.speed);
        this.height = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 10.0).defaultValue(3.0).multiplier(1.0).precision(0).animationSpeed(20.0f).showMarkers().name("Height").id("height").description("Сколько блоков в высоту ломать").aliases("height", "высота")).build();
        this.registerSetting(this.height);
        this.blocks = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 30.0).defaultValue(1.0).multiplier(1.0).precision(0).animationSpeed(20.0f).showMarkers().name("Multiply-Blocks").id("blocks").description("Сколько блоков ломать одновременно")).build();
        this.registerSetting(this.blocks);
        this.rotate = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Rotate").id("rotate").description("Плавно поворачиваться на ломаемый блок")).build();
        this.registerSetting(this.rotate);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, this::setFloatType);
        this.listen(WorldSessionEvent.class, event -> this.reset());
    }

    private void setFloatType(ClientTickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager interactions = client.interactionManager;
        if (!this.enabledSetting.isEnabled() || player == null || client.world == null || interactions == null) {
            this.sessionActive = false;
            this.releaseLook();
            return;
        }
        if (!this.sessionActive) {
            this.sessionActive = true;
            this.targets.clear();
        }
        BlockBreakingAccessor breaking = (BlockBreakingAccessor)interactions;
        breaking.setBreakingCooldown(0);
        float speedBonus = (float)this.speed.getIntValue() / 100.0f;
        if (this.targets.isEmpty()) {
            this.scanTargets(player);
        }
        BlockPos lookTarget = null;
        Iterator<BlockPos> iterator = this.targets.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (client.world.getBlockState(pos).isAir()) {
                iterator.remove();
                continue;
            }
            if (lookTarget == null) {
                lookTarget = pos;
            }
            player.swingHand(Hand.MAIN_HAND);
            interactions.updateBlockBreakingProgress(pos, Direction.UP);
            float progress = breaking.getBreakingProgress();
            if (!(progress > 1.0f - speedBonus) || !(progress < 0.99f)) continue;
            breaking.setBreakingProgress(0.99f);
        }
        if (this.rotate.isEnabled() && lookTarget != null) {
            this.lookAt(player, lookTarget);
        } else {
            this.releaseLook();
        }
    }

    private void reset() {
        this.targets.clear();
        this.sessionActive = false;
        this.releaseLook();
    }

    private void scanTargets(ClientPlayerEntity player) {
        int limit = this.blocks.getIntValue();
        int minY = (int)Math.floor(player.getY());
        int maxY = minY + this.height.getIntValue() - 1;
        double x = player.getX();
        double z = player.getZ();
        MinecraftClient client = MinecraftClient.getInstance();
        for (int y = minY; y <= maxY; ++y) {
            for (int offsetX = -3; offsetX <= 3; ++offsetX) {
                for (int offsetZ = -3; offsetZ <= 3; ++offsetZ) {
                    BlockPos pos;
                    BlockState state;
                    if (this.targets.size() >= limit) {
                        return;
                    }
                    if (Math.sqrt((double)offsetX * (double)offsetX + (double)offsetZ * (double)offsetZ) > 4.0 || (state = client.world.getBlockState(pos = BlockPos.ofFloored((double)(x + (double)offsetX), (double)y, (double)(z + (double)offsetZ)))).isAir() || this.unbreakable(state, pos) || !this.hasLineOfSight(player, pos)) continue;
                    this.targets.add(pos);
                }
            }
        }
    }

    private Vec3d blockCenter(BlockPos pos) {
        return new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
    }

    private boolean rotationOwnedByOther(RotationController rotations, ClientPlayerEntity player) {
        RotationIntent intent = rotations.getCurrentIntent();
        return intent != null && intent.hasTarget() && intent.target() != player;
    }

    private boolean hasLineOfSight(ClientPlayerEntity player, BlockPos pos) {
        RaycastContext context = new RaycastContext(player.getEyePos(), this.blockCenter(pos), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)player);
        BlockHitResult hit = MinecraftClient.getInstance().world.raycast(context);
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals((Object)pos);
    }

    private boolean unbreakable(BlockState state, BlockPos pos) {
        return state.getBlock() == Blocks.BEDROCK || state.getHardness((BlockView)MinecraftClient.getInstance().world, pos) < 0.0f;
    }

    private void lookAt(ClientPlayerEntity player, BlockPos pos) {
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations == null || this.rotationOwnedByOther(rotations, player)) {
            return;
        }
        Angle angle = Angle.fromVectors(player.getEyePos(), this.blockCenter(pos));
        rotations.applyIntent(new RotationIntent((LivingEntity)player, null, angle, AttackUrgency.HIT, CorrectionMode.FREE, true), ROTATION_OWNER);
        this.rotating = true;
        Angle applied = rotations.getAngle();
        if (applied != null) {
            player.setYaw(applied.getYaw());
            player.setPitch(applied.getPitch());
        }
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

