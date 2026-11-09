package ru.wexside.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import ru.wexside.WexSideClient;
import ru.wexside.item.ItemBadge;
import ru.wexside.misc.CaptureFramebuffer;
import ru.wexside.misc.DeferredRenderTask;
import ru.wexside.misc.EntityEspOverlayRenderer;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.misc.GlowEspSettings;
import ru.wexside.misc.ModelEspSettings;
import ru.wexside.model.esp.EspRelation;
import ru.wexside.model.esp.EspTargetType;
import ru.wexside.render.ChamsPreviewEffect;
import ru.wexside.render.ChamsSettings;
import ru.wexside.render.GlowEspPreviewEffect;
import ru.wexside.render.OffscreenRenderManager;
import ru.wexside.render.SkeletonPreviewRenderer;
import ru.wexside.ui.GuiBounds;
import ru.wexside.util.EspFeatureRegistry;
import ru.wexside.util.GuiDrawApi;

public final class EspPreviewRenderer {
    private static final int MIN_PREVIEW_SIZE = 110;
    private static final int MAX_PREVIEW_SIZE = 640;
    private static final float MAX_PITCH = 35.0f;
    private static final float ROTATION_SENSITIVITY = 1.2f;
    private static final float ROTATION_RESET_SPEED = 8.0f;
    private static final String DONATION_TAG_KEY = "don-item";
    private static final String[] DONATION_PREFIXES = new String[]{"wexside:", "wexside_", "donate:"};
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final ItemStack ITEM_PREVIEW_STACK = new ItemStack((ItemConvertible)Items.TOTEM_OF_UNDYING);
    private double lastMouseX;
    private double lastMouseY;
    private final SkeletonPreviewRenderer skeletonPreviewRenderer;
    private final EntityEspOverlayRenderer entityEspOverlayRenderer;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private Entity mobPreviewEntity;
    private float requestedSize;
    private float pitch;
    private EspTargetType espTargetType;
    private final ChamsPreviewEffect chamsPreviewEffect;
    private boolean rendered;
    private boolean rotating;
    private final GlowEspPreviewEffect glowEspPreviewEffect;
    private int previewSize = 110;
    private int framebufferWidth;
    private ClientWorld cachedWorld;
    private Entity playerPreviewEntity;
    private final CaptureFramebuffer captureFramebuffer = new CaptureFramebuffer();
    private EspRelation espRelation;
    private int previewPixelSize;
    private float yaw;
    private int chamsTextureId;
    private int framebufferHeight;
    private int skeletonTextureId;
    private int glowTextureId;

    public EspPreviewRenderer() {
        this.entityEspOverlayRenderer = new EntityEspOverlayRenderer();
        this.chamsPreviewEffect = new ChamsPreviewEffect();
        this.glowEspPreviewEffect = new GlowEspPreviewEffect();
        this.skeletonPreviewRenderer = new SkeletonPreviewRenderer();
        this.espRelation = EspRelation.DEFAULT;
    }

    private static float clampPitch(float pitch) {
        return Math.max(-35.0f, Math.min(35.0f, pitch));
    }

    private void clearPreviewMetadata(EntityRenderState illiliIIiI2) {
        illiliIIiI2.displayName = null;
        illiliIIiI2.nameLabelPos = null;
        illiliIIiI2.outlineColor = 0;
        illiliIIiI2.invisible = false;
        illiliIIiI2.onFire = false;
        illiliIIiI2.shadowRadius = 0.0f;
        illiliIIiI2.shadowPieces.clear();
        illiliIIiI2.light = 0xF000F0;
        if (illiliIIiI2 instanceof PlayerEntityRenderState) {
            PlayerEntityRenderState playerEntityRenderState = (PlayerEntityRenderState)illiliIIiI2;
            playerEntityRenderState.capeVisible = false;
        }
    }

