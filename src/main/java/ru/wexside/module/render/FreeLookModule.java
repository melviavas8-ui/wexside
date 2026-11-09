package ru.wexside.module.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;

public final class FreeLookModule
extends Module
implements ConfigSerializable {
    private static volatile FreeLookModule instance;
    private final BooleanSetting enabledSetting;
    private boolean wasEnabled;
    private boolean looking;
    private float yaw;
    private float pitch;
    private Perspective savedPerspective;

    public FreeLookModule(EventBus eventBus) {
        super(eventBus, "free_look", "Free Look", "Свободный обзор", ModuleCategory.valueOf("RENDER"), new String[0]);
        instance = this;
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Включить свободный обзор").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.tickLook());
        this.listen(WorldRenderEvent.class, event -> this.applyPerspective());
    }

    public static boolean isEnabled() {
        FreeLookModule module = instance;
        return module != null && module.looking;
    }

    public static float getFloatType() {
        FreeLookModule module = instance;
        return module == null ? 0.0f : module.pitch;
    }

    public static float getFloatType2() {
        FreeLookModule module = instance;
        return module == null ? 0.0f : module.yaw;
    }

    public static void handle(double deltaX, double deltaY) {
        FreeLookModule module = instance;
        if (module == null) {
            return;
        }
        module.yaw += (float)(deltaX * 0.15);
        module.pitch = MathHelper.clamp((float)(module.pitch + (float)(deltaY * 0.15)), (float)-90.0f, (float)90.0f);
    }

    private void applyPerspective() {
        if (!this.looking) {
            return;
        }
        MinecraftClient.getInstance().options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    private void tickLook() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (!this.enabledSetting.isEnabled()) {
            if (this.wasEnabled) {
                this.stopLooking();
                this.wasEnabled = false;
            }
            return;
        }
        this.wasEnabled = true;
        if (player == null || client.world == null) {
            this.looking = false;
            return;
        }
        if (!this.looking) {
            this.yaw = player.getYaw();
            this.pitch = player.getPitch();
            this.savedPerspective = client.options.getPerspective();
            this.looking = true;
        }
    }

    private void stopLooking() {
        if (this.savedPerspective != null) {
            MinecraftClient.getInstance().options.setPerspective(this.savedPerspective);
        }
        this.looking = false;
        this.savedPerspective = null;
    }
}

