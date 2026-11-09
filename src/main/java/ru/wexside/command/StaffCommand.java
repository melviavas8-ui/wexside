package ru.wexside.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import ru.wexside.WexSideClient;
import ru.wexside.command.Command;
import ru.wexside.command.CommandUsageException;
import ru.wexside.command.PlayerSuggestions;
import ru.wexside.misc.ClientChat;
import ru.wexside.misc.StaffNameStore;

public class StaffCommand
extends Command {
    public StaffCommand() {
        super("staff", "Управление списком персонала", "staff", "staffs");
    }

    @Override
    public String getUsage() {
        return ".staff <add|remove|list|clear> [name]";
    }

    @Override
    public void execute(String ... stringArray) throws CommandUsageException {
        if (stringArray.length < 1) {
            throw new CommandUsageException(this);
        }
        StaffNameStore staffNameStore2 = WexSideClient.getInstance().getStaffNameStore();
        if (staffNameStore2 == null) {
            ClientChat.send("StaffService недоступен");
            return;
        }
        switch (stringArray[0].toLowerCase()) {
            case "add": {
                if (stringArray.length < 2) {
                    throw new CommandUsageException(this);
                }
                if (staffNameStore2.add(stringArray[1])) {
                    String string = stringArray[1];
                    ClientChat.send("Добавил " + string + " в список персонала");
                    break;
                }
                String string = stringArray[1];
                ClientChat.send(string + " уже в списке персонала");
                break;
            }
            case "remove": 
            case "del": 
            case "rm": {
                if (stringArray.length < 2) {
                    throw new CommandUsageException(this);
                }
                if (staffNameStore2.remove(stringArray[1])) {
                    String string = stringArray[1];
                    ClientChat.send("Удалил " + string + " из списка персонала");
                    break;
                }
                String string = stringArray[1];
                ClientChat.send(string + " нет в списке персонала");
                break;
            }
            case "list": 
            case "ls": {
                Collection<String> collection = staffNameStore2.getNames();
                if (collection.isEmpty()) {
                    ClientChat.send("Список персонала пуст");
                    break;
                }
                String string = String.join((CharSequence)", ", collection);
                int n = collection.size();
                ClientChat.send("Персонал (" + n + "): " + string);
                break;
            }
            case "clear": {
                staffNameStore2.clear();
                ClientChat.send("Список персонала очищен");
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
            return List.of("add", "remove", "list", "clear");
        }
        if (n == 1 && stringArray.length >= 1) {
            String string = stringArray[0].toLowerCase(Locale.ROOT);
            if (string.equals("add")) {
                return PlayerSuggestions.onlinePlayerNames();
            }
            if (string.equals("remove") || string.equals("del") || string.equals("rm")) {
                WexSideClient wexSideClient = WexSideClient.getInstance();
                StaffNameStore staffNameStore2 = wexSideClient == null ? null : wexSideClient.getStaffNameStore();
                return staffNameStore2 == null ? List.of() : new ArrayList<String>(staffNameStore2.getNames());
            }
        }
        return List.of();
    }
}

