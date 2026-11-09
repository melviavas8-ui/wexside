/* Retained controller bodies; constructor follows the supplied patched factory. */
package ru.wexside.recovery.crystal;

import java.util.Random;

public class RichCrystalRotationController implements CrystalRotationControllerContract {
    private long tickCounter;
    private boolean viewRotationInitialized;
    private net.minecraft.client.network.ClientPlayerEntity lastLookPacketPlayer;
    private ru.wexside.util.Angle previousAngle;
    private ru.wexside.util.Angle lastPacketAngle;
    private boolean viewChangeRequested;
    private int serverCorrectionCooldown;
    private final CrystalRotationStrategyFactory strategyFactory;
    private int ticksSinceHit;
    private long lastPlayerTargetTick;
    private CrystalRotationStrategy strategy;
    static final int SERVER_CORRECTION_COOLDOWN = 4;
    private final Random random;
    private float continuousViewYaw;
    /** No putstatic exists; the supplied artifact leaves this field at its JVM default. */
    static final String UNWRITTEN_STRATEGY_NAME = null;
    private boolean viewUpdatedThisTick;
    private int ticksSinceAttack;
    private ru.wexside.util.Angle activeAngle;
    private String strategyName;
    private CrystalRotationRequest currentRequest;
    private float viewPitch;
    /** The supplied <clinit> returns without writing this field; null is intentional evidence. */
    static final CrystalRotationRequest UNWRITTEN_REQUEST_SENTINEL = null;
    private boolean lastLookPacketValid;
    private CrystalRotationRequest backgroundRequest;
    private ru.wexside.util.Angle tickAngleSnapshot;

    /** Ordinary construction reproducing GuiRepair.ensureHudServices supplied factory effects. */
    public RichCrystalRotationController() {
        this(new PatchedCrystalRotationStrategyFactory(), new Random());
    }

    public RichCrystalRotationController(CrystalRotationStrategyFactory factory, java.util.Random random) {
        this.strategyFactory = java.util.Objects.requireNonNull(factory, "factory");
        this.random = java.util.Objects.requireNonNull(random, "random");
        this.currentRequest = CrystalRotationRequest.idle();
        this.continuousViewYaw = 90.0f;
        this.viewPitch = 90.0f;
        this.viewUpdatedThisTick = true;
    }

    @Override
    public Object currentRequestOwner() { return this.currentRequest; }

    @Override
    public ru.wexside.util.Angle currentAngle() { return this.getValidatedPacketAngle(); }

    @Override
    public void apply(CrystalRotationRequest request, String strategyName) { this.applyRequest(request, strategyName); }

    public void release() {
        RichCrystalRotationController.release(this);
    }

    public boolean hasActiveAngle() {
        return this.activeAngle != null;
    }

    private void tick() {
        RichCrystalRotationController.advanceTick(this);
    }

    private static /* synthetic */ void updateWhenUnwrittenSentinel(RichCrystalRotationController ilIiIillIlIllil) {
        int n = -373832591;
        block5: while (true) {
            switch (n) {
                case -373832591: {
                    if (ilIiIillIlIllil.currentRequest == UNWRITTEN_REQUEST_SENTINEL) {
                        n = 1718196153;
                        continue block5;
                    }
                    n = 94079702;
                    continue block5;
                }
                case 1718196153: {
                    ilIiIillIlIllil.retainCurrentPlayerTarget();
                    n = 94079702;
                    continue block5;
                }
                case 94079702: {
                    return;
                }
            }
            break;
        }
        throw null;
    }

    private static /* synthetic */ void selectStrategy(RichCrystalRotationController ilIiIillIlIllil, String string) {
        int n = -13452742;
        block8: while (true) {
            switch (n) {
                case -13452742: {
                    if (ilIiIillIlIllil.strategy != null) {
                        n = 1723404750;
                        continue block8;
                    }
                    n = -455880595;
                    continue block8;
                }
                case 1723404750: {
                    if (!string.equals(ilIiIillIlIllil.strategyName)) {
                        n = -455880595;
                        continue block8;
                    }
                    n = -854244308;
                    continue block8;
                }
                case -455880595: {
                    if (ilIiIillIlIllil.strategy != null) {
                        n = 1123667085;
                        continue block8;
                    }
                    n = 792116308;
                    continue block8;
                }
                case 1123667085: {
                    ilIiIillIlIllil.strategy.onDeactivated(ilIiIillIlIllil.createRotationState(ilIiIillIlIllil.currentRequest));
                    n = 792116308;
                    continue block8;
                }
                case 792116308: {
                    ilIiIillIlIllil.strategy = ilIiIillIlIllil.strategyFactory.create(string);
                    ilIiIillIlIllil.strategyName = string;
                    ilIiIillIlIllil.strategy.onActivated(ilIiIillIlIllil.createRotationState(ilIiIillIlIllil.currentRequest));
                    n = -854244308;
                    continue block8;
                }
                case -854244308: {
                    return;
                }
            }
            break;
        }
        throw null;
    }

