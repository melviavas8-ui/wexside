package ru.wexside.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.module.hud.AnimateModule;
import ru.wexside.module.hud.ExtraTabModule;

@Mixin(value={PlayerListHud.class})
public abstract class PlayerListHudAnimationMixin {
    @Unique
    private boolean wexside$animationMatrixPushed;

    @Inject(method={"setVisible"}, at={@At(value="TAIL")})
    private void wexside$trackVisibility(boolean visible, CallbackInfo callback) {
        AnimateModule.onBooleanType(visible);
    }

    @ModifyConstant(method={"collectPlayerEntries"}, constant={@Constant(longValue=80L)})
    private long wexside$expandPlayerLimit(long vanillaLimit) {
        return ExtraTabModule.isEnabled() ? Long.MAX_VALUE : vanillaLimit;
    }

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void wexside$beginAnimation(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo callback) {
        float offset = AnimateModule.compute3(context.getScaledWindowHeight());
        if (Math.abs(offset) <= 0.001f) {
            return;
        }
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.0f, offset);
        this.wexside$animationMatrixPushed = true;
    }

    @Inject(method={"render"}, at={@At(value="RETURN")})
    private void wexside$endAnimation(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo callback) {
        if (!this.wexside$animationMatrixPushed) {
            return;
        }
        this.wexside$animationMatrixPushed = false;
        context.getMatrices().popMatrix();
    }
}

