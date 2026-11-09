package ru.wexside.mixin;

import java.util.function.Predicate;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.wexside.module.combat.NoEntityTraceModule;
import ru.wexside.module.player.OpenWallsModule;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerInteractionRaycastMixin {
    @Redirect(
        method = "getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;")
    )
    private static HitResult wexside$filterBlockRaycast(
        Entity raycastEntity, double maxDistance, float raycastTickDelta, boolean includeFluids,
        Entity camera, double blockRange, double entityRange, float tickDelta
    ) {
        HitResult original = raycastEntity.raycast(maxDistance, raycastTickDelta, includeFluids);
        if (!OpenWallsModule.isEnabled() || !(original instanceof BlockHitResult blockHit)
            || blockHit.getType() != HitResult.Type.BLOCK
            || wexside$isInteractionBlock(camera.getEntityWorld().getBlockState(blockHit.getBlockPos()))) {
            return original;
        }
        return wexside$raycastInteractionBlocks(camera, blockRange, tickDelta);
    }

    @Redirect(
        method = "getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;")
    )
    private static EntityHitResult wexside$filterEntityRaycast(
        Entity entity, Vec3d start, Vec3d end, Box box, Predicate<Entity> predicate, double maxDistanceSquared
    ) {
        return NoEntityTraceModule.isEnabled() ? null
            : ProjectileUtil.raycast(entity, start, end, box, predicate, maxDistanceSquared);
    }

    @Unique
    private static HitResult wexside$raycastInteractionBlocks(Entity camera, double range, float tickDelta) {
        World world = camera.getEntityWorld();
        Vec3d origin = camera.getCameraPosVec(tickDelta);
        Vec3d direction = camera.getRotationVec(tickDelta);
        Vec3d end = origin.add(direction.multiply(range));
        Vec3d start = origin;
        for (int step = 0; step < 200; step++) {
            BlockHitResult hit = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, camera
            ));
            if (hit.getType() == HitResult.Type.MISS
                || wexside$isInteractionBlock(world.getBlockState(hit.getBlockPos()))) {
                return hit;
            }
            Vec3d position = hit.getPos();
            if (position.distanceTo(origin) >= range) {
                return BlockHitResult.createMissed(position, hit.getSide(), hit.getBlockPos());
            }
            start = position.add(direction.multiply(0.01));
        }
        return BlockHitResult.createMissed(start, Direction.getFacing(direction), BlockPos.ofFloored(start));
    }

    @Unique
    private static boolean wexside$isInteractionBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof ChestBlock || block instanceof BarrelBlock
            || block instanceof CraftingTableBlock || block instanceof AbstractFurnaceBlock
            || block instanceof DispenserBlock || block instanceof HopperBlock
            || block instanceof EnderChestBlock;
    }
}