    private static /* synthetic */ boolean hasRecentPlayerTarget(RichCrystalRotationController ilIiIillIlIllil) {
        return ilIiIillIlIllil.lastPlayerTargetTick >= ilIiIillIlIllil.tickCounter - 1L;
    }

    private static /* synthetic */ void updateViewRotation(RichCrystalRotationController ilIiIillIlIllil) {
        net.minecraft.client.network.ClientPlayerEntity illiliIiIilil = null;
        float f = 0.0f;
        float f2 = 0.0f;
        int n = -254920671;
        block11: while (true) {
            switch (n) {
                case -254920671: {
                    illiliIiIilil = net.minecraft.client.MinecraftClient.getInstance().player;
                    if (illiliIiIilil == null) {
                        n = 260541457;
                        continue block11;
                    }
                    n = -143939767;
                    continue block11;
                }
                case 260541457: {
                    return;
                }
                case -143939767: {
                    f = illiliIiIilil.getYaw();
                    f2 = illiliIiIilil.getPitch();
                    if (!ilIiIillIlIllil.viewRotationInitialized) {
                        n = -636779961;
                        continue block11;
                    }
                    n = 437380227;
                    continue block11;
                }
                case -636779961: {
                    ilIiIillIlIllil.continuousViewYaw = f;
                    ilIiIillIlIllil.viewPitch = f2;
                    ilIiIillIlIllil.viewRotationInitialized = true;
                    n = 437380227;
                    continue block11;
                }
                case 437380227: {
                    if (ilIiIillIlIllil.activeAngle == null) {
                        n = -1780971638;
                        continue block11;
                    }
                    n = 740922373;
                    continue block11;
                }
                case -1780971638: {
                    ilIiIillIlIllil.continuousViewYaw += net.minecraft.util.math.MathHelper.wrapDegrees((float)(f - ilIiIillIlIllil.continuousViewYaw));
                    ilIiIillIlIllil.viewPitch = f2;
                    return;
                }
                case 740922373: {
                    if (ilIiIillIlIllil.viewUpdatedThisTick) {
                        n = 2051958381;
                        continue block11;
                    }
                    n = 103596513;
                    continue block11;
                }
                case 2051958381: {
                    return;
                }
                case 103596513: {
                    ilIiIillIlIllil.viewUpdatedThisTick = true;
                    ilIiIillIlIllil.continuousViewYaw += net.minecraft.util.math.MathHelper.wrapDegrees((float)(ilIiIillIlIllil.activeAngle.yawDegrees() - ilIiIillIlIllil.continuousViewYaw));
                    ilIiIillIlIllil.viewPitch = ilIiIillIlIllil.activeAngle.pitchDegrees();
                    return;
                }
            }
            break;
        }
        throw null;
    }

    private static /* synthetic */ boolean isUnwrittenRequestSentinel(RichCrystalRotationController ilIiIillIlIllil) {
        return ~(~(~(~(~(~(~(~(ilIiIillIlIllil.currentRequest == UNWRITTEN_REQUEST_SENTINEL ? 1 : 0)))))))) != 0;
    }

    private static /* synthetic */ void handleOutgoingPacket(RichCrystalRotationController ilIiIillIlIllil, ru.wexside.event.OutgoingPacketEvent outgoingEvent) {
        net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket lillIlIlllIiIll2;
        if (outgoingEvent.isCancelled()) {
            return;
        }
        net.minecraft.network.packet.Packet lilillIlIiIiIll2 = outgoingEvent.getPacket();
        if (lilillIlIiIiIll2 instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket && (lillIlIlllIiIll2 = (net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)lilillIlIiIiIll2).changesLook()) {
            ilIiIillIlIllil.lastPacketAngle = new ru.wexside.util.Angle(lillIlIlllIiIll2.getYaw(0.0f), lillIlIlllIiIll2.getPitch(0.0f));
            ilIiIillIlIllil.lastLookPacketPlayer = net.minecraft.client.MinecraftClient.getInstance().player;
            ilIiIillIlIllil.lastLookPacketValid = ilIiIillIlIllil.lastLookPacketPlayer != null;
        }
    }

