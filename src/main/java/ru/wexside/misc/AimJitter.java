package ru.wexside.misc;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.math.MathHelper;
import ru.wexside.util.Angle;

public class AimJitter {
    private final int slot;
    private Angle angle;
    private final float value;
    private int slot2;
    private final float value2;
    private final float value3;
    private Angle angle2 = Angle.angle;

    public AimJitter() {
        this(30.0f, 15.0f, 2, 0.6f);
    }

    public AimJitter(float f, float f2, int n, float f3) {
        this.angle = Angle.angle;
        this.value2 = f;
        this.value = f2;
        this.slot = n;
        this.value3 = f3;
    }

    public void update() {
        this.angle2 = Angle.angle;
        this.angle = Angle.angle;
        this.slot2 = 0;
    }

    public Angle applyJitter(Angle angle) {
        float f;
        float f2;
        if (++this.slot2 >= this.slot) {
            this.slot2 = 0;
            f2 = ThreadLocalRandom.current().nextFloat(-this.value2, this.value2);
            f = ThreadLocalRandom.current().nextFloat(-this.value, this.value);
            this.angle = new Angle(f2, f);
        }
        f2 = MathHelper.lerp((float)this.value3, (float)this.angle2.yawDegrees(), (float)this.angle.yawDegrees());
        f = MathHelper.lerp((float)this.value3, (float)this.angle2.pitchDegrees(), (float)this.angle.pitchDegrees());
        this.angle2 = new Angle(f2, f);
        float f3 = angle.yawDegrees() + this.angle2.yawDegrees();
        float f4 = MathHelper.clamp((float)(angle.pitchDegrees() + this.angle2.pitchDegrees()), (float)-90.0f, (float)90.0f);
        return new Angle(f3, f4);
    }
}

