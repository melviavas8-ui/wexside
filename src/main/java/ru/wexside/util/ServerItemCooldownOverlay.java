package ru.wexside.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import ru.wexside.WexSideClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.BakedIconEntry;
import ru.wexside.misc.BoxEspSettings;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.ServerKind;
import ru.wexside.model.esp.EspTargetClassifier;
import ru.wexside.model.esp.EspTargetType;
import ru.wexside.module.Module;
import ru.wexside.module.player.ServerHelperModule;
import ru.wexside.render.BakedItemIcon;
import ru.wexside.render.ItemIconCache;
import ru.wexside.render.RenderProjection;
import ru.wexside.server.ServerHelperAction;
import ru.wexside.server.ServerHelperActions;
import ru.wexside.server.ServerItemMatcher;
import ru.wexside.util.EspBoxBorderRenderer;
import ru.wexside.util.EspFeatureRegistry;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.MsdfFontRenderer;

public final class ServerItemCooldownOverlay {
    private static final long longType = 200L;
    private long getTextureId;
    private static final float value2 = 10.0f;
    private final Map<UUID, EntityCooldownState> map2;
    private static final int slot = -1342177280;
    private static final int slot2 = 40;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<ServerHelperAction, ItemStack> map3;
    private static final float value3 = 12.0f;
    private static final float value4 = 12.0f;
    private static final float value5 = 6.0f;
    private static final int slot3 = Integer.MIN_VALUE;
    private final EspFeatureRegistry espFeatures;
    private static final float value6 = 50.0f;
    private ServerHelperModule serverHelperModule;
    private static final MsdfFontRenderer font5 = FontRegistry.font2;
    private static final long longType3 = 250L;
    private int process2;
    private final ItemIconCache itemIconCache;

    public ServerItemCooldownOverlay(EventBus eventBus, EspFeatureRegistry espFeatures) {
        this.espFeatures = espFeatures;
        this.map2 = new HashMap<UUID, EntityCooldownState>();
        this.map3 = new HashMap<ServerHelperAction, ItemStack>();
        this.itemIconCache = new ItemIconCache();
        eventBus.subscribe(HudRenderEvent.class, this::setHudRenderEvent, -100);
        eventBus.subscribe(WorldSessionEvent.class, this::slot3);
    }

    private void drawVerticalGradientRect(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n, int n2) {
        EspBoxBorderRenderer.drawVerticalGradientRect(drawApi, matrix4f, f, f2, f3, f4, n, n2);
    }

    private void slot3(WorldSessionEvent gameEvent7) {
        this.itemIconCache.disposeCache();
        this.map3.clear();
        this.map2.clear();
    }

    private float entryWidth(EntityCooldownState entityCooldownState) {
        float f = 0.0f;
        for (CooldownDisplayEntry enabled2 : entityCooldownState.activeEntries) {
            f = Math.max(f, font5.textWidth(enabled2.label, 6.0f));
        }
        return Math.max(15.0f, f + 4.0f);
    }

    private void refreshInventoryActions(ClientPlayerEntity player2, ServerKind serverKind2) {
        this.map3.clear();
        PlayerInventory inv = player2.getInventory();
        if (inv == null) {
            return;
        }
        for (ServerHelperAction action : ServerHelperActions.COOLDOWN_TRACKED) {
            ItemStack stack = ServerItemMatcher.findStack(inv, action, serverKind2);
            if (stack.isEmpty()) continue;
            this.map3.put(action, stack);
        }
    }

    private void drawFullBoxBorder(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n, int n2, int n3, int n4) {
        EspBoxBorderRenderer.drawFullBoxBorder(drawApi, matrix4f, f, f2, f3, f4, n, n2, n3, n4);
    }

