package ru.wexside.misc;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import ru.wexside.util.GuiDrawApi;
import ru.wexside.util.MsdfFontRenderer;

public final class TextLayoutUtils {
    public static void drawWrappedText(Matrix4f matrix4f, GuiDrawApi drawApi, String string, MsdfFontRenderer font5, float f, float f2, float f3, float f4, float f5, int n) {
        List<String> list = TextLayoutUtils.wrapText(string, font5, f, f2);
        float f6 = f4;
        for (String string2 : list) {
            font5.drawText(matrix4f, drawApi, string2, f3, f6, f, n);
            String string3 = string2.isEmpty() ? " " : string2;
            f6 += font5.textHeight(string3, f) + f5;
        }
    }

    public static String trimToWidth(String string, MsdfFontRenderer font5, float f, float f2) {
        if (string == null || string.isEmpty() || f2 <= 0.0f) {
            return "";
        }
        if (font5.textWidth(string, f) <= f2) {
            return string;
        }
        String string2 = "...";
        float f3 = font5.textWidth(string2, f);
        for (int i = string.length() - 1; i > 0; --i) {
            String string3 = string.substring(0, i);
            if (!(font5.textWidth(string3, f) + f3 <= f2)) continue;
            String string4 = string2;
            String string5 = string3;
            return string5 + string4;
        }
        return string2;
    }

    private TextLayoutUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static List<String> wrapText(String string, MsdfFontRenderer font5, float f, float f2) {
        ArrayList<String> arrayList = new ArrayList<String>();
        if (string == null || f2 <= 0.0f) {
            arrayList.add("");
            return arrayList;
        }
        for (String string2 : string.split("\n", -1)) {
            TextLayoutUtils.wrapParagraph(string2, font5, f, f2, arrayList);
        }
        return arrayList;
    }

    public static String truncateWithEllipsis(String string, int n) {
        if (string == null) {
            return "";
        }
        if (n <= 0) {
            return "";
        }
        if (string.length() <= n) {
            return string;
        }
        String string2 = string.substring(0, n);
        return string2 + "...";
    }

    private static void appendWrappedWord(String string, MsdfFontRenderer font5, float f, float f2, StringBuilder stringBuilder, List<String> list) {
        String string2 = stringBuilder.length() == 0 ? "" : " ";
        String string3 = string;
        String string4 = string2;
        String string5 = String.valueOf(stringBuilder);
        String string6 = string5 + string4 + string3;
        if (font5.textWidth(string6, f) <= f2) {
            stringBuilder.setLength(0);
            stringBuilder.append(string6);
            return;
        }
        if (font5.textWidth(string, f) <= f2 / 2.0f) {
            list.add(stringBuilder.toString());
            stringBuilder.setLength(0);
            stringBuilder.append(string);
            return;
        }
        if (stringBuilder.length() > 0 && !string.isEmpty()) {
            char c = string.charAt(0);
            String string7 = String.valueOf(stringBuilder);
            String string8 = string7 + " " + c;
            if (font5.textWidth(string8, f) <= f2) {
                stringBuilder.append(' ');
            } else {
                list.add(stringBuilder.toString());
                stringBuilder.setLength(0);
            }
        }
        for (int i = 0; i < string.length(); ++i) {
            char c;
            char c2 = c = string.charAt(i);
            String string9 = stringBuilder.toString();
            String string10 = string9 + c2;
            if (font5.textWidth(string10, f) <= f2) {
                stringBuilder.append(c);
                continue;
            }
            if (stringBuilder.length() == 0) {
                stringBuilder.append(c);
                list.add(stringBuilder.toString());
                stringBuilder.setLength(0);
                continue;
            }
            list.add(stringBuilder.toString());
            stringBuilder.setLength(0);
            --i;
        }
    }

    private static void wrapParagraph(String string, MsdfFontRenderer font5, float f, float f2, List<String> list) {
        if (string.isEmpty()) {
            list.add("");
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String string2 : string.split(" ", -1)) {
            TextLayoutUtils.appendWrappedWord(string2, font5, f, f2, stringBuilder, list);
        }
        list.add(stringBuilder.toString());
    }
}