    private Consumer<DrawContext> createEntityDrawTask(Entity member4759, float f, float f2) {
        float f3;
        float f4;
        if (member4759 == null) {
            return null;
        }
        EntityRenderState illiliIIiI2 = this.client.getEntityRenderDispatcher().getAndUpdateRenderState(member4759, f);
        if (illiliIIiI2 == null) {
            return null;
        }
        this.clearPreviewMetadata(illiliIIiI2);
        Quaternionf quaternionf = new Quaternionf().rotateZ((float)Math.PI);
        if (illiliIIiI2 instanceof LivingEntityRenderState) {
            LivingEntityRenderState livingEntityRenderState = (LivingEntityRenderState)illiliIIiI2;
            livingEntityRenderState.bodyYaw = 180.0f + this.yaw;
            livingEntityRenderState.relativeHeadYaw = 0.0f;
            livingEntityRenderState.pitch = this.pitch;
            float f5 = illiliIIiI2.height <= 0.0f ? 1.8f : illiliIIiI2.height;
            f4 = (float)this.previewSize * f2;
            f3 = f5 / 2.0f + 0.035f;
        } else {
            quaternionf.rotateY((float)Math.toRadians(this.yaw));
            f4 = (float)this.previewSize * 0.8f;
            f3 = 0.0f;
        }
        Vector3f translation = new Vector3f(0.0f, f3, 0.0f);
        Quaternionf quaternionf2 = new Quaternionf().rotateX((float)Math.toRadians(-this.pitch));
        return context -> context.addEntity(illiliIIiI2, f4, translation, quaternionf, quaternionf2, 0, 0, this.previewSize, this.previewSize);
    }

    private String getDonationLabel(EspTargetType espTargetType) {
        if ((espTargetType == EspTargetType.PLAYERS || espTargetType == EspTargetType.SELF) && this.client.player != null) {
            return this.readDonationItemId(this.client.player.getOffHandStack());
        }
        return null;
    }

    public void beginRotation(int n, int n2, GuiBounds bounds2) {
        if (bounds2.contains(n, n2)) {
            this.rotating = true;
            this.lastMouseX = this.getMouseX();
            this.lastMouseY = this.getMouseY();
        }
    }

    public void close() {
        this.rendered = false;
        this.captureFramebuffer.delete();
        this.entityEspOverlayRenderer.update();
        this.chamsPreviewEffect.releaseFramebuffers();
        this.glowEspPreviewEffect.releaseFramebuffers();
        this.skeletonPreviewRenderer.close();
        this.mobPreviewEntity = null;
        this.playerPreviewEntity = null;
        this.cachedWorld = null;
    }

    public void render(GuiDrawApi drawApi, Matrix4f matrix4f, GuiBounds bounds2) {
        int n;
        if (!this.rendered || this.captureFramebuffer.getColorAttachmentGlId() <= 0) {
            return;
        }
        float f = Math.min(bounds2.getWidth(), bounds2.getHeight());
        float f2 = bounds2.getX() + (bounds2.getWidth() - f) / 2.0f;
        float f3 = bounds2.getY() + (bounds2.getHeight() - f) / 2.0f;
        float f4 = (float)this.previewPixelSize / (float)this.framebufferWidth;
        float f5 = 1.0f - (float)this.previewPixelSize / (float)this.framebufferHeight;
        EspTargetType espTargetType = this.espTargetType;
        if (this.glowTextureId != 0) {
            n = drawApi.bindTexture(this.glowTextureId, this.glowEspPreviewEffect.getWidth(), this.glowEspPreviewEffect.getHeight());
            drawApi.drawTexture(matrix4f, f2, f3, f, f, 0.0f, 1.0f, f4, f5, n, -1);
        }
        n = this.chamsTextureId != 0 ? this.chamsTextureId : this.captureFramebuffer.getColorAttachmentGlId();
        int n2 = drawApi.bindTexture(n, this.framebufferWidth, this.framebufferHeight);
        drawApi.drawTexture(matrix4f, f2, f3, f, f, 0.0f, 1.0f, f4, f5, n2, -1);
        if (this.skeletonTextureId != 0) {
            int n3 = drawApi.bindTexture(this.skeletonTextureId, this.framebufferWidth, this.framebufferHeight);
            drawApi.drawTexture(matrix4f, f2, f3, f, f, 0.0f, 1.0f, f4, f5, n3, -1);
        }
        this.entityEspOverlayRenderer.renderOverlay(drawApi, matrix4f, this.calculateOverlayBounds(espTargetType, f2, f3, f), espTargetType, this.espRelation, this.getModelYaw(espTargetType), this.getDisplayName(espTargetType), this.getHealthValue(espTargetType), this.getEffectDuration(espTargetType), this.getHeldItemBadge(espTargetType), this.getDonationLabel(espTargetType));
    }

    public void requestPreviewRender(EspTargetType espTargetType, EspRelation espRelation, float f) {
        this.requestedSize = f;
        DeferredRenderTask.setRunnable(() -> this.renderPreview(espTargetType, espRelation));
    }

