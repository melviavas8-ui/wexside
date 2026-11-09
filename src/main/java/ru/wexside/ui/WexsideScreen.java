package ru.wexside.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.misc.DeferredRenderTask;
import ru.wexside.misc.GuiInteractionState;
import ru.wexside.misc.MovablePanel;
import ru.wexside.util.ClickGuiPanel;

public final class WexsideScreen
extends Screen {
    private static int lastFramebufferWidth;
    private static int lastFramebufferHeight;
    private final MovablePanel root;
    private final Runnable deferredRenderTaskRunner;

    public WexsideScreen() {
        this(WexSideClient.getInstance().getMiscellaneous(), DeferredRenderTask::update);
    }

    public WexsideScreen(MovablePanel root) {
        this(root, DeferredRenderTask::update);
    }

    WexsideScreen(MovablePanel root, Runnable deferredRenderTaskRunner) {
        super(Text.literal(WexSideClient.VERSION_LABEL));
        this.root = root;
        this.deferredRenderTaskRunner = deferredRenderTaskRunner;
    }

    private int scaledMouseX() {
        MinecraftClient client = MinecraftClient.getInstance();
        return (int)(client.mouse.getX() / (double)client.getWindow().getScaleFactor());
    }

    private int scaledMouseY() {
        MinecraftClient client = MinecraftClient.getInstance();
        return (int)(client.mouse.getY() / (double)client.getWindow().getScaleFactor());
    }

    private void updateLayout(int framebufferWidth, int framebufferHeight) {
        if (framebufferWidth <= 0 || framebufferHeight <= 0 || framebufferWidth == lastFramebufferWidth && framebufferHeight == lastFramebufferHeight) {
            return;
        }
        this.root.setPosition((int)((float)framebufferWidth / 2.0f - this.root.getBounds().getWidth() / 2.0f), (int)((float)framebufferHeight / 2.0f - this.root.getBounds().getHeight() / 2.0f));
        lastFramebufferWidth = framebufferWidth;
        lastFramebufferHeight = framebufferHeight;
    }

    protected void init() {
        this.refreshLayout();
        GuiInteractionState.getInstance().setRootPanel(this.root);
        MovablePanel movablePanel = this.root;
        if (movablePanel instanceof ClickGuiPanel) {
            ClickGuiPanel clickGui = (ClickGuiPanel)movablePanel;
            clickGui.open();
        }
    }

    public void removed() {
        this.root.update2();
        super.removed();
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.updateMouseState();
    }

    public void renderPanel(float delta) {
        this.root.update();
        float scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        Matrix4f transform = new Matrix4f().scale(scale, scale, 1.0f).translate(this.root.getBounds().getX(), this.root.getBounds().getY(), 0.0f);
        this.root.render(delta, transform);
        this.deferredRenderTaskRunner.run();
    }

    public boolean keyPressed(KeyInput input) {
        return this.root.onKeyPressed(input.key()) || super.keyPressed(input);
    }

    public boolean charTyped(CharInput input) {
        return Character.isBmpCodePoint(input.codepoint()) && this.root.onCharTyped((char)input.codepoint()) || super.charTyped(input);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.updateMouseState();
        this.root.onMouseScroll(this.scaledMouseX(), this.scaledMouseY(), verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean mouseClicked(Click click, boolean doubled) {
        this.updateMouseState();
        int mouseX = this.scaledMouseX();
        int mouseY = this.scaledMouseY();
        return this.root.onMousePressed(mouseX, mouseY, click.button()) || super.mouseClicked(click, doubled);
    }

    public boolean mouseReleased(Click click) {
        this.updateMouseState();
        this.root.onMouseReleased(this.scaledMouseX(), this.scaledMouseY(), click.button());
        return super.mouseReleased(click);
    }

    public void mouseMoved(double mouseX, double mouseY) {
        this.updateMouseState();
        super.mouseMoved(mouseX, mouseY);
    }

    public void resize(int width, int height) {
        super.resize(width, height);
        this.refreshLayout();
    }

    public boolean shouldPause() {
        return false;
    }

    public MovablePanel root() {
        return this.root;
    }

    private void refreshLayout() {
        this.updateLayout(this.width, this.height);
    }

    private void updateMouseState() {
        MinecraftClient client = MinecraftClient.getInstance();
        GuiInteractionState mouse = GuiInteractionState.getInstance();
        mouse.setRawMousePosition((int)client.mouse.getX(), (int)client.mouse.getY());
        mouse.setScaledMousePosition(this.scaledMouseX(), this.scaledMouseY());
        mouse.setRootPanel(this.root);
    }
}

