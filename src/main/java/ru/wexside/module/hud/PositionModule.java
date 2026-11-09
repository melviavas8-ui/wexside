package ru.wexside.module.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import ru.wexside.WexSideClient;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.misc.FontRegistry;
import ru.wexside.misc.FrameInterpolator;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.util.GuiDrawApi;

public final class PositionModule
extends Module
implements ConfigSerializable {
    private static final float TEXT_SIZE = 7.0f;
    private static final float CHAT_OFFSET = 14.0f;
    private static final float CHAT_LERP_SPEED = 30.0f;
    private final BooleanSetting enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Отображает координаты и скорость игрока").withKeybind().toggle()).build();
    private float chatOffset;

    public PositionModule(EventBus eventBus) {
        super(eventBus, "position", "Position", "Отображает координаты и скорость игрока", ModuleCategory.valueOf("DISPLAY"), new String[0]);
        this.registerSetting(this.enabledSetting);
    }

    @Override
    protected void initialize() {
        this.listen(HudRenderEvent.class, event -> this.onRender());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void onRender() {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            return;
        }
        String coordinates = this.formatCoordinates(player, world);
        double deltaX = player.getX() - player.lastX;
        double deltaZ = player.getZ() - player.lastZ;
        String speed = String.format("%.2f", Math.hypot(deltaX, deltaZ) * 20.0);
        String speedLine = "bps: " + speed;
        float scale = client.getWindow().getScaleFactor();
        float height = client.getWindow().getScaledHeight();
        this.chatOffset = FrameInterpolator.lerpTowards(this.chatOffset, client.currentScreen instanceof ChatScreen ? 14.0f : 0.0f, 30.0f);
        GuiDrawApi renderer = WexSideClient.getHudRenderer();
        Matrix4f matrix = new Matrix4f().scale(scale);
        renderer.begin();
        try {
            FontRegistry.font9.drawText(matrix, renderer, coordinates, 3.0f, height - 10.0f - this.chatOffset, 7.0f, -1);
            FontRegistry.font9.drawText(matrix, renderer, speedLine, 3.0f, height - 18.0f - this.chatOffset, 7.0f, -1);
        }
        finally {
            renderer.end();
        }
    }

    private String formatCoordinates(ClientPlayerEntity player, ClientWorld world) {
        int x = player.getBlockX();
        int y = player.getBlockY();
        int z = player.getBlockZ();
        RegistryKey dimension = world.getRegistryKey();
        Identifier dimensionId = dimension.getValue();
        boolean nether = dimensionId.getPath().contains("nether");
        int altX = nether ? x * 8 : Math.floorDiv(x, 8);
        int altZ = nether ? z * 8 : Math.floorDiv(z, 8);
        return String.format("xyz: %d, %d, %d (%d, %d)", x, y, z, altX, altZ);
    }
}