    public void requestViewChange() {
        this.viewChangeRequested = true;
    }

    private static /* synthetic */ void resetTicksSinceHit(RichCrystalRotationController ilIiIillIlIllil) {
        ilIiIillIlIllil.ticksSinceHit = 0;
    }

    public void onOutgoingPacket(ru.wexside.event.OutgoingPacketEvent outgoingEvent) {
        RichCrystalRotationController.handleOutgoingPacket(this, outgoingEvent);
    }

    private ru.wexside.util.Angle quantizeAngle(ru.wexside.util.Angle iiIiliIlliliIil, ru.wexside.util.Angle iiIiliIlliliIil2) {
        return (ru.wexside.util.Angle)RichCrystalRotationController.quantizeAngle(this, iiIiliIlliliIil, iiIiliIlliliIil2);
    }

    public boolean applyAndTestOriginalIdleBranch(CrystalRotationRequest illilililiIlIil, String string) {
        return RichCrystalRotationController.applyAndTestOriginalIdleBranch(this, illilililiIlIil, string);
    }

    private static /* synthetic */ void advanceTick(RichCrystalRotationController ilIiIillIlIllil) {
        net.minecraft.client.network.ClientPlayerEntity illiliIiIilil;
        ++ilIiIillIlIllil.tickCounter;
        if (ilIiIillIlIllil.serverCorrectionCooldown > 0) {
            RichCrystalRotationController.decrementServerCorrectionCooldown(ilIiIillIlIllil);
        }
        ilIiIillIlIllil.viewUpdatedThisTick = false;
        ++ilIiIillIlIllil.ticksSinceAttack;
        ++ilIiIillIlIllil.ticksSinceHit;
        if (ilIiIillIlIllil.backgroundRequest != null) {
            illiliIiIilil = net.minecraft.client.MinecraftClient.getInstance().player;
            if (illiliIiIilil == null || illiliIiIilil != ilIiIillIlIllil.backgroundRequest.target() || !illiliIiIilil.isAlive() || net.minecraft.client.MinecraftClient.getInstance().world == null) {
                ilIiIillIlIllil.release();
            } else {
                ilIiIillIlIllil.applyAndTestOriginalIdleBranch(ilIiIillIlIllil.backgroundRequest, "Legit");
            }
        }
        if (ilIiIillIlIllil.activeAngle != null) {
            ilIiIillIlIllil.tickAngleSnapshot = ilIiIillIlIllil.activeAngle;
        } else {
            illiliIiIilil = net.minecraft.client.MinecraftClient.getInstance().player;
            ilIiIillIlIllil.tickAngleSnapshot = illiliIiIilil != null ? new ru.wexside.util.Angle(illiliIiIilil.getYaw(), illiliIiIilil.getPitch()) : null;
        }
    }

    private static /* synthetic */ ru.wexside.util.Angle quantizeAngle(RichCrystalRotationController ilIiIillIlIllil, ru.wexside.util.Angle iiIiliIlliliIil, ru.wexside.util.Angle iiIiliIlliliIil2) {
        ru.wexside.util.Angle iiIiliIlliliIil3;
        if (iiIiliIlliliIil == null) {
            return null;
        }
        ru.wexside.util.Angle iiIiliIlliliIil4 = iiIiliIlliliIil3 = ilIiIillIlIllil.activeAngle != null ? ilIiIillIlIllil.activeAngle : iiIiliIlliliIil2;
        if (iiIiliIlliliIil3 == null) {
            return iiIiliIlliliIil;
        }
        ru.wexside.util.Angle iiIiliIlliliIil5 = iiIiliIlliliIil3.deltaTo(iiIiliIlliliIil);
        float f = CrystalMouseQuantizer.quantize(iiIiliIlliliIil3.yawDegrees(), iiIiliIlliliIil5.yawDegrees(), ilIiIillIlIllil.random);
        float f2 = net.minecraft.util.math.MathHelper.clamp((float)CrystalMouseQuantizer.quantize(iiIiliIlliliIil3.pitchDegrees(), iiIiliIlliliIil5.pitchDegrees(), ilIiIillIlIllil.random), (float)-90.0f, (float)90.0f);
        return new ru.wexside.util.Angle(f, f2);
    }

