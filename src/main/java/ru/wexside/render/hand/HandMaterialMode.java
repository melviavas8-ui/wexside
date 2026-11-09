package ru.wexside.render.hand;

/** Exact option labels and shader associations recovered from the binary's selection enum. */
public enum HandMaterialMode {
    GLASS("Glass Hand", HandMaterialShaderMode.GLASS),
    FILL("Fill", HandMaterialShaderMode.FILL),
    BUBBLE("Bubble", HandMaterialShaderMode.BUBBLE),
    GALAXY("Galaxy", HandMaterialShaderMode.GALAXY);

    private final String settingName;
    private final HandMaterialShaderMode shaderMode;

    HandMaterialMode(String settingName, HandMaterialShaderMode shaderMode) {
        this.settingName = settingName;
        this.shaderMode = shaderMode;
    }

    public HandMaterialShaderMode shaderMode() { return shaderMode; }

    public static HandMaterialMode fromSetting(String value) {
        for (HandMaterialMode mode : values()) {
            if (mode.settingName.equalsIgnoreCase(value)) return mode;
        }
        return GLASS;
    }
}
