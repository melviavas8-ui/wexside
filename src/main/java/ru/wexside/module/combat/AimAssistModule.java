package ru.wexside.module.combat;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.AttackWindowEvent;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.ReachHelper;
import ru.wexside.misc.TargetFilter;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.Angle;
import ru.wexside.util.TargetSelector;

public class AimAssistModule
extends Module
implements ConfigSerializable {
    private static final String MODE_ASSIST = "Доводка";
    private static final String MODE_HELPER = "Помощник";
    private static final String MODE_CAPTURE = "Захват";
    private static final String MODE_DIRECT = "Прямой";
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Доводит камеру до цели").withKeybind().toggle()).build();
    private final ModeSetting mode;
    private final BooleanSetting magnet;
    private final NumberSetting magnetStrength;
    private final NumberSetting yawSpeed;
    private final NumberSetting pitchSpeed;
    private final NumberSetting captureSpeedNear;
    private final NumberSetting captureSpeedFar;
    private final NumberSetting captureZone;
    private final NumberSetting shakeStrength;
    private final NumberSetting aimHeight;
    private final NumberSetting attackRange;
    private final NumberSetting rotationRange;
    private final NumberSetting fov;
    private final MultiSelectSetting targets;
    private final ModeSetting sorting;
    private final BooleanSetting throughWalls;
    private final BooleanSetting focusOne;
    private final BooleanSetting noGui;
    private final BooleanSetting inInventory;
    private TargetSelector targetSelector;
    private LivingEntity target;
    private Vec3d smoothedDelta;
    private int onTargetHold;
    private int trackedId = -1;
    private int shakeTargetId = -1;
    private int wanderTargetId = -1;
    private long lastFrameNanos;
    private long nextShakeNanos;
    private boolean applied;
    private float lastYaw;
    private float lastPitch;
    private float mouseYaw;
    private float mousePitch;
    private float mouseActivity;
    private float yawQuantError;
    private float pitchQuantError;
    private float captureYawStep;
    private float capturePitchStep;
    private float assistGain = 0.36f;
    private float onTargetScale = 1.0f;
    private float magnetLock;
    private float magnetAmount;
    private float wanderX;
    private float wanderZ;
    private float shakeYaw;
    private float shakePitch;

    public AimAssistModule(EventBus eventBus) {
        super(eventBus, "aim_assist", "Aim Assist", "Помощник прицеливания", ModuleCategory.valueOf("COMBAT"), new String[0]);
        this.registerSetting(this.enabledSetting);
        this.mode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options(MODE_ASSIST, MODE_HELPER, MODE_CAPTURE, MODE_DIRECT).defaultOption(MODE_ASSIST).name("Режим").id("mode").description("Как именно тянуть камеру").withKeybind()).build();
        this.registerSetting(this.mode);
        this.magnet = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Магнит").id("magnet").description("Тянет только пока ты сам ведёшь мышкой и не больше, чем на твой же сдвиг").aliases("magnet", "магнит").visibleWhen(() -> MODE_HELPER.equals(this.mode.getSelectedOption()))).build();
        this.registerSetting(this.magnet);
        this.magnetStrength = ((NumberSettingBuilder)NumberSetting.builder().range(0.1, 2.5).defaultValue(0.9999999999999999).multiplier(1.0).precision(2).animationSpeed(20.0f).name("Сила магнита").id("magnet_strength").description("Во сколько раз магнит может усилить твой собственный сдвиг").aliases("magnet strength", "сила магнита").visibleWhen(() -> MODE_HELPER.equals(this.mode.getSelectedOption()) && this.magnet.isEnabled())).build();
        this.registerSetting(this.magnetStrength);
        this.yawSpeed = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 180.0).defaultValue(60.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Скорость по горизонтали").id("yaw_speed").description("Предел довода по yaw")).build();
        this.registerSetting(this.yawSpeed);
        this.pitchSpeed = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 180.0).defaultValue(30.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Скорость по вертикали").id("pitch_speed").description("Предел довода по pitch")).build();
        this.registerSetting(this.pitchSpeed);
        this.captureSpeedNear = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 180.0).defaultValue(25.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Скорость вблизи прицела").id("capture_speed_near").description("Скорость, когда цель почти под прицелом").visibleWhen(() -> MODE_CAPTURE.equals(this.mode.getSelectedOption()))).build();
        this.registerSetting(this.captureSpeedNear);
        this.captureSpeedFar = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 180.0).defaultValue(90.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Скорость вдали от прицела").id("capture_speed_far").description("Скорость, когда цель далеко от прицела").visibleWhen(() -> MODE_CAPTURE.equals(this.mode.getSelectedOption()))).build();
        this.registerSetting(this.captureSpeedFar);
        this.captureZone = ((NumberSettingBuilder)NumberSetting.builder().range(0.5, 90.0).defaultValue(20.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Зона перехода").id("capture_zone").description("Угол, на котором скорость переходит от дальней к ближней").visibleWhen(() -> MODE_CAPTURE.equals(this.mode.getSelectedOption()))).build();
        this.registerSetting(this.captureZone);
        this.shakeStrength = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 10.0).defaultValue(3.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Дрожание").id("shake_strength").description("Живость прицела: блуждание точки, случайные смещения и ослабление довода на цели")).build();
        this.registerSetting(this.shakeStrength);
        this.aimHeight = ((NumberSettingBuilder)NumberSetting.builder().range(0.18, 0.92).defaultValue(0.6).multiplier(1.0).precision(2).animationSpeed(20.0f).name("Высота точки").id("aim_height").description("Доля хитбокса снизу вверх, в которую целиться").aliases("height", "высота")).build();
        this.registerSetting(this.aimHeight);
        this.attackRange = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 6.0).defaultValue(3.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Дистанция удара").id("attack_range").description("Нижняя граница луча проверки прицела")).build();
        this.registerSetting(this.attackRange);
        this.rotationRange = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 12.0).defaultValue(5.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Дистанция наведения").id("rotation_range").description("Дальность поиска цели и верхняя граница луча проверки")).build();
        this.registerSetting(this.rotationRange);
        this.fov = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 360.0).defaultValue(120.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("FOV").id("fov").description("Сектор поиска цели")).build();
        this.registerSetting(this.fov);
        MultiSelectSetting targetsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Players", "Friends", "Bots", "Naked", "Invisibles", "Animals", "Mobs", "Villagers").selectAll(false).optionListEnabled(false).name("Цели").id("targets").description("Типы целей для наведения")).build();
        targetsSetting.setOptions(new String[0]);
        this.targets = targetsSetting;
        this.registerSetting(targetsSetting);
        this.sorting = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Health", "Distance", "Crosshair").defaultOption("Distance").name("Сортировка").id("sorting").description("Как выбирать цель из нескольких")).build();
        this.registerSetting(this.sorting);
        this.throughWalls = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Сквозь стены").id("through_walls").description("Наводиться на цель за блоками").aliases("walls", "стены")).build();
        this.registerSetting(this.throughWalls);
        this.focusOne = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Фокус на одном").id("focus_one").description("Держать одну цель, пока она валидна, вместо перевыбора каждый тик").aliases("focus", "фокус")).build();
        this.registerSetting(this.focusOne);
        this.noGui = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Не водить в GUI").id("no_gui").description("Отпускать камеру, пока открыт экран. Чат экраном не считается")).build();
        this.registerSetting(this.noGui);
        this.inInventory = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Водить в инвентаре").id("in_inventory").description("Исключение для инвентаря и сундуков").aliases("inventory", "инвентарь").visibleWhen(this.noGui::isEnabled)).build();
        this.registerSetting(this.inInventory);
    }

    @Override
    protected void initialize() {
        this.targetSelector = new TargetSelector();
        this.listen(ClientTickEvent.class, event -> this.selectTarget());
        this.listen(AttackWindowEvent.class, this::onAttackWindow);
    }

    private void rollShake(LivingEntity entity, boolean captured) {
        long now = System.nanoTime();
        if (!captured && this.shakeTargetId == entity.getId() && now < this.nextShakeNanos) {
            return;
        }
        this.shakeTargetId = entity.getId();
        float strength = MathHelper.clamp((float)(this.shakeStrength.getFloatValue() / 10.0f), (float)0.0f, (float)1.0f);
        float yawRange = 0.012f + strength * 0.03f;
        float pitchRange = 0.008f + strength * 0.024f;
        this.shakeYaw = ThreadLocalRandom.current().nextFloat(-yawRange, yawRange);
        this.shakePitch = ThreadLocalRandom.current().nextFloat(-pitchRange, pitchRange);
        this.nextShakeNanos = now + ThreadLocalRandom.current().nextLong(340000000L, 760000000L);
    }

    private static Vec3d predictClimb(LivingEntity entity, Vec3d motion) {
        Vec3d look = entity.getRotationVector();
        float pitchRad = entity.getPitch() * ((float)Math.PI / 180);
        double horizontalLook = Math.sqrt(look.x * look.x + look.z * look.z);
        double motionLength = motion.horizontalLength();
        double gravity = AimAssistModule.effectiveGravity(entity);
        double pitchCosSq = MathHelper.square((double)Math.cos(pitchRad));
        motion = motion.add(0.0, gravity * (-1.0 + pitchCosSq * 0.75), 0.0);
        if (motion.y < 0.0 && horizontalLook > 0.0) {
            double lift = motion.y * -0.1 * pitchCosSq;
            motion = motion.add(look.x * lift / horizontalLook, lift, look.z * lift / horizontalLook);
        }
        if (pitchRad < 0.0f && horizontalLook > 0.0) {
            double boost = motionLength * (double)(-MathHelper.sin((double)pitchRad)) * 0.04;
            motion = motion.add(-look.x * boost / horizontalLook, boost * 3.2, -look.z * boost / horizontalLook);
        }
        if (horizontalLook > 0.0) {
            motion = motion.add((look.x / horizontalLook * motionLength - motion.x) * 0.1, 0.0, (look.z / horizontalLook * motionLength - motion.z) * 0.1);
        }
        return motion.multiply(0.99, 0.98, 0.99);
    }

    private boolean blockedByScreen() {
        if (!this.noGui.isEnabled()) {
            return false;
        }
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen == null || screen instanceof ChatScreen) {
            return false;
        }
        return !this.inInventory.isEnabled() || !(screen instanceof HandledScreen);
    }

    private void onAttackWindow(AttackWindowEvent event) {
        float nextPitch;
        float nextYaw;
        if (!event.isPre() && !event.isPost()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        LivingEntity target = this.target;
        if (!this.enabledSetting.isEnabled() || player == null || !player.isAlive() || target == null || this.blockedByScreen()) {
            this.lastFrameNanos = 0L;
            this.resetAssist();
            return;
        }
        float dt = this.frameDelta();
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        this.updateWander(dt, target);
        boolean captured = this.captureTarget(target.getId());
        float mouseWeight = this.updateMouse(yaw, pitch, dt);
        String mode = this.mode.getSelectedOption();
        boolean assist = MODE_ASSIST.equals(mode);
        boolean helper = MODE_HELPER.equals(mode);
        boolean capture = MODE_CAPTURE.equals(mode);
        boolean smooth = assist || helper;
        this.rollShake(target, captured);
        Vec3d aimPoint = this.aimPoint(player, target);
        if (target.isClimbing()) {
            aimPoint = aimPoint.add(AimAssistModule.predictClimb(target, target.getVelocity()));
        }
        Vec3d eye = player.getEyePos();
        Vec3d delta = aimPoint.subtract(eye).add((double)this.wanderX, 0.0, (double)this.wanderZ);
        Vec3d look = smooth ? this.smoothDelta(delta, dt, captured) : delta;
        Vec3d vec3d = look;
        if (!smooth) {
            this.smoothedDelta = null;
            this.onTargetHold = 0;
            this.onTargetScale = 1.0f;
        }
        Angle angle = Angle.fromDelta(look);
        float targetYaw = angle.getYaw();
        Box box = target.getBoundingBox();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        float maxPitch = (float)(-Math.toDegrees(Math.atan2(box.maxY - eye.y - 0.2, horizontal)));
        float minPitch = (float)(-Math.toDegrees(Math.atan2(box.minY - eye.y - 0.2, horizontal)));
        float targetPitch = MathHelper.clamp((float)angle.getPitch(), (float)maxPitch, (float)minPitch);
        float yawDelta = MathHelper.wrapDegrees((float)(targetYaw - yaw));
        float pitchDelta = MathHelper.wrapDegrees((float)(targetPitch - pitch));
        float yawCap = Math.max(1.0f, this.yawSpeed.getFloatValue()) * 2.8f;
        float pitchCap = Math.max(1.0f, this.pitchSpeed.getFloatValue()) * 2.2f;
        float yawFactor = AimAssistModule.expApproach(dt, 2.8f, MathHelper.clamp((float)(this.yawSpeed.getFloatValue() / 60.0f), (float)0.0f, (float)4.2f) * 4.4f);
        float pitchFactor = AimAssistModule.expApproach(dt, 2.4f, MathHelper.clamp((float)(this.pitchSpeed.getFloatValue() / 30.0f), (float)0.0f, (float)4.4f) * 3.8f);
        float yawStep = MathHelper.clamp((float)(yawDelta * yawFactor), (float)(-yawCap * dt), (float)(yawCap * dt));
        float pitchStep = MathHelper.clamp((float)(pitchDelta * pitchFactor), (float)(-pitchCap * dt), (float)(pitchCap * dt));
        if (helper && this.magnet.isEnabled()) {
            boolean onTarget = this.lookingAt(player, target, yaw, pitch);
            float[] magnet = this.magnetAim(yaw, pitch, yawDelta, pitchDelta, dt, captured, onTarget);
            nextYaw = magnet[0];
            nextPitch = magnet[1];
        } else if (capture) {
            if (captured) {
                this.captureYawStep = 0.0f;
                this.capturePitchStep = 0.0f;
            }
            float error = Math.abs(yawDelta) + Math.abs(pitchDelta) * 0.7f;
            float zone = Math.max(0.5f, this.captureZone.getFloatValue());
            float t = MathHelper.clamp((float)(error / zone), (float)0.0f, (float)1.0f);
            float speed = MathHelper.lerp((float)t, (float)Math.max(1.0f, this.captureSpeedNear.getFloatValue()), (float)Math.max(1.0f, this.captureSpeedFar.getFloatValue()));
            float capYaw = speed * 2.8f;
            float capPitch = speed * 2.2f;
            float captureYawFactor = AimAssistModule.expApproach(dt, 2.8f, MathHelper.clamp((float)(speed / 60.0f), (float)0.0f, (float)4.2f) * 4.4f);
            float capturePitchFactor = AimAssistModule.expApproach(dt, 2.4f, MathHelper.clamp((float)(speed / 30.0f), (float)0.0f, (float)4.4f) * 3.8f);
            float stepYaw = MathHelper.clamp((float)(yawDelta * captureYawFactor), (float)(-capYaw * dt), (float)(capYaw * dt));
            float stepPitch = MathHelper.clamp((float)(pitchDelta * capturePitchFactor), (float)(-capPitch * dt), (float)(capPitch * dt));
            stepYaw = stepYaw * 0.7f + this.captureYawStep * 0.3f;
            stepPitch = stepPitch * 0.7f + this.capturePitchStep * 0.3f;
            this.captureYawStep = stepYaw;
            this.capturePitchStep = stepPitch;
            nextYaw = yaw + stepYaw;
            nextPitch = pitch + stepPitch;
        } else if (assist) {
            float gain = this.assistGain(yawDelta, pitchDelta, mouseWeight);
            this.assistGain = captured ? gain : MathHelper.lerp((float)MathHelper.clamp((float)AimAssistModule.expApproach(dt, 0.0f, 16.0f), (float)0.12f, (float)0.5f), (float)this.assistGain, (float)gain);
            boolean onTarget = this.lookingAt(player, target, yaw, pitch);
            if (onTarget) {
                this.onTargetHold = 5;
            } else if (this.onTargetHold > 0) {
                --this.onTargetHold;
            }
            float scale = onTarget || this.onTargetHold > 0 ? this.onTargetShakeScale() : 1.0f;
            this.onTargetScale = captured ? scale : MathHelper.lerp((float)MathHelper.clamp((float)AimAssistModule.expApproach(dt, 0.0f, 8.0f), (float)0.04f, (float)0.22f), (float)this.onTargetScale, (float)scale);
            nextYaw = yaw + yawStep * this.onTargetScale * this.assistGain;
            nextPitch = pitch + pitchStep * this.onTargetScale * this.assistGain;
        } else {
            nextYaw = yaw + yawStep;
            nextPitch = pitch + pitchStep;
        }
        double frame = AimAssistModule.frameTime();
        nextYaw = this.quantize(yaw, nextYaw, frame, true);
        nextPitch = MathHelper.clamp((float)this.quantize(pitch, MathHelper.clamp((float)nextPitch, (float)-90.0f, (float)90.0f), frame, false), (float)-90.0f, (float)90.0f);
        player.setYaw(nextYaw);
        player.setPitch(nextPitch);
        this.lastYaw = nextYaw;
        this.lastPitch = nextPitch;
        this.applied = true;
    }

    private float updateMouse(float yaw, float pitch, float dt) {
        if (!this.applied) {
            this.mouseYaw = 0.0f;
            this.mousePitch = 0.0f;
            this.mouseActivity = 0.0f;
            return 0.0f;
        }
        this.mouseYaw = MathHelper.wrapDegrees((float)(yaw - this.lastYaw));
        this.mousePitch = MathHelper.clamp((float)(pitch - this.lastPitch), (float)-45.0f, (float)45.0f);
        float deadzone = (float)Math.max(0.004, AimAssistModule.frameTime() * 0.08);
        float motion = Math.abs(this.mouseYaw) + Math.abs(this.mousePitch) * 1.35f;
        float active = MathHelper.clamp((float)((motion - deadzone) / 0.62f), (float)0.0f, (float)1.0f);
        this.mouseActivity = MathHelper.lerp((float)MathHelper.clamp((float)AimAssistModule.expApproach(dt, 0.0f, 18.0f), (float)0.16f, (float)0.48f), (float)this.mouseActivity, (float)active);
        return this.mouseActivity;
    }

    private Vec3d smoothDelta(Vec3d delta, float dt, boolean captured) {
        if (captured || this.smoothedDelta == null) {
            this.smoothedDelta = delta;
            return delta;
        }
        float t = MathHelper.clamp((float)AimAssistModule.expApproach(dt, 0.0f, 13.0f), (float)0.06f, (float)0.28f);
        this.smoothedDelta = this.smoothedDelta.add(delta.subtract(this.smoothedDelta).multiply((double)t));
        return this.smoothedDelta;
    }

    private float frameDelta() {
        long now = System.nanoTime();
        if (this.lastFrameNanos == 0L) {
            this.lastFrameNanos = now;
            return 0.016666668f;
        }
        float dt = (float)(now - this.lastFrameNanos) / 1.0E9f;
        this.lastFrameNanos = now;
        return MathHelper.clamp((float)dt, (float)0.004166667f, (float)0.033333335f);
    }

    private void resetAssist() {
        this.wanderTargetId = -1;
        this.wanderX = 0.0f;
        this.wanderZ = 0.0f;
        this.trackedId = -1;
        this.onTargetHold = 0;
        this.onTargetScale = 1.0f;
        this.assistGain = 0.36f;
        this.captureYawStep = 0.0f;
        this.capturePitchStep = 0.0f;
        this.smoothedDelta = null;
        this.shakeTargetId = -1;
        this.nextShakeNanos = 0L;
        this.shakeYaw = 0.0f;
        this.shakePitch = 0.0f;
        this.magnetLock = 0.0f;
        this.magnetAmount = 0.0f;
        this.lastYaw = 0.0f;
        this.lastPitch = 0.0f;
        this.mouseYaw = 0.0f;
        this.mousePitch = 0.0f;
        this.mouseActivity = 0.0f;
        this.applied = false;
        this.yawQuantError = 0.0f;
        this.pitchQuantError = 0.0f;
    }

    private float onTargetShakeScale() {
        return MathHelper.clamp((float)(MathHelper.clamp((float)this.shakeStrength.getFloatValue(), (float)0.0f, (float)10.0f) / 10.0f * 0.22f), (float)0.0f, (float)0.22f);
    }

    private static float expApproach(float dt, float base, float extra) {
        return 1.0f - (float)Math.exp(-dt * (base + extra));
    }

    private float quantize(float from, float to, double step, boolean yawAxis) {
        if (step <= 0.0) {
            return to;
        }
        float error = yawAxis ? this.yawQuantError : this.pitchQuantError;
        float delta = to - from + error;
        float snapped = (float)((double)Math.round((double)delta / step) * step);
        float leftover = MathHelper.clamp((float)(delta - snapped), (float)((float)(-step)), (float)((float)step));
        if (yawAxis) {
            this.yawQuantError = leftover;
        } else {
            this.pitchQuantError = leftover;
        }
        return from + snapped;
    }

    private float assistGain(float yawDelta, float pitchDelta, float mouseWeight) {
        float mouse = Math.abs(this.mouseYaw) + Math.abs(this.mousePitch) * 1.35f;
        if (mouse <= 0.002f) {
            return 0.36f;
        }
        float aligned = this.mouseYaw * yawDelta + this.mousePitch * pitchDelta * 0.72f;
        float step = Math.max(0.001f, Math.abs(yawDelta) + Math.abs(pitchDelta) * 0.72f);
        float match = MathHelper.clamp((float)(aligned / Math.max(0.001f, mouse * step)), (float)-1.0f, (float)1.0f);
        float scale = match > 0.0f ? MathHelper.lerp((float)match, (float)0.62f, (float)1.0f) : 0.3f;
        return MathHelper.clamp((float)((0.42f + mouseWeight * 0.86f) * scale), (float)0.24f, (float)1.22f);
    }

    private static double effectiveGravity(LivingEntity entity) {
        double gravity = 0.08;
        boolean falling = entity.getVelocity().y <= 0.0;
        return falling && entity.hasStatusEffect(StatusEffects.SLOW_FALLING) ? Math.min(gravity, 0.01) : gravity;
    }

    private boolean lookingAt(ClientPlayerEntity player, LivingEntity entity, float yaw, float pitch) {
        float range = (float)Math.min((double)this.rotationRange.getFloatValue(), Math.max((double)this.attackRange.getFloatValue(), (double)player.distanceTo((Entity)entity) + 0.75));
        if (ReachHelper.raycastEntity((Entity)entity, new Angle(yaw, pitch), range, this.throughWalls.isEnabled()) != null) {
            return true;
        }
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(Vec3d.fromPolar((float)pitch, (float)yaw).multiply((double)range));
        return entity.getBoundingBox().expand(0.08, 0.22, 0.08).raycast(eye, end).isPresent();
    }

    private void selectTarget() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && !player.isAlive()) {
            this.enabledSetting.setEnabled(false);
        }
        if (!this.enabledSetting.isEnabled() || player == null || MinecraftClient.getInstance().world == null) {
            this.targetSelector.update();
            this.target = null;
            return;
        }
        if (!this.focusOne.isEnabled()) {
            this.targetSelector.update();
        }
        TargetFilter filter = new TargetFilter(this.targets.getSelectedOptions(), "Legit", this.fov.getIntValue());
        this.target = this.targetSelector.selectTarget(filter, this.rotationRange.getFloatValue(), this.sorting.getSelectedOption(), this.throughWalls.isEnabled());
    }

    private Vec3d aimPoint(ClientPlayerEntity player, LivingEntity entity) {
        Box box = entity.getBoundingBox();
        Vec3d center = box.getCenter();
        Vec3d eye = player.getEyePos();
        Vec3d offset = new Vec3d(center.x - eye.x, 0.0, center.z - eye.z);
        Vec3d side = offset.lengthSquared() > 1.0E-5 ? new Vec3d(-offset.z, 0.0, offset.x).normalize() : Vec3d.ZERO;
        float strength = MathHelper.clamp((float)(this.shakeStrength.getFloatValue() / 10.0f), (float)0.0f, (float)1.0f);
        float wobble = (float)Math.sin((double)System.nanoTime() * 1.0E-9 * 1.35 + (double)((float)entity.getId() * 0.37f)) * 0.006f * strength;
        double y = box.minY + (double)(entity.getHeight() * MathHelper.clamp((float)this.aimHeight.getFloatValue(), (float)0.18f, (float)0.92f)) + (double)this.shakePitch;
        return new Vec3d(center.x + side.x * (double)(this.shakeYaw + wobble), MathHelper.clamp((double)y, (double)(box.minY + 0.18), (double)(box.maxY - 0.08)), center.z + side.z * (double)(this.shakeYaw + wobble));
    }

    private void updateWander(float dt, LivingEntity entity) {
        int id = entity.getId();
        if (this.wanderTargetId != id) {
            this.wanderX = 0.0f;
            this.wanderZ = 0.0f;
            this.wanderTargetId = id;
        }
        float strength = MathHelper.clamp((float)this.shakeStrength.getFloatValue(), (float)0.0f, (float)10.0f);
        if (strength <= 0.001f) {
            this.wanderX = 0.0f;
            this.wanderZ = 0.0f;
            return;
        }
        double time = (double)System.nanoTime() * 1.0E-9;
        float phase = (float)(id & 0x3FF) * 0.173f;
        float amplitude = strength * 0.013f;
        float x = (float)(Math.sin(time * 1.47 + (double)phase) + Math.sin(time * 2.13 + (double)(phase * 0.41f)) * (double)0.32f) * amplitude;
        float z = (float)(Math.cos(time * 1.31 + (double)(phase * 0.7f)) + Math.sin(time * 1.91 + (double)phase + (double)1.4f) * (double)0.28f) * amplitude;
        float t = MathHelper.clamp((float)AimAssistModule.expApproach(dt, 0.0f, 5.2f), (float)0.025f, (float)0.16f);
        this.wanderX = MathHelper.lerp((float)t, (float)this.wanderX, (float)MathHelper.clamp((float)x, (float)(-amplitude), (float)amplitude));
        this.wanderZ = MathHelper.lerp((float)t, (float)this.wanderZ, (float)MathHelper.clamp((float)z, (float)(-amplitude), (float)amplitude));
    }

    private float[] magnetAim(float yaw, float pitch, float yawDelta, float pitchDelta, float dt, boolean captured, boolean onTarget) {
        if (captured) {
            this.magnetLock = 0.0f;
            this.magnetAmount = 0.0f;
        }
        float mouse = Math.abs(this.mouseYaw) + Math.abs(this.mousePitch);
        float deadzone = (float)Math.max(0.02, AimAssistModule.frameTime() * 0.5);
        if (mouse < deadzone) {
            this.magnetAmount *= 0.6f;
            return new float[]{yaw, pitch};
        }
        float absYaw = Math.abs(yawDelta);
        float absPitch = Math.abs(pitchDelta);
        float error = absYaw + absPitch * 0.62f;
        float fov = MathHelper.clamp((float)(this.fov.getFloatValue() * 0.5f), (float)24.0f, (float)180.0f);
        if (!onTarget && error > fov) {
            this.magnetAmount *= 0.6f;
            return new float[]{yaw, pitch};
        }
        float step = Math.max(0.001f, absYaw + absPitch);
        float aligned = this.mouseYaw * yawDelta + this.mousePitch * pitchDelta;
        float match = MathHelper.clamp((float)(aligned / (mouse * step)), (float)0.0f, (float)1.0f);
        float lock = onTarget ? MathHelper.clamp((float)(1.0f - error / 8.5f), (float)0.35f, (float)1.0f) : 0.0f;
        this.magnetLock = MathHelper.lerp((float)MathHelper.clamp((float)AimAssistModule.expApproach(dt, 0.0f, onTarget ? 7.5f : 4.2f), (float)0.02f, (float)(onTarget ? 0.2f : 0.12f)), (float)this.magnetLock, (float)lock);
        float strength = MathHelper.clamp((float)this.magnetStrength.getFloatValue(), (float)0.1f, (float)2.5f);
        float yawScale = MathHelper.clamp((float)(this.yawSpeed.getFloatValue() / 60.0f), (float)0.5f, (float)3.0f);
        float wanted = MathHelper.clamp((float)(0.42f * yawScale * strength), (float)0.0f, (float)0.95f) * match * MathHelper.lerp((float)this.magnetLock, (float)1.0f, (float)0.45f);
        float approach = wanted > this.magnetAmount ? MathHelper.clamp((float)AimAssistModule.expApproach(dt, 0.0f, 42.0f), (float)0.2f, (float)0.85f) : MathHelper.clamp((float)AimAssistModule.expApproach(dt, 0.0f, 12.0f), (float)0.08f, (float)0.4f);
        this.magnetAmount = MathHelper.lerp((float)approach, (float)this.magnetAmount, (float)wanted);
        float addYaw = yawDelta * this.magnetAmount;
        float addPitch = pitchDelta * this.magnetAmount * 0.92f;
        float clampScale = MathHelper.clamp((float)(0.85f * strength), (float)0.3f, (float)1.7f);
        float yawLimit = Math.abs(this.mouseYaw) * clampScale;
        float pitchLimit = Math.abs(this.mousePitch) * clampScale + Math.abs(this.mouseYaw) * 0.12f;
        addYaw = AimAssistModule.clampToDelta(MathHelper.clamp((float)addYaw, (float)(-yawLimit), (float)yawLimit), yawDelta);
        addPitch = AimAssistModule.clampToDelta(MathHelper.clamp((float)addPitch, (float)(-pitchLimit), (float)pitchLimit), pitchDelta);
        return new float[]{yaw + addYaw, pitch + addPitch};
    }

    private static float clampToDelta(float value, float delta) {
        float abs = Math.abs(delta);
        return abs <= 0.001f ? 0.0f : MathHelper.clamp((float)value, (float)(-abs), (float)abs);
    }

    private boolean captureTarget(int id) {
        if (this.trackedId == id) {
            return false;
        }
        this.trackedId = id;
        this.onTargetHold = 0;
        this.onTargetScale = 1.0f;
        this.smoothedDelta = null;
        this.magnetLock = 0.0f;
        this.magnetAmount = 0.0f;
        return true;
    }

    private static double frameTime() {
        return MinecraftClient.getInstance().getRenderTickCounter().getDynamicDeltaTicks();
    }
}

