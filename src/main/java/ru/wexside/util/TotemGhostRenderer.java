package ru.wexside.util;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import ru.wexside.event.TotemPopEvent;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.misc.TotemEffectRenderer;
import ru.wexside.util.TotemEffectSettings;

public final class TotemGhostRenderer
implements TotemEffectRenderer {
    private static final long LIFETIME_MS = 900L;
    private static final int LAYERS = 3;
    private static final int MAX_GHOSTS = 16;
    private static final Vec3d FALLBACK_LOOK = new Vec3d(0.0, 0.0, 1.0);
    private final List<TotemGhost> ghosts = new ArrayList<TotemGhost>();
    private final TotemEffectSettings settings;

    public TotemGhostRenderer(TotemEffectSettings settings) {
        this.settings = settings;
    }

    @Override
    public void renderWorld(WorldRenderEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            this.clearHolograms();
            return;
        }
        if (this.ghosts.isEmpty()) {
            return;
        }
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPosition = camera.getCameraPos();
        Matrix4f matrix = new Matrix4f().rotation((Quaternionfc)camera.getRotation());
        BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        BufferBuilder outline = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        long now = System.currentTimeMillis();
        Iterator<TotemGhost> iterator = this.ghosts.iterator();
        while (iterator.hasNext()) {
            TotemGhost ghost = iterator.next();
            if (now - ghost.spawnedAt() >= 900L) {
                iterator.remove();
                continue;
            }
            this.drawGhost(ghost, now, cameraPosition, matrix, fill, outline);
        }
        RenderLayers.debugQuads().draw(fill.end());
        RenderLayers.linesTranslucent().draw(outline.end());
    }

    private void drawGhost(TotemGhost ghost, long now, Vec3d camera, Matrix4f matrix, BufferBuilder fill, BufferBuilder outline) {
        float progress = MathHelper.clamp((float)((float)(now - ghost.spawnedAt()) / 900.0f), (float)0.0f, (float)1.0f);
        int baseColor = this.settings.getIntType3();
        Vec3d side = ghost.look().lengthSquared() > 1.0E-6 ? new Vec3d(-ghost.look().z, 0.0, ghost.look().x).normalize() : new Vec3d(1.0, 0.0, 0.0);
        for (int layer = 2; layer >= 0; --layer) {
            float delayed = Math.max(0.0f, progress - (float)layer * 0.08f);
            float fade = (1.0f - delayed) * (1.0f - delayed) * (1.0f - (float)layer * 0.18f);
            if (fade <= 0.025f) continue;
            float eased = TotemGhostRenderer.easeOutCubic(delayed);
            double travel = 0.16 + (double)eased * 0.7 + (double)layer * 0.08;
            double wobble = Math.sin((double)ghost.phase() + (double)delayed * Math.PI * 2.0 + (double)layer * 0.35) * 0.08;
            double rise = 0.16 + (double)TotemGhostRenderer.sineIn(delayed) * 0.56 + (double)layer * 0.03;
            Vec3d position = ghost.origin().add(ghost.look().multiply(travel)).add(side.multiply(wobble)).add(0.0, rise, 0.0);
            float scale = 1.0f + (1.0f - delayed) * 0.06f + (float)layer * 0.012f;
            Box box = TotemGhostRenderer.box(position, ghost.width(), ghost.height(), scale);
            TotemGhostRenderer.drawFilledBox(fill, matrix, camera, box, TotemGhostRenderer.withAlpha(baseColor, Math.round(107.0f * fade)));
            TotemGhostRenderer.drawBoxOutline(outline, matrix, camera, box, TotemGhostRenderer.withAlpha(baseColor, Math.round(230.0f * fade)));
        }
    }

    @Override
    public void setTotemPopEvent(TotemPopEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity)entity;
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d look = client.player == null ? FALLBACK_LOOK : client.player.getRotationVec(1.0f);
        Vec3d vec3d = look;
        if (look.lengthSquared() <= 1.0E-6) {
            look = FALLBACK_LOOK;
        }
        Box bounds = player.getBoundingBox();
        this.ghosts.add(new TotemGhost(event.toVec3d(), look.normalize(), bounds.maxX - bounds.minX, player.getHeight(), event.getTime(), ThreadLocalRandom.current().nextFloat() * ((float)Math.PI * 2)));
        int overflow = this.ghosts.size() - 16;
        if (overflow > 0) {
            this.ghosts.subList(0, overflow).clear();
        }
    }

    @Override
    public void clearHolograms() {
        this.ghosts.clear();
    }

    private static Box box(Vec3d position, double width, double height, float scale) {
        double halfWidth = width * (double)scale * 0.5;
        return new Box(position.x - halfWidth, position.y, position.z - halfWidth, position.x + halfWidth, position.y + height * (double)scale, position.z + halfWidth);
    }

    private static float sineIn(float value) {
        return (float)Math.sin((double)MathHelper.clamp((float)value, (float)0.0f, (float)1.0f) * Math.PI * 0.5);
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0f - MathHelper.clamp((float)value, (float)0.0f, (float)1.0f);
        return 1.0f - inverse * inverse * inverse;
    }

    private static int withAlpha(int color, int alpha) {
        return color & 0xFFFFFF | Math.clamp((long)alpha, 0, 255) << 24;
    }

    private static void drawFilledBox(BufferBuilder vertices, Matrix4f matrix, Vec3d camera, Box box, int color) {
        TotemGhostRenderer.quad(vertices, matrix, camera, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
        TotemGhostRenderer.quad(vertices, matrix, camera, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ, color);
        TotemGhostRenderer.quad(vertices, matrix, camera, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ, color);
        TotemGhostRenderer.quad(vertices, matrix, camera, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
        TotemGhostRenderer.quad(vertices, matrix, camera, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, color);
        TotemGhostRenderer.quad(vertices, matrix, camera, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ, color);
    }

    private static void drawBoxOutline(BufferBuilder vertices, Matrix4f matrix, Vec3d camera, Box box, int color) {
        TotemGhostRenderer.line(vertices, matrix, camera, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, color);
        TotemGhostRenderer.line(vertices, matrix, camera, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, color);
        TotemGhostRenderer.line(vertices, matrix, camera, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
        TotemGhostRenderer.line(vertices, matrix, camera, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, color);
        TotemGhostRenderer.line(vertices, matrix, camera, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, color);
        TotemGhostRenderer.line(vertices, matrix, camera, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
        TotemGhostRenderer.line(vertices, matrix, camera, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
        TotemGhostRenderer.line(vertices, matrix, camera, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, color);
        TotemGhostRenderer.line(vertices, matrix, camera, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, color);
        TotemGhostRenderer.line(vertices, matrix, camera, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, color);
        TotemGhostRenderer.line(vertices, matrix, camera, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, color);
        TotemGhostRenderer.line(vertices, matrix, camera, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
    }

    private static void quad(BufferBuilder vertices, Matrix4f matrix, Vec3d camera, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, int color) {
        TotemGhostRenderer.vertex(vertices, matrix, camera, x1, y1, z1, color);
        TotemGhostRenderer.vertex(vertices, matrix, camera, x2, y2, z2, color);
        TotemGhostRenderer.vertex(vertices, matrix, camera, x3, y3, z3, color);
        TotemGhostRenderer.vertex(vertices, matrix, camera, x4, y4, z4, color);
    }

    private static void line(BufferBuilder vertices, Matrix4f matrix, Vec3d camera, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        TotemGhostRenderer.vertex(vertices, matrix, camera, x1, y1, z1, color);
        TotemGhostRenderer.vertex(vertices, matrix, camera, x2, y2, z2, color);
    }

    private static void vertex(BufferBuilder vertices, Matrix4f matrix, Vec3d camera, double x, double y, double z, int color) {
        vertices.vertex((Matrix4fc)matrix, (float)(x - camera.x), (float)(y - camera.y), (float)(z - camera.z)).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24 & 0xFF);
    }

    private record TotemGhost(Vec3d origin, Vec3d look, double width, double height, long spawnedAt, float phase) {
    }
}

