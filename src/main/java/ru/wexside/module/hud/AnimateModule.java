package ru.wexside.module.hud;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.Window;
import org.joml.Matrix3x2fStack;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.misc.HandledScreenAccessor;
import ru.wexside.misc.NativeHandle;
import ru.wexside.misc.TransitionAnimation;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.setting.NumberSetting;
import ru.wexside.setting.NumberSettingBuilder;
import ru.wexside.util.Easing;

public final class AnimateModule
extends Module
implements ConfigSerializable {
    static volatile AnimateModule animateModule2;
    private final BooleanSetting enabledSetting;
    private final MultiSelectSetting targets;
    private final BooleanSetting bounce;
    private final NumberSetting duration;
    private final NumberSetting inputDuration;
    private final TransitionAnimation containerAnimation;
    private Screen closingScreen;
    private Screen openingScreen;
    private boolean chatOpen;
    private boolean renderingClosingScreen;
    private long tabAnimationStartMs;

    public AnimateModule(EventBus eventBus) {
        super(eventBus, "animate", "Animate", "Анимация открытия и закрытия экранов", ModuleCategory.valueOf("DISPLAY"), new String[0]);
        animateModule2 = this;
        this.containerAnimation = new TransitionAnimation(Easing.LINEAR);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Анимирует экраны").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        MultiSelectSetting targetsSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options("Container", "Chat", "Tab").selectAll(true).optionListEnabled(false).name("Targets").id("targets").description("Что анимировать")).build();
        targetsSetting.setOptions(new String[]{"Container", "Chat", "Tab"});
        this.targets = targetsSetting;
        this.registerSetting(targetsSetting);
        this.bounce = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Bounce").id("bounce").description("Отскок").aliases("bounce", "отскок").visibleWhen(() -> this.targets.getSelectedOptions().contains("Container"))).build();
        this.registerSetting(this.bounce);
        this.duration = ((NumberSettingBuilder)NumberSetting.builder().range(50.0, 300.0).defaultValue(250.0).multiplier(1.0).precision(0).animationSpeed(20.0f).markers(50.0).snapTo(50.0).name("Duration").id("duration").description("Длительность анимации")).build();
        this.registerSetting(this.duration);
        this.inputDuration = ((NumberSettingBuilder)NumberSetting.builder().range(50.0, 300.0).defaultValue(170.0).multiplier(1.0).precision(0).animationSpeed(20.0f).markers(50.0).snapTo(10.0).name("Input Duration").id("input_duration").description("Длительность анимации поля ввода чата").aliases("input duration", "поле ввода").visibleWhen(() -> this.targets.getSelectedOptions().contains("Chat"))).build();
        this.registerSetting(this.inputDuration);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
    }

    private void onTick() {
        if (!this.enabledSetting.isEnabled() || !this.isTargetEnabled("Container")) {
            this.openingScreen = null;
            this.closingScreen = null;
            this.containerAnimation.finish();
            return;
        }
        double step = 50.0 / Math.max(50.0, this.duration.getValue());
        MinecraftClient client = MinecraftClient.getInstance();
        Screen currentScreen = client.currentScreen;
        if (currentScreen instanceof HandledScreen) {
            if (this.openingScreen != currentScreen) {
                this.openingScreen = currentScreen;
                this.containerAnimation.reset();
            }
            this.closingScreen = null;
            this.containerAnimation.advance(true, step);
        } else if (currentScreen == null) {
            if (this.openingScreen != null) {
                this.closingScreen = this.openingScreen;
                this.openingScreen = null;
            }
            if (this.closingScreen != null) {
                this.containerAnimation.advance(false, step);
                if (this.containerAnimation.isAtStart()) {
                    this.closingScreen = null;
                }
            }
        } else {
            this.openingScreen = null;
            this.closingScreen = null;
        }
    }

    private boolean isTargetEnabled(String target) {
        return this.targets.getSelectedOptions().contains(target);
    }

    public static boolean compute2(Screen screen) {
        AnimateModule module = animateModule2;
        if (module == null || !module.enabledSetting.isEnabled()) {
            return false;
        }
        Objects.requireNonNull(module);
        if (!module.isTargetEnabled("Container")) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != screen || !(screen instanceof HandledScreen)) {
            return false;
        }
        if (module.openingScreen != screen) {
            module.openingScreen = screen;
            module.closingScreen = null;
            module.containerAnimation.reset();
        }
        return true;
    }

    public static float compute3(int height) {
        AnimateModule module = animateModule2;
        if (module == null || !module.enabledSetting.isEnabled() || !module.isTargetEnabled("Tab")) {
            return 0.0f;
        }
        float durationMs = (float)module.duration.getValue();
        if (durationMs <= 0.0f) {
            return 0.0f;
        }
        float progress = Math.min((float)(System.currentTimeMillis() - module.tabAnimationStartMs) / durationMs, 1.0f);
        float eased = Easing.EASE_OUT_CUBIC.apply(progress);
        return -(1.0f - eased) * (float)height * 0.4f;
    }

    public static float compute4(float width, NativeHandle chatHud) {
        if (!AnimateModule.isEnabled2()) {
            return width;
        }
        float durationMs = AnimateModule.getFloatType2();
        if (durationMs <= 0.0f) {
            return width;
        }
        float elapsed = System.currentTimeMillis() - chatHud.getLongType();
        float progress = Math.min(elapsed / durationMs, 1.0f);
        return Easing.LINEAR.apply(width * progress);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void onHudRender(DrawContext context) {
        Screen screen;
        AnimateModule module = animateModule2;
        if (module == null || !module.enabledSetting.isEnabled() || !module.isTargetEnabled("Container")) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (module.closingScreen == null && module.openingScreen != null && client.currentScreen == null) {
            module.closingScreen = module.openingScreen;
            module.openingScreen = null;
        }
        if ((screen = module.closingScreen) == null || client.currentScreen != null || !(screen instanceof HandledScreen)) {
            return;
        }
        HandledScreen handledScreen = (HandledScreen)screen;
        Window window = client.getWindow();
        int mouseX = (int)client.mouse.getScaledX(window);
        int mouseY = (int)client.mouse.getScaledY(window);
        float tickDelta = client.getRenderTickCounter().getTickProgress(true);
        float fade = AnimateModule.compute5(module);
        if (fade > 0.0f) {
            int topColor = (int)(192.0f * fade) << 24 | 0x101010;
            int bottomColor = (int)(208.0f * fade) << 24 | 0x101010;
            context.fillGradient(0, 0, handledScreen.width, handledScreen.height, topColor, bottomColor);
        }
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        module.renderingClosingScreen = true;
        try {
            AnimateModule.handle(matrices, handledScreen.width, handledScreen.height);
            ((HandledScreenAccessor)handledScreen).drawContainerBackground(context, tickDelta, mouseX, mouseY);
            handledScreen.render(context, mouseX, mouseY, tickDelta);
        }
        catch (RuntimeException ignored) {
            module.closingScreen = null;
        }
        finally {
            module.renderingClosingScreen = false;
            matrices.popMatrix();
        }
    }

    private boolean isAnimatingContainer() {
        if (!this.enabledSetting.isEnabled()) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        return this.openingScreen != null && client.currentScreen == this.openingScreen || this.renderingClosingScreen;
    }

    public static float getFloatType() {
        AnimateModule module = animateModule2;
        return module == null ? 0.0f : (float)module.inputDuration.getValue();
    }

    public static void onBooleanType(boolean open) {
        AnimateModule module = animateModule2;
        if (module == null) {
            return;
        }
        if (open && !module.chatOpen) {
            module.tabAnimationStartMs = System.currentTimeMillis();
        }
        module.chatOpen = open;
    }

    public static void handle(Matrix3x2fStack matrices, float width, float height) {
        AnimateModule module = animateModule2;
        if (module == null) {
            return;
        }
        float scale = module.getContainerScale();
        matrices.translate(width / 2.0f, height / 2.0f);
        matrices.scale(scale, scale);
        matrices.translate(-width / 2.0f, -height / 2.0f);
    }

    public static float getFloatType2() {
        AnimateModule module = animateModule2;
        return module == null ? 0.0f : (float)module.duration.getValue();
    }

    private static float compute5(AnimateModule module) {
        float progress = module.containerAnimation.getPrimaryProgress();
        return Math.max(0.0f, Math.min(1.0f, progress));
    }

    public static int compute6(int x) {
        AnimateModule module = animateModule2;
        if (module == null || !module.isAnimatingContainer()) {
            return x;
        }
        float center = (float)MinecraftClient.getInstance().getWindow().getScaledWidth() / 2.0f;
        float scale = Math.max(0.001f, module.getContainerScale());
        return Math.round(center + ((float)x - center) / scale);
    }

    public static int compute7(int y) {
        AnimateModule module = animateModule2;
        if (module == null || !module.isAnimatingContainer()) {
            return y;
        }
        float center = (float)MinecraftClient.getInstance().getWindow().getScaledHeight() / 2.0f;
        float scale = Math.max(0.001f, module.getContainerScale());
        return Math.round(center + ((float)y - center) / scale);
    }

    private float getContainerScale() {
        float progress = this.containerAnimation.getPrimaryProgress();
        if (this.closingScreen != null) {
            return 1.0f - Easing.EASE_OUT_CUBIC.apply(1.0f - progress);
        }
        if (this.bounce.isEnabled()) {
            return Easing.EASE_OUT_BACK.apply(progress);
        }
        return Easing.EASE_OUT_CUBIC.apply(progress);
    }

    public static boolean isEnabled2() {
        AnimateModule module = animateModule2;
        if (module == null || !module.enabledSetting.isEnabled()) {
            return false;
        }
        Objects.requireNonNull(module);
        return module.isTargetEnabled("Chat");
    }

    public static float getFloatType4() {
        AnimateModule module = animateModule2;
        return module != null && module.isAnimatingContainer() ? module.getContainerScale() : 1.0f;
    }
}

