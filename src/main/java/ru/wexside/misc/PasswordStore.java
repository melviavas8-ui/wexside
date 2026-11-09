package ru.wexside.misc;

import java.io.IOException;
import java.util.Locale;
import ru.wexside.misc.PasswordConfigStore;

public class PasswordStore {
    private final PasswordConfigStore configStore;

    public PasswordStore(PasswordConfigStore configStore) {
        this.configStore = configStore;
    }

    public void savePassword(String serverAddress, String username, String password) {
        String normalizedUsername = PasswordStore.normalizeUsername(username);
        if (normalizedUsername.isEmpty() || password == null || password.isBlank()) {
            return;
        }
        this.configStore.getPasswords().put(PasswordStore.createAccountKey(serverAddress, username), password);
        this.persist();
    }

    public String getPassword(String serverAddress, String username) {
        if (PasswordStore.normalizeUsername(username).isEmpty()) {
            return null;
        }
        return this.configStore.getPasswords().get(PasswordStore.createAccountKey(serverAddress, username));
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private void persist() {
        try {
            this.configStore.save();
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public static String normalizeServerAddress(String serverAddress) {
        if (serverAddress == null) {
            return "";
        }
        String normalizedAddress = serverAddress.trim().toLowerCase(Locale.ROOT);
        int portSeparator = normalizedAddress.indexOf(58);
        if (portSeparator >= 0) {
            normalizedAddress = normalizedAddress.substring(0, portSeparator);
        }
        if (normalizedAddress.isEmpty() || normalizedAddress.matches("[0-9.]+")) {
            return normalizedAddress;
        }
        if (normalizedAddress.split("\\.").length >= 3) {
            normalizedAddress = normalizedAddress.substring(normalizedAddress.indexOf(46) + 1);
        }
        return normalizedAddress;
    }

    private static String createAccountKey(String serverAddress, String username) {
        String normalizedAddress = PasswordStore.normalizeServerAddress(serverAddress);
        String normalizedUsername = PasswordStore.normalizeUsername(username);
        return normalizedAddress.isEmpty() ? normalizedUsername : normalizedAddress + "|" + normalizedUsername;
    }
}

