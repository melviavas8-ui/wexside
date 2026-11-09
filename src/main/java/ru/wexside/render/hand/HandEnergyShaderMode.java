package ru.wexside.render.hand;

/** Shader IDs recovered from the original enum constructor arguments. */
public enum HandEnergyShaderMode {
    AURA(0), STATIC(1), RIBBONS(2);

    private final int shaderModeId;
    HandEnergyShaderMode(int shaderModeId) { this.shaderModeId = shaderModeId; }
    public int getShaderModeId() { return shaderModeId; }
}