    private static /* synthetic */ void release(RichCrystalRotationController ilIiIillIlIllil) {
        ru.wexside.util.RotationState ilIiIlIililiIil = null;
        int n = -1882598471;
        block5: while (true) {
            switch (n) {
                case -1882598471: {
                    ilIiIillIlIllil.backgroundRequest = null;
                    if (ilIiIillIlIllil.strategy != null) {
                        n = 1114213982;
                        continue block5;
                    }
                    n = 817957570;
                    continue block5;
                }
                case 1114213982: {
                    ilIiIlIililiIil = ilIiIillIlIllil.createRotationState(ilIiIillIlIllil.currentRequest);
                    ilIiIillIlIllil.strategy.onDeactivated(ilIiIlIililiIil);
                    n = 817957570;
                    continue block5;
                }
                case 817957570: {
                    ilIiIillIlIllil.activeAngle = null;
                    ilIiIillIlIllil.previousAngle = null;
                    ilIiIillIlIllil.tickAngleSnapshot = null;
                    ilIiIillIlIllil.currentRequest = CrystalRotationRequest.idle();
                    return;
                }
            }
            break;
        }
        throw null;
    }

    public ru.wexside.util.Angle getValidatedPacketAngle() {
        return RichCrystalRotationController.getValidatedPacketAngle(this);
    }

    private static /* synthetic */ ru.wexside.util.Angle getLastPacketAngle(RichCrystalRotationController ilIiIillIlIllil) {
        return ilIiIillIlIllil.lastPacketAngle;
    }

    public void retainCurrentPlayerTarget() {
        RichCrystalRotationController.retainCurrentPlayerTarget(this);
    }

    public void onCrystalUpdate(CrystalUpdateEvent illlllIlIillIil) {
        this.tick();
    }

    private static /* synthetic */ void resetTicksSinceAttack(RichCrystalRotationController ilIiIillIlIllil) {
        ilIiIillIlIllil.ticksSinceAttack = 0;
    }

    private void selectStrategy(String string) {
        RichCrystalRotationController.selectStrategy(this, string);
    }

    public void onIncomingPacket(ru.wexside.event.IncomingPacketEvent iillllIlIlllIil) {
        RichCrystalRotationController.handleIncomingPacket(this, iillllIlIlllIil);
    }

    public boolean hasBackgroundRequest() {
        return RichCrystalRotationController.hasBackgroundRequest(this);
    }

    public void resetTicksSinceHit() {
        RichCrystalRotationController.resetTicksSinceHit(this);
    }

    private static /* synthetic */ ru.wexside.util.RotationState createRotationState(RichCrystalRotationController ilIiIillIlIllil, CrystalRotationRequest illilililiIlIil) {
        net.minecraft.client.network.ClientPlayerEntity illiliIiIilil = net.minecraft.client.MinecraftClient.getInstance().player;
        ru.wexside.util.Angle iiIiliIlliliIil = illiliIiIilil != null ? new ru.wexside.util.Angle(illiliIiIilil.getYaw(), illiliIiIilil.getPitch()) : ru.wexside.util.Angle.ZERO;
        float f = illiliIiIilil != null ? illiliIiIilil.getAttackCooldownProgress(0.0f) : 0.0f;
        boolean bl = f >= 0.8999999761581421f;
        return new ru.wexside.util.RotationState(ilIiIillIlIllil.activeAngle, ilIiIillIlIllil.previousAngle, ilIiIillIlIllil.lastPacketAngle, iiIiliIlliliIil, illilililiIlIil.target(), illilililiIlIil.aimPoint(), ilIiIillIlIllil.ticksSinceAttack, ilIiIillIlIllil.ticksSinceHit, f, bl);
    }

    private static /* synthetic */ boolean isPacketPitchWithin(RichCrystalRotationController ilIiIillIlIllil, float f, float f2) {
        ru.wexside.util.Angle iiIiliIlliliIil = ilIiIillIlIllil.getValidatedPacketAngle();
        return ~(~(~(~(iiIiliIlliliIil != null && Math.abs(iiIiliIlliliIil.pitchDegrees() - f) <= f2 ? 1 : 0)))) != 0;
    }

