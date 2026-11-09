package ru.wexside.misc;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionfc;

public final class HandRenderTransforms {
    private HandRenderTransforms() {
    }

    public static void translate(Object matrices, double x, double y, double z) {
        ((MatrixStack)matrices).translate(x, y, z);
    }

    public static void translateLocal(Object matrices, float x, float y, float z) {
        ((MatrixStack)matrices).translate(x, y, z);
    }

    public static void scale(Object matrices, float x, float y, float z) {
        ((MatrixStack)matrices).scale(x, y, z);
    }

    public static void multiply(Object matrices, Quaternionfc rotation) {
        ((MatrixStack)matrices).multiply(rotation);
    }

    public static void rotateX(Object matrices, float degrees) {
        HandRenderTransforms.multiply(matrices, (Quaternionfc)RotationAxis.POSITIVE_X.rotationDegrees(degrees));
    }

    public static void rotateY(Object matrices, float degrees) {
        HandRenderTransforms.multiply(matrices, (Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(degrees));
    }

    public static void rotateZ(Object matrices, float degrees) {
        HandRenderTransforms.multiply(matrices, (Quaternionfc)RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
    }

    public static void rotateYNegative(Object matrices, float degrees) {
        HandRenderTransforms.multiply(matrices, (Quaternionfc)RotationAxis.NEGATIVE_Y.rotationDegrees(degrees));
    }

    public static void rotateYNegativeRadians(Object matrices, float radians) {
        HandRenderTransforms.multiply(matrices, (Quaternionfc)RotationAxis.NEGATIVE_Y.rotation(radians));
    }

    public static void rotateZSpin(Object matrices, float degrees) {
        HandRenderTransforms.rotateZ(matrices, degrees);
    }
}

