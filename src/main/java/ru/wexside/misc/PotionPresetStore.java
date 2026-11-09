package ru.wexside.misc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.wexside.misc.ConfigReadResult;
import ru.wexside.misc.ConfigStore;
import ru.wexside.misc.EncryptedConfigIO;
import ru.wexside.misc.JsonConfigStore;
import ru.wexside.misc.PotionPreset;

public final class PotionPresetStore
extends JsonConfigStore
implements ConfigStore {
    private List<PotionPreset> presets = new ArrayList<PotionPreset>();

    public PotionPresetStore(File file, Gson gson2) {
        super(file, gson2);
    }

    @Override
    public void load() {
        block12: {
            ConfigReadResult config = EncryptedConfigIO.readConfig(this.file, this.gson2);
            if (config == null) {
                return;
            }
            try {
                JsonObject json2 = config.json();
                if (json2 == null || !json2.has("magic") || !"wexside".equals(json2.get("magic").getAsString())) {
                    return;
                }
                this.presets.clear();
                if (json2.has("presets") && json2.get("presets").isJsonArray()) {
                    for (JsonElement iiIiiliiiI2 : json2.getAsJsonArray("presets")) {
                        try {
                            JsonObject json3 = iiIiiliiiI2.getAsJsonObject();
                            ArrayList<String> arrayList = new ArrayList<String>();
                            if (json3.has("potions") && json3.get("potions").isJsonArray()) {
                                for (JsonElement iiIiiliiiI3 : json3.getAsJsonArray("potions")) {
                                    arrayList.add(iiIiiliiiI3.getAsString());
                                }
                            }
                            this.presets.add(new PotionPreset(json3.get("name").getAsString(), json3.has("bind") ? json3.get("bind").getAsInt() : 0, json3.has("favorite") && json3.get("favorite").getAsBoolean(), arrayList));
                        }
                        catch (RuntimeException runtimeException) {}
                    }
                }
                if (!config.needsMigration()) break block12;
                try {
                    this.save();
                }
                catch (IOException exception) {
                    throw new IllegalStateException("Failed to migrate potion presets", exception);
                }
            }
            catch (RuntimeException runtimeException) {
                // empty catch block
            }
        }
    }

    @Override
    public void save() throws IOException {
        JsonObject json2 = new JsonObject();
        json2.addProperty("magic", "wexside");
        json2.addProperty("version", (Number)1);
        JsonArray iIIiliiIiI2 = new JsonArray();
        for (PotionPreset preset : this.presets) {
            JsonObject json3 = new JsonObject();
            json3.addProperty("name", preset.name());
            json3.addProperty("bind", (Number)preset.keyCode());
            json3.addProperty("favorite", Boolean.valueOf(preset.favorite()));
            JsonArray iIIiliiIiI3 = new JsonArray();
            for (String string : preset.potionIds()) {
                iIIiliiIiI3.add(string == null ? "" : string);
            }
            json3.add("potions", (JsonElement)iIIiliiIiI3);
            iIIiliiIiI2.add((JsonElement)json3);
        }
        json2.add("presets", (JsonElement)iIIiliiIiI2);
        EncryptedConfigIO.writeConfig(this.file, json2, Set.of("magic", "version"), this.gson2);
    }

    public void setList(List<PotionPreset> presets) {
        this.presets = presets == null ? new ArrayList<PotionPreset>() : new ArrayList<PotionPreset>(presets);
    }

    public List<PotionPreset> getList() {
        return this.presets;
    }
}