    public ru.wexside.util.Angle getLastPacketAngle() {
        return (ru.wexside.util.Angle)RichCrystalRotationController.getLastPacketAngle(this);
    }

    private static /* synthetic */ boolean hasBackgroundRequest(RichCrystalRotationController ilIiIillIlIllil) {
        return ilIiIillIlIllil.backgroundRequest != null;
    }

    private static /* synthetic */ int getTicksSinceHit(RichCrystalRotationController ilIiIillIlIllil) {
        return -(-(-(-(-(-(-(-(-(-(-(-(-(-ilIiIillIlIllil.ticksSinceHit)))))))))))));
    }

    private ru.wexside.util.RotationState createRotationState(CrystalRotationRequest illilililiIlIil) {
        return (ru.wexside.util.RotationState)RichCrystalRotationController.createRotationState(this, illilililiIlIil);
    }

    private static /* synthetic */ ru.wexside.util.Angle getValidatedPacketAngle(RichCrystalRotationController ilIiIillIlIllil) {
        return ilIiIillIlIllil.lastLookPacketValid && ilIiIillIlIllil.lastLookPacketPlayer == net.minecraft.client.MinecraftClient.getInstance().player && ilIiIillIlIllil.serverCorrectionCooldown == 0 ? ilIiIillIlIllil.lastPacketAngle : null;
    }

    public boolean isPacketPitchWithin(float f, float f2) {
        return RichCrystalRotationController.isPacketPitchWithin(this, f, f2);
    }

    private static /* synthetic */ void applyNavigationAngle(RichCrystalRotationController ilIiIillIlIllil, ru.wexside.util.Angle iiIiliIlliliIil) {
        ilIiIillIlIllil.backgroundRequest = null;
        ilIiIillIlIllil.currentRequest = UNWRITTEN_REQUEST_SENTINEL;
        ilIiIillIlIllil.selectStrategy("Nav");
        ru.wexside.util.RotationState ilIiIlIililiIil = ilIiIillIlIllil.createRotationState(UNWRITTEN_REQUEST_SENTINEL);
        ru.wexside.misc.RotationApplyResult iiIlllliIilill = ilIiIillIlIllil.strategy.step(ilIiIlIililiIil, new CrystalRotationRequest(null, null, iiIiliIlliliIil, CrystalRotationStrength.HARD, CrystalMovementCorrection.NONE, false));
        ilIiIillIlIllil.previousAngle = ilIiIillIlIllil.activeAngle;
        ilIiIillIlIllil.activeAngle = ilIiIillIlIllil.quantizeAngle(iiIlllliIilill.angle(), ilIiIlIililiIil.playerAngle());
    }

    private static /* synthetic */ void retainCurrentPlayerTarget(RichCrystalRotationController ilIiIillIlIllil) {
        net.minecraft.client.network.ClientPlayerEntity illiliIiIilil = null;
        int n = -1791720405;
        block10: while (true) {
            switch (n) {
                case -1791720405: {
                    if (ilIiIillIlIllil.backgroundRequest == null) {
                        n = -1602451485;
                        continue block10;
                    }
                    n = -1061558740;
                    continue block10;
                }
                case -1602451485: {
                    if (ilIiIillIlIllil.activeAngle == null) {
                        n = -1061558740;
                        continue block10;
                    }
                    n = 1111793376;
                    continue block10;
                }
                case -1061558740: {
                    return;
                }
                case 1111793376: {
                    illiliIiIilil = net.minecraft.client.MinecraftClient.getInstance().player;
                    if (illiliIiIilil != null) {
                        n = -880693283;
                        continue block10;
                    }
                    n = -1603383627;
                    continue block10;
                }
                case -880693283: {
                    if (illiliIiIilil.isAlive()) {
                        n = 1610808258;
                        continue block10;
                    }
                    n = -1603383627;
                    continue block10;
                }
                case 1610808258: {
                    if (net.minecraft.client.MinecraftClient.getInstance().world == null) {
                        n = -1603383627;
                        continue block10;
                    }
                    n = -809787544;
                    continue block10;
                }
                case -1603383627: {
                    ilIiIillIlIllil.release();
                    return;
                }
                case -809787544: {
                    ilIiIillIlIllil.currentRequest = ilIiIillIlIllil.backgroundRequest = new CrystalRotationRequest((net.minecraft.entity.LivingEntity)illiliIiIilil, null, null, CrystalRotationStrength.NONE, CrystalMovementCorrection.NONE, false);
                    return;
                }
            }
            break;
        }
        throw null;
    }

