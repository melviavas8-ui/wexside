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
import ru.wexside.misc.Waypoint;
import ru.wexside.misc.WaypointType;

public final class WaypointConfigStore
extends JsonConfigStore
implements ConfigStore {
    private final List<Waypoint> waypoints = new ArrayList<Waypoint>();

    public WaypointConfigStore(File file, Gson gson2) {
        super(file, gson2);
    }

    @Override
    public void load() {
        ConfigReadResult config = EncryptedConfigIO.readConfig(this.file, this.gson2);
        JsonObject json2 = config.json();
        if (json2.has("magic") && "wexside".equals(json2.get("magic").getAsString())) {
            this.waypoints.clear();
            if (json2.has("waypoints") && json2.get("waypoints").isJsonArray()) {
                for (JsonElement iiIiiliiiI2 : json2.getAsJsonArray("waypoints")) {
                    String string;
                    JsonObject json3;
                    if (!iiIiiliiiI2.isJsonObject() || !(json3 = iiIiiliiiI2.getAsJsonObject()).has("name") || (string = json3.get("name").getAsString()).isBlank()) continue;
                    int n = 0;
                    int n2 = 0;
                    int n3 = 0;
                    if (json3.has("x")) {
                        n = json3.get("x").getAsInt();
                    }
                    if (json3.has("y")) {
                        n2 = json3.get("y").getAsInt();
                    }
                    if (json3.has("z")) {
                        n3 = json3.get("z").getAsInt();
                    }
                    WaypointType waypointType = WaypointType.WAYPOINT;
                    if (json3.has("type")) {
                        waypointType = WaypointType.valueOf(json3.get("type").getAsString());
                    }
                    this.waypoints.add(new Waypoint(string, n, n2, n3, waypointType));
                }
            }
            if (config.needsMigration()) {
                try {
                    this.save();
                }
                catch (IOException exception) {
                    throw new IllegalStateException("Failed to migrate waypoints", exception);
                }
            }
        }
    }

    @Override
    public void save() throws IOException {
        JsonObject json2 = new JsonObject();
        json2.addProperty("magic", "wexside");
        json2.addProperty("version", (Number)1);
        JsonArray iIIiliiIiI2 = new JsonArray();
        for (Waypoint waypoint2 : this.waypoints) {
            JsonObject json3 = new JsonObject();
            json3.addProperty("name", waypoint2.name());
            json3.addProperty("x", (Number)waypoint2.x());
            json3.addProperty("y", (Number)waypoint2.y());
            json3.addProperty("z", (Number)waypoint2.z());
            json3.addProperty("type", waypoint2.type().name());
            iIIiliiIiI2.add((JsonElement)json3);
        }
        json2.add("waypoints", (JsonElement)iIIiliiIiI2);
        EncryptedConfigIO.writeConfig(this.file, json2, Set.of("magic", "version"), this.gson2);
    }

    public List<Waypoint> getList() {
        return this.waypoints;
    }
}

