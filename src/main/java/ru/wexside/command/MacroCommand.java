package ru.wexside.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.Formatting;
import ru.wexside.WexSideClient;
import ru.wexside.command.Command;
import ru.wexside.command.CommandUsageException;
import ru.wexside.input.InputBindings;
import ru.wexside.misc.ClientChat;
import ru.wexside.misc.MacroDefinition;
import ru.wexside.misc.MacroManager;
import ru.wexside.misc.MacroType;

public final class MacroCommand
extends Command {
    private static final String string21 = " ";

    @Override
    public String getUsage() {
        return ".macro <add <chat|command> <name> <key> \"<message>\" | remove <name> | list | clear>";
    }

    @Override
    public void execute(String ... stringArray) throws CommandUsageException {
        if (stringArray.length < 1) {
            throw new CommandUsageException(this);
        }
        MacroManager macroManager2 = WexSideClient.getMacroManager();
        if (macroManager2 == null) {
            ClientChat.send("MacroService недоступен");
            return;
        }
        switch (stringArray[0].toLowerCase(Locale.ROOT)) {
            case "add": {
                if (stringArray.length < 5) {
                    throw new CommandUsageException(this);
                }
                MacroType macroType = this.parseType(stringArray[1]);
                if (macroType == null) {
                    String string = String.valueOf(Formatting.RESET);
                    String string2 = String.valueOf(Formatting.RED);
                    String string3 = String.valueOf(Formatting.RESET);
                    String string4 = String.valueOf(Formatting.RED);
                    ClientChat.send("Тип должен быть " + string4 + "chat" + string3 + " или " + string2 + "command" + string + ".");
                    return;
                }
                String string = stringArray[2];
                int n = InputBindings.keyCode(stringArray[3].toUpperCase(Locale.ROOT));
                if (n == -1) {
                    ClientChat.send("Кнопка не найдена.");
                    return;
                }
                String string5 = this.parseMessageArguments(stringArray, 4);
                if (string5 == null || string5.isBlank()) {
                    String string6 = String.valueOf(Formatting.RESET);
                    String string7 = String.valueOf(Formatting.RED);
                    ClientChat.send("Сообщение должно быть в кавычках, например " + string7 + "\"im win ezz\"" + string6 + ".");
                    return;
                }
                if (macroManager2.containsMacro(string)) {
                    String string8 = String.valueOf(Formatting.RESET);
                    String string9 = string;
                    String string10 = String.valueOf(Formatting.RED);
                    ClientChat.send("Макрос " + string10 + string9 + string8 + " уже есть в списке макросов.");
                    return;
                }
                macroManager2.addMacro(string, string5, n, macroType);
                String string11 = this.describeType(macroType);
                String string12 = String.valueOf(Formatting.RESET);
                String string13 = string;
                String string14 = String.valueOf(Formatting.RED);
                ClientChat.send("Макрос " + string14 + string13 + string12 + " (" + string11 + ") добавлен в список макросов.");
                break;
            }
            case "remove": 
            case "del": 
            case "rm": {
                if (stringArray.length != 2) {
                    throw new CommandUsageException(this);
                }
                String string = stringArray[1];
                if (macroManager2.removeMacro(string)) {
                    String string15 = String.valueOf(Formatting.RESET);
                    String string16 = string;
                    String string17 = String.valueOf(Formatting.RED);
                    ClientChat.send("Макрос " + string17 + string16 + string15 + " убран из списка макросов.");
                    break;
                }
                String string18 = String.valueOf(Formatting.RESET);
                String string19 = string;
                String string20 = String.valueOf(Formatting.RED);
                ClientChat.send("Макроса " + string20 + string19 + string18 + " нет в списке макросов.");
                break;
            }
            case "clear": {
                macroManager2.update();
                ClientChat.send("Список макросов очищен.");
                break;
            }
            case "list": 
            case "ls": {
                List<MacroDefinition> list = macroManager2.getList();
                if (list.isEmpty()) {
                    ClientChat.send("Список макросов пуст.");
                    break;
                }
                int n = list.size();
                ClientChat.send("Список макросов (" + n + "):");
                for (MacroDefinition macroDefinition : list) {
                    ClientChat.send(this.formatMacroEntry(macroDefinition));
                }
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
        if (stringArray.length < 1) {
            return List.of();
        }
        String string = stringArray[0].toLowerCase(Locale.ROOT);
        if (n == 1 && string.equals("add")) {
            return List.of("chat", "command");
        }
        if (n == 1 && (string.equals("remove") || string.equals("del") || string.equals("rm"))) {
            MacroManager macroManager2 = WexSideClient.getMacroManager();
            if (macroManager2 == null) {
                return List.of();
            }
            ArrayList<String> arrayList = new ArrayList<String>();
            for (MacroDefinition macroDefinition : macroManager2.getList()) {
                arrayList.add(macroDefinition.getName());
            }
            return arrayList;
        }
        if (n == 3 && string.equals("add")) {
            return InputBindings.keyNames();
        }
        return List.of();
    }

    private MacroType parseType(String string) {
        if (string == null) {
            return null;
        }
        return switch (string.toLowerCase(Locale.ROOT)) {
            case "chat", "чат", "msg", "message" -> MacroType.CHAT;
            case "command", "cmd", "команда", "комманда" -> MacroType.COMMAND;
            default -> null;
        };
    }

    private String parseMessageArguments(String[] stringArray, int n) {
        int n2;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = n; i < stringArray.length; ++i) {
            if (i > n) {
                stringBuilder.append(string21);
            }
            stringBuilder.append(stringArray[i]);
        }
        String string = stringBuilder.toString().trim();
        if (string.length() >= 2 && string.charAt(0) == '\"' && (n2 = string.lastIndexOf(34)) > 0) {
            return string.substring(1, n2);
        }
        return string;
    }

    private String describeType(MacroType macroType) {
        return macroType == MacroType.COMMAND ? "команда" : "чат";
    }

    private String formatMacroEntry(MacroDefinition macroDefinition) {
        String string = InputBindings.keyName(macroDefinition.getKeyCode());
        String string2 = macroDefinition.getMessage();
        String string3 = this.describeType(macroDefinition.getType());
        String string4 = string;
        String string5 = String.valueOf(Formatting.RESET);
        String string6 = macroDefinition.getName();
        String string7 = String.valueOf(Formatting.RED);
        return string7 + string6 + string5 + " [" + string4 + "] (" + string3 + ") " + string2;
    }

    public MacroCommand() {
        super("macro", "Управление макросами (текст/команда по кнопке)", "macro", "macros");
    }
}