    private static /* synthetic */ void decrementServerCorrectionCooldown(RichCrystalRotationController ilIiIillIlIllil) {
        --ilIiIillIlIllil.serverCorrectionCooldown;
    }



    private static /* synthetic */ ru.wexside.misc.RotationApplyResult applyRequest(RichCrystalRotationController ilIiIillIlIllil, CrystalRotationRequest illilililiIlIil, String string) {
        ru.wexside.util.RotationState ilIiIlIililiIil = null;
        ru.wexside.misc.RotationApplyResult iiIlllliIilill = null;
        int n = -1569236290;
        block8: while (true) {
            switch (n) {
                case -1569236290: {
                    if (illilililiIlIil != ilIiIillIlIllil.backgroundRequest) {
                        n = 1352981387;
                        continue block8;
                    }
                    n = -306573060;
                    continue block8;
                }
                case 1352981387: {
                    ilIiIillIlIllil.backgroundRequest = null;
                    n = -306573060;
                    continue block8;
                }
                case -306573060: {
                    ilIiIillIlIllil.selectStrategy(string);
                    ilIiIillIlIllil.currentRequest = illilililiIlIil;
                    if (illilililiIlIil.hasTarget()) {
                        n = -952050723;
                        continue block8;
                    }
                    n = -358438480;
                    continue block8;
                }
                case -952050723: {
                    if (illilililiIlIil.target() == net.minecraft.client.MinecraftClient.getInstance().player) {
                        n = 970394922;
                        continue block8;
                    }
                    n = -358438480;
                    continue block8;
                }
                case 970394922: {
                    ilIiIillIlIllil.lastPlayerTargetTick = ilIiIillIlIllil.tickCounter;
                    n = -358438480;
                    continue block8;
                }
                case -358438480: {
                    ilIiIlIililiIil = ilIiIillIlIllil.createRotationState(illilililiIlIil);
                    iiIlllliIilill = ilIiIillIlIllil.strategy.step(ilIiIlIililiIil, illilililiIlIil);
                    ilIiIillIlIllil.previousAngle = ilIiIillIlIllil.activeAngle;
                    ilIiIillIlIllil.activeAngle = ilIiIillIlIllil.quantizeAngle(iiIlllliIilill.angle(), ilIiIlIililiIil.playerAngle());
                    return iiIlllliIilill;
                }
            }
            break;
        }
        throw null;
    }

    private static /* synthetic */ void touchOwnedPlayerRequest(RichCrystalRotationController ilIiIillIlIllil, CrystalRotationRequest illilililiIlIil) {
        int n = -1510443254;
        block7: while (true) {
            switch (n) {
                case -1510443254: {
                    if (ilIiIillIlIllil.currentRequest == illilililiIlIil) {
                        n = -259586716;
                        continue block7;
                    }
                    n = -1311824310;
                    continue block7;
                }
                case -259586716: {
                    if (ilIiIillIlIllil.activeAngle != null) {
                        n = 450351726;
                        continue block7;
                    }
                    n = -1311824310;
                    continue block7;
                }
                case 450351726: {
                    if (illilililiIlIil.target() != net.minecraft.client.MinecraftClient.getInstance().player) {
                        n = -1311824310;
                        continue block7;
                    }
                    n = 1252027014;
                    continue block7;
                }
                case -1311824310: {
                    return;
                }
                case 1252027014: {
                    ilIiIillIlIllil.lastPlayerTargetTick = ilIiIillIlIllil.tickCounter;
                    ilIiIillIlIllil.previousAngle = ilIiIillIlIllil.activeAngle;
                    return;
                }
            }
            break;
        }
        throw null;
    }

    public ru.wexside.misc.RotationApplyResult applyRequest(CrystalRotationRequest illilililiIlIil, String string) {
        return (ru.wexside.misc.RotationApplyResult)RichCrystalRotationController.applyRequest(this, illilililiIlIil, string);
    }

