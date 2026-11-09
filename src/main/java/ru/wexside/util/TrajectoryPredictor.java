package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.wexside.prediction.ProjectileImpact;
import ru.wexside.prediction.ProjectileMotionStep;
import ru.wexside.prediction.TrajectoryPrediction;
import ru.wexside.render.RenderCamera;
import ru.wexside.util.ProjectileType;

public final class TrajectoryPredictor {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    static final int slot = 12;
    static final float value3 = 10.0f;
    private Vec3d smoothedShooterMotion = Vec3d.ZERO;
    static final int slot2 = 200;
    static final double value4 = 0.3;
    static final double value5 = 0.3;
    static final double value6 = 0.6;
    static final float value7 = (float)Math.PI / 180;

    public List<TrajectoryPrediction> predictHeldItemTrajectories(LivingEntity screenHandler, ItemStack stack, float f) {
        float[] fArray;
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        if (ProjectileType.fromItem(stack.getItem()) == null) {
            return List.of();
        }
        this.updateShooterVelocity(screenHandler);
        boolean bl = this.hasMultishot(stack);
        if (bl) {
            float[] fArray2 = new float[3];
            fArray2[0] = 0.0f;
            fArray2[1] = -10.0f;
            fArray = fArray2;
            fArray2[2] = 10.0f;
        } else {
            float[] fArray3;
            fArray = fArray3 = new float[1];
            fArray3[0] = 0.0f;
        }
        float[] fArray4 = fArray;
        ArrayList<TrajectoryPrediction> arrayList = new ArrayList<TrajectoryPrediction>(fArray4.length);
        for (float f2 : fArray4) {
            TrajectoryPrediction trajectoryPrediction = this.predictItemLaunch(screenHandler, stack, f, f2);
            if (trajectoryPrediction == null) continue;
            arrayList.add(trajectoryPrediction);
        }
        return arrayList;
    }

    public TrajectoryPrediction predictEntityTrajectory(Entity entity2, float f) {
        if (this.mc.world == null || entity2 == null) {
            return null;
        }
        if (entity2 instanceof ItemEntity) {
            ItemEntity iIIIlIIIII2 = (ItemEntity)entity2;
            return this.predictDroppedItemTrajectory(iIIIlIIIII2, f);
        }
        ProjectileType iiIlilIilI2 = ProjectileType.fromEntity(entity2);
        if (iiIlilIilI2 == null) {
            return null;
        }
        Vec3d vec2 = new Vec3d(entity2.getX(), entity2.getY(), entity2.getZ());
        Vec3d vec3 = entity2.getVelocity();
        boolean bl = this.updatesMotionBeforeMove(iiIlilIilI2);
        return this.simulateTrajectory(entity2, vec2, vec3, RaycastContext.FluidHandling.NONE, iiIlilIilI2, (vec4, vec5) -> {
            boolean bl2 = this.isInFluid(vec4);
            return ProjectileKinematics.integrateVelocity(vec5, this.getEntityDrag(entity2, bl2), this.getEntityGravity(entity2), bl);
        }, bl);
    }

    public TrajectoryPrediction predictDroppedItemTrajectory(ItemEntity iIIIlIIIII2, float f) {
        if (this.mc.world == null || iIIIlIIIII2 == null) {
            return null;
        }
        return this.simulateTrajectory((Entity)iIIIlIIIII2, new Vec3d(iIIIlIIIII2.getX(), iIIIlIIIII2.getY(), iIIIlIIIII2.getZ()), iIIIlIIIII2.getVelocity(), RaycastContext.FluidHandling.NONE, ProjectileType.ITEM, (vec4, vec5) -> ProjectileKinematics.integrateVelocity(vec5, this.getEntityDrag((Entity)iIIIlIIIII2, this.isInFluid(vec4)), this.getEntityGravity((Entity)iIIIlIIIII2), false), false);
    }

    public Vec3d getLaunchOrigin(LivingEntity screenHandler, float f) {
        if (screenHandler == this.mc.player && this.mc.options.getPerspective().isFirstPerson() && RenderCamera.position() != null) {
            return RenderCamera.position();
        }
        return screenHandler.getCameraPosVec(f);
    }

