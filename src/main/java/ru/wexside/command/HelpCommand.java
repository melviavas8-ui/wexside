package ru.wexside.command;

import java.util.List;
import ru.wexside.command.Command;
import ru.wexside.command.CommandUsageException;
import ru.wexside.misc.ClientChat;

public final class HelpCommand
extends Command {
    private final List<Command> commands;

    public HelpCommand(List<Command> list) {
        super("help", "Показывает список доступных команд", "help");
        this.commands = list;
    }

    @Override
    public String getUsage() {
        return ".help";
    }

    @Override
    public void execute(String ... stringArray) throws CommandUsageException {
        ClientChat.send("Список доступных команд:");
        for (Command command : this.commands) {
            String string = command.getCommandAliases().length == 0 ? command.getCommandName() : command.getCommandAliases()[0];
            String string2 = command.getCommandDescription();
            String string3 = string;
            ClientChat.send("." + string3 + " - " + string2);
        }
    }
}

