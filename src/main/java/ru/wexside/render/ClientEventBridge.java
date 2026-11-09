package ru.wexside.render;

import java.io.IOException;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import ru.wexside.WexSideClient;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.DamageEvent;
import ru.wexside.event.DamageType;
import ru.wexside.event.EventBus;
import ru.wexside.event.HudRenderEvent;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.event.TotemPopEvent;
import ru.wexside.event.WorldRenderEvent;
import ru.wexside.event.WorldSessionEvent;
import ru.wexside.misc.ConfigManager;
import ru.wexside.module.hud.AnimateModule;
import ru.wexside.module.misc.AutoConfigSaveModule;
import ru.wexside.module.render.BlockOverlayModule;
import ru.wexside.module.render.ColorCorrectionModule;
import ru.wexside.module.render.TargetESPModule;
import ru.wexside.module.render.TracersModule;
import ru.wexside.render.RenderFrameClock;
import ru.wexside.render.RenderFrameState;
import ru.wexside.ui.WexsideScreen;
import ru.wexside.util.ClientClock;

public final class ClientEventBridge {
    private static DrawContext pendingHudContext;
    private static RenderTickCounter pendingHudTickCounter;

    private ClientEventBridge() {
    }

    public static void register(EventBus eventBus) {
        eventBus.subscribe(IncomingPacketEvent.class, event -> ClientEventBridge.onIncomingPacket(eventBus, event));
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientEventBridge.onEndTick(eventBus, client));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientEventBridge.saveActiveConfig());
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            RenderFrameClock.advanceFrame();
            pendingHudContext = context;
            pendingHudTickCounter = tickCounter;
        });
        WorldRenderEvents.END_EXTRACTION.register(context -> RenderFrameState.update(context.camera().getCameraPos(), context.cullProjectionMatrix(), context.viewMatrix()));
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outline) -> !BlockOverlayModule.isEnabled());
        WorldRenderEvents.END_MAIN.register(context -> {
            eventBus.post(new WorldRenderEvent(context.matrices(), ClientClock.tickDelta()));
            TargetESPModule.tick3();
            TracersModule.tick2();
            ColorCorrectionModule.tick();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> eventBus.post(new WorldSessionEvent(WorldSessionEvent.Change.JOINED)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RenderFrameState.clear();
            eventBus.post(new WorldSessionEvent(WorldSessionEvent.Change.DISCONNECTED));
        });
    }

    public static void renderHudAfterVanillaGui() {
        DrawContext context = pendingHudContext;
        RenderTickCounter tickCounter = pendingHudTickCounter;
        pendingHudContext = null;
        pendingHudTickCounter = null;
        EventBus eventBus = WexSideClient.getEventBus();
        if (eventBus != null && context != null && tickCounter != null) {
            AnimateModule.onHudRender(context);
            if (!(MinecraftClient.getInstance().currentScreen instanceof WexsideScreen)) {
                eventBus.post(new HudRenderEvent(context, tickCounter));
            }
        }
    }

    private static void onIncomingPacket(EventBus eventBus, IncomingPacketEvent event) {
        EntityDamageS2CPacket damage;
        Entity entity;
        EntityStatusS2CPacket status;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        Packet<?> class_25962 = event.getPacket();
        if (class_25962 instanceof EntityStatusS2CPacket && (status = (EntityStatusS2CPacket)class_25962).getStatus() == 35) {
            entity = status.getEntity((World)client.world);
            if (entity != null) {
                eventBus.post(new TotemPopEvent(entity, new Vec3d(entity.getX(), entity.getY() + (double)entity.getHeight() * 0.5, entity.getZ()), System.currentTimeMillis()));
            }
            return;
        }
        if (client.player != null && event.getPacket() instanceof EntityDamageS2CPacket && (damage = (EntityDamageS2CPacket)event.getPacket()).entityId() == client.player.getId()) {
            DamageSource source = damage.createDamageSource((World)client.world);
            DamageType type = source.getSource() != null && source.getAttacker() != null && source.getSource() != source.getAttacker() ? DamageType.PROJECTILE : (source.getAttacker() != null ? DamageType.DIRECT : DamageType.ENVIRONMENTAL);
            eventBus.post(new DamageEvent(type));
        }
    }

    private static void onEndTick(EventBus eventBus, MinecraftClient client) {
        ClientClock.advanceTick();
        if (client.player != null && client.world != null) {
            eventBus.post(new ClientTickEvent());
        }
    }

    private static void saveActiveConfig() {
        if (!AutoConfigSaveModule.isActive()) {
            return;
        }
        ConfigManager configManager = WexSideClient.getConfigManager();
        if (configManager == null || configManager.hasPendingImportedEntries()) {
            return;
        }
        String profile = configManager.getCurrentProfileName();
        if (profile == null || profile.isBlank()) {
            profile = "default";
        }
        try {
            configManager.saveProfile(profile);
        }
        catch (IOException exception) {
            WexSideClient.getInstance().getLogger().warn("Unable to auto-save profile {}", (Object)profile, (Object)exception);
        }
    }
}

