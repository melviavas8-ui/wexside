package ru.wexside.module.player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.ClientTickEvent;
import ru.wexside.event.EventBus;
import ru.wexside.event.IncomingPacketEvent;
import ru.wexside.misc.ClientChat;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleCategory;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.BooleanSettingBuilder;
import ru.wexside.setting.MultiSelectSetting;
import ru.wexside.setting.MultiSelectSettingBuilder;
import ru.wexside.util.ColorUtils;

public final class UseTrackerModule
extends Module
implements ConfigSerializable {
    static final String DRINK = "Drink";
    static final String SPLASH = "Splash";
    static final String USE = "Use";
    private static final long SPLASH_CACHE_NS = 2000000000L;
    private final BooleanSetting enabledSetting;
    private final MultiSelectSetting types;
    private final Map<Integer, SplashCache> splashByPlayer = new HashMap<Integer, SplashCache>();
    private final Map<Integer, UseProgress> useByPlayer = new HashMap<Integer, UseProgress>();

    public UseTrackerModule(EventBus eventBus) {
        super(eventBus, "use_tracker", "Use Tracker", "Использования предметов игроками в чат", ModuleCategory.valueOf("PLAYER"), new String[0]);
        this.enabledSetting = ((BooleanSettingBuilder)BooleanSetting.builder().value(false).defaultValue(false).name("Enabled").id("enabled").description("Использования предметов игроками в чат").withKeybind().toggle()).build();
        this.registerSetting(this.enabledSetting);
        MultiSelectSetting typesSetting = ((MultiSelectSettingBuilder)MultiSelectSetting.getMultiSelectSettingBuilder().options(USE, DRINK, SPLASH).selectAll(true).optionListEnabled(false).name("Types").id("types").description("Какие использования писать в чат")).build();
        typesSetting.setOptions(new String[]{USE, DRINK, SPLASH});
        this.types = typesSetting;
        this.registerSetting(typesSetting);
    }

    @Override
    protected void initialize() {
        this.listen(ClientTickEvent.class, event -> this.onTick());
        this.listen(IncomingPacketEvent.class, event -> this.onPacket(event.getPacket()));
    }

    private void onTick() {
        if (!this.enabledSetting.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        this.trackUses(client);
        if (this.types.getSelectedOptions().contains(SPLASH)) {
            this.cacheSplashPotions(client);
        }
    }

    private void onPacket(Packet<?> packet) {
        if (!this.enabledSetting.isEnabled() || !this.types.getSelectedOptions().contains(SPLASH)) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread()) {
            client.execute(() -> this.onPacket(packet));
            return;
        }
        if (client.world == null) {
            return;
        }
        if (packet instanceof BundleS2CPacket) {
            BundleS2CPacket bundle = (BundleS2CPacket)packet;
            for (Packet nested : bundle.getPackets()) {
                this.onPacket(nested);
            }
            return;
        }
        if (packet instanceof WorldEventS2CPacket) {
            WorldEventS2CPacket worldEvent = (WorldEventS2CPacket)packet;
            this.onSplash(worldEvent, client);
        }
    }

    private boolean isSplashPotion(ItemStack stack) {
        return stack != null && (stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION));
    }

    private static MutableText accent(String text) {
        return Text.literal((String)text).styled(style -> style.withColor(ColorUtils.lightContrastColor & 0xFFFFFF));
    }

    private PlayerEntity nearestThrower(MinecraftClient client, double x, double y, double z) {
        PlayerEntity nearest = null;
        double best = 0.7;
        for (PlayerEntity player : client.world.getPlayers()) {
            Vec3d look = player.getRotationVec(1.0f);
            Vec3d handPos = player.getEyePos().add(look.multiply(0.3)).add(-look.z * 0.35, 0.0, look.x * 0.35);
            double distance = handPos.squaredDistanceTo(x, y, z);
            if (distance >= best) continue;
            best = distance;
            nearest = player;
        }
        return nearest;
    }

    private void onSplash(WorldEventS2CPacket packet, MinecraftClient client) {
        int eventId = packet.getEventId();
        if (eventId != 2002 && eventId != 2007) {
            return;
        }
        BlockPos member2225 = packet.getPos();
        PlayerEntity thrower = this.nearestThrower(client, (double)member2225.getX() + 0.5, (double)member2225.getY() + 0.5, (double)member2225.getZ() + 0.5);
        if (thrower == null) {
            return;
        }
        ItemStack stack = this.splashStack(thrower);
        if (stack.isEmpty()) {
            return;
        }
        String name = UseTrackerModule.potionName(stack);
        if (name == null) {
            name = stack.getName().getString();
        }
        MutableText message = Text.empty();
        if (thrower == client.player) {
            message.append((Text)Text.literal((String)"Вы использовали "));
        } else {
            message.append((Text)UseTrackerModule.accent(thrower.getName().getString())).append((Text)Text.literal((String)" использовал "));
        }
        message.append((Text)UseTrackerModule.accent(name));
        ClientChat.send((Text)message);
    }

    private ItemStack splashStack(PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        if (this.isSplashPotion(main)) {
            return main;
        }
        SplashCache cache = this.splashByPlayer.get(player.getId());
        if (cache != null && System.nanoTime() - cache.timeNs <= 2000000000L) {
            return cache.stack;
        }
        return ItemStack.EMPTY;
    }

    private void cacheSplashPotions(MinecraftClient client) {
        long now = System.nanoTime();
        HashSet<Integer> alive = new HashSet<Integer>();
        for (PlayerEntity player : client.world.getPlayers()) {
            alive.add(player.getId());
            ItemStack stack = player.getMainHandStack();
            if (!this.isSplashPotion(stack)) continue;
            this.splashByPlayer.put(player.getId(), new SplashCache(stack.copy(), now));
        }
        this.splashByPlayer.entrySet().removeIf(entry -> !alive.contains(entry.getKey()) || now - ((SplashCache)entry.getValue()).timeNs > 2000000000L);
    }

    private void announceUse(PlayerEntity player, boolean self, ItemStack stack, UseAction action) {
        boolean drink = action == UseAction.DRINK;
        if (!this.types.getSelectedOptions().contains(drink ? DRINK : USE)) {
            return;
        }
        MutableText message = Text.empty();
        if (self) {
            message.append((Text)Text.literal((String)(drink ? "Вы выпили " : "Вы съели ")));
        } else {
            message.append((Text)UseTrackerModule.accent(player.getName().getString())).append((Text)Text.literal((String)(drink ? " выпил " : " съел ")));
        }
        message.append((Text)UseTrackerModule.accent(stack.getName().getString()));
        ClientChat.send((Text)message);
    }

    private void trackUses(MinecraftClient client) {
        HashSet<Integer> alive = new HashSet<Integer>();
        for (PlayerEntity player : client.world.getPlayers()) {
            alive.add(player.getId());
            this.useByPlayer.computeIfAbsent(player.getId(), id -> new UseProgress()).tick(player, player == client.player);
        }
        this.useByPlayer.keySet().removeIf(id -> !alive.contains(id));
    }

    private static String potionName(ItemStack stack) {
        PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) {
            return null;
        }
        return stack.getName().getString();
    }

    private record SplashCache(ItemStack stack, long timeNs) {
    }

    private final class UseProgress {
        private int useTime;
        private int maxUseTime;
        private ItemStack stack = ItemStack.EMPTY;

        private UseProgress() {
        }

        void tick(PlayerEntity player, boolean self) {
            if (player.isUsingItem()) {
                this.stack = player.getActiveItem().copy();
                this.useTime = player.getItemUseTime();
                this.maxUseTime = this.stack.getMaxUseTime((LivingEntity)player);
                return;
            }
            if (this.useTime > 0 && this.maxUseTime > 0 && this.useTime >= this.maxUseTime - 1 && !this.stack.isEmpty()) {
                UseTrackerModule.this.announceUse(player, self, this.stack, this.stack.getUseAction());
            }
            this.useTime = 0;
            this.maxUseTime = 0;
            this.stack = ItemStack.EMPTY;
        }
    }
}

