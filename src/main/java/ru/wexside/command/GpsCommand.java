package ru.wexside.command;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ru.wexside.command.Command;
import ru.wexside.command.CommandUsageException;
import ru.wexside.misc.ClientChat;
import ru.wexside.misc.Gps;

public final class GpsCommand
extends Command {
    public GpsCommand() {
        super("gps", "GPS-стрелка к координатам", "gps", "navigation");
    }

    @Override
    public String getUsage() {
        return ".gps <x> <z>  |  .gps (без аргументов \u2014 отключить)";
    }

    @Override
    public void execute(String ... args) throws CommandUsageException {
        if (args.length == 0) {
            Gps.clear();
            ClientChat.send("GPS отключён");
            return;
        }
        if (args.length != 2) {
            throw new CommandUsageException(this);
        }
        try {
            int x = Integer.parseInt(args[0].trim());
            int z = Integer.parseInt(args[1].trim());
            Gps.set(x, z);
            ClientChat.send("GPS установлен: " + x + ", " + z);
        }
        catch (NumberFormatException exception) {
            throw new CommandUsageException(this);
        }
    }

    @Override
    public List<String> complete(int index, String[] args) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return List.of();
        }
        if (index == 0) {
            return List.of(String.valueOf(player.getBlockX()));
        }
        if (index == 1) {
            return List.of(String.valueOf(player.getBlockZ()));
        }
        return List.of();
    }
}

