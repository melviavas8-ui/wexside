package ru.wexside.util;

import java.util.Objects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class Angle {
    public static final Angle ZERO;
    public static final Angle angle;
    private final float yaw;
    private final float pitch;

    public Angle(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public float yawDegrees() {
        return this.yaw;
    }

    public float pitchDegrees() {
        return this.pitch;
    }

    public boolean equals(Object clipToSpace) {
        if (this == clipToSpace) {
            return true;
        }
        if (!(clipToSpace instanceof Angle)) {
            return false;
        }
        Angle other = (Angle)clipToSpace;
        return Float.compare(this.yaw, other.yaw) == 0 && Float.compare(this.pitch, other.pitch) == 0;
    }

    public String toString() {
        return "Angle[yaw=" + this.yaw + ", pitch=" + this.pitch + "]";
    }

    public int hashCode() {
        return Objects.hash(Float.valueOf(this.yaw), Float.valueOf(this.pitch));
    }

    public static Angle fromVectors(Vec3d from, Vec3d to) {
        return Angle.fromDelta(to.subtract(from));
    }

    public Vec3d toVec3d() {
        float degToRad = (float)Math.PI / 180;
        float yawRad = -this.yaw * degToRad;
        float pitchRad = this.pitch * degToRad;
        float yawCos = MathHelper.cos((double)yawRad);
        float yawSin = MathHelper.sin((double)yawRad);
        float pitchCos = MathHelper.cos((double)pitchRad);
        float pitchSin = MathHelper.sin((double)pitchRad);
        return new Vec3d((double)(yawSin * pitchCos), (double)(-pitchSin), (double)(yawCos * pitchCos));
    }

    public float angularDistanceTo(Angle other) {
        Angle delta = this.deltaTo(other);
        return (float)Math.hypot(delta.yaw, delta.pitch);
    }

    public static Angle fromEntityLookVectorOffset(Vec3d from, LivingEntity entity) {
        Vec3d to = entity.getRotationVec(1.0f).add(0.0, -0.4, 0.0);
        return Angle.fromVectors(from, to);
    }

    public static Angle fromDirectionVector(Vec3d delta) {
        return Angle.fromDelta(delta);
    }

    public static Angle fromDelta(Vec3d delta) {
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(delta.y, horizontal)));
        return new Angle(yaw, pitch);
    }

    public Angle deltaTo(Angle other) {
        float yawDelta = MathHelper.wrapDegrees((float)(other.yaw - this.yaw));
        float pitchDelta = MathHelper.wrapDegrees((float)(other.pitch - this.pitch));
        return new Angle(yawDelta, pitchDelta);
    }

    public Angle withClampedPitch() {
        return new Angle(this.yaw, MathHelper.clamp((float)this.pitch, (float)-90.0f, (float)90.0f));
    }

    static {
        angle = ZERO = new Angle(0.0f, 0.0f);
    }
}