    private static /* synthetic */ void handleIncomingPacket(RichCrystalRotationController ilIiIillIlIllil, ru.wexside.event.IncomingPacketEvent iillllIlIlllIil) {
        net.minecraft.network.packet.Packet lilillIlIiIiIll2 = null;
        net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket liIiIlliIlIiIll2 = null;
        int n = -1280391986;
        block7: while (true) {
            switch (n) {
                case -1280391986: {
                    lilillIlIiIiIll2 = iillllIlIlllIil.getPacket();
                    if (lilillIlIiIiIll2 instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket) {
                        n = 477063879;
                        continue block7;
                    }
                    n = 388024508;
                    continue block7;
                }
                case 477063879: {
                    liIiIlliIlIiIll2 = (net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket)lilillIlIiIiIll2;
                    if (ilIiIillIlIllil.backgroundRequest != null) {
                        n = 527558098;
                        continue block7;
                    }
                    n = -1665256807;
                    continue block7;
                }
                case 527558098: {
                    ilIiIillIlIllil.release();
                    n = -1665256807;
                    continue block7;
                }
                case -1665256807: {
                    ilIiIillIlIllil.lastLookPacketValid = false;
                    ilIiIillIlIllil.lastPacketAngle = new ru.wexside.util.Angle(liIiIlliIlIiIll2.change().yaw(), liIiIlliIlIiIll2.change().pitch());
                    ilIiIillIlIllil.viewRotationInitialized = false;
                    ilIiIillIlIllil.serverCorrectionCooldown = 4;
                    n = 388024508;
                    continue block7;
                }
                case 388024508: {
                    return;
                }
            }
            break;
        }
        throw null;
    }

    public void updateWhenUnwrittenSentinel() {
        RichCrystalRotationController.updateWhenUnwrittenSentinel(this);
    }

    public int getTicksSinceHit() {
        return RichCrystalRotationController.getTicksSinceHit(this);
    }

    private void updateViewRotation() {
        RichCrystalRotationController.updateViewRotation(this);
    }

    private static /* synthetic */ boolean applyAndTestOriginalIdleBranch(RichCrystalRotationController ilIiIillIlIllil, CrystalRotationRequest illilililiIlIil, String string) {
        ru.wexside.misc.RotationApplyResult iiIlllliIilill = null;
        int n = 698356488;
        block5: while (true) {
            switch (n) {
                case 698356488: {
                    iiIlllliIilill = ilIiIillIlIllil.applyRequest(illilililiIlIil, string);
                    if (true /* Original compares null with the initialized nonnull IDLE enum. */) {
                        n = 399151406;
                        continue block5;
                    }
                    n = -1334014068;
                    continue block5;
                }
                case 399151406: {
                    return false ^ false ^ false ^ false;
                }
                case -1334014068: {
                    ilIiIillIlIllil.release();
                    return true ^ false ^ false ^ false;
                }
            }
            break;
        }
        throw null;
    }

    public CrystalRotationRequest getCurrentRequest() {
        return this.currentRequest;
    }

    public void resetTicksSinceAttack() {
        RichCrystalRotationController.resetTicksSinceAttack(this);
    }

    public boolean hasRecentPlayerTarget() {
        return RichCrystalRotationController.hasRecentPlayerTarget(this);
    }

    public void applyNavigationAngle(ru.wexside.util.Angle iiIiliIlliliIil) {
        RichCrystalRotationController.applyNavigationAngle(this, iiIiliIlliliIil);
    }

    public ru.wexside.util.Angle getTickAngleSnapshot() {
        return this.tickAngleSnapshot;
    }

    public void touchOwnedPlayerRequest(CrystalRotationRequest illilililiIlIil) {
        RichCrystalRotationController.touchOwnedPlayerRequest(this, illilililiIlIil);
    }



    public void onWorldSession(ru.wexside.event.WorldSessionEvent liIlliIlIililil2) {
        this.release();
        this.lastLookPacketValid = false;
        this.lastLookPacketPlayer = null;
        this.viewRotationInitialized = false;
    }

    public boolean isUnwrittenRequestSentinel() {
        return RichCrystalRotationController.isUnwrittenRequestSentinel(this);
    }

    public void clearViewChangeRequest() {
        this.viewChangeRequested = false;
    }

    public ru.wexside.util.Angle getActiveAngle() {
        return this.activeAngle;
    }

    public Float getContinuousViewYaw() {
        if (this.serverCorrectionCooldown > 0) {
            return null;
        }
        this.updateViewRotation();
        return this.viewRotationInitialized ? Float.valueOf(this.continuousViewYaw) : null;
    }

    public boolean shouldChangeView() {
        return this.viewChangeRequested && this.activeAngle != null;
    }

    public float getViewPitch() {
        this.updateViewRotation();
        return this.viewPitch;
    }
}
