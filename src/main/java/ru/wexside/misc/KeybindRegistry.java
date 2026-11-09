package ru.wexside.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import ru.wexside.config.ConfigSerializable;
import ru.wexside.event.EventBus;
import ru.wexside.misc.KeybindBinding;
import ru.wexside.misc.KeybindDispatcher;
import ru.wexside.misc.KeybindInputBridge;
import ru.wexside.setting.SettingKeybind;

public class KeybindRegistry
implements ConfigSerializable {
    private final List<KeybindBinding> bindings = new ArrayList<KeybindBinding>();
    private final KeybindInputBridge inputBridge;
    private final KeybindDispatcher dispatcher = new KeybindDispatcher(this.bindings);

    @Override
    public String getConfigId() {
        return "$binds";
    }

    @Override
    public void writeConfig(DataOutputStream dataOutputStream) throws IOException {
        ArrayList<SettingKeybind> settingKeybinds = new ArrayList<SettingKeybind>();
        for (KeybindBinding binding : this.bindings) {
            if (!(binding instanceof SettingKeybind)) continue;
            SettingKeybind settingKeybind = (SettingKeybind)binding;
            if (binding.getBindInput().isUnbound() || settingKeybind.getSetting().getConfigId().isBlank()) continue;
            settingKeybinds.add(settingKeybind);
        }
        dataOutputStream.writeInt(settingKeybinds.size());
        for (SettingKeybind keybind : settingKeybinds) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            keybind.writeConfig(new DataOutputStream(buffer));
            byte[] payload = buffer.toByteArray();
            dataOutputStream.writeUTF(keybind.getSetting().getConfigId());
            dataOutputStream.writeInt(payload.length);
            dataOutputStream.write(payload);
        }
    }

    @Override
    public void readConfig(DataInputStream dataInputStream) throws IOException {
        LinkedHashMap<String, SettingKeybind> keybindsBySettingId = new LinkedHashMap<String, SettingKeybind>();
        for (KeybindBinding binding : this.bindings) {
            if (!(binding instanceof SettingKeybind)) continue;
            SettingKeybind keybind = (SettingKeybind)binding;
            keybind.resetBindingState();
            if (keybind.getSetting().getConfigId().isBlank()) continue;
            keybindsBySettingId.put(keybind.getSetting().getConfigId(), keybind);
        }
        int entryCount = dataInputStream.readInt();
        ArrayList<BindPayload> payloads = new ArrayList<BindPayload>();
        for (int index = 0; index < entryCount; ++index) {
            String settingId = dataInputStream.readUTF();
            int payloadLength = dataInputStream.readInt();
            if (payloadLength < 0 || payloadLength > dataInputStream.available()) {
                throw new IOException("Payload length " + payloadLength + " for bind " + settingId + " does not fit the config");
            }
            byte[] payload = new byte[payloadLength];
            dataInputStream.readFully(payload);
            payloads.add(new BindPayload(settingId, payload));
        }
        for (BindPayload payload : payloads) {
            SettingKeybind settingKeybind = (SettingKeybind)keybindsBySettingId.get(payload.settingId());
            if (settingKeybind == null) continue;
            try {
                settingKeybind.readConfig(new DataInputStream(new ByteArrayInputStream(payload.data())));
            }
            catch (IOException | RuntimeException exception) {}
        }
    }

    public KeybindDispatcher getDispatcher() {
        return this.dispatcher;
    }

    public void unregister(KeybindBinding binding) {
        if (binding != null) {
            this.bindings.remove(binding);
        }
    }

    public List<KeybindBinding> getBindings() {
        return List.copyOf(this.bindings);
    }

    public void register(KeybindBinding binding) {
        if (binding != null && !this.bindings.contains(binding)) {
            this.bindings.add(binding);
        }
    }

    public void resetSettingKeybinds() {
        for (KeybindBinding binding : this.bindings) {
            if (!(binding instanceof SettingKeybind)) continue;
            SettingKeybind settingKeybind = (SettingKeybind)binding;
            settingKeybind.resetBindingState();
        }
    }

    public KeybindInputBridge getInputBridge() {
        return this.inputBridge;
    }

    public KeybindRegistry(EventBus eventBus) {
        this.inputBridge = new KeybindInputBridge(eventBus, this.dispatcher);
    }

    private record BindPayload(String settingId, byte[] data) {
    }
}

