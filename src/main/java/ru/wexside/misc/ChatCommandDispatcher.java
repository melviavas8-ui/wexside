package ru.wexside.misc;

import java.util.Arrays;
import java.util.List;
import ru.wexside.command.Command;
import ru.wexside.command.CommandUsageException;
import ru.wexside.event.EventBus;
import ru.wexside.event.OutgoingChatEvent;
import ru.wexside.misc.ClientChat;
import ru.wexside.module.misc.CommandCharacterModule;

public class ChatCommandDispatcher {
    private final List<Command> commands;

    public ChatCommandDispatcher(EventBus eventBus, List<Command> list) {
        eventBus.subscribe(OutgoingChatEvent.class, this::member5787);
        this.commands = list;
    }

    public String getString() {
        return CommandCharacterModule.getString();
    }

    void member5787(OutgoingChatEvent gameEvent15) {
        String string;
        String string2 = gameEvent15.getString();
        if (!string2.startsWith(string = this.getString())) {
            return;
        }
        gameEvent15.update();
        String[] stringArray = string2.split(" ");
        for (Command command : this.commands) {
            String[] stringArray2 = command.getCommandAliases();
            int n = stringArray2.length;
            for (int i = 0; i < n; ++i) {
                String string5 = string;
                String string3 = stringArray2[i];
                String string4 = string3;
                if (!stringArray[0].equalsIgnoreCase(string5 + string4)) continue;
                try {
                    command.execute(Arrays.copyOfRange(stringArray, 1, stringArray.length));
                }
                catch (CommandUsageException iIililIlII2) {
                    String string6 = iIililIlII2.getMessage();
                    ClientChat.send("Неверный синтаксис команды: " + string6);
                }
                catch (RuntimeException runtimeException) {
                    String string7 = runtimeException.getMessage();
                    ClientChat.send("Ошибка выполнения команды: " + string7);
                }
                return;
            }
        }
        ClientChat.send("Команда не найдена.");
    }
}

