package ru.wexside.command;

import java.util.List;
import ru.wexside.command.CommandUsageException;

public abstract class Command {
    private final String commandDescription;
    private final String[] commandAliases;
    private final String commandName;

    protected Command(String string, String string2, String ... stringArray) {
        String[] stringArray2;
        this.commandName = string;
        this.commandDescription = string2;
        if (stringArray.length == 0) {
            String[] stringArray3 = new String[1];
            stringArray2 = stringArray3;
            stringArray3[0] = string;
        } else {
            stringArray2 = stringArray;
        }
        this.commandAliases = stringArray2;
    }

    public abstract String getUsage();

    public abstract void execute(String ... var1) throws CommandUsageException;

    public String[] getCommandAliases() {
        return this.commandAliases;
    }

    public String getCommandDescription() {
        return this.commandDescription;
    }

    public String getCommandName() {
        return this.commandName;
    }

    public List<String> complete(int n, String[] stringArray) {
        return List.of();
    }
}

