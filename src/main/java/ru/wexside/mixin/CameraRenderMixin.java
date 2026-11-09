package ru.wexside.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.module.movement.FreeCameraModule;
import ru.wexside.module.render.CameraModule;
import ru.wexside.module.render.FreeLookModule;

@Mixin(value={Camera.class})
public abstract class CameraRenderMixin {
    @Shadow
    protected abstract void setRotation(float var1, float var2);

    @Shadow
    protected abstract void setPos(Vec3d var1);

    @Inject(method={"update"}, at={@At(value="TAIL")})
    private void wexside$applyFreeLookRotation(World world, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo callback) {
        if (FreeCameraModule.isEnabled()) {
            this.setPos(FreeCameraModule.compute(tickProgress));
            this.setRotation(FreeCameraModule.getFloatType2(), FreeCameraModule.getFloatType());
        } else if (FreeLookModule.isEnabled()) {
            this.setRotation(FreeLookModule.getFloatType2(), FreeLookModule.getFloatType());
        }
    }

    @Inject(method={"clipToSpace"}, at={@At(value="HEAD")}, cancellable=true)
    private void wexside$disableCameraClipping(float desiredDistance, CallbackInfoReturnable<Float> callback) {
        if (CameraModule.isEnabled4()) {
            callback.setReturnValue(Float.valueOf(desiredDistance));
        }
    }

    @Inject(method={"clipToSpace"}, at={@At(value="RETURN")}, cancellable=true)
    private void wexside$smoothCameraDistance(float desiredDistance, CallbackInfoReturnable<Float> callback) {
        if (CameraModule.isEnabled3() && !CameraModule.isEnabled4()) {
            callback.setReturnValue(Float.valueOf(CameraModule.compute(((Float)callback.getReturnValue()).floatValue())));
        }
    }

    @Inject(method={"reset"}, at={@At(value="TAIL")})
    private void wexside$resetCameraSmoothing(CallbackInfo callback) {
        CameraModule.tick();
    }
}