    public void updatePreviewRotation() {
        if (this.rotating && !this.isLeftMouseDown()) {
            this.rotating = false;
        }
        if (this.rotating) {
            double d = this.getMouseX();
            double d2 = this.getMouseY();
            this.yaw = EspPreviewRenderer.wrapYaw(this.yaw - (float)(d - this.lastMouseX) * 1.2f);
            this.pitch = EspPreviewRenderer.clampPitch(this.pitch + (float)(d2 - this.lastMouseY) * 1.2f);
            this.lastMouseX = d;
            this.lastMouseY = d2;
            return;
        }
        this.yaw = FrameInterpolator.lerpTowards(this.yaw, 0.0f, 8.0f);
        this.pitch = FrameInterpolator.lerpTowards(this.pitch, 0.0f, 8.0f);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderPreview(EspTargetType espTargetType, EspRelation espRelation) {
        Consumer<DrawContext> consumer;
        this.rendered = false;
        this.chamsTextureId = 0;
        this.glowTextureId = 0;
        this.skeletonTextureId = 0;
        this.espTargetType = espTargetType;
        EspRelation espRelation2 = this.espRelation = espRelation == null ? EspRelation.DEFAULT : espRelation;
        if (this.client.world == null || this.client.player == null) {
            return;
        }
        OffscreenRenderManager renderPipeline2 = WexSideClient.getOffscreenRenderManager();
        if (renderPipeline2 == null) {
            return;
        }
        Window window2 = this.client.getWindow();
        int n = window2.getFramebufferWidth();
        int n2 = window2.getFramebufferHeight();
        float f = window2.getScaleFactor();
        this.updatePreviewSize(f, n, n2);
        int n3 = Math.round((float)this.previewSize * f);
        if (n <= 0 || n2 <= 0 || n3 <= 0 || n3 > n || n3 > n2) {
            return;
        }
        float f2 = this.client.getRenderTickCounter().getTickProgress(true);
        Consumer<DrawContext> consumer2 = consumer = espTargetType == EspTargetType.ITEMS ? null : this.createPreviewDrawTask(espTargetType, f2);
        if (espTargetType != EspTargetType.ITEMS && consumer == null) {
            return;
        }
        int n4 = GL30.glGetInteger((int)36006);
        int[] nArray = new int[4];
        GL11.glGetIntegerv((int)2978, (int[])nArray);
        try {
            Entity member4759;
            ModelEspSettings modelEspSettings;
            GlowEspSettings glowEspSettings;
            ChamsSettings chamsSettings;
            if (espTargetType == EspTargetType.ITEMS) {
                renderPipeline2.captureItem(this.captureFramebuffer, n, n2, ITEM_PREVIEW_STACK, ItemDisplayContext.GUI, (float)this.previewSize / 2.0f, (float)this.previewSize / 2.0f, this.getItemRenderScale(), -this.yaw, this.pitch);
            } else {
                renderPipeline2.captureGui(this.captureFramebuffer, n, n2, consumer);
            }
            this.previewPixelSize = n3;
            this.framebufferWidth = n;
            this.framebufferHeight = n2;
            this.rendered = true;
            this.entityEspOverlayRenderer.prepareEquipmentIcons(espTargetType, this.espRelation, this.getEquipment(espTargetType), f);
            EspFeatureRegistry espFeatures = WexSideClient.getEspFeatureRegistry();
            ChamsSettings chamsSettings2 = chamsSettings = espFeatures != null && espTargetType == EspTargetType.PLAYERS ? espFeatures.getChamsSettings(this.espRelation) : null;
            if (chamsSettings != null && chamsSettings.isEnabled()) {
                this.chamsTextureId = this.chamsPreviewEffect.render(this.captureFramebuffer, n, n2, n3, chamsSettings);
            }
            GlowEspSettings glowEspSettings2 = glowEspSettings = espFeatures != null && espTargetType == EspTargetType.PLAYERS ? espFeatures.getGlowSettings(this.espRelation) : null;
            if (glowEspSettings != null && glowEspSettings.isEnabled()) {
                this.glowTextureId = this.glowEspPreviewEffect.render(this.captureFramebuffer, n, n2, glowEspSettings);
            }
            ModelEspSettings modelEspSettings2 = modelEspSettings = espFeatures != null && espTargetType == EspTargetType.PLAYERS ? espFeatures.getModelEspSettings(this.espRelation) : null;
            if (modelEspSettings != null && modelEspSettings.isEnabled() && (member4759 = this.getPlayerPreviewEntity()) instanceof PlayerEntity) {
                PlayerEntity player2 = (PlayerEntity)member4759;
                this.skeletonTextureId = this.skeletonPreviewRenderer.render(renderPipeline2, player2, n, n2, (float)this.previewSize / 2.0f, (float)this.previewSize / 2.0f, (float)this.previewSize * 0.4f, -0.95f, 180.0f + this.yaw, modelEspSettings);
            }
        }
        catch (RuntimeException runtimeException) {
        }
        finally {
            GL30.glBindFramebuffer((int)36160, (int)n4);
            GL11.glViewport((int)nArray[0], (int)nArray[1], (int)nArray[2], (int)nArray[3]);
        }
    }

    private ItemBadge getHeldItemBadge(EspTargetType espTargetType) {
        if ((espTargetType == EspTargetType.PLAYERS || espTargetType == EspTargetType.SELF) && this.client.player != null) {
            ItemBadge itemBadge = ItemBadge.fromStack(this.client.player.getMainHandStack());
            return itemBadge != null ? itemBadge : ItemBadge.fromStack(this.client.player.getOffHandStack());
        }
        return null;
    }

    private String getDisplayName(EspTargetType espTargetType) {
        return switch (espTargetType == null ? EspTargetType.PLAYERS : espTargetType) {
            case EspTargetType.ENTITIES -> {
                if (this.mobPreviewEntity != null) {
                    yield this.mobPreviewEntity.getName().getString();
                }
                yield "Entity";
            }
            case EspTargetType.ITEMS -> ITEM_PREVIEW_STACK.getName().getString();
            default -> this.client.player != null ? this.client.player.getName().getString() : "Player";
        };
    }

    private Entity getPlayerPreviewEntity() {
        if (this.client.world == null || this.client.player == null) {
            return null;
        }
        if (this.playerPreviewEntity != null && this.cachedWorld == this.client.world) {
            return this.playerPreviewEntity;
        }
        try {
            OtherClientPlayerEntity otherClientPlayerEntity = new OtherClientPlayerEntity(this.client.world, this.client.player.getGameProfile());
            otherClientPlayerEntity.copyPositionAndRotation((Entity)this.client.player);
            otherClientPlayerEntity.equipStack(EquipmentSlot.HEAD, EspPreviewRenderer.withEnchantmentGlint(new ItemStack((ItemConvertible)Items.DIAMOND_HELMET)));
            otherClientPlayerEntity.equipStack(EquipmentSlot.CHEST, EspPreviewRenderer.withEnchantmentGlint(new ItemStack((ItemConvertible)Items.DIAMOND_CHESTPLATE)));
            otherClientPlayerEntity.equipStack(EquipmentSlot.LEGS, EspPreviewRenderer.withEnchantmentGlint(new ItemStack((ItemConvertible)Items.DIAMOND_LEGGINGS)));
            otherClientPlayerEntity.equipStack(EquipmentSlot.FEET, EspPreviewRenderer.withEnchantmentGlint(new ItemStack((ItemConvertible)Items.DIAMOND_BOOTS)));
            otherClientPlayerEntity.equipStack(EquipmentSlot.MAINHAND, EspPreviewRenderer.withEnchantmentGlint(new ItemStack((ItemConvertible)Items.DIAMOND_SWORD)));
            otherClientPlayerEntity.equipStack(EquipmentSlot.OFFHAND, EspPreviewRenderer.withEnchantmentGlint(new ItemStack((ItemConvertible)Items.ENCHANTED_GOLDEN_APPLE)));
            this.playerPreviewEntity = otherClientPlayerEntity;
            this.cachedWorld = this.client.world;
        }
        catch (RuntimeException runtimeException) {
            return null;
        }
        return this.playerPreviewEntity;
    }

    private Consumer<DrawContext> createPreviewDrawTask(EspTargetType espTargetType, float f) {
        return switch (espTargetType) {
            default -> throw new MatchException(null, null);
            case EspTargetType.PLAYERS, EspTargetType.SELF -> this.createEntityDrawTask(this.getPlayerPreviewEntity(), f, 0.4f);
            case EspTargetType.ENTITIES -> this.createEntityDrawTask(this.getMobPreviewEntity(), f, 0.42f);
            case EspTargetType.ITEMS -> null;
        };
    }

    private List<ItemStack> getEquipment(EspTargetType espTargetType) {
        if (espTargetType == EspTargetType.PLAYERS || espTargetType == EspTargetType.SELF) {
            Entity preview = this.getPlayerPreviewEntity();
            if (!(preview instanceof LivingEntity)) {
                return List.of();
            }
            LivingEntity entity2 = (LivingEntity)preview;
            ArrayList<ItemStack> equipment = new ArrayList<ItemStack>();
            equipment.add(entity2.getMainHandStack());
            equipment.add(entity2.getOffHandStack());
            for (EquipmentSlot iliiIIiliI2 : ARMOR_SLOTS) {
                equipment.add(entity2.getEquippedStack(iliiIIiliI2));
            }
            equipment.removeIf(ItemStack::isEmpty);
            return equipment;
        }
        return List.of();
    }

    private double getMouseX() {
        Window window2 = this.client.getWindow();
        return this.client.mouse.getScaledX(window2);
    }

    private String readDonationItemId(ItemStack stack2) {
        NbtComponent customData = (NbtComponent)stack2.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return null;
        }
        NbtCompound iiililiiII2 = customData.copyNbt();
        if (!iiililiiII2.contains(DONATION_TAG_KEY)) {
            return null;
        }
        String string = iiililiiII2.getString(DONATION_TAG_KEY, "");
        for (String string2 : DONATION_PREFIXES) {
            if (!string.startsWith(string2)) continue;
            return string.substring(string2.length()).toUpperCase();
        }
        return null;
    }