    private Vec3d getItemLaunchDirection(LivingEntity screenHandler, Item iiIilIIilI2, float f, float f2) {
        Vec3d vec4 = this.directionFromAngles(screenHandler.getPitch(f), screenHandler.getYaw(f), this.getItemPitchOffset(iiIilIIilI2));
        if (!(iiIilIIilI2 instanceof CrossbowItem) || f2 == 0.0f) {
            return vec4;
        }
        return this.rotateAroundAxis(vec4, screenHandler.getRotationVec(f), f2).normalize();
    }

    private double getEntityGravity(Entity entity2) {
        if (entity2 instanceof ExperienceBottleEntity) {
            return 0.07;
        }
        ProjectileType iiIlilIilI2 = ProjectileType.fromEntity(entity2);
        if (iiIlilIilI2 == ProjectileType.TRIDENT || iiIlilIilI2 == ProjectileType.ARROW || iiIlilIilI2 == ProjectileType.POTION) {
            return 0.05;
        }
        if (iiIlilIilI2 == ProjectileType.PEARL) {
            return 0.03;
        }
        if (iiIlilIilI2 == ProjectileType.ITEM) {
            return 0.04;
        }
        return 0.03;
    }

    private boolean updatesMotionBeforeMove(ProjectileType iiIlilIilI2) {
        return iiIlilIilI2 == ProjectileType.POTION || iiIlilIilI2 == ProjectileType.PEARL;
    }

    private Vec3d directionFromAngles(float f, float f2, float f3) {
        float f4 = f * ((float)Math.PI / 180);
        float f5 = f2 * ((float)Math.PI / 180);
        float f6 = (f + f3) * ((float)Math.PI / 180);
        double d = -Math.sin(f5) * Math.cos(f4);
        double d2 = -Math.sin(f6);
        double d3 = Math.cos(f5) * Math.cos(f4);
        return new Vec3d(d, d2, d3).normalize();
    }

    private TrajectoryPrediction predictItemLaunch(LivingEntity screenHandler, ItemStack stack, float f, float f2) {
        boolean bl;
        if (this.mc.world == null) {
            return null;
        }
        Item iiIilIIilI2 = stack.getItem();
        float f3 = this.getLaunchSpeed(screenHandler, stack);
        if (f3 <= 0.0f) {
            return null;
        }
        Vec3d vec2 = this.getLaunchOrigin(screenHandler, f);
        Vec3d vec3 = this.getItemLaunchDirection(screenHandler, iiIilIIilI2, f, f2);
        Vec3d vec6 = this.getUnadjustedLaunchDirection(screenHandler, iiIilIIilI2, f, f2);
        Vec3d vec7 = this.getInheritedShooterMotion(iiIilIIilI2);
        Vec3d vec8 = ProjectileKinematics.launchVelocity(vec3, (double)f3, vec7);
        Vec3d vec9 = vec6.multiply((double)f3);
        ProjectileMotionStep ilIIlIIIlI2 = (vec4, vec5) -> {
            boolean inFluid = this.isInFluid(vec4);
            return ProjectileKinematics.integrateVelocity(vec5, this.getItemDrag(iiIilIIilI2, inFluid), this.getItemGravity(iiIilIIilI2), false);
        };
        ProjectileType iiIlilIilI2 = ProjectileType.fromItem(iiIilIIilI2);
        TrajectoryPrediction trajectoryPrediction = this.simulateTrajectory((Entity)screenHandler, vec2, vec8, RaycastContext.FluidHandling.NONE, iiIlilIilI2, ilIIlIIIlI2, false);
        if (trajectoryPrediction == null) {
            return null;
        }
        boolean bl2 = bl = this.getItemPitchOffset(iiIilIIilI2) != 0.0f || vec7.lengthSquared() >= 1.0E-6;
        if (!bl) {
            return trajectoryPrediction;
        }
        TrajectoryPrediction trajectoryPrediction2 = this.simulateTrajectory((Entity)screenHandler, vec2, vec9, RaycastContext.FluidHandling.NONE, iiIlilIilI2, ilIIlIIIlI2, false);
        return this.blendLaunchPaths(trajectoryPrediction, trajectoryPrediction2);
    }

