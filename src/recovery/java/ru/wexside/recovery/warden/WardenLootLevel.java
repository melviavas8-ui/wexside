package ru.wexside.recovery.warden;

/** Original IiliIillliIllil, including its label-lookup fallback and truncating scale. */
public enum WardenLootLevel {
    LOW("Низкий", 1.5), MEDIUM("Средний", 1.0), HIGH("Высокий", 0.8);

    private final String label;
    private final double factor;

    WardenLootLevel(String label, double factor) {
        this.label = label;
        this.factor = factor;
    }

    public String label() {
        return label;
    }

    public double factor() {
        return factor;
    }

    public int scale(int threshold) {
        return (int) (threshold * factor);
    }

    public static WardenLootLevel fromLabel(String label) {
        for (WardenLootLevel level : values()) {
            if (level.label.equals(label)) {
                return level;
            }
        }
        return MEDIUM;
    }
}