    private float getModelYaw(EspTargetType espTargetType) {
        return espTargetType == EspTargetType.ITEMS ? -this.yaw : this.yaw;
    }

    private GuiBounds calculateOverlayBounds(EspTargetType espTargetType, float f, float f2, float f3) {
        float f4;
        float f5;
        float f6;
        float f7 = switch (espTargetType == null ? EspTargetType.PLAYERS : espTargetType) {
            default -> throw new MatchException(null, null);
            case EspTargetType.ENTITIES -> {
                f6 = 0.3f;
                f5 = 0.3f;
                f4 = 0.17f;
                yield 0.04f;
            }
            case EspTargetType.ITEMS -> {
                f6 = 0.24f;
                f5 = 0.24f;
                f4 = 0.2f;
                yield 0.2f;
            }
            case EspTargetType.PLAYERS, EspTargetType.SELF -> {
                f6 = 0.34f;
                f5 = 0.34f;
                f4 = 0.05f;
                yield 0.11f;
            }
        };
        float f8 = f + f6 * f3;
        float f9 = f2 + f4 * f3;
        float f10 = f3 * (1.0f - f6 - f5);
        float f11 = f3 * (1.0f - f4 - f7);
        return new GuiBounds(f8, f9, f10, f11);
    }

    private boolean isLeftMouseDown() {
        return GLFW.glfwGetMouseButton((long)this.client.getWindow().getHandle(), (int)0) == 1;
    }

