package ru.wexside.command;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import ru.wexside.WexSideClient;
import ru.wexside.command.Command;
import ru.wexside.command.CommandUsageException;
import ru.wexside.misc.BlockedSoundList;
import ru.wexside.misc.ClientChat;

public final class SoundCommand
extends Command {
    public SoundCommand() {
        super("sound", "Управление заглушаемыми звуками (кастомные/серверные id)", "sound", "sounds", "звук", "звуки");
    }

    @Override
    public String getUsage() {
        return ".sound <add <id> | remove <id> | list | clear>";
    }

    @Override
    public void execute(String ... stringArray) throws CommandUsageException {
        if (stringArray.length < 1) {
            throw new CommandUsageException(this);
        }
        BlockedSoundList blockedSoundList2 = WexSideClient.getBlockedSoundList();
        if (blockedSoundList2 == null) {
            ClientChat.send("SoundRemoverService недоступен");
            return;
        }
        switch (stringArray[0].toLowerCase(Locale.ROOT)) {
            case "add": {
                if (stringArray.length != 2) {
                    throw new CommandUsageException(this);
                }
                String string = this.normalizeSoundIdentifier(stringArray[1]);
                if (string == null) {
                    String string2 = stringArray[1];
                    String string3 = String.valueOf(Formatting.RED);
                    ClientChat.send("Неверный id звука: " + string3 + string2);
                    return;
                }
                boolean bl = blockedSoundList2.add(string);
                this.setString(string);
                if (bl) {
                    String string4 = String.valueOf(Formatting.RESET);
                    String string5 = string;
                    String string6 = String.valueOf(Formatting.RED);
                    ClientChat.send("Звук " + string6 + string5 + string4 + " добавлен в заглушённые.");
                    break;
                }
                String string7 = String.valueOf(Formatting.RESET);
                String string8 = string;
                String string9 = String.valueOf(Formatting.RED);
                ClientChat.send("Звук " + string9 + string8 + string7 + " уже заглушён.");
                break;
            }
            case "remove": 
            case "del": 
            case "rm": {
                if (stringArray.length != 2) {
                    throw new CommandUsageException(this);
                }
                String string = this.normalizeSoundIdentifier(stringArray[1]);
                if (string != null && blockedSoundList2.remove(string)) {
                    String string10 = String.valueOf(Formatting.RESET);
                    String string11 = string;
                    String string12 = String.valueOf(Formatting.RED);
                    ClientChat.send("Звук " + string12 + string11 + string10 + " убран из заглушённых.");
                    break;
                }
                String string13 = String.valueOf(Formatting.RESET);
                String string14 = stringArray[1];
                String string15 = String.valueOf(Formatting.RED);
                ClientChat.send("Звука " + string15 + string14 + string13 + " нет в списке.");
                break;
            }
            case "clear": {
                blockedSoundList2.clear();
                ClientChat.send("Список заглушённых звуков очищен.");
                break;
            }
            case "list": 
            case "ls": {
                List<String> list = blockedSoundList2.getBlockedSounds();
                if (list.isEmpty()) {
                    ClientChat.send("Список заглушённых звуков пуст.");
                    break;
                }
                int n = list.size();
                ClientChat.send("Заглушённые звуки (" + n + "):");
                Iterator<String> iterator = list.iterator();
                while (iterator.hasNext()) {
                    String string;
                    String string16 = string = iterator.next();
                    String string17 = String.valueOf(Formatting.RED);
                    ClientChat.send(string17 + string16);
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
        String string;
        if (n == 0) {
            return List.of("add", "remove", "list", "clear");
        }
        if (n == 1 && stringArray.length >= 1 && ((string = stringArray[0].toLowerCase(Locale.ROOT)).equals("remove") || string.equals("del") || string.equals("rm"))) {
            BlockedSoundList blockedSoundList2 = WexSideClient.getBlockedSoundList();
            return blockedSoundList2 == null ? List.of() : blockedSoundList2.getBlockedSounds();
        }
        return List.of();
    }

    private String normalizeSoundIdentifier(String string) {
        if (string == null || string.isBlank()) {
            return null;
        }
        Identifier identifier = Identifier.tryParse((String)string.trim().toLowerCase(Locale.ROOT));
        return identifier == null ? null : identifier.toString();
    }

    private void setString(String string) {
        Identifier identifier = Identifier.tryParse((String)string);
        if (identifier == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getSoundManager().stopSounds(identifier, null);
    }
}

