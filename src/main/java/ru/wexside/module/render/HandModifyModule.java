package ru.wexside.module.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.FirstPersonItemTransformEvent;
import ru.wexside.event.FirstPersonSwingTransformEvent;
import ru.wexside.event.HandSwingSpeedEvent;
import ru.wexside.misc.HandRenderTransforms;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.module.combat.AttackAuraModule;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.ModeSetting;
import ru.wexside.setting.ModeSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;

public final class HandModifyModule
extends Module
implements ConfigSerializable {
    private static volatile HandModifyModule instance;
    private final BooleanSetting enabledSetting;
    private final BooleanSetting positionHand;
    private final NumberSetting mainHandX;
    private final NumberSetting mainHandY;
    private final NumberSetting mainHandZ;
    private final NumberSetting offHandX;
    private final NumberSetting offHandY;
    private final NumberSetting offHandZ;
    private final BooleanSetting animationHand;
    private final ModeSetting swingType;
    private final BooleanSetting rotate;
    private final NumberSetting rotateSpeed;
    private final BooleanSetting attackAuraOnly;
    private final BooleanSetting staticPosition;
    private long rotateStartNanos;
    private AttackAuraModule attackAuraModule;

    public HandModifyModule(EventBus eventBus) {
        super(eventBus, "hand_modify", "Hand Modify", "Смещение позиции руки и кастомная анимация маха", ModuleCategory.valueOf("RENDER"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Включить модификацию руки").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        this.positionHand = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Position hand").id("position_hand").description("Смещать позицию руки")).build();
        this.registerSetting(this.positionHand);
        this.mainHandX = this.offsetSetting("main_hand_x", "Main hand X");
        this.mainHandY = this.offsetSetting("main_hand_y", "Main hand Y");
        this.mainHandZ = this.offsetSetting("main_hand_z", "Main hand Z");
        this.offHandX = this.offsetSetting("off_hand_x", "Off hand X");
        this.offHandY = this.offsetSetting("off_hand_y", "Off hand Y");
        this.offHandZ = this.offsetSetting("off_hand_z", "Off hand Z");
        this.animationHand = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Animation hand").id("animation_hand").description("Кастомная анимация")).build();
        this.registerSetting(this.animationHand);
        this.swingType = ((ModeSettingBuilder)ModeSetting.getModeSettingBuilder().options("Smooth", "Mode 2", "Mode 3", "Mode 4", "Mode 5", "Mode 6", "Mode 7", "Vanilla", "None").defaultOption("Smooth").name("Swing type").id("swing_type").description("Тип анимации").visibleWhen(this.animationHand::isEnabled)).build();
        this.registerSetting(this.swingType);
        this.rotate = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Rotate").id("rotate").description("Вращать предмет в руке при ударе").visibleWhen(this.animationHand::isEnabled)).build();
        this.registerSetting(this.rotate);
        this.rotateSpeed = ((NumberSettingBuilder)NumberSetting.builder().range(1.0, 5.0).defaultValue(1.0).multiplier(1.0).precision(0).animationSpeed(20.0f).name("Rotate speed").id("rotate_speed").description("Скорость вращения").visibleWhen(() -> this.animationHand.isEnabled() && this.rotate.isEnabled())).build();
        this.registerSetting(this.rotateSpeed);
        this.attackAuraOnly = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Attack aura only").id("attack_aura_only").description("Анимация только при включённой Attack Aura").visibleWhen(this.animationHand::isEnabled)).build();
        this.registerSetting(this.attackAuraOnly);
        this.staticPosition = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Static hand position").id("static_position").description("Статичное положение руки")).build();
        this.registerSetting(this.staticPosition);
    }

    @Override
    protected void initialize() {
        this.listen(FirstPersonItemTransformEvent.class, this::onHandPosition);
        this.listen(HandSwingSpeedEvent.class, this::onSwingSpeed);
        this.listen(FirstPersonSwingTransformEvent.class, this::onSwingAnimation);
    }

    public static boolean isStaticHandPositionEnabled() {
        HandModifyModule module = instance;
        return module != null && module.enabledSetting.isEnabled() && module.staticPosition.isEnabled();
    }

    private void onHandPosition(FirstPersonItemTransformEvent event) {
        if (!this.enabledSetting.isEnabled() || !this.positionHand.isEnabled()) {
            return;
        }
        Hand hand = event.getHand();
        if (hand == Hand.MAIN_HAND && event.getStack().getItem() instanceof CrossbowItem) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        int side = HandModifyModule.sideMultiplier(player);
        MatrixStack matrices = event.getMatrices();
        if (hand == Hand.MAIN_HAND) {
            this.applyOffset(matrices, side, this.mainHandX, this.mainHandY, this.mainHandZ);
        } else {
            this.applyOffset(matrices, side, this.offHandX, this.offHandY, this.offHandZ);
        }
    }

    private void onSwingAnimation(FirstPersonSwingTransformEvent event) {
        if (!this.enabledSetting.isEnabled() || !this.animationHand.isEnabled()) {
            return;
        }
        if (event.getHand() != Hand.MAIN_HAND) {
            return;
        }
        String mode = this.swingType.getSelectedOption();
        if (mode == null || "Vanilla".equals(mode)) {
            return;
        }
        if (this.attackAuraOnly.isEnabled() && !this.isAttackAuraActive()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        Item heldItem = player.getMainHandStack().getItem();
        if (heldItem instanceof CrossbowItem || heldItem instanceof BowItem || heldItem instanceof TridentItem) {
            return;
        }
        MatrixStack matrices = event.getMatrices();
        float swingProgress = event.getSwingProgress();
        float equipProgress = event.getEquipProgress();
        int side = HandModifyModule.sideMultiplier(player);
        float swingSin = MathHelper.sin((double)(swingProgress * (float)Math.PI));
        float sqrtSwing = MathHelper.sin((double)(MathHelper.sqrt((float)swingProgress) * (float)Math.PI));
        switch (mode) {
            case "None": {
                HandRenderTransforms.translateLocal(matrices, (float)side * 0.56f, -0.52f + equipProgress * -0.6f, -0.72f);
                break;
            }
            case "Mode 2": {
                this.applyMode2(matrices, side, swingSin);
                break;
            }
            case "Mode 3": {
                this.applyMode3(matrices, side, swingSin);
                break;
            }
            case "Mode 4": {
                this.applyMode4(matrices, side, sqrtSwing);
                break;
            }
            case "Mode 5": {
                this.applyMode5(matrices, side, sqrtSwing);
                break;
            }
            case "Mode 6": {
                this.applyMode6(matrices, side, swingSin);
                break;
            }
            case "Mode 7": {
                this.applyMode7(matrices, side, sqrtSwing);
                break;
            }
            default: {
                this.applySmooth(matrices, side, swingProgress, equipProgress, sqrtSwing);
            }
        }
        if (this.rotate.isEnabled()) {
            this.applySpin(matrices, swingProgress);
        }
        event.update();
    }

    private void onSwingSpeed(HandSwingSpeedEvent event) {
        if (!this.enabledSetting.isEnabled() || !this.animationHand.isEnabled()) {
            return;
        }
        event.setSpeedMultiplier(1.0f);
        event.update();
    }

    private void applyOffset(Object matrices, int side, NumberSetting x, NumberSetting y, NumberSetting z) {
        HandRenderTransforms.translate(matrices, (double)side * x.getValue(), y.getValue(), z.getValue());
    }

    private void applyMode2(Object matrices, int side, float swingSin) {
        HandRenderTransforms.translateLocal(matrices, (float)side * 0.56f, -0.35f, -0.72f);
        HandRenderTransforms.scale(matrices, 0.8f, 0.8f, 0.8f);
        HandRenderTransforms.rotateX(matrices, (float)side * 80.0f);
        HandRenderTransforms.rotateY(matrices, (float)side * -57.0f);
        HandRenderTransforms.rotateZ(matrices, -80.0f - 70.0f * swingSin);
    }

    private void applyMode3(Object matrices, int side, float swingSin) {
        HandRenderTransforms.translateLocal(matrices, (float)side * 0.56f, -0.35f, -0.72f);
        HandRenderTransforms.scale(matrices, 0.8f, 0.8f, 0.8f);
        HandRenderTransforms.rotateX(matrices, (float)side * 80.0f);
        HandRenderTransforms.rotateY(matrices, (float)side * -35.0f);
        HandRenderTransforms.rotateZ(matrices, -70.0f - 70.0f * swingSin);
    }

    private void applyMode4(Object matrices, int side, float sqrtSwing) {
        HandRenderTransforms.translateLocal(matrices, (float)side * 0.56f, -0.4f, -0.72f);
        HandRenderTransforms.scale(matrices, 0.8f, 0.8f, 0.8f);
        HandRenderTransforms.rotateZ(matrices, 50.0f);
        HandRenderTransforms.rotateX(matrices, (float)side * -60.0f);
        HandRenderTransforms.rotateY(matrices, (float)side * (110.0f + 25.0f * sqrtSwing));
    }

    private void applyMode5(Object matrices, int side, float sqrtSwing) {
        HandRenderTransforms.translateLocal(matrices, (float)side * 0.56f, -0.52f, -0.72f);
        HandRenderTransforms.rotateZ(matrices, -60.0f * sqrtSwing);
        HandRenderTransforms.rotateYNegative(matrices, (float)side * -25.0f * sqrtSwing);
        HandRenderTransforms.rotateYNegativeRadians(matrices, (float)side * -0.2f * sqrtSwing);
    }

    private void applyMode6(Object matrices, int side, float swingSin) {
        HandRenderTransforms.translateLocal(matrices, (float)side * 0.56f, -0.35f, -0.72f);
        HandRenderTransforms.scale(matrices, 0.8f, 0.8f, 0.8f);
        HandRenderTransforms.rotateX(matrices, (float)side * 25.0f);
        HandRenderTransforms.rotateY(matrices, (float)side * -15.0f);
        HandRenderTransforms.rotateZ(matrices, -70.0f - 100.0f * swingSin);
    }

    private void applyMode7(Object matrices, int side, float sqrtSwing) {
        HandRenderTransforms.translateLocal(matrices, (float)side * 0.56f, -0.35f, -0.72f);
        HandRenderTransforms.scale(matrices, 0.8f, 0.8f, 0.8f);
        HandRenderTransforms.rotateX(matrices, (float)side * 80.0f);
        HandRenderTransforms.rotateY(matrices, (float)side * -15.0f);
        HandRenderTransforms.rotateZ(matrices, -100.0f - 70.0f * sqrtSwing);
    }

    private void applySmooth(Object matrices, int side, float swingProgress, float equipProgress, float sqrtSwing) {
        HandRenderTransforms.translateLocal(matrices, (float)side * 0.56f, -0.52f + equipProgress * -0.25f, -0.72f);
        HandRenderTransforms.rotateX(matrices, (float)side * (45.0f + MathHelper.sin((double)(swingProgress * swingProgress * (float)Math.PI)) * -20.0f));
        HandRenderTransforms.rotateY(matrices, (float)side * sqrtSwing * -20.0f);
        HandRenderTransforms.rotateZ(matrices, sqrtSwing * -80.0f);
        HandRenderTransforms.rotateX(matrices, (float)side * -45.0f);
    }

    private void applySpin(Object matrices, float swingProgress) {
        if (swingProgress > 0.0f) {
            long elapsedMs = (System.nanoTime() - this.rotateStartNanos) / 1000000L;
            int spinDegrees = (int)((double)elapsedMs * 2.4 / (double)this.rotateSpeed.getIntValue() % 720.0);
            if (spinDegrees >= 360) {
                spinDegrees -= 720;
            }
            HandRenderTransforms.rotateZSpin(matrices, spinDegrees);
        } else {
            this.rotateStartNanos = System.nanoTime();
        }
    }

    private boolean isAttackAuraActive() {
        AttackAuraModule aura = this.attackAuraModule;
        if (aura == null) {
            if (WexSideClient.getInstance() == null || WexSideClient.getInstance().getModuleManager() == null) {
                return false;
            }
            this.attackAuraModule = aura = WexSideClient.getInstance().getModuleManager().getModule(AttackAuraModule.class);
        }
        return aura != null && aura.isActive();
    }

    private static int sideMultiplier(ClientPlayerEntity player) {
        Arm mainArm = player != null ? player.getMainArm() : Arm.RIGHT;
        return mainArm == Arm.RIGHT ? 1 : -1;
    }

    private NumberSetting offsetSetting(String id, String name) {
        NumberSetting setting = ((NumberSettingBuilder)NumberSetting.builder().range(-1.0, 1.0).defaultValue(0.0).multiplier(1.0).precision(2).animationSpeed(20.0f).name(name).id(id).description("Смещение руки по оси").visibleWhen(this.positionHand::isEnabled)).build();
        this.registerSetting(setting);
        return setting;
    }
}