    private void updateDistanceEntries(EntityCooldownState entityCooldownState, float f) {
        for (ServerHelperAction action : ServerHelperActions.COOLDOWN_TRACKED) {
            boolean bl;
            String string2 = action.id();
            float f2 = action.activationDistance();
            ItemStack stack = this.map3.get(action);
            boolean bl2 = bl = f2 > 0.0f && !action.donationItem() && f <= f2 && stack != null && !stack.isEmpty();
            if (bl) {
                float f3 = MathHelper.clamp((float)((f2 - f) / f2), (float)0.0f, (float)1.0f);
                float f4 = 1.0f - (1.0f - f3) * (1.0f - f3);
                int n = Math.round(10.0f + f4 * 90.0f);
                int n2 = (int)(255.0f * (1.0f - f4));
                int n3 = (int)(255.0f * f4);
                CooldownDisplayEntry enabled2 = entityCooldownState.entriesById.computeIfAbsent(string2, string -> new CooldownDisplayEntry());
                enabled2.stack = stack;
                int n4 = n;
                enabled2.label = n4 + "%";
                enabled2.color = n2 << 16 | n3 << 8;
                enabled2.setVisible(true);
                continue;
            }
            CooldownDisplayEntry enabled2 = entityCooldownState.entriesById.get(string2);
            if (enabled2 == null) continue;
            enabled2.setVisible(false);
        }
    }

    private void drawCornerBoxBorder(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n, int n2, int n3, int n4) {
        EspBoxBorderRenderer.drawCornerBoxBorder(drawApi, matrix4f, f, f2, f3, f4, n, n2, n3, n4);
    }

