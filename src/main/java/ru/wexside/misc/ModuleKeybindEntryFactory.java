package ru.wexside.misc;

import java.util.ArrayList;
import java.util.List;
import ru.wexside.WexSideClient;
import ru.wexside.misc.KeybindDescriptor;
import ru.wexside.module.Module;
import ru.wexside.module.ModuleManager;
import ru.wexside.setting.BindSetting;
import ru.wexside.setting.Setting;
import ru.wexside.setting.SettingKeybind;
import ru.wexside.util.EspFeatureRegistry;
import ru.wexside.util.ModuleKeybindGroup;

public final class ModuleKeybindEntryFactory {
    private ModuleKeybindEntryFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static List<KeybindDescriptor> collectModuleBindings(Module module) {
        ArrayList<KeybindDescriptor> arrayList = new ArrayList<KeybindDescriptor>();
        for (Setting setting : module.getSettings()) {
            SettingKeybind configSerializable;
            if (setting instanceof BindSetting) {
                BindSetting bindSetting = (BindSetting)setting;
                arrayList.add(KeybindDescriptor.fromBindSetting(bindSetting));
            }
            if (!setting.hasKeybind() || (configSerializable = setting.getKeybind()) == null) continue;
            arrayList.add(KeybindDescriptor.fromSettingKeybind(configSerializable));
        }
        return arrayList;
    }

    public static List<ModuleKeybindGroup> createGroups(ModuleManager moduleManager) {
        ArrayList<ModuleKeybindGroup> arrayList = new ArrayList<ModuleKeybindGroup>();
        ArrayList<Module> arrayList2 = new ArrayList<Module>(moduleManager.getModules());
        EspFeatureRegistry espFeatures = WexSideClient.getEspFeatureRegistry();
        if (espFeatures != null) {
            arrayList2.addAll(espFeatures.getModules());
        }
        for (Module module : arrayList2) {
            List<KeybindDescriptor> list = ModuleKeybindEntryFactory.collectModuleBindings(module);
            if (list.isEmpty()) continue;
            arrayList.add(new ModuleKeybindGroup(module, list));
        }
        return arrayList;
    }
}

