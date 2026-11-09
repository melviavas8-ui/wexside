package ru.wexside.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.TridentItem;

public enum ProjectileType {
    TRIDENT,
    PEARL,
    ARROW,
    CROSSBOW,
    POTION,
    ITEM;


    public static ProjectileType fromItem(Item item) {
        if (item instanceof TridentItem) {
            return TRIDENT;
        }
        if (item instanceof EnderPearlItem || item instanceof SnowballItem || item instanceof EggItem) {
            return PEARL;
        }
        if (item instanceof BowItem) {
            return ARROW;
        }
        if (item instanceof CrossbowItem) {
            return CROSSBOW;
        }
        if (item instanceof PotionItem || item instanceof ExperienceBottleItem) {
            return POTION;
        }
        return null;
    }

    public static ProjectileType fromEntity(Entity entity) {
        if (entity instanceof TridentEntity) {
            return TRIDENT;
        }
        if (entity instanceof EnderPearlEntity || entity instanceof SnowballEntity || entity instanceof EggEntity) {
            return PEARL;
        }
        if (entity instanceof PotionEntity || entity instanceof ExperienceBottleEntity) {
            return POTION;
        }
        if (entity instanceof PersistentProjectileEntity) {
            return ARROW;
        }
        if (entity instanceof ItemEntity) {
            return ITEM;
        }
        return null;
    }
}