    private static int replaceOpacity(int n, float f) {
        int n2 = (int)(MathHelper.clamp((float)f, (float)0.0f, (float)1.0f) * 255.0f);
        return n2 << 24 | n & 0xFFFFFF;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderEntityOverlays(List<Entity> list, ClientPlayerEntity player2, Matrix4f matrix4f, float f) {
        ServerKind serverKind2 = this.getServerKind();
        long l = System.currentTimeMillis();
        if (l - this.getTextureId >= 200L) {
            this.refreshInventoryActions(player2, serverKind2);
            this.getTextureId = l;
        }
        this.itemIconCache.advanceFrame();
        ++this.process2;
        for (Entity object22 : list) {
            Vector2f screenPosition;
            if (object22 == player2 || !(object22 instanceof LivingEntity) || !object22.isAlive() || (screenPosition = RenderProjection.projectEntityCenter(object22, matrix4f)) == null) continue;
            EntityCooldownState entityState = this.map2.computeIfAbsent(object22.getUuid(), uUID -> new EntityCooldownState());
            entityState.lastSeenFrame = this.process2;
            entityState.screenX = screenPosition.x;
            entityState.screenY = screenPosition.y;
            entityState.markVisible(true);
            this.updateDistanceEntries(entityState, player2.distanceTo(object22));
        }
        ArrayList<EntityCooldownState> arrayList = new ArrayList<EntityCooldownState>();
        Iterator<EntityCooldownState> iterator2 = this.map2.values().iterator();
        while (iterator2.hasNext()) {
            EntityCooldownState state = iterator2.next();
            if (this.process2 - state.lastSeenFrame > 40) {
                iterator2.remove();
                continue;
            }
            if (state.lastSeenFrame != this.process2) continue;
            state.update(this.itemIconCache);
            if (state.activeEntries.isEmpty()) continue;
            arrayList.add(state);
        }
        if (arrayList.isEmpty()) {
            this.itemIconCache.trimCache();
            return;
        }
        ArrayList<BakedIconEntry> arrayList2 = new ArrayList<BakedIconEntry>();
        this.itemIconCache.appendPendingBakes(f, arrayList2);
        if (!arrayList2.isEmpty()) {
            WexSideClient.getOffscreenRenderManager().bakeIcons(arrayList2);
        }
        GuiDrawApi renderer = WexSideClient.getHudRenderer();
        Matrix4f scaledMatrix = new Matrix4f().scale(f);
        renderer.begin();
        try {
            for (EntityCooldownState entityCooldownState : arrayList) {
                this.renderActionIcons(renderer, scaledMatrix, entityCooldownState);
            }
        }
        finally {
            renderer.end();
        }
        renderer.begin();
        try {
            for (EntityCooldownState entityCooldownState : arrayList) {
                this.renderActionLabels(renderer, scaledMatrix, entityCooldownState);
            }
        }
        finally {
            renderer.end();
        }
        this.itemIconCache.trimCache();
    }

    private void renderActionIcons(GuiDrawApi drawApi, Matrix4f matrix4f, EntityCooldownState entityCooldownState) {
        int n = entityCooldownState.activeEntries.size();
        float f = this.entryWidth(entityCooldownState);
        float f2 = entityCooldownState.screenX - (float)n * f / 2.0f;
        float f3 = entityCooldownState.screenY + 4.0f;
        float f4 = entityCooldownState.alpha();
        float f5 = 1.0f;
        for (int i = 0; i < n; ++i) {
            CooldownDisplayEntry enabled2 = entityCooldownState.activeEntries.get(i);
            float f6 = f4 * enabled2.alpha();
            if (f6 <= 0.0f) continue;
            float f7 = f2 + (float)i * f + f / 2.0f;
            float f8 = f7 - 6.0f;
            drawApi.drawRoundedRectangleBordered(matrix4f, f8, f3, 12.0f, 12.0f, 12.0f, 0.0f, ServerItemCooldownOverlay.multiplyOpacity(-1342177280, f6));
            this.itemIconCache.drawTintedBakedIcon(drawApi, matrix4f, enabled2.icon, f7 - 5.0f, f3 + f5, 10.0f, ServerItemCooldownOverlay.replaceOpacity(0xFFFFFF, f6));
        }
    }

    private float[] projectEntityBounds(Entity entity2, Matrix4f matrix4f) {
        float f = RenderProjection.tickProgress();
        Vec3d vec = entity2.getLerpedPos(f);
        Box box = entity2.getBoundingBox().offset(vec.x - entity2.getX(), vec.y - entity2.getY(), vec.z - entity2.getZ());
        float f2 = Float.MAX_VALUE;
        float f3 = Float.MAX_VALUE;
        float f4 = -3.4028235E38f;
        float f5 = -3.4028235E38f;
        int n = 0;
        for (int i = 0; i < 8; ++i) {
            double d;
            double d2;
            double d3 = (i & 1) == 0 ? box.minX : box.maxX;
            Vector2f vector2f = RenderProjection.project(d3, d2 = (i & 2) == 0 ? box.minY : box.maxY, d = (i & 4) == 0 ? box.minZ : box.maxZ, matrix4f);
            if (vector2f == null) continue;
            ++n;
            f2 = Math.min(f2, vector2f.x);
            f3 = Math.min(f3, vector2f.y);
            f4 = Math.max(f4, vector2f.x);
            f5 = Math.max(f5, vector2f.y);
        }
        if (n == 0) {
            return null;
        }
        return new float[]{f2, f3, f4, f5};
    }

    private void fillBounds(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n) {
        EspBoxBorderRenderer.fillBounds(drawApi, matrix4f, f, f2, f3, f4, n);
    }

    private ServerKind getServerKind() {
        if (this.serverHelperModule == null) {
            for (Module module : WexSideClient.getInstance().getModuleManager().getModules()) {
                ServerHelperModule serverHelperModule;
                if (!(module instanceof ServerHelperModule)) continue;
                this.serverHelperModule = serverHelperModule = (ServerHelperModule)module;
                break;
            }
        }
        return this.serverHelperModule != null ? this.serverHelperModule.getServerKind() : ServerKind.GENERAL;
    }

    private void renderActionLabels(GuiDrawApi drawApi, Matrix4f matrix4f, EntityCooldownState entityCooldownState) {
        int n = entityCooldownState.activeEntries.size();
        float f = this.entryWidth(entityCooldownState);
        float f2 = entityCooldownState.screenX - (float)n * f / 2.0f;
        float f3 = entityCooldownState.screenY + 4.0f;
        float f4 = f3 + 12.0f + 3.0f;
        float f5 = font5.textHeight("0%", 6.0f);
        float f6 = entityCooldownState.alpha();
        for (int i = 0; i < n; ++i) {
            CooldownDisplayEntry enabled2 = entityCooldownState.activeEntries.get(i);
            float f7 = f6 * enabled2.alpha();
            if (f7 <= 0.0f || enabled2.label == null || enabled2.label.isEmpty()) continue;
            float f8 = f2 + (float)i * f + f / 2.0f;
            float f9 = font5.textWidth(enabled2.label, 6.0f);
            float f10 = f8 - f9 / 2.0f;
            drawApi.drawRoundedRectangle(matrix4f, f10 - 1.5f, f4 - 1.0f, f9 + 3.0f, f5 + 2.0f, 4.0f, ServerItemCooldownOverlay.multiplyOpacity(-1342177280, f7));
            font5.drawText(matrix4f, drawApi, enabled2.label, f10, f4, 6.0f, ServerItemCooldownOverlay.replaceOpacity(enabled2.color, f7));
        }
    }

    private void drawHorizontalGradientRect(GuiDrawApi drawApi, Matrix4f matrix4f, float f, float f2, float f3, float f4, int n, int n2) {
        EspBoxBorderRenderer.drawHorizontalGradientRect(drawApi, matrix4f, f, f2, f3, f4, n, n2);
    }

    private static int multiplyOpacity(int n, float f) {
        int n2 = (int)((float)(n >>> 24 & 0xFF) * MathHelper.clamp((float)f, (float)0.0f, (float)1.0f));
        return n2 << 24 | n & 0xFFFFFF;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void setHudRenderEvent(HudRenderEvent gameEvent20) {
        BoxEspSettings rectangle;
        if (!this.espFeatures.hasEnabledBox2d()) {
            this.itemIconCache.disposeCache();
            this.map2.clear();
            return;
        }
        ClientPlayerEntity player2 = this.mc.player;
        ClientWorld world2 = this.mc.world;
        if (player2 == null || world2 == null) {
            return;
        }
        Frustum frustum2 = RenderProjection.frustum();
        Matrix4f matrix4f = RenderProjection.viewProjectionMatrix();
        boolean bl = this.mc.options != null && this.mc.options.getPerspective().isFirstPerson();
        ArrayList<Entity> arrayList2 = new ArrayList<Entity>();
        for (Entity entity2 : world2.getEntities()) {
            EspTargetType targetType = EspTargetClassifier.targetType(entity2, player2);
            BoxEspSettings rectangle2 = this.espFeatures.getBox2dSettings(targetType, EspTargetClassifier.relation(entity2));
            if (targetType == null || rectangle2 == null || !rectangle2.isEnabled() || !entity2.isAlive() || entity2 == player2 && bl || !RenderProjection.isVisible(entity2, frustum2)) continue;
            arrayList2.add(entity2);
        }
        if (arrayList2.isEmpty()) {
            return;
        }
        arrayList2.sort(Comparator.comparingDouble(entity -> player2.getEntityPos().squaredDistanceTo(entity.getEntityPos())));
        float f = this.mc.getWindow().getScaleFactor();
        Matrix4f guiMatrix = new Matrix4f().scale(f);
        GuiDrawApi renderer = WexSideClient.getHudRenderer();
        renderer.begin();
        try {
            for (Entity entity3 : arrayList2) {
                float f2;
                rectangle = this.espFeatures.getBox2dSettings(EspTargetClassifier.targetType(entity3, player2), EspTargetClassifier.relation(entity3));
                float[] bounds = this.projectEntityBounds(entity3, matrix4f);
                if (rectangle == null || bounds == null) continue;
                float f3 = bounds[0];
                float f4 = bounds[1];
                float right = bounds[2];
                float bottom = bounds[3];
                LivingEntity living = entity3 instanceof LivingEntity ? (LivingEntity)entity3 : null;
                boolean bl2 = rectangle.isBoxEnabled();
                boolean bl3 = rectangle.isCornerStyle();
                int n = rectangle.getBoxColor(0.0f);
                int n2 = rectangle.getBoxColor(0.25f);
                int n3 = rectangle.getBoxColor(0.5f);
                int n4 = rectangle.getBoxColor(0.75f);
                boolean bl4 = rectangle.isHealthBarEnabled();
                int n5 = rectangle.getHealthColor(0.0f);
                int n6 = rectangle.getHealthColor(0.5f);
                boolean bl5 = rectangle.isArmorBarEnabled();
                int n7 = rectangle.getArmorColor(0.25f);
                int n8 = rectangle.getArmorColor(0.75f);
                if (bl2) {
                    boolean bl6;
                    boolean bl7 = bl6 = bl3 && player2.distanceTo(entity3) < 50.0f;
                    if (bl6) {
                        this.drawCornerBoxBorder(renderer, guiMatrix, f3, f4, right, bottom, n, n2, n3, n4);
                    } else {
                        this.drawFullBoxBorder(renderer, guiMatrix, f3, f4, right, bottom, n, n2, n3, n4);
                    }
                }
                if (bl5 && living != null && (f2 = MathHelper.clamp((float)(living.getHealth() / living.getMaxHealth()), (float)0.0f, (float)1.0f)) != 0.0f) {
                    this.fillBounds(renderer, guiMatrix, f3, bottom + 0.5f, right, bottom + 2.5f, Integer.MIN_VALUE);
                    this.drawHorizontalGradientRect(renderer, guiMatrix, f3 + 0.5f, bottom + 1.0f, f3 + 0.5f + (right - 0.5f - (f3 + 0.5f)) * f2, bottom + 2.0f, n7, n8);
                }
                if (!bl4 || living == null || living.getAbsorptionAmount() <= 0.0f) continue;
                float f5 = MathHelper.clamp((float)(living.getAbsorptionAmount() / Math.max(1.0f, living.getMaxHealth())), (float)0.0f, (float)1.0f);
                this.fillBounds(renderer, guiMatrix, f3 - 2.5f, f4, f3 - 0.5f, bottom, Integer.MIN_VALUE);
                this.drawVerticalGradientRect(renderer, guiMatrix, f3 - 2.0f, f4 + 0.5f + (bottom - 1.0f - f4) * (1.0f - f5), f3 - 1.0f, bottom - 0.5f, n5, n6);
            }
        }
        finally {
            renderer.end();
        }
        ArrayList<Entity> cooldownEntities = new ArrayList<Entity>();
        for (Entity entity2 : arrayList2) {
            rectangle = this.espFeatures.getBox2dSettings(EspTargetClassifier.targetType(entity2, player2), EspTargetClassifier.relation(entity2));
            if (rectangle == null || !rectangle.isPartnerItemsEnabled()) continue;
            cooldownEntities.add(entity2);
        }
        if (!cooldownEntities.isEmpty()) {
            this.renderEntityOverlays(cooldownEntities, player2, matrix4f, f);
        } else if (!this.map2.isEmpty()) {
            this.itemIconCache.disposeCache();
            this.map2.clear();
        }
    }

    private static final class EntityCooldownState {
        private final Map<String, CooldownDisplayEntry> entriesById = new HashMap<String, CooldownDisplayEntry>();
        private final List<CooldownDisplayEntry> activeEntries = new ArrayList<CooldownDisplayEntry>();
        private int lastSeenFrame;
        private float screenX;
        private float screenY;
        private float alpha;

        private EntityCooldownState() {
        }

        private void markVisible(boolean visible) {
            if (!visible) {
                this.alpha = 0.0f;
            }
        }

        private void update(ItemIconCache iconCache) {
            this.activeEntries.clear();
            float targetAlpha = 0.0f;
            for (CooldownDisplayEntry entry2 : this.entriesById.values()) {
                entry2.update(iconCache);
                if (!(entry2.alpha() > 0.01f)) continue;
                this.activeEntries.add(entry2);
                targetAlpha = Math.max(targetAlpha, entry2.alpha());
            }
            this.activeEntries.sort(Comparator.comparing(entry -> entry.label));
            this.alpha = FrameInterpolator.lerpTowards(this.alpha, targetAlpha, 20.0f);
        }

        private float alpha() {
            return this.alpha;
        }
    }

    private static final class CooldownDisplayEntry {
        private String label = "";
        private ItemStack stack = ItemStack.EMPTY;
        private int color = 0xFFFFFF;
        private boolean visible;
        private float visibility;
        private BakedItemIcon icon;

        private CooldownDisplayEntry() {
        }

        private void setVisible(boolean visible) {
            this.visible = visible;
        }

        private void update(ItemIconCache iconCache) {
            this.visibility = FrameInterpolator.lerpTowards(this.visibility, this.visible ? 1.0f : 0.0f, 20.0f);
            if (!this.stack.isEmpty()) {
                this.icon = iconCache.get(this.stack);
            }
            this.visible = false;
        }

        private float alpha() {
            return this.visibility;
        }
    }
}

