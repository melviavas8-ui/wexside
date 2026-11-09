package ru.wexside.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.event.OutgoingPacketEvent;
import ru.wexside.misc.RotationApplyResult;
import ru.wexside.misc.RotationStrategy;
import ru.wexside.misc.RotationStrategyFactory;
import ru.wexside.util.Angle;
import ru.wexside.util.RotationIntent;
import ru.wexside.util.RotationState;

public class RotationController {
    static final int CORRECTION_GRACE_TICKS = 4;
    private float viewPitch = 90.0f;
    private boolean rotationEnabled;
    private Angle previousAngle;
    private RotationStrategy strategy;
    private int correctionGraceTicks;
    private String strategyName;
    private float viewYaw = 90.0f;
    private Angle tickStartAngle;
    private Angle appliedAngle;
    private float observedPitch = 90.0f;
    private boolean viewAnglesInitialized = true;
    private Angle serverAngle;
    private int ticksSinceAttack;
    private boolean observedPitchValid = true;
    private int ticksSinceHit;
    private RotationIntent currentIntent = RotationIntent.empty();
    private final RotationStrategyFactory strategyFactory;
    private boolean viewAnglesRefreshed;

    private void onOutgoingLookPacket(OutgoingPacketEvent gameEvent12) {
        PlayerMoveC2SPacket.LookAndOnGround lookAndOnGround;
        Packet<?> packet2 = gameEvent12.getPacket();
        if (packet2 instanceof PlayerMoveC2SPacket.LookAndOnGround && (lookAndOnGround = (PlayerMoveC2SPacket.LookAndOnGround)packet2).changesLook()) {
            this.serverAngle = new Angle(lookAndOnGround.getYaw(0.0f), lookAndOnGround.getPitch(0.0f));
        }
    }

    public float getViewPitch() {
        this.refreshViewAngles();
        return this.viewPitch;
    }

    public Float getViewYaw() {
        if (this.correctionGraceTicks > 0) {
            return null;
        }
        this.refreshViewAngles();
        return this.viewAnglesInitialized ? Float.valueOf(this.viewYaw) : null;
    }

    public boolean isActive() {
        return this.rotationEnabled && this.appliedAngle != null;
    }

    public void resetAttackTicks() {
        this.ticksSinceAttack = 0;
    }

    public boolean isAvailable() {
        return this.appliedAngle != null;
    }

    public void resetHitTicks() {
        this.ticksSinceHit = 0;
    }

    public Angle getAngle() {
        return this.appliedAngle;
    }

    public int getHitTickCount() {
        return this.ticksSinceHit;
    }

    public void clearIntent() {
        if (this.strategy != null) {
            RotationState iIiIlIIliI2 = this.createRotationState(this.currentIntent);
            this.strategy.onDeactivated(iIiIlIIliI2);
        }
        this.appliedAngle = null;
        this.previousAngle = null;
        this.tickStartAngle = null;
        this.currentIntent = RotationIntent.empty();
    }

    public RotationApplyResult apply(RotationIntent intent, String rotationMode) {
        return this.applyIntent(intent, rotationMode);
    }

    public void reset() {
        this.clearIntent();
    }

    public void markAttacked() {
        this.resetAttackTicks();
    }

    public void notifyHit() {
        this.resetHitTicks();
    }

    public boolean hasRotation() {
        return this.isAvailable();
    }

    public int ticksSinceHit() {
        return this.getHitTickCount();
    }

    public RotationApplyResult applyIntent(RotationIntent intent2, String string) {
        this.selectStrategy(string);
        this.currentIntent = intent2;
        RotationState iIiIlIIliI2 = this.createRotationState(intent2);
        RotationApplyResult rotationApplyResult = this.strategy.applyRotation(iIiIlIIliI2, intent2);
        this.previousAngle = this.appliedAngle;
        this.appliedAngle = this.normalizeAppliedAngle(rotationApplyResult.angle(), iIiIlIIliI2.getPlayerAngle());
        return rotationApplyResult;
    }

    public boolean isObservedPitchWithin(float f, float f2) {
        return this.observedPitchValid && Math.abs(this.observedPitch - f) <= f2;
    }

    public RotationIntent getCurrentIntent() {
        return this.currentIntent;
    }