    private static double smoothstep(double d) {
        d = Math.max(0.0, Math.min(1.0, d));
        return d * d * (3.0 - 2.0 * d);
    }

    private TrajectoryPrediction simulateTrajectory(Entity entity2, Vec3d vec4, Vec3d vec5, RaycastContext.FluidHandling fluidHandling, ProjectileType iiIlilIilI2, ProjectileMotionStep motionStep, boolean applyBeforeMove) {
        if (this.mc.world == null) {
            return null;
        }
        ArrayList<Vec3d> arrayList = new ArrayList<Vec3d>(201);
        arrayList.add(vec4);
        Vec3d vec2 = vec4;
        Vec3d vec3 = vec5;
        for (int i = 0; i < 200; ++i) {
            Vec3d vec6;
            if (applyBeforeMove) {
                vec3 = motionStep.apply(vec2, vec3);
            }
            Vec3d vec7 = vec2.add(vec3);
            BlockHitResult hit2 = this.mc.world.raycast(new RaycastContext(vec2, vec7, RaycastContext.ShapeType.COLLIDER, fluidHandling, entity2));
            EntityHitResult iIIIllIilI2 = this.raycastEntities(vec2, vec7, entity2, iiIlilIilI2);
            double d = hit2.getType() == HitResult.Type.BLOCK ? vec2.squaredDistanceTo(hit2.getPos()) : Double.MAX_VALUE;
            double d2 = iIIIllIilI2 != null ? vec2.squaredDistanceTo(iIIIllIilI2.getPos()) : Double.MAX_VALUE;
            Vec3d vec8 = vec6 = vec3.lengthSquared() > 1.0E-8 ? vec3.normalize() : Vec3d.ZERO;
            if (iIIIllIilI2 != null && d2 <= d) {
                Vec3d vec9 = iIIIllIilI2.getPos();
                arrayList.add(vec9);
                Direction process17 = vec6.lengthSquared() > 1.0E-8 ? Direction.getFacing((double)vec6.x, (double)vec6.y, (double)vec6.z).getOpposite() : Direction.UP;
                return new TrajectoryPrediction(arrayList, new ProjectileImpact(vec9, vec6, process17, iIIIllIilI2.getEntity()), i);
            }
            if (hit2.getType() == HitResult.Type.BLOCK) {
                Vec3d vec10 = hit2.getPos();
                arrayList.add(vec10);
                return new TrajectoryPrediction(arrayList, new ProjectileImpact(vec10, vec6, hit2.getSide(), null), i);
            }
            vec2 = vec7;
            arrayList.add(vec2);
            if (!applyBeforeMove) {
                vec3 = motionStep.apply(vec2, vec3);
            }
            if (vec2.y < (double)this.mc.world.getBottomY()) break;
        }
        return new TrajectoryPrediction(arrayList, null, Math.max(0, arrayList.size() - 1));
    }

    private TrajectoryPrediction blendLaunchPaths(TrajectoryPrediction trajectoryPrediction, TrajectoryPrediction trajectoryPrediction2) {
        if (trajectoryPrediction2 == null) {
            return trajectoryPrediction;
        }
        List<Vec3d> list = trajectoryPrediction.points();
        List<Vec3d> list2 = trajectoryPrediction2.points();
        int n = Math.min(12, Math.min(list.size(), list2.size()));
        ArrayList<Vec3d> arrayList = new ArrayList<Vec3d>(list.size());
        for (int i = 0; i < list.size(); ++i) {
            if (i < n) {
                double d = TrajectoryPredictor.smoothstep((double)i / 12.0);
                Vec3d vec4 = list2.get(i);
                Vec3d vec5 = list.get(i);
                arrayList.add(new Vec3d(vec4.x + (vec5.x - vec4.x) * d, vec4.y + (vec5.y - vec4.y) * d, vec4.z + (vec5.z - vec4.z) * d));
                continue;
            }
            arrayList.add(list.get(i));
        }
        return new TrajectoryPrediction(arrayList, trajectoryPrediction.impact(), trajectoryPrediction.flightTicks());
    }

