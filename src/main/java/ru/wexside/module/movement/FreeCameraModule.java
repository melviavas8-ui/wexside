package ru.wexside.module.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;

public class FreeCameraModule
extends Module
implements ConfigSerializable {
    private static volatile FreeCameraModule instance;
    private float pitch;
    private float yaw;
    private final BooleanSetting enabledSetting;
    private final NumberSetting speed;
    private Vec3d previousPos = Vec3d.ZERO;
    private ClientWorld trackedWorld;
    private Perspective savedPerspective;
    private Vec3d cameraPos = Vec3d.ZERO;
    private final NumberSetting motionY;
    private boolean active;

    public FreeCameraModule(EventBus eventBus) {
        super(eventBus, "free_camera", "Free Camera", "Позволяет свободно перемещаться камерой в мире", ModuleCategory.valueOf("MOVEMENT"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.speed = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 10.0).defaultValue(5.0).multiplier(0.1).precision(0).animationSpeed(20.0f).showMarkers().name("Speed").id("speed").description("Скорость движения свободной камеры").aliases("speed", "скорость")).build();
        this.registerSetting(this.speed);
        this.motionY = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 10.0).defaultValue(5.0).multiplier(0.1).precision(0).animationSpeed(20.0f).showMarkers().name("Motion Y").id("motion_y").description("Скорость вертикального движения камеры").aliases("motion y", "вертикальная скорость")).build();
        this.registerSetting(this.motionY);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.tickCamera());
        this.listen(WorldSessionEvent.class, event -> this.shutdown());
    }

    public static float getFloatType() {
        FreeCameraModule module = instance;
        return module == null ? 0.0f : module.pitch;
    }

    private void disableCamera() {
        this.restorePerspective();
        this.active = false;
    }

    public static boolean isEnabled() {
        FreeCameraModule module = instance;
        return module != null && module.active;
    }

    public static void handle(double deltaX, double deltaY) {
        FreeCameraModule module = instance;
        if (module == null) {
            return;
        }
        module.yaw += (float)(deltaX * 0.15);
        module.pitch = MathHelper.clamp((float)(module.pitch + (float)(deltaY * 0.15)), (float)-90.0f, (float)90.0f);
    }

    private void enableCamera() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || !player.isAlive()) {
            this.enabledSetting.setEnabled(false);
            return;
        }
        this.previousPos = this.cameraPos = player.getEyePos();
        this.yaw = player.getYaw();
        this.pitch = player.getPitch();
        this.savedPerspective = client.options.getPerspective();
        client.options.setPerspective(Perspective.FIRST_PERSON);
        this.trackedWorld = client.world;
        this.active = true;
    }

    private void shutdown() {
        this.restorePerspective();
        this.active = false;
        this.enabledSetting.setEnabled(false);
        this.enabledSetting.getKeybind().refreshToggleState();
    }

    public static float getFloatType2() {
        FreeCameraModule module = instance;
        return module == null ? 0.0f : module.yaw;
    }

    private void tickCamera() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (this.trackedWorld != world) {
            this.trackedWorld = world;
            if (this.active) {
                this.shutdown();
                return;
            }
        }
        if (this.active && (client.player == null || !client.player.isAlive())) {
            this.shutdown();
            return;
        }
        if (this.enabledSetting.isEnabled() && !this.active) {
            this.enableCamera();
        } else if (!this.enabledSetting.isEnabled() && this.active) {
            this.disableCamera();
        }
        if (this.active) {
            this.moveCamera();
        }
    }

    private void restorePerspective() {
        if (this.savedPerspective != null) {
            MinecraftClient.getInstance().options.setPerspective(this.savedPerspective);
            this.savedPerspective = null;
        }
    }

    public static Vec3d compute(float tickDelta) {
        FreeCameraModule module = instance;
        if (module == null) {
            return Vec3d.ZERO;
        }
        return new Vec3d(MathHelper.lerp((double)tickDelta, (double)module.previousPos.x, (double)module.cameraPos.x), MathHelper.lerp((double)tickDelta, (double)module.previousPos.y, (double)module.cameraPos.y), MathHelper.lerp((double)tickDelta, (double)module.previousPos.z, (double)module.cameraPos.z));
    }

    private void moveCamera() {
        this.previousPos = this.cameraPos;
        GameOptions options = MinecraftClient.getInstance().options;
        double forward = (options.forwardKey.isPressed() ? 1 : 0) - (options.backKey.isPressed() ? 1 : 0);
        double strafe = (options.rightKey.isPressed() ? 1 : 0) - (options.leftKey.isPressed() ? 1 : 0);
        double vertical = (options.jumpKey.isPressed() ? 1 : 0) - (options.sneakKey.isPressed() ? 1 : 0);
        double speed = this.speed.getValue() * 3.0;
        double dx = 0.0;
        double dz = 0.0;
        if (forward != 0.0 || strafe != 0.0) {
            double yawRad = Math.toRadians(this.yaw);
            double sin = Math.sin(yawRad);
            double cos = Math.cos(yawRad);
            dx = -forward * sin - strafe * cos;
            dz = forward * cos - strafe * sin;
            double length = Math.sqrt(dx * dx + dz * dz);
            dx = dx / length * speed;
            dz = dz / length * speed;
        }
        double dy = vertical * this.motionY.getValue();
        this.cameraPos = this.cameraPos.add(dx, dy, dz);
    }
}

