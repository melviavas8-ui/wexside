package ru.wexside.notification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import ru.wexside.WexSideClient;
import ru.wexside.module.Module;
import ru.wexside.notification.EffectExpiredNotification;
import ru.wexside.notification.ModuleToggleNotification;
import ru.wexside.notification.NotificationCenter;
import ru.wexside.setting.BooleanSetting;
import ru.wexside.setting.Setting;

public final class NotificationTracker {
    private final Map<Module, Boolean> moduleStates = new HashMap<Module, Boolean>();
    private Set<RegistryEntry<StatusEffect>> activeEffects;

    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        NotificationCenter center = WexSideClient.getNotificationCenter();
        if (client.player == null || center == null) {
            this.activeEffects = null;
            return;
        }
        this.trackModules(center);
        this.trackEffects(client.player.getStatusEffects(), center);
    }

    private void trackModules(NotificationCenter center) {
        Iterator<Module> iterator = WexSideClient.getInstance().getModuleManager().getModules().iterator();
        while (iterator.hasNext()) {
            BooleanSetting toggle;
            Module module;
            Setting setting = (module = iterator.next()).getToggleSetting();
            boolean enabled = setting instanceof BooleanSetting && (toggle = (BooleanSetting)setting).isEnabled();
            Boolean previous = this.moduleStates.put(module, enabled);
            if (previous == null || previous == enabled) continue;
            center.push(new ModuleToggleNotification(module.getDisplayName(), enabled));
        }
    }

    private void trackEffects(Iterable<StatusEffectInstance> effects, NotificationCenter center) {
        HashSet<RegistryEntry<StatusEffect>> current = new HashSet<RegistryEntry<StatusEffect>>();
        for (StatusEffectInstance class_12932 : effects) {
            current.add((RegistryEntry<StatusEffect>)class_12932.getEffectType());
        }
        if (this.activeEffects != null) {
            for (RegistryEntry class_68802 : this.activeEffects) {
                if (current.contains(class_68802)) continue;
                center.push(new EffectExpiredNotification((RegistryEntry<StatusEffect>)class_68802));
            }
        }
        this.activeEffects = current;
    }
}

