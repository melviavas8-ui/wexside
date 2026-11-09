package ru.wexside.module.misc;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public class HeadHitterModule
extends Module
implements ConfigSerializable {
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();

    public HeadHitterModule(EventBus eventBus) {
        super(eventBus, "head_hitter", "Head Hitter", "Подпрыгивает при касании блока головой", ModuleCategory.valueOf("MISC"), new String[0]);
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        double jump;
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        Box box = player.getBoundingBox().contract(-0.02);
        Box head = new Box(box.minX, box.maxY - 0.4, box.minZ, box.maxX, box.maxY, box.maxZ);
        if (this.collides(head, client)) {
            jump = 0.41;
        } else if (this.collides(box, client)) {
            jump = 0.42;
        } else {
            return;
        }
        WexSideClient.getTickOverride().setBooleanType(true);
        Vec3d velocity = player.getVelocity();
        player.setVelocity(velocity.x, jump, velocity.z);
    }

    private boolean collides(Box box, MinecraftClient client) {
        Box search = box.expand(0.03, 0.0, 0.03);
        int minX = MathHelper.floor((double)search.minX);
        int minY = MathHelper.floor((double)search.minY);
        int minZ = MathHelper.floor((double)search.minZ);
        int maxX = MathHelper.floor((double)search.maxX);
        int maxY = MathHelper.floor((double)search.maxY);
        int maxZ = MathHelper.floor((double)search.maxZ);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    VoxelShape shape;
                    pos.set(x, y, z);
                    BlockState state = client.world.getBlockState((BlockPos)pos);
                    if (state.isAir() || (shape = state.getCollisionShape((BlockView)client.world, (BlockPos)pos)).isEmpty()) continue;
                    for (Box part : shape.getBoundingBoxes()) {
                        Box worldBox = part.offset((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
                        if (!worldBox.intersects(search) || state.isOpaqueFullCube() && state.getBlock() != Blocks.COBWEB) continue;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

