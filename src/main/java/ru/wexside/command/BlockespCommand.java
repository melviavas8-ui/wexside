package ru.wexside.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import ru.wexside.WexSideClient;
import ru.wexside.command.Command;
import ru.wexside.command.CommandUsageException;
import ru.wexside.misc.BlockEspStore;
import ru.wexside.misc.ClientChat;

public final class BlockespCommand
extends Command {
    private static final int slot = -1;

    public BlockespCommand() {
        super("blockesp", "Управление списком блоков для Block ESP", "blockesp", "be");
    }

    @Override
    public String getUsage() {
        return ".blockesp <add <blockId> [hexColor] | remove <blockId> | list | clear>";
    }

    @Override
    public void execute(String ... stringArray) throws CommandUsageException {
        if (stringArray.length < 1) {
            throw new CommandUsageException(this);
        }
        BlockEspStore blockEspStore2 = WexSideClient.getBlockEspStore();
        if (blockEspStore2 == null) {
            ClientChat.send("BlockEspService недоступен");
            return;
        }
        switch (stringArray[0].toLowerCase(Locale.ROOT)) {
            case "add": {
                if (stringArray.length < 2) {
                    throw new CommandUsageException(this);
                }
                String string = this.resolveBlockIdentifier(stringArray[1]);
                if (string == null) {
                    String string2 = stringArray[1];
                    ClientChat.send("Неизвестный блок: " + string2);
                    return;
                }
                int n = -1;
                if (stringArray.length >= 3) {
                    Integer n2 = this.parseArgbHex(stringArray[2]);
                    if (n2 == null) {
                        String string3 = stringArray[2];
                        ClientChat.send("Неверный hex-цвет: " + string3 + " (примеры: ff0000, ff00ff00)");
                        return;
                    }
                    n = n2;
                }
                boolean bl = blockEspStore2.contains(string);
                blockEspStore2.put(string, n);
                String string4 = this.formatArgbHex(n);
                String string5 = string;
                String string6 = bl ? "Обновил" : "Добавил";
                ClientChat.send(string6 + " блок " + string5 + " (#" + string4 + ")");
                break;
            }
            case "remove": 
            case "del": 
            case "rm": {
                String string;
                if (stringArray.length < 2) {
                    throw new CommandUsageException(this);
                }
                String string7 = this.resolveBlockIdentifier(stringArray[1]);
                String string8 = string = string7 != null ? string7 : stringArray[1].trim().toLowerCase(Locale.ROOT);
                if (blockEspStore2.remove(string)) {
                    String string9 = string;
                    ClientChat.send("Удалил блок " + string9);
                    break;
                }
                String string10 = string;
                ClientChat.send(string10 + " не в списке");
                break;
            }
            case "list": 
            case "ls": {
                Map<String, Integer> map = blockEspStore2.getBlocks();
                if (map.isEmpty()) {
                    ClientChat.send("Список блоков пуст");
                    break;
                }
                int n = map.size();
                StringBuilder stringBuilder = new StringBuilder("Блоки (" + n + "): ");
                boolean bl = true;
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    if (!bl) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(entry.getKey()).append(" #").append(this.formatArgbHex(entry.getValue()));
                    bl = false;
                }
                ClientChat.send(stringBuilder.toString());
                break;
            }
            case "clear": {
                blockEspStore2.clear();
                ClientChat.send("Список блоков очищен");
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
            ArrayList<String> arrayList = new ArrayList<String>();
            for (Identifier identifier : Registries.BLOCK.getIds()) {
                arrayList.add(this.formatBlockIdentifier(identifier));
            }
            return arrayList;
        }
        if (n == 2 && string.equals("add")) {
            return List.of("ffffff", "ff0000", "00ff00", "0000ff", "ffff00", "00ffff", "ff00ff");
        }
        if (n == 1 && (string.equals("remove") || string.equals("del") || string.equals("rm"))) {
            BlockEspStore blockEspStore2 = WexSideClient.getBlockEspStore();
            if (blockEspStore2 == null) {
                return List.of();
            }
            ArrayList<String> arrayList = new ArrayList<String>();
            for (String string2 : blockEspStore2.getBlocks().keySet()) {
                arrayList.add(this.stripMinecraftNamespace(string2));
            }
            return arrayList;
        }
        return List.of();
    }

    private Integer parseArgbHex(String string) {
        String string2 = string.trim();
        if (string2.startsWith("#")) {
            string2 = string2.substring(1);
        } else if (string2.startsWith("0x") || string2.startsWith("0X")) {
            string2 = string2.substring(2);
        }
        if (string2.length() != 6 && string2.length() != 8) {
            return null;
        }
        try {
            long l = Long.parseLong(string2, 16);
            return string2.length() == 6 ? (int)(0xFF000000L | l) : (int)l;
        }
        catch (NumberFormatException numberFormatException) {
            return null;
        }
    }

    private String stripMinecraftNamespace(String string) {
        String string2 = "minecraft:";
        return string.startsWith(string2) ? string.substring(string2.length()) : string;
    }

    private String resolveBlockIdentifier(String string) {
        Identifier identifier = Identifier.tryParse((String)string.trim().toLowerCase(Locale.ROOT));
        if (identifier == null) {
            return null;
        }
        Optional<Block> optional = Registries.BLOCK.getOptionalValue(identifier);
        if (optional.isEmpty() || optional.get() == Blocks.AIR) {
            return null;
        }
        return Registries.BLOCK.getId(optional.get()).toString();
    }

    private String formatArgbHex(int n) {
        return String.format("%08X", n);
    }

    private String formatBlockIdentifier(Identifier identifier) {
        return "minecraft".equals(identifier.getNamespace()) ? identifier.getPath() : identifier.toString();
    }
}

