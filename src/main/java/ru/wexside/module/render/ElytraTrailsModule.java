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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.lwjgl.opengl.GL46;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ColorSetting;
import ru.wexside.setting.ColorSettingBuilder;

public final class ElytraTrailsModule
extends Module
implements ConfigSerializable {
    private static final double MIN_DISTANCE = 3.0E-5;
    private static final int MIN_POINTS = 4;
    private final BooleanSetting enabledSetting;
    private final ColorSetting color;
    private final List<TrailPoint> trail = new ArrayList<TrailPoint>();

    public ElytraTrailsModule(EventBus eventBus) {
        super(eventBus, "elytra_trails", "Elytra Trails", "След за игроком при полёте на элитрах", ModuleCategory.valueOf("RENDER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Включить след при полёте на элитрах").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        ColorSetting colorSetting = ((ColorSettingBuilder)ColorSetting.builder().selectedIndex(0).name("Color").id("color").description("Цвет следа").aliases("color", "цвет")).build();
        colorSetting.setPrimaryColor(0, -11753627);
        colorSetting.setPrimaryColor(1, -1543135);
        colorSetting.setPrimaryColor(2, -9279489);
        colorSetting.setPrimaryColor(3, -46001);
        colorSetting.setPrimaryColor(4, -13218);
        colorSetting.setPrimaryColor(5, -10582785);
        colorSetting.setPrimaryColor(6, -2732032);
        this.color = colorSetting;
        this.registerSetting(colorSetting);
    }

    @Override
    protected void initialize() {
        this.listen(WorldRenderEvent.class, this::onWorldRender);
    }

    private void onWorldRender(WorldRenderEvent event) {
        if (!this.enabledSetting.isEnabled()) {
            this.trail.clear();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || !client.player.isClimbing()) {
            this.trail.clear();
            return;
        }
        Vec3d pos = client.player.getLerpedPos(event.getFloatType());
        if (this.trail.isEmpty()) {
            this.trail.add(new TrailPoint(pos, pos, false));
        } else {
            TrailPoint last = this.trail.getLast();
            if (pos.squaredDistanceTo(last.to) > 3.0E-5) {
                this.trail.add(new TrailPoint(last.to, pos, true));
            }
        }
        this.trail.removeIf(TrailPoint::expired);
        this.renderTrail(event);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderTrail(WorldRenderEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !client.player.isGliding() || client.options.getPerspective().isFirstPerson() || this.trail.size() < 4) {
            return;
        }
        boolean cullEnabled = GL46.glIsEnabled((int)2884);
        boolean depthMask = GL46.glGetBoolean((int)2930);
        GL46.glDisable((int)2884);
        GL46.glDepthMask((boolean)false);
        try {
            Camera camera = client.gameRenderer.getCamera();
            Vec3d cameraPos = camera.getCameraPos();
            float[] rgba = this.colorComponents(this.color.getColor());
            Matrix4f matrix = new Matrix4f().rotation((Quaternionfc)camera.getRotation());
            BufferBuilder consumer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (int i = 1; i < this.trail.size() - 2; ++i) {
                TrailPoint prev = this.trail.get(Math.max(0, i - 1));
                TrailPoint current = this.trail.get(i);
                TrailPoint next = this.trail.get(i + 1);
                TrailPoint after = this.trail.get(Math.min(this.trail.size() - 1, i + 2));
                if (!current.valid || !next.valid || current.from.squaredDistanceTo(current.to) <= 1.0E-7 || next.from.squaredDistanceTo(next.to) <= 1.0E-7) continue;
                TrailVertex left = this.buildVertex(current, prev, next, i, cameraPos, event.getFloatType(), rgba, 0);
                TrailVertex right = this.buildVertex(next, current, after, i + 1, cameraPos, event.getFloatType(), rgba, 1);
                if (left.alpha <= 0.001f || right.alpha <= 0.001f) continue;
                this.emitRibbon(consumer, matrix, left, right);
            }
            RenderLayers.debugQuads().draw(consumer.end());
        }
        finally {
            GL46.glDepthMask((boolean)depthMask);
            if (cullEnabled) {
                GL46.glEnable((int)2884);
            } else {
                GL46.glDisable((int)2884);
            }
        }
    }

    private TrailVertex buildVertex(TrailPoint point, TrailPoint prev, TrailPoint next, int index, Vec3d cameraPos, float tickDelta, float[] rgba, int layer) {
        float alpha;
        float blue;
        float green;
        float red;
        float width;
        Vec3d pos = point.lerp(tickDelta);
        Vec3d prevPos = prev.lerp(tickDelta);
        Vec3d nextPos = next.lerp(tickDelta);
        Vec3d tangent = nextPos.subtract(prevPos);
        if (tangent.lengthSquared() < 1.0E-5) {
            tangent = point.to.subtract(point.from);
        }
        if (tangent.lengthSquared() < 1.0E-5) {
            tangent = new Vec3d(0.0, 0.0, 1.0);
        }
        tangent = tangent.normalize();
        Vec3d side = new Vec3d(-tangent.z, 0.0, tangent.x);
        side = side.lengthSquared() < 1.0E-5 ? new Vec3d(1.0, 0.0, 0.0) : side.normalize();
        Vec3d relative = pos.subtract(cameraPos);
        float progress = point.progress();
        float fade = point.fade();
        float time = MinecraftClient.getInstance().world.getTime();
        float wave = MathHelper.sin((double)(time * 0.15f + (float)index * 0.5f)) * 0.15f * fade;
        if (layer == 0) {
            width = 0.2f + 0.2f * progress + wave * 0.1f;
            float dim = progress * 0.3f;
            red = rgba[0] * (1.0f - dim);
            green = rgba[1] * (1.0f - dim);
            blue = rgba[2] * (1.0f - dim);
            alpha = fade * rgba[3];
        } else if (layer == 1) {
            width = 0.2f + (MathHelper.sin((double)(time * 0.2f)) * 0.1f + 0.1f) * fade + wave * 0.1f;
            float boost = progress * 0.2f;
            red = MathHelper.clamp((float)(rgba[0] + boost), (float)0.0f, (float)1.0f);
            green = MathHelper.clamp((float)(rgba[1] + boost), (float)0.0f, (float)1.0f);
            blue = MathHelper.clamp((float)(rgba[2] + boost), (float)0.0f, (float)1.0f);
            alpha = fade * rgba[3];
        } else {
            width = 0.1f + wave * 0.05f;
            red = rgba[0];
            green = rgba[1];
            blue = rgba[2];
            alpha = MathHelper.clamp((float)(fade * rgba[3] * 0.3f), (float)0.0f, (float)0.3f);
        }
        return new TrailVertex((float)relative.x, (float)relative.y, (float)relative.z, side, Math.max(width, 0.001f), red, green, blue, MathHelper.clamp((float)alpha, (float)0.0f, (float)1.0f));
    }

    private void emitRibbon(BufferBuilder consumer, Matrix4f matrix, TrailVertex left, TrailVertex right) {
        Vec3d a = left.side.multiply((double)(-left.width));
        Vec3d b = left.side.multiply((double)left.width);
        Vec3d c = right.side.multiply((double)right.width);
        Vec3d d = right.side.multiply((double)(-right.width));
        this.vertex(consumer, matrix, left.x + (float)a.x, left.y + (float)a.y, left.z + (float)a.z, left.red, left.green, left.blue, left.alpha);
        this.vertex(consumer, matrix, left.x + (float)b.x, left.y + (float)b.y, left.z + (float)b.z, left.red, left.green, left.blue, left.alpha);
        this.vertex(consumer, matrix, right.x + (float)c.x, right.y + (float)c.y, right.z + (float)c.z, right.red, right.green, right.blue, right.alpha);
        this.vertex(consumer, matrix, right.x + (float)d.x, right.y + (float)d.y, right.z + (float)d.z, right.red, right.green, right.blue, right.alpha);
    }

    private void vertex(BufferBuilder consumer, Matrix4f matrix, float x, float y, float z, float red, float green, float blue, float alpha) {
        consumer.vertex((Matrix4fc)matrix, x, y, z).color(red, green, blue, alpha);
    }

    private float[] colorComponents(int color) {
        return new float[]{(float)(color >> 16 & 0xFF) / 255.0f, (float)(color >> 8 & 0xFF) / 255.0f, (float)(color & 0xFF) / 255.0f, (float)(color >>> 24 & 0xFF) / 255.0f};
    }

    static final class TrailPoint {
        final Vec3d from;
        final Vec3d to;
        final boolean valid;

        TrailPoint(Vec3d from, Vec3d to, boolean valid) {
            this.from = from;
            this.to = to;
            this.valid = valid;
        }

        Vec3d lerp(float tickDelta) {
            return new Vec3d(MathHelper.lerp((double)tickDelta, (double)this.from.x, (double)this.to.x), MathHelper.lerp((double)tickDelta, (double)this.from.y, (double)this.to.y), MathHelper.lerp((double)tickDelta, (double)this.from.z, (double)this.to.z));
        }

        float progress() {
            return MathHelper.clamp((float)((float)this.from.distanceTo(this.to) * 4.0f), (float)0.0f, (float)1.0f);
        }

        float fade() {
            return MathHelper.clamp((float)(1.0f - this.progress()), (float)0.0f, (float)1.0f);
        }

        boolean expired() {
            return !this.valid;
        }
    }

    static final class TrailVertex {
        final float x;
        final float y;
        final float z;
        final Vec3d side;
        final float width;
        final float red;
        final float green;
        final float blue;
        final float alpha;

        TrailVertex(float x, float y, float z, Vec3d side, float width, float red, float green, float blue, float alpha) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.side = side;
            this.width = width;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }
    }
}

