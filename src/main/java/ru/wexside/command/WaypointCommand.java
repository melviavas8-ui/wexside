package ru.wexside.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ru.wexside.WexSideClient;
import ru.wexside.command.Command;
import ru.wexside.command.CommandUsageException;
import ru.wexside.misc.ClientChat;
import ru.wexside.misc.Waypoint;
import ru.wexside.misc.WaypointStore;

public final class WaypointCommand
extends Command {
    public WaypointCommand() {
        super("waypoint", "Управление вейпоинтами", "waypoint", "way");
    }

    @Override
    public String getUsage() {
        return ".waypoint <add <name> <x> <y> <z> | remove <name> | clear>";
    }

    @Override
    public void execute(String ... stringArray) throws CommandUsageException {
        if (stringArray.length < 1) {
            throw new CommandUsageException(this);
        }
        WaypointStore waypointStore2 = WexSideClient.getWaypointStore();
        if (waypointStore2 == null) {
            ClientChat.send("WaypointService недоступен");
            return;
        }
        switch (stringArray[0].toLowerCase(Locale.ROOT)) {
            case "add": {
                int n;
                int n2;
                int n3;
                if (stringArray.length != 5) {
                    throw new CommandUsageException(this);
                }
                String string = stringArray[1];
                try {
                    n3 = Integer.parseInt(stringArray[2]);
                    n2 = Integer.parseInt(stringArray[3]);
                    n = Integer.parseInt(stringArray[4]);
                }
                catch (NumberFormatException numberFormatException) {
                    throw new CommandUsageException(this);
                }
                waypointStore2.add(new Waypoint(string, n3, n2, n));
                int n4 = n;
                int n5 = n2;
                int n6 = n3;
                String string2 = string;
                ClientChat.send("Вейпоинт " + string2 + " добавлен (" + n6 + ", " + n5 + ", " + n4 + ")");
                break;
            }
            case "remove": 
            case "del": 
            case "rm": {
                if (stringArray.length != 2) {
                    throw new CommandUsageException(this);
                }
                String string = stringArray[1];
                if (waypointStore2.removeByName(string)) {
                    String string3 = string;
                    ClientChat.send("Вейпоинт " + string3 + " удалён");
                    break;
                }
                String string4 = string;
                ClientChat.send("Вейпоинт " + string4 + " не найден");
                break;
            }
            case "clear": {
                waypointStore2.clear();
                ClientChat.send("Список вейпоинтов очищен");
                break;
            }
            default: {
                throw new CommandUsageException(this);
            }
        }
    }

    @Override
    public List<String> complete(int n, String[] stringArray) {
        if (n == 0) {
            return List.of("add", "remove", "clear");
        }
        if (stringArray.length < 1) {
            return List.of();
        }
        String string = stringArray[0].toLowerCase(Locale.ROOT);
        if (n == 1 && (string.equals("remove") || string.equals("del") || string.equals("rm"))) {
            WaypointStore waypointStore2 = WexSideClient.getWaypointStore();
            if (waypointStore2 == null) {
                return List.of();
            }
            ArrayList<String> arrayList = new ArrayList<String>();
            for (Waypoint waypoint2 : waypointStore2.getWaypoints()) {
                arrayList.add(waypoint2.name());
            }
            return arrayList;
        }
        if (string.equals("add") && n >= 2 && n <= 4) {
            ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
            if (player2 == null) {
                return List.of();
            }
            int n2 = switch (n) {
                case 2 -> player2.getBlockX();
                case 3 -> player2.getBlockY();
                default -> player2.getBlockZ();
            };
            return List.of(String.valueOf(n2));
        }
        return List.of();
    }
}

