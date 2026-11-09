package ru.wexside.util;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import ru.wexside.misc.MeshModel;
import ru.wexside.misc.SpatialTransform;
import ru.wexside.render.ModelRenderBatch;
import ru.wexside.util.InlineMesh;
import ru.wexside.util.ModelRenderOptions;

public final class ModelRenderQueue {
    private final List<ModelRenderBatch> batches = new ArrayList<ModelRenderBatch>();
    private static final SpatialTransform IDENTITY_TRANSFORM = new SpatialTransform(0.0, 0.0, 0.0, 1.0f, 1.0f, 1.0f);

    public boolean isEmpty() {
        return this.batches.isEmpty();
    }

    public void clearBatches() {
        this.batches.clear();
    }

    private static void recenterTransforms(Matrix4f[] matrix4fArray, double[] dArray) {
        for (Matrix4f matrix4f : matrix4fArray) {
            if (matrix4f == null) continue;
            matrix4f.setTranslation((float)((double)matrix4f.m30() - dArray[0]), (float)((double)matrix4f.m31() - dArray[1]), (float)((double)matrix4f.m32() - dArray[2]));
        }
    }

    private Matrix4f buildRotationScaleMatrix(SpatialTransform spatialTransform) {
        Matrix4f matrix4f = new Matrix4f();
        if (spatialTransform.yawDegrees() != 0.0f) {
            matrix4f.rotateY((float)Math.toRadians(spatialTransform.yawDegrees()));
        }
        if (spatialTransform.pitchDegrees() != 0.0f) {
            matrix4f.rotateX((float)Math.toRadians(spatialTransform.pitchDegrees()));
        }
        if (spatialTransform.rollDegrees() != 0.0f) {
            matrix4f.rotateZ((float)Math.toRadians(spatialTransform.rollDegrees()));
        }
        return matrix4f.scale(spatialTransform.scaleX(), spatialTransform.scaleY(), spatialTransform.scaleZ());
    }

    public List<ModelRenderBatch> getBatches() {
        return this.batches;
    }

    private Matrix4f[] copyMeshTransforms(List<Matrix4f> list, int n) {
        Matrix4f[] matrix4fArray = new Matrix4f[n];
        for (int i = 0; i < n; ++i) {
            Matrix4fc matrix4fc = list != null && i < list.size() ? (Matrix4fc)list.get(i) : null;
            matrix4fArray[i] = matrix4fc != null ? new Matrix4f(matrix4fc) : new Matrix4f();
        }
        return matrix4fArray;
    }

    private double computeBatchRadius(Matrix4f[] matrix4fArray) {
        if (matrix4fArray == null || matrix4fArray.length == 0) {
            return 1.0;
        }
        double d = 1.0;
        for (Matrix4f matrix4f : matrix4fArray) {
            if (matrix4f == null) continue;
            double d2 = matrix4f.m30();
            double d3 = matrix4f.m31();
            double d4 = matrix4f.m32();
            d = Math.max(d, Math.sqrt(d2 * d2 + d3 * d3 + d4 * d4) + 1.0);
        }
        return d;
    }

    private static double[] computeTransformCenter(Matrix4f[] matrix4fArray) {
        double d = 0.0;
        double d2 = 0.0;
        double d3 = 0.0;
        int n = 0;
        for (Matrix4f matrix4f : matrix4fArray) {
            if (matrix4f == null) continue;
            d += (double)matrix4f.m30();
            d2 += (double)matrix4f.m31();
            d3 += (double)matrix4f.m32();
            ++n;
        }
        if (n == 0) {
            return new double[]{0.0, 0.0, 0.0};
        }
        return new double[]{d / (double)n, d2 / (double)n, d3 / (double)n};
    }

    private void enqueueBatch(List<InlineMesh> list, Matrix4f[] matrix4fArray, double d, double d2, double d3, int n, ModelRenderOptions modelRenderOptions) {
        if (list == null || list.isEmpty()) {
            return;
        }
        ModelRenderOptions modelRenderOptions2 = modelRenderOptions != null ? modelRenderOptions : ModelRenderOptions.getAlternateRenderOptions();
        this.batches.add(new ModelRenderBatch(list, matrix4fArray, n, modelRenderOptions2.resolveOutlineColor(n), d, d2, d3, this.computeBatchRadius(matrix4fArray), modelRenderOptions2));
    }

    public void enqueueModel(MeshModel meshModel, SpatialTransform spatialTransform, int n, ModelRenderOptions modelRenderOptions) {
        if (meshModel == null) {
            return;
        }
        this.enqueueTransformedMeshes(meshModel.getList(), null, spatialTransform, n, modelRenderOptions);
    }

    private void enqueueTransformedMeshes(List<InlineMesh> list, List<Matrix4f> list2, SpatialTransform spatialTransform, int n, ModelRenderOptions modelRenderOptions) {
        if (list == null || list.isEmpty()) {
            return;
        }
        SpatialTransform spatialTransform2 = spatialTransform != null ? spatialTransform : IDENTITY_TRANSFORM;
        Matrix4f matrix4f = this.buildRotationScaleMatrix(spatialTransform2);
        Matrix4f[] matrix4fArray = new Matrix4f[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            Matrix4fc matrix4fc;
            Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f);
            if (list2 != null && i < list2.size() && (matrix4fc = (Matrix4fc)list2.get(i)) != null) {
                matrix4f2.mul(matrix4fc);
            }
            matrix4fArray[i] = matrix4f2;
        }
        this.enqueueBatch(list, matrix4fArray, spatialTransform2.centerX(), spatialTransform2.centerY(), spatialTransform2.centerZ(), n, modelRenderOptions);
    }

    public void processModelInstances(MeshModel model, List<Matrix4f> transforms, SpatialTransform transform, int color, ModelRenderOptions options) {
        if (model == null) {
            return;
        }
        this.enqueueTransformedMeshes(model.getList(), transforms, transform, color, options);
    }

    public void enqueueCenteredMeshes(List<InlineMesh> list, List<Matrix4f> list2, int n, ModelRenderOptions modelRenderOptions) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Matrix4f[] matrix4fArray = this.copyMeshTransforms(list2, list.size());
        double[] dArray = ModelRenderQueue.computeTransformCenter(matrix4fArray);
        ModelRenderQueue.recenterTransforms(matrix4fArray, dArray);
        this.enqueueBatch(list, matrix4fArray, dArray[0], dArray[1], dArray[2], n, modelRenderOptions);
    }
}