    private double getItemDrag(Item iiIilIIilI2, boolean bl) {
        if (iiIilIIilI2 instanceof TridentItem) {
            return 0.99;
        }
        if (iiIilIIilI2 instanceof EnderPearlItem || iiIilIIilI2 instanceof SnowballItem || iiIilIIilI2 instanceof EggItem) {
            return bl ? 0.8 : 0.99;
        }
        if (iiIilIIilI2 instanceof BowItem || iiIilIIilI2 instanceof CrossbowItem) {
            return bl ? 0.6 : 0.99;
        }
        if (this.isPotionItem(iiIilIIilI2) || iiIilIIilI2 instanceof ExperienceBottleItem) {
            return bl ? 0.8 : 0.99;
        }
        return 0.98;
    }

    private EntityHitResult raycastEntities(Vec3d vec4, Vec3d vec5, Entity entity2, ProjectileType iiIlilIilI2) {
        if (this.mc.world == null) {
            return null;
        }
        Box box = new Box(vec4, vec5).expand(0.3);
        Entity iIiiiilIiI2 = null;
        Vec3d vec2 = null;
        double d = Double.MAX_VALUE;
        for (Entity candidate : this.mc.world.getOtherEntities(entity2, box)) {
            double d2;
            Optional intersection;
            ProjectileEntity projectile;
            if (!candidate.isAlive() || entity2 instanceof ProjectileEntity && (projectile = (ProjectileEntity)entity2).getOwner() == candidate || iiIlilIilI2 == ProjectileType.POTION && ProjectileType.fromEntity(candidate) == ProjectileType.POTION || (intersection = candidate.getBoundingBox().expand(0.3).raycast(vec4, vec5)).isEmpty() || !((d2 = vec4.squaredDistanceTo((Vec3d)intersection.get())) < d)) continue;
            d = d2;
            iIiiiilIiI2 = candidate;
            vec2 = (Vec3d)intersection.get();
        }
        if (iIiiiilIiI2 == null || vec2 == null) {
            return null;
        }
        return new EntityHitResult(iIiiiilIiI2, vec2);
    }

    public Vec3d interpolateEntityPosition(Entity entity2, float f) {
        return new Vec3d(MathHelper.lerp((double)f, (double)entity2.lastX, (double)entity2.getX()), MathHelper.lerp((double)f, (double)entity2.lastY, (double)entity2.getY()), MathHelper.lerp((double)f, (double)entity2.lastZ, (double)entity2.getZ()));
    }

    private boolean isPotionItem(Item iiIilIIilI2) {
        return iiIilIIilI2 instanceof ThrowablePotionItem || iiIilIIilI2 instanceof PotionItem;
    }

    private float getItemPitchOffset(Item iiIilIIilI2) {
        if (iiIilIIilI2 instanceof ExperienceBottleItem || this.isPotionItem(iiIilIIilI2)) {
            return -20.0f;
        }
        return 0.0f;
    }

    private float getLaunchSpeed(LivingEntity screenHandler, ItemStack stack) {
        Item iiIilIIilI2 = stack.getItem();
        if (iiIilIIilI2 instanceof ExperienceBottleItem) {
            return 0.7f;
        }
        if (this.isPotionItem(iiIilIIilI2)) {
            return 0.5f;
        }
        if (iiIilIIilI2 instanceof EnderPearlItem || iiIilIIilI2 instanceof SnowballItem || iiIilIIilI2 instanceof EggItem) {
            return 1.5f;
        }
        if (iiIilIIilI2 instanceof BowItem) {
            int n = screenHandler.isUsingItem() ? stack.getMaxUseTime(screenHandler) - screenHandler.getItemUseTimeLeft() : 20;
            return BowItem.getPullProgress((int)n) * 3.0f;
        }
        if (iiIilIIilI2 instanceof CrossbowItem) {
            return 3.15f;
        }
        if (iiIilIIilI2 instanceof TridentItem) {
            return 2.5f + Math.max(0.0f, EnchantmentHelper.getTridentSpinAttackStrength((ItemStack)stack, (LivingEntity)screenHandler)) * 0.5f;
        }
        return 1.5f;
    }

