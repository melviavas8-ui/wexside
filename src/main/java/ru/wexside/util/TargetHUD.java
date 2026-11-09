package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.WexSideClient;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.PreparedLayer;
import ru.wexside.misc.TextureHandle;
import ru.wexside.misc.ThemeColors;
import ru.wexside.module.Module;
import ru.wexside.module.combat.AttackAuraModule;
import ru.wexside.render.HudIconRenderer;
import ru.wexside.render.IconAtlasEntry;
import ru.wexside.render.ItemIconCache;
import ru.wexside.render.PlayerHeadRenderCache;
import ru.wexside.render.RenderFrameClock;
import ru.wexside.util.AbstractHudElement;
import ru.wexside.util.ColorUtils;
import ru.wexside.util.GuiDrawApi;

public final class TargetHUD
extends AbstractHudElement {
    private final int slot;
    private float activeItemProgress;
    private final BooleanSupplier booleanSupplier;
    private final Map<UUID, IconAtlasEntry> map2;
    private AttackAuraModule attackAuraModule;
    private LivingEntity entity2;
    private long longType;
    private int slot2 = -1;
    static final int slot3 = 64;
    private boolean showActiveItem;
    private LivingEntity entity3;
    private float hurtProgress;
    private final int slot4 = ColorUtils.rgba(0, 0, 0, 40);
    private LivingEntity entity4;
    private int targetCacheFrame = -1;
    private float healthProgress;
    private final ItemIconCache itemIconCache = new ItemIconCache();
    private float previousHurtProgress;
    private int lastItemUseTicks;
    private final int slot7;
    private final BooleanSupplier booleanSupplier2;

    public TargetHUD(BooleanSupplier booleanSupplier, BooleanSupplier booleanSupplier2, BooleanSupplier booleanSupplier3) {
        super("Target HUD", booleanSupplier);
        this.slot7 = 4;
        this.slot = 2;
        this.map2 = new PlayerHeadRenderCache(64);
        this.booleanSupplier = booleanSupplier2;
        this.booleanSupplier2 = booleanSupplier3;
    }

    public void clearCaches() {
        this.itemIconCache.disposeCache();
        for (IconAtlasEntry renderPipeline10 : this.map2.values()) {
            renderPipeline10.close();
        }
        this.map2.clear();
    }

    @Override
    protected float getWidth() {
        return 118.5f;
    }

    @Override
    protected void updateLayout() {
        LivingEntity entity5 = this.getCurrentTarget();
        boolean bl = false;
        if (entity5 != null) {
            this.entity2 = entity5;
            this.previousHurtProgress = this.hurtProgress;
            this.hurtProgress = this.hurtPulse(entity5.hurtTime);
            float f = TargetHUD.healthFraction(entity5);
            this.healthProgress = FrameInterpolator.lerpTowards(this.healthProgress, f, 20.0f);
            bl = entity5.isUsingItem() && this.hasRecentItemUseChange(entity5);
        }
        this.showActiveItem = bl;
        this.activeItemProgress = FrameInterpolator.lerpTowards(this.activeItemProgress, bl ? 1.0f : 0.0f, 15.0f);
        this.prepareIconAtlases();
    }

    @Override
    protected void renderContent(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5) {
        if (this.entity2 == null) {
            return;
        }
        this.renderPanelSurface(drawApi, matrix4f, f, f2, f3, f4, 8.0f, f5);
        drawApi.beginStencil(1);
        drawApi.drawRoundedRectangle(matrix4f, f, f2, f3, f4, 8.0f * f5, -1);
        drawApi.applyStencilMask(1);
        this.renderAnimatedHead(drawApi, matrix4f, f, f2, f5);
        this.renderActiveItem(drawApi, matrix4f, f, f2, f5);
        this.renderTargetType(drawApi, matrix4f, f, f2, f5);
        this.renderTargetName(drawApi, matrix4f, f, f2, f5);
        this.renderEquipmentRow(drawApi, matrix4f, f, f2, f5);
        this.renderHealthRing(drawApi, matrix4f, f, f2, f5);
        drawApi.endStencil();
    }

    private void prepareIconAtlases() {
        AbstractClientPlayerEntity player;
        IconAtlasEntry headTexture;
        if (this.entity2 == null) {
            return;
        }
        float f = MinecraftClient.getInstance().getWindow().getScaleFactor();
        ArrayList<BakedIconEntry> arrayList = new ArrayList<BakedIconEntry>();
        LivingEntity livingEntity = this.entity2;
        if (livingEntity instanceof AbstractClientPlayerEntity && (headTexture = this.map2.computeIfAbsent((player = (AbstractClientPlayerEntity)livingEntity).getUuid(), ignored -> new IconAtlasEntry(true))).needsBake(f)) {
            SkinTextures skin = player.getSkin();
            arrayList.add(new BakedIconEntry(headTexture, (context, x, y, size) -> HudIconRenderer.drawPlayerHead(context, skin, x, y, size)));
        }
        this.itemIconCache.advanceFrame();
        for (ItemStack equipment : this.getList()) {
            if (equipment.isEmpty()) continue;
            this.itemIconCache.getOrCreateIcon(equipment);
        }
        ItemStack activeItem = this.entity2.getActiveItem();
        if (this.showActiveItem && !activeItem.isEmpty()) {
            this.itemIconCache.getOrCreateIcon(activeItem);
        }
        this.itemIconCache.appendPendingBakes(f, arrayList);
        if (!arrayList.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(arrayList);
        }
        this.itemIconCache.trimCache();
    }

    @Override
    protected float getHeight() {
        return 36.0f;
    }

    @Override
    protected boolean isContentVisible() {
        if (this.getCurrentTarget() == null) {
            this.slot2 = -1;
            return false;
        }
        int n = this.getIntType();
        if (this.slot2 < 0) {
            this.slot2 = n;
        }
        return n - this.slot2 >= 2;
    }

    private LivingEntity getLivingEntity() {
        if (this.attackAuraModule == null) {
            for (Module module : WexSideClient.getInstance().getModuleManager().getModules()) {
                AttackAuraModule attackAuraModule;
                if (!(module instanceof AttackAuraModule)) continue;
                this.attackAuraModule = attackAuraModule = (AttackAuraModule)module;
                break;
            }
        }
        return this.attackAuraModule != null ? this.attackAuraModule.getLivingEntity() : null;
    }

    private IconAtlasEntry headAtlasEntry(LivingEntity entity5) {
        if (!(entity5 instanceof PlayerEntity)) {
            return null;
        }
        PlayerEntity player2 = (PlayerEntity)entity5;
        return this.map2.get(player2.getUuid());
    }

    private LivingEntity raycastLivingTarget(float f) {
        Entity iIiiiilIiI2;
        Box box;
        Vec3d vec;
        Vec3d vec2;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player3 = mc.player;
        if (player3 == null || mc.world == null) {
            return null;
        }
        float f2 = mc.getRenderTickCounter().getTickProgress(false);
        Vec3d vec3 = player3.getCameraPosVec(f2);
        EntityHitResult iIIIllIilI2 = ProjectileUtil.raycast((Entity)player3, (Vec3d)vec3, (Vec3d)(vec2 = vec3.add((vec = player3.getRotationVec(f2)).multiply((double)f))), (Box)(box = player3.getBoundingBox().stretch(vec.multiply((double)f)).expand(1.0)), entity -> entity instanceof LivingEntity && entity != player3 && entity.isAlive(), (double)(f * f));
        if (iIIIllIilI2 != null && (iIiiiilIiI2 = iIIIllIilI2.getEntity()) instanceof LivingEntity) {
            LivingEntity entity5 = (LivingEntity)iIiiiilIiI2;
            return entity5;
        }
        return null;
    }

    private void renderTargetType(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3) {
        String string2;
        int n = ThemeColors.accent();
        String string = !(this.entity2 instanceof PlayerEntity) ? this.entityTypeLabel(this.entity2) : (TargetHUD.containsLetterOrDigit(string2 = this.teamPrefix(this.entity2)) ? string2 : "Игрок");
        FontRegistry.font6.drawText(matrix4f, drawApi, string, f + 35.0f * f3, f2 + 6.25f * f3, 5.0f * f3, n);
    }

    private float hurtPulse(int n) {
        int n2 = 10 - n;
        if (n2 == 10) {
            return 0.0f;
        }
        float f = (float)n2 / 10.0f * 2.0f;
        if (f > 1.0f) {
            f = 2.0f - f;
        }
        return f;
    }

    private void renderTargetHead(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n) {
        drawApi.drawRoundedShadow(matrix4f, f, f2, f3, f3, 6.0f * f4, 5.0f * f4, this.slot4);
        IconAtlasEntry renderPipeline10 = this.headAtlasEntry(this.entity2);
        if (renderPipeline10 != null && renderPipeline10.isAllocated()) {
            int n2 = drawApi.bindTexture(renderPipeline10.getTextureId(), renderPipeline10.getIconSize(), renderPipeline10.getIconSize());
            drawApi.drawRoundedTexture(matrix4f, f, f2, f3, f3, 0.0f, 1.0f, 1.0f, 0.0f, 6.0f * f4, n2, n);
        } else {
            drawApi.drawRoundedRectangle(matrix4f, f, f2, f3, f3, 6.0f * f4, n);
        }
    }

    private String teamPrefix(LivingEntity entity5) {
        Team team2 = entity5.getScoreboardTeam();
        if (team2 == null) {
            return "";
        }
        return team2.getPrefix().getString().replaceAll("\u00a7.?", "").trim();
    }

    private void renderActiveItem(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3) {
        if (this.activeItemProgress < 0.001f || this.entity2 == null || !this.showActiveItem) {
            return;
        }
        ItemStack stack = this.entity2.getActiveItem();
        if (stack.isEmpty()) {
            return;
        }
        int n = (int)Math.clamp(this.activeItemProgress * 255.0f, 0.0f, 255.0f);
        int n2 = ColorUtils.rgba(255, 255, 255, n);
        float f4 = 12.0f * f3;
        float f5 = f + 12.0f * f3;
        float f6 = f2 + 8.0f * f3;
        this.itemIconCache.drawTintedBakedIcon(drawApi, matrix4f, this.itemIconCache.getOrCreateIcon(stack), f5, f6, f4, n2);
        String string = String.format(Locale.ROOT, "%.2fс", Float.valueOf(this.activeItemSeconds(stack)));
        float f7 = FontRegistry.font6.textWidth(string, 5.0f);
        float f8 = f + 18.0f * f3 - f7 * f3 / 2.0f;
        float f9 = f2 + 21.0f * f3;
        FontRegistry.font6.drawText(matrix4f, drawApi, string, f8, f9, 5.0f * f3, n2);
    }

    private List<ItemStack> getList() {
        ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>(6);
        arrayList.add(this.entity2.getEquippedStack(EquipmentSlot.HEAD));
        arrayList.add(this.entity2.getEquippedStack(EquipmentSlot.CHEST));
        arrayList.add(this.entity2.getEquippedStack(EquipmentSlot.LEGS));
        arrayList.add(this.entity2.getEquippedStack(EquipmentSlot.FEET));
        arrayList.add(this.entity2.getMainHandStack());
        arrayList.add(this.entity2.getOffHandStack());
        return arrayList;
    }

    private void renderAnimatedHead(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3) {
        float f4 = f + 5.0f * f3;
        float f5 = f2 + 5.0f * f3;
        float f6 = 26.0f * f3;
        float f7 = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
        float f8 = this.previousHurtProgress + (this.hurtProgress - this.previousHurtProgress) * f7;
        float f9 = this.booleanSupplier2.getAsBoolean() ? f8 * 0.5f : 0.0f;
        int n = Math.round((1.0f - f9) * 255.0f);
        int n2 = ColorUtils.rgba(255, n, n, 255);
        if (this.activeItemProgress < 0.001f) {
            this.renderTargetHead(drawApi, matrix4f, f4, f5, f6, f3, n2);
        } else {
            this.renderBlurredHead(drawApi, matrix4f, f4, f5, f6, f3, n2);
        }
    }

    private String entityTypeLabel(LivingEntity entity5) {
        if (entity5 instanceof MerchantEntity) {
            return "Житель";
        }
        if (entity5 instanceof AnimalEntity) {
            return "Животное";
        }
        if (entity5 instanceof MobEntity) {
            return "Моб";
        }
        return "Существо";
    }

    private void renderEquipmentRow(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3) {
        List<ItemStack> list = this.getList();
        float f4 = f + 35.0f * f3;
        float f5 = 6.0f * f3;
        float f6 = f2 + 23.25f * f3;
        for (int i = 0; i < list.size(); ++i) {
            ItemStack stack = list.get(i);
            if (!stack.isEmpty()) {
                this.itemIconCache.drawTintedBakedIcon(drawApi, matrix4f, this.itemIconCache.getOrCreateIcon(stack), f4, f6, f5, -1);
            } else if (i < 4) {
                drawApi.drawRoundedRectangle(matrix4f, f4, f6, f5, f5, 4.0f * f3, ThemeColors.visualizerSlot());
            }
            f4 += 8.5f * f3;
        }
    }

    private static boolean containsLetterOrDigit(String string) {
        for (int i = 0; i < string.length(); ++i) {
            if (!Character.isLetterOrDigit(string.charAt(i))) continue;
            return true;
        }
        return false;
    }

    private void renderBlurredHead(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n) {
        float f5 = 16.0f * f4;
        drawApi.begin();
        PreparedLayer preparedLayer = drawApi.prepareLayer(matrix4f, f, f2, f3, f3, f5);
        drawApi.beginLayerFrame(preparedLayer.getTexture());
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)preparedLayer.getContentMatrix()).translate(preparedLayer.contentX(), preparedLayer.contentY(), 0.0f);
        this.renderTargetHead(drawApi, matrix4f2, 0.0f, 0.0f, f3, f4, n);
        drawApi.endLayerFrame();
        TextureHandle texture = drawApi.blurTexture(preparedLayer.getTexture(), this.activeItemProgress * 4.0f);
        drawApi.drawLayerTexture(matrix4f, texture, preparedLayer.drawX(), preparedLayer.drawY(), preparedLayer.drawWidth(), preparedLayer.drawHeight(), 0.0f, 1.0f, preparedLayer.maxU(), 1.0f - preparedLayer.maxV(), -1);
        drawApi.end();
    }

    private String ellipsizeName(String string, float f, float f2) {
        if (FontRegistry.font2.textWidth(string, f) <= f2) {
            return string;
        }
        String string2 = "\u2026";
        float f3 = FontRegistry.font2.textWidth(string2, f);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            String string3 = stringBuilder.toString();
            String string4 = string3 + c;
            if (FontRegistry.font2.textWidth(string4, f) + f3 > f2) break;
            stringBuilder.append(string.charAt(i));
        }
        String string5 = string2;
        String string6 = String.valueOf(stringBuilder);
        return string6 + string5;
    }

    private LivingEntity resolveTarget() {
        LivingEntity entity5 = this.getLivingEntity();
        if (entity5 != null) {
            return entity5;
        }
        LivingEntity raycastTarget = this.booleanSupplier.getAsBoolean() ? this.raycastLivingTarget(50.0f) : null;
        LivingEntity livingEntity = raycastTarget;
        if (raycastTarget != null) {
            return raycastTarget;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof ChatScreen && mc.player != null) {
            return mc.player;
        }
        return null;
    }

    private float activeItemSeconds(ItemStack stack) {
        int n = this.entity2.getItemUseTime();
        int n2 = switch (stack.getUseAction()) {
            case UseAction.BOW, UseAction.SPEAR, UseAction.CROSSBOW -> this.entity2.getItemUseTimeLeft();
            case UseAction.EAT, UseAction.DRINK -> 20 - n;
            case UseAction.BLOCK -> 25 - n;
            case UseAction.TOOT_HORN, UseAction.BRUSH -> 10 - n;
            default -> n;
        };
        return Math.max(0.0f, (float)n2 / 20.0f);
    }

    private void renderHealthRing(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3) {
        int n;
        float f4 = f + 0.5f + 100.5f * f3;
        float f5 = f2 + 0.5f + 18.0f * f3;
        float f6 = 10.0f * f3;
        float f7 = 2.5f * f3;
        drawApi.drawCircle(matrix4f, f4, f5, 0.0f, 360.0f, f7, f6, ThemeColors.separatorHover());
        float f8 = this.healthProgress * 360.0f;
        if (f8 > 0.0f) {
            drawApi.drawCircle(matrix4f, f4, f5, 0.0f, f8, f7, f6, ThemeColors.accent());
        }
        boolean bl = (n = TargetHUD.healthValue(this.entity2)) == 0 || n == 1000;
        String string = bl ? "?" : String.valueOf(n);
        float f9 = FontRegistry.font6.textWidth(string, 6.5f);
        float f10 = FontRegistry.font6.textHeight(string, 6.5f);
        FontRegistry.font6.drawText(matrix4f, drawApi, string, f4 - f9 * f3 / 2.0f, f5 - f10 * f3 / 2.0f, 6.5f * f3, ThemeColors.hudTextPrimary());
    }

    private LivingEntity getCurrentTarget() {
        int n = RenderFrameClock.currentFrame();
        if (n != this.targetCacheFrame) {
            this.targetCacheFrame = n;
            this.entity3 = this.resolveTarget();
        }
        return this.entity3;
    }

    private boolean hasRecentItemUseChange(LivingEntity entity5) {
        int n = entity5.getItemUseTimeLeft();
        long l = System.currentTimeMillis();
        if (entity5 != this.entity4 || n != this.lastItemUseTicks) {
            this.entity4 = entity5;
            this.lastItemUseTicks = n;
            this.longType = l;
            return true;
        }
        return l - this.longType < 750L;
    }

    private void renderTargetName(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3) {
        String string = this.entity2.getName().getString();
        float f4 = 55.5f;
        String string2 = this.ellipsizeName(string, 7.5f, f4);
        FontRegistry.font2.drawText(matrix4f, drawApi, string2, f + 35.0f * f3, f2 + 12.25f * f3, 7.5f * f3, ThemeColors.hudTextPrimary());
    }

    private int getIntType() {
        ClientPlayerEntity player3 = MinecraftClient.getInstance().player;
        return player3 != null ? player3.age : 0;
    }

    private static int healthValue(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            ReadableScoreboardScore score;
            Scoreboard scoreboard = client.world.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            ReadableScoreboardScore class_90132 = score = objective == null ? null : scoreboard.getScore((ScoreHolder)entity, objective);
            if (score != null) {
                return score.getScore();
            }
        }
        return Math.round(entity.getHealth());
    }

    private static float healthFraction(LivingEntity entity) {
        float maximum = entity.getMaxHealth();
        if (maximum <= 0.0f) {
            return 0.0f;
        }
        return Math.clamp((float)TargetHUD.healthValue(entity) / maximum, 0.0f, 1.0f);
    }
}

