package ru.wexside.module.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.FriendList;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.ColorSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.ColorUtils;

public final class TracersModule
extends Module
implements ConfigSerializable {
    private static final double LOOK_DISTANCE = 75.0;
    private static volatile TracersModule instance;
    private final BooleanSetting enabledSetting;
    private final MultiSelectSetting types;
    private final ColorSetting playersColor;
    private final ColorSetting entitiesColor;
    private final ColorSetting friendsColor;
    private final NumberSetting width;
    private final List<TracerLine> lines = new ArrayList<TracerLine>();

    public TracersModule(EventBus eventBus) {
        super(eventBus, "tracers", "Tracers", "Линии до сущностей на экране", ModuleCategory.valueOf("RENDER"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Линии до объекта на экране").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        MultiSelectSetting typesSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Players", "Entities", "Friends").selectAll(false).optionListEnabled(false).name("Types").id("types").description("Выбранные сущности")).build();
        typesSetting.setOptions(new String[0]);
        this.types = typesSetting;
        this.registerSetting(typesSetting);
        ColorSetting players = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("Players Color").id("players_color").description("Цвет линий до игроков").visibleWhen(() -> this.hasType("Players"))).build();
        this.applyPalette(players);
        this.playersColor = players;
        this.registerSetting(players);
        ColorSetting entities = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("Entities Color").id("entities_color").description("Цвет линий до мобов").visibleWhen(() -> this.hasType("Entities"))).build();
        this.applyPalette(entities);
        this.entitiesColor = entities;
        this.registerSetting(entities);
        ColorSetting friends = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("Friends Color").id("friends_color").description("Цвет линий до друзей").visibleWhen(() -> this.hasType("Friends"))).build();
        this.applyPalette(friends);
        this.friendsColor = friends;
        this.registerSetting(friends);
        this.width = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 2.0).defaultValue(1.0).multiplier(1.0).precision(1).animationSpeed(20.0f).snapTo(0.5).name("Width").id("width").description("Толщина линий").aliases("width", "толщина")).build();
        this.registerSetting(this.width);
    }

    @Override
    protected void initialize() {
        this.listen(WorldRenderEvent.class, this::collectLines);
    }

    public static void tick2() {
        TracersModule module = instance;
        if (module != null) {
            module.drawLines();
        }
    }

    private void collectLines(WorldRenderEvent event) {
        this.lines.clear();
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
        if (cameraPos == null) {
            return;
        }
        boolean players = this.hasType("Players");
        boolean entities = this.hasType("Entities");
        boolean friends = this.hasType("Friends");
        if (!(players || entities || friends)) {
            return;
        }
        int playerColor = this.playersColor.getColor(0.0f);
        int entityColor = this.entitiesColor.getColor(0.0f);
        int friendColor = this.friendsColor.getColor(0.0f);
        float tickDelta = event.getFloatType();
        for (Entity entity : client.world.getEntities()) {
            Integer color = this.colorFor(entity, players, entities, friends, playerColor, entityColor, friendColor);
            if (color == null) continue;
            Vec3d offset = entity.getLerpedPos(tickDelta).add(0.0, (double)(entity.getHeight() / 2.0f), 0.0).subtract(cameraPos);
            this.lines.add(new TracerLine(offset, color));
        }
    }

    private void drawLines() {
        if (this.lines.isEmpty()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            this.lines.clear();
            return;
        }
        Camera camera = client.gameRenderer.getCamera();
        Vec3d look = Vec3d.fromPolar((float)camera.getPitch(), (float)camera.getYaw()).multiply(75.0);
        Matrix4f matrix = new Matrix4f();
        BufferBuilder consumer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (TracerLine line : this.lines) {
            Vec3d delta = line.offset.subtract(look);
            if (delta.lengthSquared() < 1.0E-4) continue;
            Vec3d normal = delta.normalize();
            int[] rgba = ColorUtils.unpackRgba(line.color);
            consumer.vertex((Matrix4fc)matrix, (float)look.x, (float)look.y, (float)look.z).color(rgba[0], rgba[1], rgba[2], rgba[3]);
            consumer.vertex((Matrix4fc)matrix, (float)line.offset.x, (float)line.offset.y, (float)line.offset.z).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }
        RenderLayers.lines().draw(consumer.end());
        this.lines.clear();
    }

    private Integer colorFor(Entity entity, boolean players, boolean entities, boolean friends, int playerColor, int entityColor, int friendColor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity == null || entity == client.player || entity == client.getCameraEntity() || !entity.isAlive()) {
            return null;
        }
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            if (this.isFriend(player)) {
                return friends ? Integer.valueOf(friendColor) : null;
            }
            return players ? Integer.valueOf(playerColor) : null;
        }
        if (entity instanceof LivingEntity) {
            return entities ? Integer.valueOf(entityColor) : null;
        }
        return null;
    }

    private boolean isFriend(PlayerEntity player) {
        FriendList friends = WexSideClient.getFriends();
        return friends != null && friends.contains(player.getName().getString());
    }

    private boolean hasType(String type) {
        return this.types.getSelectedOptions().contains(type);
    }

    private void applyPalette(ColorSetting setting) {
        setting.setPrimaryColor(0, -11753627);
        setting.setPrimaryColor(1, -1543135);
        setting.setPrimaryColor(2, -9279489);
        setting.setPrimaryColor(3, -46001);
        setting.setPrimaryColor(4, -13218);
        setting.setPrimaryColor(5, -10582785);
        setting.setPrimaryColor(6, -2732032);
    }

    private record TracerLine(Vec3d offset, int color) {
    }
}