    private double getEntityDrag(Entity entity2, boolean bl) {
        ProjectileType iiIlilIilI2 = ProjectileType.fromEntity(entity2);
        if (iiIlilIilI2 == ProjectileType.TRIDENT) {
            return 0.99;
        }
        if (iiIlilIilI2 == ProjectileType.PEARL) {
            return bl ? 0.8 : 0.99;
        }
        if (iiIlilIilI2 == ProjectileType.ARROW) {
            return bl ? 0.6 : 0.99;
        }
        if (iiIlilIilI2 == ProjectileType.POTION) {
            return bl ? 0.8 : 0.99;
        }
        if (iiIlilIilI2 == ProjectileType.ITEM) {
            return 0.98;
        }
        return 0.99;
    }

    private double getItemGravity(Item iiIilIIilI2) {
        if (iiIilIIilI2 instanceof TridentItem) {
            return 0.05;
        }
        if (iiIilIIilI2 instanceof EnderPearlItem || iiIilIIilI2 instanceof SnowballItem || iiIilIIilI2 instanceof EggItem) {
            return 0.03;
        }
        if (iiIilIIilI2 instanceof BowItem || iiIilIIilI2 instanceof CrossbowItem) {
            return 0.05;
        }
        if (iiIilIIilI2 instanceof ExperienceBottleItem) {
            return 0.07;
        }
        if (this.isPotionItem(iiIilIIilI2)) {
            return 0.05;
        }
        return 0.04;
    }

    private Vec3d getUnadjustedLaunchDirection(LivingEntity screenHandler, Item iiIilIIilI2, float f, float f2) {
        Vec3d vec4 = this.directionFromAngles(screenHandler.getPitch(f), screenHandler.getYaw(f), 0.0f);
        if (!(iiIilIIilI2 instanceof CrossbowItem) || f2 == 0.0f) {
            return vec4;
        }
        return this.rotateAroundAxis(vec4, screenHandler.getRotationVec(f), f2).normalize();
    }

    private boolean hasMultishot(ItemStack stack) {
        if (!(stack.getItem() instanceof CrossbowItem)) {
            return false;
        }
        ItemEnchantmentsComponent itemEnchantmentsComponent = EnchantmentHelper.getEnchantments((ItemStack)stack);
        for (RegistryEntry enchantment : itemEnchantmentsComponent.getEnchantments()) {
            if (!enchantment.matchesKey(Enchantments.MULTISHOT) || itemEnchantmentsComponent.getLevel(enchantment) <= 0) continue;
            return true;
        }
        return false;
    }

    private Vec3d getInheritedShooterMotion(Item iiIilIIilI2) {
        if (ProjectileType.fromItem(iiIilIIilI2) == ProjectileType.CROSSBOW) {
            return Vec3d.ZERO;
        }
        Vec3d vec4 = this.smoothedShooterMotion;
        double d = vec4.lengthSquared();
        if (d > 0.36) {
            vec4 = vec4.multiply(0.6 / Math.sqrt(d));
        }
        return vec4;
    }

    private boolean isInFluid(Vec3d vec4) {
        if (this.mc.world == null) {
            return false;
        }
        return !this.mc.world.getFluidState(BlockPos.ofFloored((Position)vec4)).isEmpty();
    }

    private void updateShooterVelocity(LivingEntity screenHandler) {
        double d = screenHandler.getX() - screenHandler.lastX;
        double d2 = screenHandler.isOnGround() ? 0.0 : Math.min(0.0, screenHandler.getY() - screenHandler.lastY);
        double d3 = screenHandler.getZ() - screenHandler.lastZ;
        this.smoothedShooterMotion = new Vec3d(this.smoothedShooterMotion.x + (d - this.smoothedShooterMotion.x) * 0.3, this.smoothedShooterMotion.y + (d2 - this.smoothedShooterMotion.y) * 0.3, this.smoothedShooterMotion.z + (d3 - this.smoothedShooterMotion.z) * 0.3);
    }

    private Vec3d rotateAroundAxis(Vec3d vec4, Vec3d vec5, float f) {
        if (vec5.lengthSquared() < 1.0E-8) {
            return vec4;
        }
        Vec3d vec2 = vec5.normalize();
        double d = Math.toRadians(f);
        double d2 = Math.cos(d);
        double d3 = Math.sin(d);
        return vec4.multiply(d2).add(vec2.crossProduct(vec4).multiply(d3)).add(vec2.multiply(vec2.dotProduct(vec4) * (1.0 - d2)));
    }
}