    public void disableRotation() {
        this.rotationEnabled = false;
    }

    public void enableRotation() {
        this.rotationEnabled = true;
    }

    private void onClientTick(ClientTickEvent gameEvent6) {
        this.advanceRotationTick();
    }

    private void refreshViewAngles() {
        if (this.viewAnglesRefreshed) {
            return;
        }
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null) {
            return;
        }
        this.viewAnglesRefreshed = true;
        float f = player2.getYaw();
        float f2 = player2.getPitch();
        if (!this.viewAnglesInitialized) {
            this.viewYaw = f;
            this.viewPitch = f2;
            this.viewAnglesInitialized = true;
        }
        float f3 = this.appliedAngle != null ? this.appliedAngle.yawDegrees() : f;
        float f4 = this.appliedAngle != null ? this.appliedAngle.pitchDegrees() : f2;
        this.viewYaw += MathHelper.wrapDegrees((float)(f3 - this.viewYaw));
        this.viewPitch = f4;
    }

    private void selectStrategy(String string) {
        if (this.strategy == null || !string.equals(this.strategyName)) {
            if (this.strategy != null) {
                this.strategy.onDeactivated(this.createRotationState(this.currentIntent));
            }
            this.strategy = this.strategyFactory.getStrategy(string);
            this.strategyName = string;
            this.strategy.onActivated(this.createRotationState(this.currentIntent));
        }
    }

    private void onIncomingPositionLook(IncomingPacketEvent gameEvent5) {
        Packet<?> packet2 = gameEvent5.getPacket();
        if (packet2 instanceof PlayerPositionLookS2CPacket) {
            PlayerPositionLookS2CPacket positionPacket = (PlayerPositionLookS2CPacket)packet2;
            this.serverAngle = new Angle(positionPacket.change().yaw(), positionPacket.change().pitch());
            this.viewAnglesInitialized = false;
            this.correctionGraceTicks = 4;
        }
    }

    private Angle normalizeAppliedAngle(Angle angle, Angle angle2) {
        Angle angle3;
        if (angle == null) {
            return null;
        }
        Angle angle4 = angle3 = this.appliedAngle != null ? this.appliedAngle : angle2;
        if (angle3 == null) {
            return angle;
        }
        Angle angle5 = angle3.deltaTo(angle);
        float f = angle3.yawDegrees() + MathHelper.wrapDegrees((float)angle5.yawDegrees());
        float f2 = MathHelper.clamp((float)(angle3.pitchDegrees() + MathHelper.wrapDegrees((float)angle5.pitchDegrees())), (float)-90.0f, (float)90.0f);
        return new Angle(f, f2);
    }

    private RotationState createRotationState(RotationIntent intent2) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        Angle angle = player2 != null ? new Angle(player2.getYaw(), player2.getPitch()) : Angle.angle;
        float f = player2 != null ? player2.getAttackCooldownProgress(0.0f) : 0.0f;
        boolean bl = f >= 0.9f;
        return new RotationState(this.appliedAngle, this.previousAngle, this.serverAngle, angle, intent2.target(), intent2.aimPoint(), this.ticksSinceAttack, this.ticksSinceHit, f, bl);
    }

    private void advanceRotationTick() {
        ClientPlayerEntity player2;
        if (this.correctionGraceTicks > 0) {
            --this.correctionGraceTicks;
        }
        this.viewAnglesRefreshed = false;
        ++this.ticksSinceAttack;
        ++this.ticksSinceHit;
        this.tickStartAngle = this.appliedAngle != null ? this.appliedAngle : ((player2 = MinecraftClient.getInstance().player) != null ? new Angle(player2.getYaw(), player2.getPitch()) : null);
    }

    public void onTick(float f) {
        this.observedPitch = f;
        this.observedPitchValid = true;
    }

    public RotationController(RotationStrategyFactory callback35, EventBus eventBus) {
        this.strategyFactory = callback35;
        eventBus.subscribe(ClientTickEvent.class, ignored -> this.advanceRotationTick());
        eventBus.subscribe(IncomingPacketEvent.class, this::onIncomingPositionLook);
        eventBus.subscribe(OutgoingPacketEvent.class, this::onOutgoingLookPacket);
    }
}

