package ru.wexside.misc;

public interface TextInputModel {
    default public boolean accepts(char character, String currentText) {
        return true;
    }

    default public void onFocusGained() {
    }

    public int getMaximumLength();

    default public void onFocusLost() {
    }

    public String getText();

    default public String filterInput(String input, String existingText) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String prefix = existingText == null ? "" : existingText;
        StringBuilder acceptedText = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); ++index) {
            char character = input.charAt(index);
            if (Character.isISOControl(character)) continue;
            if (prefix.length() + acceptedText.length() >= this.getMaximumLength()) break;
            if (!this.accepts(character, prefix + String.valueOf(acceptedText))) continue;
            acceptedText.append(character);
        }
        return acceptedText.toString();
    }

    default public String getClipboardText() {
        return this.getText();
    }

    public void setText(String var1);
}

