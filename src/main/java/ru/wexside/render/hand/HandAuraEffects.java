package ru.wexside.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import ru.wexside.render.HandEnergyRenderer;
import ru.wexside.render.HandMaterialRenderer;

/** Bridges module settings and hand render phases to the recovered compositors. */
public final class HandAuraEffects {
    private static boolean energyEnabled;
    private static boolean materialEnabled;
    private static EnergyAuraMode energyMode = EnergyAuraMode.STATIC;

    private HandAuraEffects() {}

    public static EnergyAuraMode resolveEnergyMode(String setting) { return EnergyAuraMode.fromSetting(setting); }
    public static HandMaterialMode resolveMaterialMode(String setting) { return HandMaterialMode.fromSetting(setting); }

    public static void reset() {
        HandEnergyRenderer.getInstance().releaseResources();
        HandMaterialRenderer.getInstance().releaseResources();
    }

    public static void disableAll() {
        setEnergyEnabled(false);
        setMaterialEnabled(false);
    }

    public static void setEnergyEnabled(boolean enabled) {
        energyEnabled = enabled;
        HandEnergyRenderer.getInstance().setEnabled(enabled);
    }

    public static void setMaterialEnabled(boolean enabled) {
        materialEnabled = enabled;
        HandMaterialRenderer.getInstance().setEnabled(enabled);
    }

    public static void applyHandEnergy(Hand hand, MatrixStack matrices) {
        if (energyEnabled && energyMode != EnergyAuraMode.STATIC && matrices != null) {
            HandEnergyRenderer.getInstance().recordHandMotion(hand, matrices);
        }
    }

    public static void configureEnergy(boolean enabled, EnergyAuraMode mode, int primaryColor, int secondaryColor,
                                       boolean gradient, float radius, boolean fillHands) {
        setEnergyEnabled(enabled);
        if (!enabled) return;
        energyMode = mode == null ? EnergyAuraMode.STATIC : mode;
        var renderer = HandEnergyRenderer.getInstance();
        renderer.setMode(switch (energyMode) {
            case STATIC -> HandEnergyShaderMode.STATIC;
            case HAND_AURA -> HandEnergyShaderMode.AURA;
            case RIBBONS -> HandEnergyShaderMode.RIBBONS;
        });
        renderer.setColors(primaryColor, secondaryColor, gradient);
        renderer.setEffectRadius(radius);
        renderer.setFillHands(fillHands);
    }

    public static void configureMaterial(boolean enabled, HandMaterialMode mode, float radius, int primaryColor,
                                         int secondaryColor, boolean gradient) {
        setMaterialEnabled(enabled);
        if (!enabled) return;
        var renderer = HandMaterialRenderer.getInstance();
        HandMaterialMode selectedMode = mode == null ? HandMaterialMode.GLASS : mode;
        renderer.setMode(selectedMode.shaderMode());
        renderer.setRadius(radius);
        renderer.setColors(primaryColor, secondaryColor, gradient);
        // Original ordinal cases also retain these stores, despite having no surviving renderer reads.
        if (selectedMode == HandMaterialMode.GLASS) {
            renderer.setUnusedMaterialScalarC(2.2f + radius * 1.15f);
            renderer.setUnusedMaterialStepCount(3);
            renderer.setSaturation(1.0f);
            renderer.setReflect(true);
            renderer.setUnusedMaterialScalarB(0.12f);
            renderer.setUnusedMaterialScalarA(0.14f);
        } else if (selectedMode == HandMaterialMode.FILL) {
            renderer.setUnusedMaterialScalarC(1.4f + radius * 0.8f);
            renderer.setUnusedMaterialStepCount(2);
            renderer.setSaturation(1.0f);
            renderer.setReflect(false);
            renderer.setUnusedMaterialScalarB(0.92f);
            renderer.setUnusedMaterialScalarA(0.08f);
        }
    }

    public static void beforeHandRender(MatrixStack matrices, float tickDelta) {
        if (energyEnabled) HandEnergyRenderer.getInstance().captureSceneBefore();
        if (materialEnabled) HandMaterialRenderer.getInstance().captureSceneBefore();
    }

    public static void afterHandRender(MatrixStack matrices) {
        if (materialEnabled) {
            if (energyEnabled) HandEnergyRenderer.getInstance().captureDiffAfter();
            HandMaterialRenderer.getInstance().captureSceneAfter();
            HandMaterialRenderer.getInstance().compositeHands();
        }
        if (energyEnabled) HandEnergyRenderer.getInstance().compositeHands();
    }
}