    private int getEffectDuration(EspTargetType espTargetType) {
        return espTargetType == EspTargetType.ENTITIES ? 2000 : 0;
    }

    private float getItemRenderScale() {
        return (float)this.previewSize * 0.7f;
    }

    private static ItemStack withEnchantmentGlint(ItemStack stack2) {
        stack2.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, Boolean.valueOf(true));
        return stack2;
    }

    private Entity getMobPreviewEntity() {
        if (this.mobPreviewEntity == null && this.client.world != null) {
            try {
                this.mobPreviewEntity = EntityType.ZOMBIE.create((World)this.client.world, SpawnReason.COMMAND);
            }
            catch (RuntimeException runtimeException) {
                // empty catch block
            }
        }
        return this.mobPreviewEntity;
    }

    private double getMouseY() {
        Window window2 = this.client.getWindow();
        return this.client.mouse.getScaledY(window2);
    }

    private int getHealthValue(EspTargetType espTargetType) {
        return switch (espTargetType == null ? EspTargetType.PLAYERS : espTargetType) {
            default -> throw new MatchException(null, null);
            case EspTargetType.ENTITIES -> 20;
            case EspTargetType.ITEMS -> -1;
            case EspTargetType.PLAYERS, EspTargetType.SELF -> this.client.player != null ? Math.round(this.client.player.getHealth()) : 20;
        };
    }

    private void updatePreviewSize(float f, int n, int n2) {
        int n3 = f <= 0.0f ? 110 : Math.round(this.requestedSize * 2.0f / f);
        int n4 = f <= 0.0f ? 640 : (int)Math.floor((float)Math.min(n, n2) / f);
        this.previewSize = Math.max(110, Math.min(Math.min(640, n4), n3));
    }

    private static float wrapYaw(float f) {
        float f2 = f % 360.0f;
        if (f2 > 180.0f) {
            f2 -= 360.0f;
        }
        if (f2 < -180.0f) {
            f2 += 360.0f;
        }
        return f2;
    }
}

