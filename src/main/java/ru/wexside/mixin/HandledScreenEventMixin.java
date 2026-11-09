package ru.wexside.mixin;

import net.minecraft.client.gui.Click;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wexside.input.InputBindings;
import ru.wexside.module.player.ItemScrollerModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wexside.WexSideClient;
import ru.wexside.event.EventBus;
import ru.wexside.event.ItemHoverEvent;
import ru.wexside.event.TooltipRenderEvent;
import ru.wexside.module.hud.AnimateModule;

@Mixin(value={HandledScreen.class})
public abstract class HandledScreenEventMixin {
    @Shadow
    protected Slot focusedSlot;
    @Shadow
    @Final
    protected ScreenHandler handler;

    @Shadow
    protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);

    @Unique
    private boolean wexside$animationMatrixPushed;

    @Inject(method = "renderMain", at = @At("TAIL"))
    private void wexside$scrollHeldItems(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo callback) {
        if (!ItemScrollerModule.isEnabled() || this.focusedSlot == null || !this.focusedSlot.hasStack()) {
            return;
        }
        if (!InputBindings.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT)
                || !InputBindings.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            return;
        }
        if (MinecraftClient.getInstance().player == null) {
            return;
        }
        if (ItemScrollerModule.acquireScrollPermit()) {
            this.onMouseClick(this.focusedSlot, this.focusedSlot.id, 1, SlotActionType.QUICK_MOVE);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void wexside$dropMatchingItems(KeyInput input, CallbackInfoReturnable<Boolean> callback) {
        if (!ItemScrollerModule.isEnabled() || this.focusedSlot == null || !this.focusedSlot.hasStack()) {
            return;
        }
        int modifiers = input.modifiers();
        if ((modifiers & GLFW.GLFW_MOD_SHIFT) == 0 || (modifiers & GLFW.GLFW_MOD_CONTROL) == 0) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null || !client.options.dropKey.matchesKey(input)) {
            return;
        }
        ItemStack hoveredStack = this.focusedSlot.getStack();
        for (int slotIndex = 0; slotIndex < this.handler.getStacks().size(); slotIndex++) {
            if (this.handler.getStacks().get(slotIndex).getItem() != hoveredStack.getItem()) {
                continue;
            }
            client.interactionManager.clickSlot(this.handler.syncId, slotIndex, 1, SlotActionType.THROW, client.player);
        }
        callback.setReturnValue(true);
    }

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void wexside$beginScreenAnimation(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo callback) {
        if (!AnimateModule.compute2((Screen)((Object)this))) {
            return;
        }
        context.getMatrices().pushMatrix();
        AnimateModule.handle(context.getMatrices(), context.getScaledWindowWidth(), context.getScaledWindowHeight());
        this.wexside$animationMatrixPushed = true;
    }

    @Inject(method={"render"}, at={@At(value="RETURN")})
    private void wexside$endScreenAnimation(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo callback) {
        if (!this.wexside$animationMatrixPushed) {
            return;
        }
        this.wexside$animationMatrixPushed = false;
        context.getMatrices().popMatrix();
    }

    @ModifyVariable(method={"render"}, at=@At(value="HEAD"), argsOnly=true, ordinal=0)
    private int wexside$transformRenderMouseX(int mouseX) {
        return AnimateModule.compute6(mouseX);
    }

    @ModifyVariable(method={"render"}, at=@At(value="HEAD"), argsOnly=true, ordinal=1)
    private int wexside$transformRenderMouseY(int mouseY) {
        return AnimateModule.compute7(mouseY);
    }

    @ModifyVariable(method={"mouseClicked", "mouseDragged", "mouseReleased"}, at=@At(value="HEAD"), argsOnly=true)
    private Click wexside$transformAnimatedClick(Click click) {
        int transformedX = AnimateModule.compute6((int)Math.round(click.x()));
        int transformedY = AnimateModule.compute7((int)Math.round(click.y()));
        if (transformedX == (int)Math.round(click.x()) && transformedY == (int)Math.round(click.y())) {
            return click;
        }
        return new Click((double)transformedX, (double)transformedY, click.buttonInfo());
    }

    @Inject(method={"drawMouseoverTooltip"}, at={@At(value="HEAD")}, cancellable=true)
    private void beforeItemTooltip(DrawContext context, int mouseX, int mouseY, CallbackInfo callback) {
        EventBus events = WexSideClient.getEventBus();
        if (events == null || this.focusedSlot == null) {
            return;
        }
        ItemStack stack = this.focusedSlot.getStack();
        if (stack.isEmpty()) {
            return;
        }
        ItemHoverEvent event = new ItemHoverEvent(stack);
        events.post(event);
        if (event.isCancelled()) {
            callback.cancel();
        }
    }

    @Inject(method={"render"}, at={@At(value="TAIL")})
    private void afterScreenRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo callback) {
        EventBus events = WexSideClient.getEventBus();
        if (events != null) {
            events.post(new TooltipRenderEvent(context, mouseX, mouseY));
        }
    }
}

