package ru.wexside.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import ru.wexside.WexSideClient;

public class HitCooldown {
    private long longType = System.currentTimeMillis();
    private long anyOf = 0L;
    static final float value = 0.3f;
    static final float value2 = 0.9f;
    private boolean tpsSync;
    private boolean enabled;
    private boolean enabled2 = true;

    private float getFloatType() {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player2 == null) {
            return 0.0f;
        }
        return player2.getAttackCooldownProgress(this.getCooldownTickDelta());
    }

    public boolean isAttackReady(boolean bl) {
        return this.isAttackReady(bl, 0.9f);
    }

    public void setLongType(long l) {
        this.longType = System.currentTimeMillis();
        this.anyOf = l;
    }

    private float getCooldownTickDelta() {
        float f;
        if (this.enabled) {
            f = 0.5f;
        } else {
            RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
            if (renderTickCounter == null) {
                return 0.0f;
            }
            f = renderTickCounter.getDynamicDeltaTicks();
        }
        if (this.tpsSync && WexSideClient.getServerTickRate() != null) {
            f = WexSideClient.getServerTickRate().scaleForTickRate(f);
        }
        return f;
    }

    public void setAttackRequested(boolean requested) {
        this.setBooleanType(requested);
    }

    public void setVanillaHitsAllowed(boolean allowed) {
        this.setBooleanType2(allowed);
    }

    public void setTpsSync(boolean tpsSync) {
        this.updateTpsSyncFlag(tpsSync);
    }

    public void setBooleanType(boolean bl) {
        this.enabled = bl;
    }

    public void setBooleanType2(boolean bl) {
        this.enabled2 = bl;
    }

    public void updateTpsSyncFlag(boolean bl) {
        this.tpsSync = bl;
    }

    public long getMillisSinceLastHit() {
        return System.currentTimeMillis() - this.longType;
    }

    public boolean isActive() {
        return this.getMillisSinceLastHit() >= this.anyOf;
    }

    private boolean isAvailable() {
        RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
        if (renderTickCounter == null) {
            return false;
        }
        return renderTickCounter.getDynamicDeltaTicks() > 0.3f;
    }

    public boolean isAttackReady(boolean bl, float f) {
        if (!this.enabled && !this.isAvailable()) {
            return false;
        }
        if (!this.enabled && !this.enabled2) {
            return false;
        }
        float f2 = this.getFloatType();
        long l = System.currentTimeMillis() - this.longType;
        return l >= this.anyOf && (bl || f2 > f);
    }
}

