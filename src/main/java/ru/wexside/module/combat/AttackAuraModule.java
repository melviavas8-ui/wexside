package ru.wexside.module.combat;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.Vec3d;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.AttackWindowEvent;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.event.OutgoingPacketEvent;
import ru.wexside.event.PreAttackEvent;
import ru.wexside.misc.AttackOptions;
import ru.wexside.misc.AttackUrgency;
import ru.wexside.misc.CorrectionMode;
import ru.wexside.misc.HotbarTracker;
import ru.wexside.misc.RaycastMode;
import ru.wexside.misc.ReachHelper;
import ru.wexside.misc.RotationApplyResult;
import ru.wexside.misc.ShieldBreaker;
import ru.wexside.misc.SprintReset;
import ru.wexside.misc.SprintResetMode;
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
import ru.wexside.util.AimCalculator;
import ru.wexside.util.Angle;
import ru.wexside.util.AuraProcessor;
import ru.wexside.util.CriticalsHandler;
import ru.wexside.util.RotationController;
import ru.wexside.util.RotationIntent;
import ru.wexside.util.TargetSelector;

public class AttackAuraModule
extends Module
implements ConfigSerializable {
    private static volatile AttackAuraModule instance;
    private final BooleanSetting enabledSetting;
    private final NumberSetting range;
    private final NumberSetting additionalRange;
    private final MultiSelectSetting targets;
    private final ModeSetting sorting;
    private final ModeSetting rotationMode;
    private final NumberSetting fov;
    private final ModeSetting attackMode;
    private final NumberSetting cps;
    private final ModeSetting earlyHit;
    private final NumberSetting accuracy;
    private final ModeSetting correctionMode;
    private final ModeSetting sprintReset;
    private final BooleanSetting noSprintNear;
    private final NumberSetting noSprintDistance;
    private final BooleanSetting ignoreWhileUsing;
    private final BooleanSetting throughWalls;
    private final BooleanSetting criticals;
    private final BooleanSetting jumpOnly;
    private final NumberSetting maceFallDistance;
    private final BooleanSetting swordOnly;
    private final BooleanSetting breakShield;
    private final BooleanSetting desyncShield;
    private final BooleanSetting tpsSync;
    private final HotbarTracker hotbarTracker = new HotbarTracker();
    private final Map<Integer, Integer> hitDelayHistogram = new HashMap<Integer, Integer>();
    private TargetSelector targetSelector;
    private AimCalculator aimCalculator;
    private CriticalsHandler criticalsHandler;
    private AuraProcessor auraProcessor;
    private LivingEntity target;
    private boolean rotationReady;
    private boolean adaptiveReady;
    private boolean calibratingAdaptive;
    private boolean heardHitSound;
    private int silenceTicks;
    private int quietTicks;
    private int calibrationTicks;
    private int learnedHitDelay;
    private int packetsThisWindow;

    public AttackAuraModule(EventBus eventBus) {
        super(eventBus, "attack_aura", "Attack Aura", "Автоматически бьёт игроков вокруг", ModuleCategory.valueOf("COMBAT"), new String[0]);
        MultiSelectSetting targetsSetting;
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Test killaura module").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.range = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 12.0).defaultValue(3.0).multiplier(1.0).precision(0).animationSpeed(20.0f).showMarkers().name("Максимальное расстояние").id("range").description("Максимальная дистанция для атаки")).build();
        this.registerSetting(this.range);
        this.additionalRange = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 6.0).defaultValue(2.0).multiplier(1.0).precision(0).animationSpeed(20.0f).showMarkers().name("Дополнительное расстояние").id("additional_range").description("Дополнительная дистанция поиска целей и вращения")).build();
        this.registerSetting(this.additionalRange);
        this.targets = targetsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Players", "Friends", "Bots", "Naked", "Invisibles", "Animals", "Mobs", "Villagers").selectAll(false).optionListEnabled(false).name("Цели").id("targets").description("Типы целей для атаки").withKeybind()).build();
        this.registerSetting(targetsSetting);
        this.sorting = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Health", "Distance", "Crosshair").defaultOption("Distance").name("Сортировка").id("sorting").description("Сортировка целей для атаки").withKeybind()).build();
        this.registerSetting(this.sorting);
        this.rotationMode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Simple", "FT Snap", "RW", "Spooky", "Spooky Test").defaultOption("Simple").name("Режим").id("mode").description("Режим вращения").withKeybind()).build();
        this.registerSetting(this.rotationMode);
        this.fov = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 360.0).defaultValue(180.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("FOV").id("fov").description("Поле зрения для поиска цели").visibleWhen(() -> "FT Snap".equals(this.rotationMode.getSelectedOption()))).build();
        this.registerSetting(this.fov);
        this.attackMode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("1.9+", "1.8").defaultOption("1.9+").name("Режим атаки").id("attack_mode").description("Режим атаки").withKeybind()).build();
        this.registerSetting(this.attackMode);
        this.cps = ((NumberSettingBuilder)NumberSetting.builder().range(5.0, 20.0).defaultValue(15.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("CPS").id("cps").description("Количество кликов в секунду").visibleWhen(() -> "1.8".equals(this.attackMode.getSelectedOption()))).build();
        this.registerSetting(this.cps);
        this.earlyHit = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Всегда", "Адаптивный", "Отключен").defaultOption("Адаптивный").name("Ранний удар").id("early_hit").description("Модуль бьёт раньше ванильного удара\nРекомендуется адаптивный режим, всегда - на страх и риск").withKeybind()).build();
        this.registerSetting(this.earlyHit);
        this.accuracy = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 100.0).defaultValue(100.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Точность").id("accuracy").description("Шанс попасть при ударе")).build();
        this.registerSetting(this.accuracy);
        this.correctionMode = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Focused", "Free", "Lock").defaultOption("Focused").name("Режим коррекции").id("correction_mode").description("Режим коррекции движения").withKeybind()).build();
        this.registerSetting(this.correctionMode);
        this.sprintReset = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Packet", "Semi-Legit", "Legit").defaultOption("Semi-Legit").name("Сброс спринта").id("sprint_reset").description("Режим сброса спринта").withKeybind()).build();
        this.registerSetting(this.sprintReset);
        this.noSprintNear = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Без спринта у цели").id("no_sprint_near").description("Отключение спринта рядом с целью").withKeybind().visibleWhen(() -> "Legit".equals(this.sprintReset.getSelectedOption()))).build();
        this.registerSetting(this.noSprintNear);
        this.noSprintDistance = ((NumberSettingBuilder)NumberSetting.builder().range(0.0, 5.0).defaultValue(3.0).multiplier(1.0).precision(1).animationSpeed(20.0f).showMarkers().snapTo(0.5).name("Дистанция без спринта").id("no_sprint_distance").description("Расстояние до цели для отключения спринта").visibleWhen(() -> "Legit".equals(this.sprintReset.getSelectedOption()) && this.noSprintNear.isEnabled())).build();
        this.registerSetting(this.noSprintDistance);
        this.ignoreWhileUsing = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Игнор при использовании").id("ignore_while_using").description("Отключить модуль, пока вы используете предмет").withKeybind()).build();
        this.registerSetting(this.ignoreWhileUsing);
        this.throughWalls = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Сквозь стены").id("through_walls").description("Позволяет атаковать сквозь стены").withKeybind()).build();
        this.registerSetting(this.throughWalls);
        this.criticals = ((BooleanSettingBuilder)BooleanSetting.builder().value(true).defaultValue(false).name("Криты").id("criticals").description("Атака только критическим уроном").withKeybind()).build();
        this.registerSetting(this.criticals);
        this.jumpOnly = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Только при прыжке").id("jump_only").description("Наносит критический урон только\nпри зажатой кнопке прыжка").withKeybind().visibleWhen(() -> this.criticals.isEnabled())).build();
        this.registerSetting(this.jumpOnly);
        this.maceFallDistance = ((NumberSettingBuilder)NumberSetting.builder().range(1.5, 8.0).defaultValue(1.6).multiplier(1.0).precision(1).animationSpeed(20.0f).snapTo(0.1).name("Булава: мин. падение").id("mace_fall_distance").description("Дистанция падения, с которой булава бьёт смэшем")).build();
        this.registerSetting(this.maceFallDistance);
        this.swordOnly = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Только меч").id("sword_only").description("Бить только мечом").withKeybind()).build();
        this.registerSetting(this.swordOnly);
        this.breakShield = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Ломать щит").id("break_shield").description("Ломать щиты противников").withKeybind()).build();
        this.registerSetting(this.breakShield);
        this.desyncShield = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Десинхр. щита").id("desync_shield").description("Десинхронизация щита").withKeybind()).build();
        this.registerSetting(this.desyncShield);
        this.tpsSync = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("TPS Sync").id("tps_sync").description("Синхронизирует кулдаун с TPS сервера").withKeybind()).build();
        this.registerSetting(this.tpsSync);
    }

    @Override
    protected void initialize() {
        this.targetSelector = new TargetSelector();
        this.aimCalculator = new AimCalculator();
        this.criticalsHandler = new CriticalsHandler();
        this.auraProcessor = new AuraProcessor(this.criticalsHandler);
        this.listen(ClientTickEvent.class, event -> this.onClientTick());
        this.listen(IncomingPacketEvent.class, this::onIncomingPacket);
        this.listen(OutgoingPacketEvent.class, this::onOutgoingPacket);
        this.listen(AttackWindowEvent.class, this::onAttackWindow);
        this.listen(PreAttackEvent.class, this::onPreAttack);
    }

    public boolean isActive() {
        return this.enabledSetting.isEnabled();
    }

    public LivingEntity getLivingEntity() {
        return this.target;
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public static boolean isActive4() {
        AttackAuraModule module = instance;
        return module != null && module.enabledSetting.isEnabled() && module.target != null;
    }

    public static boolean hasTarget() {
        return AttackAuraModule.isActive4();
    }

    private void onClientTick() {
        LivingEntity selected;
        this.criticalsHandler.tick();
        this.auraProcessor.tick();
        SprintReset.setActive(false);
        ShieldBreaker.reset();
        RotationController rotations = WexSideClient.getRotationController();
        if (rotations == null) {
            return;
        }
        if (!this.enabledSetting.isEnabled() || MinecraftClient.getInstance().player == null) {
            this.targetSelector.reset();
            this.clearTarget();
            this.stopRotations(rotations);
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        TargetFilter filter = new TargetFilter(this.targets.getSelectedOptions(), this.sorting.getSelectedOption(), this.fov.getIntValue());
        float searchRange = this.range.getFloatValue() + this.additionalRange.getFloatValue();
        if (this.auraProcessor.getMaceCheck().isHoldingMace(player)) {
            searchRange = Math.max(searchRange, this.range.getFloatValue() + 1.5f);
        }
        if ((selected = this.targetSelector.findTarget(filter, searchRange, this.sorting.getSelectedOption(), this.throughWalls.isEnabled())) == null) {
            this.clearTarget();
            this.holdIdleRotation(rotations);
            return;
        }
        this.tryBreakShield(player);
        SprintReset.setActive(this.shouldBlockSprint(selected));
        Vec3d aimPoint = this.aimCalculator.calculateAimPoint(selected, this.currentLook(), this.range.getFloatValue(), searchRange, this.throughWalls.isEnabled());
        if (aimPoint == null) {
            this.clearTarget();
            this.holdIdleRotation(rotations);
            return;
        }
        Vec3d eyes = MinecraftClient.getInstance().player.getEyePos();
        Angle targetAngle = Angle.fromVectors(eyes, aimPoint);
        CorrectionMode correction = CorrectionMode.fromName(this.correctionMode.getSelectedOption());
        AttackUrgency urgency = this.resolveAttackUrgency(selected, targetAngle);
        RotationIntent intent = new RotationIntent(selected, aimPoint, targetAngle, urgency, correction, false);
        RotationApplyResult result = rotations.apply(intent, this.rotationMode.getSelectedOption());
        this.target = selected;
        this.rotationReady = result.isReady();
        if (urgency == AttackUrgency.HIT && this.rotationReady) {
            rotations.markAttacked();
        }
    }

    private void tryAttack(boolean ready) {
        if (!this.enabledSetting.isEnabled() || this.target == null) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        if (this.ignoreWhileUsing.isEnabled() && MinecraftClient.getInstance().options.useKey.isPressed()) {
            return;
        }
        if (this.swordOnly.isEnabled() && !this.isHoldingSword()) {
            return;
        }
        this.auraProcessor.getHitCooldown().setAttackRequested(ready);
        this.auraProcessor.getHitCooldown().setTpsSync(this.tpsSync.isEnabled());
        Angle look = WexSideClient.getRotationController().getAngle();
        if (look == null) {
            return;
        }
        AttackOptions options = new AttackOptions(look, this.range.getFloatValue(), this.cps.getIntValue(), true, true, SprintResetMode.fromName(this.sprintReset.getSelectedOption()), this.criticals.isEnabled(), this.throughWalls.isEnabled() ? RaycastMode.THROUGH_WALLS : RaycastMode.VISIBLE, "1.8".equals(this.attackMode.getSelectedOption()), this.accuracy.getValue(), this.breakShield.isEnabled(), this.desyncShield.isEnabled(), this.jumpOnly.isEnabled(), (float)this.maceFallDistance.getValue());
        if (this.auraProcessor.attack(this.target, options)) {
            WexSideClient.getRotationController().notifyHit();
        }
    }

    private void onAttackWindow(AttackWindowEvent event) {
        String mode;
        if (event.isPre()) {
            this.updateAdaptiveWindow();
        }
        if ("Отключен".equals(mode = this.earlyHit.getSelectedOption())) {
            if (event.isPre()) {
                this.tryAttack(true);
            }
            return;
        }
        if ("Всегда".equals(mode)) {
            this.tryAttack(!event.isPost());
            return;
        }
        if (this.isAdaptiveEarlyHit()) {
            this.tryAttack(event.isPre());
        } else {
            this.tryAttack(!event.isPost());
        }
    }

    private boolean isAdaptiveEarlyHit() {
        return "Адаптивный".equals(this.earlyHit.getSelectedOption()) && this.adaptiveReady;
    }

    private void updateAdaptiveWindow() {
        boolean wasReady = this.adaptiveReady;
        this.tickAdaptiveReady();
        if (this.isAdaptiveEarlyHit()) {
            if (!wasReady) {
                this.calibratingAdaptive = true;
                this.calibrationTicks = 0;
                this.learnedHitDelay = 0;
                this.hitDelayHistogram.clear();
            } else if (this.calibratingAdaptive) {
                this.hitDelayHistogram.merge(this.packetsThisWindow, 1, Integer::sum);
                if (++this.calibrationTicks >= 40) {
                    this.learnedHitDelay = this.mostCommonHitDelay();
                    this.calibratingAdaptive = false;
                }
            }
            this.packetsThisWindow = 0;
            this.auraProcessor.getHitCooldown().setVanillaHitsAllowed(false);
        } else {
            this.packetsThisWindow = 0;
            this.auraProcessor.getHitCooldown().setVanillaHitsAllowed(true);
        }
    }

    private void tickAdaptiveReady() {
        if (!this.adaptiveReady) {
            if (this.heardHitSound) {
                this.quietTicks = 0;
                if (++this.silenceTicks >= 50) {
                    this.adaptiveReady = true;
                }
            } else if (++this.quietTicks >= 4) {
                this.silenceTicks = 0;
            }
        }
        this.heardHitSound = false;
    }

    private void onIncomingPacket(IncomingPacketEvent event) {
        Packet<?> packet;
        if (event.getPacket() instanceof HealthUpdateS2CPacket) {
            this.criticalsHandler.getVelocityTracker().reset();
        }
        if ((packet = event.getPacket()) instanceof EntityVelocityUpdateS2CPacket) {
            EntityVelocityUpdateS2CPacket velocityPacket = (EntityVelocityUpdateS2CPacket)packet;
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null && velocityPacket.getEntityId() == player.getId()) {
                this.criticalsHandler.getVelocityTracker().setYVelocity(velocityPacket.getVelocity().y);
            }
        }
        if (event.getPacket() instanceof PlaySoundS2CPacket) {
            this.heardHitSound = true;
        } else if (event.getPacket() instanceof GameJoinS2CPacket) {
            this.criticalsHandler.getVelocityTracker().reset();
            this.adaptiveReady = false;
            this.silenceTicks = 0;
            this.quietTicks = 0;
            this.heardHitSound = false;
            this.calibratingAdaptive = false;
            this.calibrationTicks = 0;
            this.learnedHitDelay = 0;
            this.packetsThisWindow = 0;
            this.hitDelayHistogram.clear();
            this.auraProcessor.getHitCooldown().setVanillaHitsAllowed(true);
        }
    }

    private void onOutgoingPacket(OutgoingPacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround) {
            PlayerMoveC2SPacket.LookAndOnGround lookAndMove = (PlayerMoveC2SPacket.LookAndOnGround)packet;
            this.criticalsHandler.getVelocityTracker().onMovementPacket(lookAndMove);
        }
        if (!(event.getPacket() instanceof PlayerMoveC2SPacket)) {
            return;
        }
        if (!this.isAdaptiveEarlyHit()) {
            return;
        }
        ++this.packetsThisWindow;
        if (!this.calibratingAdaptive && this.learnedHitDelay > 0 && this.packetsThisWindow >= this.learnedHitDelay) {
            this.auraProcessor.getHitCooldown().setVanillaHitsAllowed(true);
        }
    }

    private void onPreAttack(PreAttackEvent event) {
        RotationController rotations = WexSideClient.getRotationController();
        if (this.enabledSetting.isEnabled() && rotations != null && rotations.ticksSinceHit() < 4) {
            event.cancel();
        }
        if (!event.isCancelled()) {
            this.criticalsHandler.onAttack();
        }
    }

    private AttackUrgency resolveAttackUrgency(LivingEntity selected, Angle targetAngle) {
        boolean cooldownReady;
        if (!"FT Snap".equals(this.rotationMode.getSelectedOption())) {
            return AttackUrgency.SKIP;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return AttackUrgency.SKIP;
        }
        if (this.auraProcessor.getMaceCheck().isHoldingMace(player)) {
            if (!this.auraProcessor.getHitCooldown().isActive()) {
                return AttackUrgency.SKIP;
            }
            if (ReachHelper.raycastEntity((Entity)selected, targetAngle, this.range.getFloatValue(), this.throughWalls.isEnabled()) != selected) {
                return AttackUrgency.SKIP;
            }
            boolean falling = !player.isOnGround() && !player.isClimbing() && player.fallDistance > 0.0;
            return falling ? AttackUrgency.HIT : AttackUrgency.SKIP;
        }
        boolean bl = cooldownReady = "1.8".equals(this.attackMode.getSelectedOption()) || player.getAttackCooldownProgress(0.5f) >= 0.9f;
        if (!cooldownReady) {
            return AttackUrgency.SKIP;
        }
        if (!this.auraProcessor.getHitCooldown().isActive()) {
            return AttackUrgency.SKIP;
        }
        if (ReachHelper.raycastEntity((Entity)selected, targetAngle, this.range.getFloatValue(), this.throughWalls.isEnabled()) != selected) {
            return AttackUrgency.SKIP;
        }
        RaycastMode raycastMode = this.throughWalls.isEnabled() ? RaycastMode.THROUGH_WALLS : RaycastMode.VISIBLE;
        return this.criticalsHandler.passesCriticalRequirement(this.criticals.isEnabled(), raycastMode) ? AttackUrgency.HIT : AttackUrgency.SKIP;
    }

    private boolean shouldBlockSprint(LivingEntity selected) {
        return this.shouldStopSprintNearTarget(selected) || this.shouldStopSprintForMace(selected) || this.shouldStopSprintForShieldBreak(selected);
    }

    private boolean shouldStopSprintNearTarget(LivingEntity selected) {
        if (!this.noSprintNear.isEnabled()) {
            return false;
        }
        if (!"Legit".equals(this.sprintReset.getSelectedOption())) {
            return false;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && TargetSelector.distanceTo(selected) <= (double)this.noSprintDistance.getFloatValue();
    }

    private boolean shouldStopSprintForMace(LivingEntity selected) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || !this.auraProcessor.getMaceCheck().isHoldingMace(player)) {
            return false;
        }
        return TargetSelector.distanceTo(selected) <= (double)(this.range.getFloatValue() + 1.5f);
    }

    private boolean shouldStopSprintForShieldBreak(LivingEntity selected) {
        if (!"Legit".equals(this.sprintReset.getSelectedOption()) || !ShieldBreaker.isActive()) {
            return false;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }
        if (!this.auraProcessor.getHitCooldown().isActive()) {
            return false;
        }
        return !(TargetSelector.distanceTo(selected) > (double)this.range.getFloatValue());
    }

    private void tryBreakShield(ClientPlayerEntity player) {
        ItemStack predicted;
        if (!"Legit".equals(this.sprintReset.getSelectedOption()) && !this.auraProcessor.getMaceCheck().isHoldingMace(player)) {
            return;
        }
        try {
            predicted = this.hotbarTracker.simulate(1);
        }
        catch (Throwable ignored) {
            return;
        }
        if (predicted == null) {
            return;
        }
        int maxUseTime = predicted.getMaxUseTime((LivingEntity)player);
        double useProgress = maxUseTime > 0 ? (double)player.getItemUseTime() / (double)maxUseTime : 0.0;
        ShieldBreaker.consider(player.isBlocking(), useProgress);
    }

    private void stopRotations(RotationController rotations) {
        if (rotations.hasRotation()) {
            rotations.reset();
        }
    }

    private void holdIdleRotation(RotationController rotations) {
        if (!rotations.hasRotation()) {
            return;
        }
        rotations.apply(RotationIntent.empty(), this.rotationMode.getSelectedOption());
    }

    private void clearTarget() {
        this.target = null;
        this.rotationReady = false;
    }

    private Angle currentLook() {
        Angle rotation = WexSideClient.getRotationController().getAngle();
        if (rotation != null) {
            return rotation;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null ? new Angle(player.getYaw(), player.getPitch()) : Angle.ZERO;
    }

    private boolean isHoldingSword() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && player.getMainHandStack().isIn(ItemTags.SWORDS);
    }

    private int mostCommonHitDelay() {
        int delay = 0;
        int bestCount = -1;
        for (Map.Entry<Integer, Integer> entry : this.hitDelayHistogram.entrySet()) {
            int packets = entry.getKey();
            int count = entry.getValue();
            if (count < bestCount || count == bestCount && packets <= delay) continue;
            delay = packets;
            bestCount = count;
        }
        return delay;
    }
}

