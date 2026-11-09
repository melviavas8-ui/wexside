package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ru.wexside.misc.TargetFilter;

public final class CrosshairTarget {
    public LivingEntity findMatchingTarget(TargetFilter filter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return null;
        }
        HitResult crosshair = mc.crosshairTarget;
        if (!(crosshair instanceof EntityHitResult)) {
            return null;
        }
        EntityHitResult hit = (EntityHitResult)crosshair;
        Entity entity = hit.getEntity();
        if (!(entity instanceof LivingEntity)) {
            return null;
        }
        LivingEntity living = (LivingEntity)entity;
        if (!filter.matches(living)) {
            return null;
        }
        return living;
    }
}

