package ru.wexside.util;

public final class RussianPluralForms {
    private final String string4;
    private final String string5;
    private final String string6;

    public RussianPluralForms(String string, String string2, String string3) {
        if (string == null || string.isBlank()) {
            throw new IllegalArgumentException("nominativeSingular must not be blank");
        }
        if (string2 == null || string2.isBlank()) {
            throw new IllegalArgumentException("genitiveSingular must not be blank");
        }
        if (string3 == null || string3.isBlank()) {
            throw new IllegalArgumentException("genitivePlural must not be blank");
        }
        this.string6 = string;
        this.string5 = string2;
        this.string4 = string3;
    }

    public String formByIndex(int n) {
        return switch (n) {
            case 0 -> this.string6;
            case 1 -> this.string5;
            default -> this.string4;
        };
    }

    private static int pluralFormIndex(double d) {
        double d2 = Math.abs(d);
        if (Math.abs(d2 - Math.rint(d2)) > 1.0E-9) {
            return 1;
        }
        long l = Math.abs(Math.round(d2));
        long l2 = l % 10L;
        long l3 = l % 100L;
        if (l2 == 1L && l3 != 11L) {
            return 0;
        }
        if (l2 >= 2L && l2 <= 4L && (l3 < 12L || l3 > 14L)) {
            return 1;
        }
        return 2;
    }

    public String formForNumber(double d) {
        return this.formByIndex(RussianPluralForms.pluralFormIndex(d));
    }
}

