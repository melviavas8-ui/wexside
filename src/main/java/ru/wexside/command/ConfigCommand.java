package ru.wexside.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.wexside.command.Command;
import ru.wexside.command.CommandUsageException;
import ru.wexside.misc.ClientChat;
import ru.wexside.misc.ConfigManager;

public final class ConfigCommand
extends Command {
    private static final String string6 = ".wex";
    ConfigManager configManager;
    private static final int slot = 32;

    public ConfigCommand(ConfigManager configManager2) {
        super("cfg", "Управляет конфигами клиента", "cfg", "config");
        this.configManager = configManager2;
    }

    private void setString(String string) {
        if (!this.configManager.profileExists(string)) {
            String string2 = string;
            ClientChat.send("Конфиг не найден: " + string2);
            return;
        }
        this.configManager.loadProfile(string);
        String string3 = string;
        ClientChat.send("Конфиг загружен: " + string3);
        try {
            this.update();
        }
        catch (IOException iOException) {
            ClientChat.send("Не удалось обновить список конфигов: " + iOException.getMessage());
        }
    }

    private void update() throws IOException {
        List<String> list = this.configManager.listProfiles();
        if (list.isEmpty()) {
            ClientChat.send("Конфиги не найдены");
            return;
        }
        ClientChat.send("Доступные конфиги:");
        String string = ConfigCommand.stripProfileExtension(this.configManager.getCurrentProfileName());
        for (String string2 : list) {
            String string3 = ConfigCommand.stripProfileExtension(string2);
            boolean bl = string3.equals(string);
            ClientChat.send((Text)this.createProfileChatEntry(string3, bl));
        }
    }

    @Override
    public String getUsage() {
        return ".cfg <save/load/clear/list/dir> [name]";
    }

    @Override
    public void execute(String ... stringArray) throws CommandUsageException {
        if (stringArray.length == 0) {
            throw new CommandUsageException(this);
        }
        String string = stringArray[0].toLowerCase(Locale.ROOT);
        try {
            String string2;
            switch (string) {
                case "clear": {
                    this.resetCurrentProfile();
                    return;
                }
                case "list": {
                    this.update();
                    return;
                }
                case "dir": {
                    this.openConfigFolder();
                    return;
                }
            }
            String string3 = string2 = stringArray.length > 1 ? String.join((CharSequence)" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)).trim() : "";
            if (string2.isBlank()) {
                String string4 = this.configManager.getCurrentProfileName();
                string2 = string4 != null && !string4.isBlank() ? string4 : "default";
            }
            switch (string) {
                case "save": {
                    this.setString2(string2);
                    break;
                }
                case "load": {
                    this.setString(string2);
                    break;
                }
                default: {
                    throw new CommandUsageException(this);
                }
            }
        }
        catch (IOException iOException) {
            String string5 = iOException.getMessage();
            ClientChat.send("Ошибка конфига: " + string5);
        }
    }

    private void openConfigFolder() throws IOException {
        this.configManager.openConfigFolder();
        ClientChat.send("Папка конфигов открыта");
    }

    private MutableText createProfileChatEntry(String string, boolean bl) {
        String string2 = string;
        String string3 = string;
        Style actionStyle = Style.EMPTY.withClickEvent((ClickEvent)new ClickEvent.RunCommand(".cfg load " + string2)).withHoverEvent((HoverEvent)new HoverEvent.ShowText((Text)Text.literal((String)("Нажмите, чтобы загрузить " + string3))));
        MutableText configName = Text.literal((String)string).setStyle(actionStyle);
        MutableText status = bl ? Text.literal((String)" (Загружен)").formatted(Formatting.GREEN) : Text.literal((String)" (Загрузить)").formatted(Formatting.GRAY);
        return Text.literal((String)"- ").append((Text)configName).append((Text)status);
    }

    @Override
    public List<String> complete(int n, String[] stringArray) {
        if (n == 0) {
            return List.of("save", "load", "clear", "list", "dir");
        }
        if (n == 1 && stringArray.length >= 1 && "load".equalsIgnoreCase(stringArray[0])) {
            try {
                ArrayList<String> arrayList = new ArrayList<String>();
                for (String string : this.configManager.listProfiles()) {
                    arrayList.add(ConfigCommand.stripProfileExtension(string));
                }
                return arrayList;
            }
            catch (IOException iOException) {
                return List.of();
            }
        }
        return List.of();
    }

    private void resetCurrentProfile() throws IOException {
        this.configManager.resetProfile();
        ClientChat.send("Конфиг сброшен");
    }

    private void setString2(String string) throws IOException {
        if (string.length() > 32) {
            ClientChat.send("Название конфига не должно превышать 32 символов");
            return;
        }
        this.configManager.saveProfile(string);
        String string2 = string;
        ClientChat.send("Конфиг сохранён: " + string2);
    }

    private static String stripProfileExtension(String string) {
        if (string == null) {
            return null;
        }
        return string.endsWith(string6) ? string.substring(0, string.length() - string6.length()) : string;
    }
}

