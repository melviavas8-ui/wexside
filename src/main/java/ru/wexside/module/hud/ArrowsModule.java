package ru.wexside.module.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.misc.ArrowStyle;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FriendList;
import ru.wexside.misc.Gps;
import ru.wexside.misc.HandledScreenAccessor;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.ColorSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.entity.NpcDetector;

public final class ArrowsModule
extends Module
implements ConfigSerializable {
    private static final String GROUP_PLAYERS = "Players";
    private static final String GROUP_ENTITIES = "Entities";
    private static final String GROUP_ITEMS = "Items";
    private static final String GROUP_FRIENDS = "Friends";
    private static final float GPS_CLAMP_PADDING = 48.0f;
    private static final float ENTITY_CLAMP_PADDING = 28.0f;
    private static final float TARGET_CLAMP_PADDING = 20.0f;
    private static final float DISTANCE_TEXT_SIZE = 6.5f;
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Стрелки к объектам на экране").withKeybind().toggle()).build();
    private final ModeSetting style;
    private final MultiSelectSetting groups;
    private final NumberSetting size;
    private final NumberSetting radius;
    private final BooleanSetting showDistance;
    private final BooleanSetting showHealth;
    private final BooleanSetting overlap;
    private final NumberSetting threshold;
    private final ColorSetting playersColor;
    private final ColorSetting entitiesColor;
    private final ColorSetting itemsColor;
    private final ColorSetting friendsColor;
    private final ColorSetting gpsColor;

    public ArrowsModule(EventBus eventBus) {
        super(eventBus, "arrows", "Arrows", "Стрелки-указатели к объектам и GPS-координатам", ModuleCategory.valueOf("DISPLAY"), "arrows", "стрелки");
        this.registerSetting(this.enabledSetting);
        this.style = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Triangle", "Classic").defaultOption("Classic").name("Style").id("style").description("Текстура стрелки")).build();
        this.registerSetting(this.style);
        MultiSelectSetting groupsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options(GROUP_PLAYERS, GROUP_ENTITIES, GROUP_ITEMS, GROUP_FRIENDS).selectAll(false).optionListEnabled(false).name("Groups").id("groups").description("Какие цели показывать")).build();
        groupsSetting.setOptions(new String[]{GROUP_PLAYERS, GROUP_ENTITIES});
        this.groups = groupsSetting;
        this.registerSetting(groupsSetting);
        this.size = ((NumberSettingBuilder)NumberSetting.builder().range(0.5, 1.0).defaultValue(0.7).multiplier(1.0).precision(1).animationSpeed(20.0f).name("Size").id("size").description("Размер стрелок")).build();
        this.registerSetting(this.size);
        this.radius = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 10.0).defaultValue(4.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Radius").id("radius").description("Радиус круга стрелок")).build();
        this.registerSetting(this.radius);
        this.showDistance = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Show Distance").id("show_distance").description("Отображать дистанцию")).build();
        this.registerSetting(this.showDistance);
        this.showHealth = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Show Health").id("show_health").description("Отображать здоровье")).build();
        this.registerSetting(this.showHealth);
        this.overlap = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Hide Overlap").id("overlap").description("Скрывать перекрывающиеся стрелки")).build();
        this.registerSetting(this.overlap);
        this.threshold = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 100.0).defaultValue(50.0).multiplier(0.01).precision(0).animationSpeed(20.0f).markers(10.0).name("Threshold").id("threshold").description("Сила скрытия: выше \u2014 прячет даже при слабом перекрытии стрелок").aliases("threshold", "порог").visibleWhen(this.overlap::isEnabled)).build();
        this.registerSetting(this.threshold);
        ColorSetting players = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(3).name("Players Color").id("players_color").description("Цвет стрелки, подсвечивающий игрока").visibleWhen(() -> this.groups.getSelectedOptions().contains(GROUP_PLAYERS))).build();
        this.applyPalette(players);
        this.playersColor = players;
        this.registerSetting(players);
        ColorSetting entities = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(4).name("Entities Color").id("entities_color").description("Цвет стрелки, подсвечивающий моба").visibleWhen(() -> this.groups.getSelectedOptions().contains(GROUP_ENTITIES))).build();
        this.applyPalette(entities);
        this.entitiesColor = entities;
        this.registerSetting(entities);
        ColorSetting items = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(5).name("Items Color").id("items_color").description("Цвет стрелки, подсвечивающий предмет").visibleWhen(() -> this.groups.getSelectedOptions().contains(GROUP_ITEMS))).build();
        this.applyPalette(items);
        this.itemsColor = items;
        this.registerSetting(items);
        ColorSetting friends = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(6).name("Friends Color").id("friends_color").description("Цвет стрелки, подсвечивающий друга").visibleWhen(() -> this.groups.getSelectedOptions().contains(GROUP_FRIENDS))).build();
        this.applyPalette(friends);
        this.friendsColor = friends;
        this.registerSetting(friends);
        ColorSetting gps = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(2).name("GPS Color").id("gps_color").description("Цвет GPS-стрелки")).build();
        this.applyPalette(gps);
        this.gpsColor = gps;
        this.registerSetting(gps);
    }

    @Override
    protected void initialize() {
        this.listen(HudRenderEvent.class, this::onRender);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void onRender(HudRenderEvent event) {
        float orbitRadius;
        boolean enabled = this.enabledSetting.isEnabled();
        if (!enabled && !Gps.isActive()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            return;
        }
        Window window = client.getWindow();
        float scale = window.getScaleFactor();
        float centerX = (float)window.getScaledWidth() / 2.0f;
        float centerY = (float)window.getScaledHeight() / 2.0f;
        float tickDelta = client.getRenderTickCounter().getTickProgress(true);
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        if (cameraPos == null) {
            return;
        }
        float yaw = MathHelper.lerpAngleDegrees((float)tickDelta, (float)player.lastYaw, (float)player.getYaw());
        double yawRadians = Math.toRadians(yaw);
        double forwardX = Math.cos(yawRadians);
        double forwardZ = Math.sin(yawRadians);
        float arrowSize = 18.0f * this.size.getFloatValue();
        Screen screen = client.currentScreen;
        boolean containerOpen = screen instanceof HandledScreen;
        int containerX = 0;
        int containerY = 0;
        int containerWidth = 0;
        int containerHeight = 0;
        if (containerOpen) {
            HandledScreenAccessor container = (HandledScreenAccessor)screen;
            containerX = container.getContainerX();
            containerY = container.getContainerY();
            containerWidth = container.getContainerWidth();
            containerHeight = container.getContainerHeight();
            orbitRadius = (float)Math.max(containerWidth, containerHeight) / 2.0f;
        } else {
            orbitRadius = (float)(40.0 + this.radius.getValue() * 3.0);
        }
        float entityOrbitRadius = orbitRadius * 1.5f;
        float gpsOrbitRadius = entityOrbitRadius + 28.0f;
        boolean showPlayers = this.groups.getSelectedOptions().contains(GROUP_PLAYERS);
        boolean showEntities = this.groups.getSelectedOptions().contains(GROUP_ENTITIES);
        boolean showItems = this.groups.getSelectedOptions().contains(GROUP_ITEMS);
        boolean showFriends = this.groups.getSelectedOptions().contains(GROUP_FRIENDS);
        ArrayList<EntityTarget> targets = new ArrayList<EntityTarget>();
        if (enabled && (showPlayers || showEntities || showItems || showFriends)) {
            FriendList friends = WexSideClient.getFriends();
            NpcDetector npcDetector = WexSideClient.getNpcDetector();
            for (Entity entity : world.getEntities()) {
                ArrowTargetKind kind;
                if (entity == player || !entity.isAlive()) continue;
                if (entity instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity)entity;
                    if (npcDetector != null && npcDetector.isNpc(living)) continue;
                }
                if ((kind = this.classify(entity, friends)) == null || !this.isGroupEnabled(kind, showPlayers, showEntities, showItems, showFriends)) continue;
                targets.add(new EntityTarget(entity, kind, player.distanceTo(entity)));
            }
            targets.sort(Comparator.comparingDouble(EntityTarget::distance));
        }
        ArrayList<ArrowRenderState> arrows = new ArrayList<ArrowRenderState>();
        boolean hideOverlap = this.overlap.isEnabled();
        float overlapThreshold = 1.0f - this.threshold.getFloatValue();
        for (int index = 0; index < targets.size(); ++index) {
            Entity entity;
            EntityTarget target = (EntityTarget)targets.get(index);
            Vec3d entityPos = target.entity.getLerpedPos(tickDelta);
            float angle = ArrowsModule.computeAngle(entityPos.x - cameraPos.x, entityPos.z - cameraPos.z, forwardX, forwardZ);
            float[] position = ArrowsModule.computeOrbitPosition(angle, centerX, centerY, entityOrbitRadius, containerOpen, containerX, containerY, containerWidth, containerHeight, 20.0f);
            float x = position[0];
            float y = position[1];
            if (hideOverlap && ArrowsModule.overlapsExisting(arrows, x, y, arrowSize, overlapThreshold)) continue;
            float colorMix = targets.size() > 1 ? (float)index / (float)targets.size() : 0.0f;
            float healthRatio = -1.0f;
            if (this.showHealth.isEnabled() && (entity = target.entity) instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)entity;
                healthRatio = MathHelper.clamp((float)(living.getHealth() / Math.max(living.getHealth(), living.getMaxHealth())), (float)0.0f, (float)1.0f);
            }
            arrows.add(new ArrowRenderState(x, y, angle, this.colorFor(target.kind, colorMix), (int)target.distance, healthRatio, this.showDistance.isEnabled()));
        }
        if (Gps.isActive()) {
            float gpsAngle = ArrowsModule.computeAngle((double)Gps.getX() - cameraPos.x, (double)Gps.getZ() - cameraPos.z, forwardX, forwardZ);
            float[] gpsPosition = ArrowsModule.computeOrbitPosition(gpsAngle, centerX, centerY, gpsOrbitRadius, containerOpen, containerX, containerY, containerWidth, containerHeight, 48.0f);
            double dx = (double)Gps.getX() - player.getX();
            double dz = (double)Gps.getZ() - player.getZ();
            int gpsDistance = (int)Math.sqrt(dx * dx + dz * dz);
            arrows.add(new ArrowRenderState(gpsPosition[0], gpsPosition[1], gpsAngle, this.gpsColor.getColor(), gpsDistance, -1.0f, true));
        }
        if (arrows.isEmpty()) {
            return;
        }
        ArrowStyle arrowStyle = ArrowStyle.fromDisplayName(this.style.getSelectedOption());
        Matrix4f baseMatrix = new Matrix4f().scale(scale);
        GuiDrawApi renderer = WexSideClient.getHudRenderer();
        renderer.begin();
        try {
            int textureId = renderer.bindTexture(arrowStyle.getTexture().getTextureId(), arrowStyle.getTexture().getWidth(), arrowStyle.getTexture().getHeight());
            float halfSize = arrowSize / 2.0f;
            for (ArrowRenderState arrow : arrows) {
                Matrix4f arrowMatrix = new Matrix4f().scale(scale).translate(arrow.x, arrow.y, 0.0f).rotateZ(arrow.angle);
                renderer.drawTexture(arrowMatrix, -halfSize, -halfSize, arrowSize, arrowSize, 0.0f, 0.0f, 1.0f, 1.0f, textureId, arrow.color);
            }
            for (ArrowRenderState arrow : arrows) {
                if (arrow.healthRatio >= 0.0f) {
                    this.drawHealthBar(renderer, baseMatrix, arrow.x, arrow.y - arrowSize * 0.5f, arrowSize, arrow.healthRatio);
                }
                if (!arrow.showDistance) continue;
                this.drawDistance(renderer, baseMatrix, arrow.x, arrow.y + arrowSize * 0.45f, arrow.distanceMeters);
            }
        }
        finally {
            renderer.end();
        }
    }

    private ArrowTargetKind classify(Entity entity, FriendList friends) {
        if (entity instanceof ArmorStandEntity || entity instanceof ProjectileEntity || entity instanceof MobEntity) {
            return ArrowTargetKind.ENTITIES;
        }
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            boolean isFriend = friends != null && friends.contains(Formatting.strip((String)player.getName().getString()));
            return isFriend ? ArrowTargetKind.FRIENDS : ArrowTargetKind.PLAYERS;
        }
        if (entity instanceof ItemEntity) {
            return ArrowTargetKind.ITEMS;
        }
        return null;
    }

    private boolean isGroupEnabled(ArrowTargetKind kind, boolean players, boolean entities, boolean items, boolean friends) {
        return switch (kind.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> players;
            case 1 -> friends;
            case 2 -> entities;
            case 3 -> items;
        };
    }

    private int colorFor(ArrowTargetKind kind, float mix) {
        return switch (kind.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> this.playersColor.getColor(mix);
            case 1 -> this.friendsColor.getColor(mix);
            case 2 -> this.entitiesColor.getColor(mix);
            case 3 -> this.itemsColor.getColor(mix);
        };
    }

    private static float computeAngle(double deltaX, double deltaZ, double forwardX, double forwardZ) {
        double rotatedX = -(deltaZ * forwardX - deltaX * forwardZ);
        double rotatedZ = -(deltaX * forwardX + deltaZ * forwardZ);
        return (float)Math.atan2(rotatedX, rotatedZ);
    }

    private static float[] computeOrbitPosition(float angle, float centerX, float centerY, float radius, boolean clampToContainer, int containerX, int containerY, int containerWidth, int containerHeight, float padding) {
        float x = centerX + radius * (float)Math.cos(angle);
        float y = centerY + radius * (float)Math.sin(angle);
        if (clampToContainer) {
            x = MathHelper.clamp((float)x, (float)((float)containerX - padding), (float)((float)(containerX + containerWidth) + padding));
            y = MathHelper.clamp((float)y, (float)((float)containerY - padding), (float)((float)(containerY + containerHeight) + padding));
        }
        return new float[]{x, y};
    }

    private static float overlapRatio(float left, float top, float size, float otherLeft, float otherTop, float otherSize) {
        float overlapHeight;
        float overlapWidth = Math.max(0.0f, Math.min(left + size, otherLeft + otherSize) - Math.max(left, otherLeft));
        float overlapArea = overlapWidth * (overlapHeight = Math.max(0.0f, Math.min(top + size, otherTop + otherSize) - Math.max(top, otherTop)));
        if (overlapArea == 0.0f) {
            return 0.0f;
        }
        return overlapArea / Math.min(size * size, otherSize * otherSize);
    }

    private static boolean overlapsExisting(List<ArrowRenderState> arrows, float x, float y, float size, float threshold) {
        float half = size / 2.0f;
        float left = x - half;
        float top = y - half;
        for (ArrowRenderState existing : arrows) {
            float existingLeft = existing.x - half;
            float existingTop = existing.y - half;
            if (!(ArrowsModule.overlapRatio(left, top, size, existingLeft, existingTop, size) > threshold)) continue;
            return true;
        }
        return false;
    }

    private void drawDistance(GuiDrawApi renderer, Matrix4f matrix, float x, float y, int meters) {
        String text = meters + "m";
        float width = FontRegistry.font5.textWidth(text, 6.5f);
        FontRegistry.font5.drawText(matrix, renderer, text, x - width / 2.0f, y, 6.5f, -1);
    }

    private void drawHealthBar(GuiDrawApi renderer, Matrix4f matrix, float x, float y, float size, float ratio) {
        float barWidth = size * 0.7f;
        float barHeight = 1.5f;
        float barX = x - barWidth / 2.0f;
        int fillColor = ratio > 0.5f ? -16711936 : (ratio > 0.25f ? -256 : -65536);
        renderer.fillRectangle(matrix, barX, y, barWidth, barHeight, Integer.MIN_VALUE);
        renderer.fillRectangle(matrix, barX, y, barWidth * ratio, barHeight, fillColor);
    }

    private void applyPalette(ColorSetting colorSetting) {
        colorSetting.setPrimaryColor(0, -11753627);
        colorSetting.setPrimaryColor(1, -1543135);
        colorSetting.setPrimaryColor(2, -9279489);
        colorSetting.setPrimaryColor(3, -46001);
        colorSetting.setPrimaryColor(4, -13218);
        colorSetting.setPrimaryColor(5, -10582785);
        colorSetting.setPrimaryColor(6, -2732032);
    }

    private static enum ArrowTargetKind {
        PLAYERS,
        FRIENDS,
        ENTITIES,
        ITEMS;

    }

    private record EntityTarget(Entity entity, ArrowTargetKind kind, double distance) {
    }

    private record ArrowRenderState(float x, float y, float angle, int color, int distanceMeters, float healthRatio, boolean showDistance) {
    }
}

